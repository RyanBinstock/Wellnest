package com.code.wlu.cp470.wellnest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.code.wlu.cp470.wellnest.data.SnapTaskModels.Task;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.contracts.SnapTaskContract;
import com.code.wlu.cp470.wellnest.data.local.managers.SnapTaskManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive tests for SnapTaskManager.
 * <p>
 * Covers:
 * - ensureSingletonRows() via observable effects
 * - upsertTask (insert & update)
 * - getSnapTask (by uid and by name fallback)
 * - setTaskCompleted
 * - getSnapTaskScore defaulting to 0
 * - upsertSnapTaskScore (update & insert path)
 * - addToSnapTaskScore (atomic add, negative, missing-row)
 * - argument validation/error paths
 */
@RunWith(AndroidJUnit4.class)
public class SnapTaskManagerInstrumentedTest {

    private Context ctx;
    private WellnestDatabaseHelper helper;
    private SQLiteDatabase db;
    private SnapTaskManager mgr;

    @Before
    public void setUp() {
        ctx = ApplicationProvider.getApplicationContext();
        helper = new WellnestDatabaseHelper(ctx);
        db = helper.getWritableDatabase();
        // Clean before each test to avoid cross-test leakage
        helper.cleanDatabase(db);
        mgr = new SnapTaskManager(db); // triggers ensureSingletonRows()
    }

    @After
    public void tearDown() {
        helper.cleanDatabase(db);
        if (db != null && db.isOpen()) db.close();
        if (helper != null) helper.close();
    }

    // ---------- Helpers ----------

    private int rawScore() {
        try (Cursor c = db.query(
                SnapTaskContract.SnapTask_Score.TABLE,
                new String[]{SnapTaskContract.SnapTask_Score.Col.SCORE},
                SnapTaskContract.SnapTask_Score.Col.UID + "=?",
                new String[]{"1"}, null, null, null)) {
            if (!c.moveToFirst() || c.isNull(0)) return 0;
            return c.getInt(0);
        }
    }

    private Task requireTask(String uid) {
        Task t = mgr.getSnapTask(uid, null);
        assertNotNull("Expected task to exist: " + uid, t);
        return t;
    }

    // ---------- Score bootstrap ----------

    @Test
    public void score_bootstrapsToZero_onManagerInit() {
        // ensureSingletonRows() runs in constructor
        assertEquals(0, mgr.getSnapTaskScore().intValue());
        assertEquals(0, rawScore());
    }

    // ---------- upsertTask & retrieval ----------

    @Test
    public void upsertTask_insert_then_getByUid() {
        String uid = "task_1";
        assertTrue(mgr.upsertTask(uid, "Wash Dishes", 5, "Evening sink", false));

        Task t = requireTask(uid);
        assertEquals(uid, t.getUid());
        assertEquals("Wash Dishes", t.getName());
        assertEquals(5, t.getPoints());
        assertEquals("Evening sink", t.getDescription());
        assertFalse(t.getCompleted());
    }

    @Test
    public void getTask_byNameFallback_whenUidEmpty() {
        String uid = "task_2";
        mgr.upsertTask(uid, "Laundry", 3, "Dark cycle", null);

        Task t = mgr.getSnapTask("", "Laundry");
        assertNotNull(t);
        assertEquals(uid, t.getUid());
        assertEquals(3, t.getPoints());
        assertEquals("Dark cycle", t.getDescription());
        assertFalse("Null completed should default to false", t.getCompleted());
    }

    @Test
    public void upsertTask_update_existingRow() {
        String uid = "task_3";
        mgr.upsertTask(uid, "Vacuum", 4, "Living room", false);

        // Update fields
        assertTrue(mgr.upsertTask(uid, "Vacuum Upstairs", 7, "Hall + rooms", true));

        Task t = requireTask(uid);
        assertEquals("Vacuum Upstairs", t.getName());
        assertEquals(7, t.getPoints());
        assertEquals("Hall + rooms", t.getDescription());
        assertTrue(t.getCompleted());
    }

    @Test
    public void getTask_missing_returnsNull() {
        assertNull(mgr.getSnapTask("nope", null));
        assertNull(mgr.getSnapTask(null, null));
        assertNull(mgr.getSnapTask("", ""));
    }

