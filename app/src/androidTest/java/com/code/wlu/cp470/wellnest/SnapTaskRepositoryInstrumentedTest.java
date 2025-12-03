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

import android.content.SharedPreferences;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class SnapTaskRepositoryInstrumentedTest {

    private static final String USER_REPO_PREFS = "user_repo_prefs";
    private static final String PREFS_UID = "uid";

    private Context context;
    private WellnestDatabaseHelper helper;
    private SQLiteDatabase db;
    private SnapTaskManager localManager;
    private FirebaseSnapTaskManager remoteManager;
    private SnapTaskRepository repo;

    /**
     * Fake remote manager that avoids hitting Firestore in tests and returns
     * a deterministic small set of tasks, plus score storage for sync testing.
     */
    private static class FakeRemoteManager extends FirebaseSnapTaskManager {
        private int fakeScore = 0;
        private String fakeUid = "test_uid";

        public void setFakeScore(int score) {
            this.fakeScore = score;
        }

        public int getFakeScore() {
            return fakeScore;
        }

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

        @Override
        public SnapTaskModels.SnapTaskScore getScore(String uid) {
            return new SnapTaskModels.SnapTaskScore(uid != null ? uid : fakeUid, fakeScore);
        }

        @Override
        public boolean upsertScore(SnapTaskModels.SnapTaskScore snapTaskScore) {
            if (snapTaskScore != null) {
                this.fakeScore = snapTaskScore.getScore();
                this.fakeUid = snapTaskScore.getUid();
                return true;
            }
            return false;
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
        // Clean up UserRepository prefs to avoid test pollution
        context.getSharedPreferences(USER_REPO_PREFS, Context.MODE_PRIVATE)
                .edit().clear().apply();
    }

    /**
     * Helper method to seed the uid in UserRepository's SharedPreferences
     */
    private void seedUserRepoUid(String uid) {
        context.getSharedPreferences(USER_REPO_PREFS, Context.MODE_PRIVATE)
                .edit().putString(PREFS_UID, uid).apply();
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

    // ============================================================
    // Once-Daily Score Sync Tests
    // ============================================================

    /**
     * Test that syncSnapTaskScoreOnceDaily syncs scores when due (past sync date).
     * Verifies that max(local, remote) reconciliation and date update occur.
     */
    @Test
    public void syncSnapTaskScoreOnceDaily_whenDue_syncsScoresAndRecordsDate() {
        String testUid = "sync_test_uid_1";
        FakeRemoteManager fakeRemote = (FakeRemoteManager) remoteManager;

        // Pre-seed UserRepository's SharedPreferences with test uid
        seedUserRepoUid(testUid);

        // Clear any existing sync date by setting it to a past day
        SharedPreferences prefs = context.getSharedPreferences("snapTask_repo_prefs", Context.MODE_PRIVATE);
        String key = "last_sync_snap_task_score_epoch_day_" + testUid;
        prefs.edit().putLong(key, LocalDate.now().toEpochDay() - 1).apply();

        // Set up: local=10, remote=20 => expected final=20
        repo.upsertSnapTaskScore(10);
        fakeRemote.setFakeScore(20);

        // Act
        repo.syncSnapTaskScoreOnceDaily();

        // Assert: local should be updated to 20, remote should still be 20
        assertEquals("Local score should be max(10,20)=20", 20, repo.getSnapTaskScore().intValue());
        assertEquals("Remote score should be 20", 20, fakeRemote.getFakeScore());

        // Assert: sync date should be updated to today
        long storedEpochDay = prefs.getLong(key, Long.MIN_VALUE);
        assertEquals("Sync date should be today", LocalDate.now().toEpochDay(), storedEpochDay);
    }

    /**
     * Test that syncSnapTaskScoreOnceDaily is skipped when already synced today.
     * Verifies that scores remain unchanged and no reconciliation happens.
     */
    @Test
    public void syncSnapTaskScoreOnceDaily_whenAlreadySyncedToday_skipsSync() {
        String testUid = "sync_test_uid_2";
        FakeRemoteManager fakeRemote = (FakeRemoteManager) remoteManager;

        // Pre-seed UserRepository's SharedPreferences with test uid
        seedUserRepoUid(testUid);

        // Set sync date to today
        SharedPreferences prefs = context.getSharedPreferences("snapTask_repo_prefs", Context.MODE_PRIVATE);
        String key = "last_sync_snap_task_score_epoch_day_" + testUid;
        prefs.edit().putLong(key, LocalDate.now().toEpochDay()).apply();

        // Set up: local=5, remote=50 => should NOT sync because already done today
        repo.upsertSnapTaskScore(5);
        fakeRemote.setFakeScore(50);

        // Act
        repo.syncSnapTaskScoreOnceDaily();

        // Assert: local score unchanged (no sync happened)
        assertEquals("Local score should remain 5 (no sync)", 5, repo.getSnapTaskScore().intValue());
        // Remote should also be unchanged
        assertEquals("Remote score should remain 50", 50, fakeRemote.getFakeScore());
    }

    /**
     * Test that syncSnapTaskScoreOnceDaily handles local > remote correctly.
     * When local is higher, remote should be updated.
     */
    @Test
    public void syncSnapTaskScoreOnceDaily_localHigher_updatesRemote() {
        String testUid = "sync_test_uid_3";
        FakeRemoteManager fakeRemote = (FakeRemoteManager) remoteManager;

        // Pre-seed UserRepository's SharedPreferences with test uid
        seedUserRepoUid(testUid);

        // Clear sync date
        SharedPreferences prefs = context.getSharedPreferences("snapTask_repo_prefs", Context.MODE_PRIVATE);
        String key = "last_sync_snap_task_score_epoch_day_" + testUid;
        prefs.edit().remove(key).apply();

        // Set up: local=100, remote=30 => expected final=100
        repo.upsertSnapTaskScore(100);
        fakeRemote.setFakeScore(30);

        // Act
        repo.syncSnapTaskScoreOnceDaily();

        // Assert: both should be 100
        assertEquals("Local score should be 100", 100, repo.getSnapTaskScore().intValue());
        assertEquals("Remote score should be updated to 100", 100, fakeRemote.getFakeScore());
    }

    /**
     * Test that syncSnapTaskScoreOnceDaily handles null/empty uid gracefully.
     */
    @Test
    public void syncSnapTaskScoreOnceDaily_nullUid_doesNotCrash() {
        // Clear the uid from UserRepository prefs to simulate null/empty uid
        context.getSharedPreferences(USER_REPO_PREFS, Context.MODE_PRIVATE)
                .edit().remove(PREFS_UID).apply();
        
        // Should not throw, just return early
        repo.syncSnapTaskScoreOnceDaily();
        
        // Test with empty uid
        context.getSharedPreferences(USER_REPO_PREFS, Context.MODE_PRIVATE)
                .edit().putString(PREFS_UID, "").apply();
        repo.syncSnapTaskScoreOnceDaily();
        // No assertions needed - just verifying no crash
    }
}