package com.code.wlu.cp470.wellnest;

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
}
