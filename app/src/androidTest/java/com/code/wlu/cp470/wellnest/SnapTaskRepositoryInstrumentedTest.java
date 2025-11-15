package com.code.wlu.cp470.wellnest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.code.wlu.cp470.wellnest.data.SnapTaskModels;
import com.code.wlu.cp470.wellnest.data.SnapTaskRepository;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.SnapTaskManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseSnapTaskManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class SnapTaskRepositoryInstrumentedTest {

    private Context context;
    private WellnestDatabaseHelper helper;
    private SQLiteDatabase db;
    private SnapTaskManager localManager;
    private FirebaseSnapTaskManager remoteManager;
    private SnapTaskRepository repo;

    /**
     * Simple fake remote manager that avoids hitting Firestore in tests and returns
     * a deterministic small set of tasks.
     */
    private static class FakeRemoteManager extends FirebaseSnapTaskManager {
        @Override
        public List<SnapTaskModels.Task> getTasks() {
            List<SnapTaskModels.Task> tasks = new ArrayList<>();
            tasks.add(new SnapTaskModels.Task(
                    "remote_1",
                    "Remote Task",
                    5,
                    "Remote description",
                    false
            ));
            return tasks;
        }
    }

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        helper = new WellnestDatabaseHelper(context);
        db = helper.getWritableDatabase();
        helper.cleanDatabase(db);

        localManager = new SnapTaskManager(db);
        remoteManager = new FakeRemoteManager();
        repo = new SnapTaskRepository(context, localManager, remoteManager);
    }

    @After
    public void tearDown() {
        helper.cleanDatabase(db);
        if (db != null && db.isOpen()) {
            db.close();
        }
        if (helper != null) {
            helper.close();
        }
    }

    /**
     * Verify that marking a task completed through the repository updates the underlying
     * DB row and that both the repository and manager views of the task agree.
     */
    @Test
    public void setTaskCompleted_updatesRow_andVisibleViaManagerAndRepo() {
        String uid = "task_repo_1";

        repo.upsertTask(uid, "Wash Dishes", 5, "Evening sink", false);

        SnapTaskModels.Task before = repo.getSnapTask(uid, "Wash Dishes");
        assertNotNull(before);
        assertFalse(before.getCompleted());

        assertTrue(repo.setTaskCompleted(uid));

        SnapTaskModels.Task viaRepo = repo.getSnapTask(uid, "Wash Dishes");
        assertNotNull(viaRepo);
        assertTrue(viaRepo.getCompleted());

        SnapTaskModels.Task viaManager = localManager.getSnapTask(uid, null);
        assertNotNull(viaManager);
        assertTrue(viaManager.getCompleted());
    }

    /**
     * Verify that the list returned by the repository has completion state consistent
     * with the underlying manager/DB for both completed and incomplete tasks.
     */
    @Test
    public void getTasks_returnsCompletedAndIncompleteStatesConsistentWithManager() {
        String uidIncomplete = "task_repo_2";
        String uidComplete = "task_repo_3";

        repo.upsertTask(uidIncomplete, "Laundry", 3, "Dark cycle", false);
        repo.upsertTask(uidComplete, "Vacuum", 4, "Living room", true);

        List<SnapTaskModels.Task> repoTasks = repo.getTasks();
        assertEquals(2, repoTasks.size());

        SnapTaskModels.Task incomplete = null;
        SnapTaskModels.Task complete = null;
        for (SnapTaskModels.Task t : repoTasks) {
            if (uidIncomplete.equals(t.getUid())) {
                incomplete = t;
            } else if (uidComplete.equals(t.getUid())) {
                complete = t;
            }
        }

        assertNotNull(incomplete);
        assertNotNull(complete);
        assertFalse(incomplete.getCompleted());
        assertTrue(complete.getCompleted());

        SnapTaskModels.Task mIncomplete = localManager.getSnapTask(uidIncomplete, null);
        SnapTaskModels.Task mComplete = localManager.getSnapTask(uidComplete, null);
        assertNotNull(mIncomplete);
        assertNotNull(mComplete);
        assertEquals(incomplete.getCompleted(), mIncomplete.getCompleted());
        assertEquals(complete.getCompleted(), mComplete.getCompleted());
    }

    /**
     * Verify that addToSnapTaskScore increments the score via the repository and that
     * subsequent reads reflect the increment.
     */
    @Test
    public void addToSnapTaskScore_incrementsAndPersistsScore() {
        assertEquals(0, repo.getSnapTaskScore().intValue());

        int s1 = repo.addToSnapTaskScore(10);
        assertEquals(10, s1);
        assertEquals(10, repo.getSnapTaskScore().intValue());

        int s2 = repo.addToSnapTaskScore(5);
        assertEquals(15, s2);
        assertEquals(15, repo.getSnapTaskScore().intValue());
    }

    /**
     * Verify that adding 0 points via the repository is effectively a no-op on the score.
     */
    @Test
    public void addToSnapTaskScore_zeroDelta_isNoOpOnScore() {
        repo.upsertSnapTaskScore(25);
        assertEquals(25, repo.getSnapTaskScore().intValue());

        int s = repo.addToSnapTaskScore(0);
        assertEquals(25, s);
        assertEquals(25, repo.getSnapTaskScore().intValue());
    }
}