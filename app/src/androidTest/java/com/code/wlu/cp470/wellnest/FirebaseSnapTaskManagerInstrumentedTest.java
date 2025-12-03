package com.code.wlu.cp470.wellnest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.code.wlu.cp470.wellnest.data.SnapTaskModels;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseSnapTaskManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

@RunWith(JUnit4.class)
public class FirebaseSnapTaskManagerInstrumentedTest {
    private FirebaseSnapTaskManager manager;
 
    @Test
    public void getTasks() {
        manager = new FirebaseSnapTaskManager();
        List<SnapTaskModels.Task> tasks = manager.getTasks();
        assert (tasks.size() == 5);
    }

    @Test
    public void upsertAndGetScore_roundTrip() {
        manager = new FirebaseSnapTaskManager();
        String uid = "test_snap_user_" + System.currentTimeMillis();
        SnapTaskModels.SnapTaskScore score = new SnapTaskModels.SnapTaskScore(uid, 123);

        boolean result = manager.upsertScore(score);
        assertTrue("Upsert should succeed", result);

        SnapTaskModels.SnapTaskScore retrieved = manager.getScore(uid);
        assertNotNull("Retrieved score should not be null", retrieved);
        assertEquals("UID should match", uid, retrieved.getUid());
        assertEquals("Score should match", 123, retrieved.getScore());
    }

    @Test
    public void getScore_newUser_returnsZero() {
        manager = new FirebaseSnapTaskManager();
        String uid = "test_snap_new_user_" + System.currentTimeMillis();
        SnapTaskModels.SnapTaskScore retrieved = manager.getScore(uid);

        assertNotNull("Retrieved score should not be null", retrieved);
        assertEquals("UID should match", uid, retrieved.getUid());
        assertEquals("New user score should be 0", 0, retrieved.getScore());
    }
}