    @Test
    public void getTasks_returnsAll() {
        List<Task> tasks = new ArrayList<>();
        tasks.add(new Task("task_1", "Wash Dishes", 5, "Evening sink", false));
        tasks.add(new Task("task_2", "Laundry", 3, "Dark cycle", false));
        tasks.add(new Task("task_3", "Vacuum", 4, "Living room", false));
        tasks.add(new Task("task_4", "Trash", 1, "Curb by 7am", false));

        for (Task t : tasks) {
            mgr.upsertTask(t.getUid(), t.getName(), t.getPoints(), t.getDescription(), t.getCompleted());
        }

        assertEquals(4, mgr.getSnapTasks().size());
        List<Task> retrievedTasks = mgr.getSnapTasks();
        for (int i = 0; i < tasks.size(); i++) {
            assertEquals(tasks.get(i).getUid(), retrievedTasks.get(i).getUid());
            assertEquals(tasks.get(i).getName(), retrievedTasks.get(i).getName());
            assertEquals(tasks.get(i).getPoints(), retrievedTasks.get(i).getPoints());
            assertEquals(tasks.get(i).getDescription(), retrievedTasks.get(i).getDescription());
            assertEquals(tasks.get(i).getCompleted(), retrievedTasks.get(i).getCompleted());
        }
    }

    // ---------- completion flag ----------

    @Test
    public void setTaskCompleted_updatesFlag() {
        String uid = "task_4";
        mgr.upsertTask(uid, "Trash", 1, "Curb by 7am", false);

        assertTrue(mgr.setTaskCompleted(uid));
        assertTrue(requireTask(uid).getCompleted());
    }

    @Test
    public void setTaskCompleted_nonexistent_returnsFalse() {
        assertFalse(mgr.setTaskCompleted("missing"));
    }

    // ---------- score upsert & add ----------

    @Test
    public void upsertSnapTaskScore_updatesExistingRow() {
        // bootstrap is 0
        assertTrue(mgr.upsertSnapTaskScore(40));
        assertEquals(40, mgr.getSnapTaskScore().intValue());
        assertEquals(40, rawScore());
    }

    @Test
    public void upsertSnapTaskScore_insertsWhenMissing() {
        // simulate missing singleton row
        db.delete(SnapTaskContract.SnapTask_Score.TABLE, null, null);

        assertTrue("Insert path should succeed", mgr.upsertSnapTaskScore(25));
        assertEquals(25, mgr.getSnapTaskScore().intValue());
    }

    @Test
    public void addToSnapTaskScore_accumulatesAndReturnsNewValue() {
        assertEquals(0, mgr.getSnapTaskScore().intValue());
        int v1 = mgr.addToSnapTaskScore(10);
        assertEquals(10, v1);
        assertEquals(10, mgr.getSnapTaskScore().intValue());

        int v2 = mgr.addToSnapTaskScore(5);
        assertEquals(15, v2);
        assertEquals(15, mgr.getSnapTaskScore().intValue());

        int v3 = mgr.addToSnapTaskScore(-3);
        assertEquals(12, v3);
        assertEquals(12, mgr.getSnapTaskScore().intValue());
    }

    @Test
    public void addToSnapTaskScore_createsRowWhenMissing_usesDeltaAsStart() {
        // remove singleton to force fallback path
        db.delete(SnapTaskContract.SnapTask_Score.TABLE, null, null);

        int v = mgr.addToSnapTaskScore(7);
        assertEquals(7, v);
        assertEquals(7, mgr.getSnapTaskScore().intValue());
    }

    @Test
    public void getSnapTaskScore_whenRowMissing_defaultsToZero() {
        db.delete(SnapTaskContract.SnapTask_Score.TABLE, null, null);
        assertEquals(0, mgr.getSnapTaskScore().intValue());
    }

    // ---------- argument validation/error paths ----------

    @Test(expected = IllegalArgumentException.class)
    public void upsertTask_throws_onNullUid() {
        mgr.upsertTask(null, "X", 1, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void upsertTask_throws_onEmptyUid() {
        mgr.upsertTask("", "X", 1, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTaskCompleted_throws_onNullUid() {
        mgr.setTaskCompleted(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTaskCompleted_throws_onEmptyUid() {
        mgr.setTaskCompleted("");
    }
}
