package com.code.wlu.cp470.wellnest.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.code.wlu.cp470.wellnest.data.SnapTaskModels.Task;
import com.code.wlu.cp470.wellnest.data.local.managers.SnapTaskManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseSnapTaskManager;

import java.util.List;

public class SnapTaskRepository {
    private static final String PREFS = "snapTask_repo_prefs";
    private final SharedPreferences prefs;
    private final Context context;
    private final FirebaseSnapTaskManager remote;
    private final SnapTaskManager local;

    public SnapTaskRepository(Context context, SnapTaskManager localManager, FirebaseSnapTaskManager remoteManager) {
        if (context == null) throw new IllegalArgumentException("context == null");
        if (localManager == null) throw new IllegalArgumentException("localManager == null");
        if (remoteManager == null) throw new IllegalArgumentException("remoteManager == null");
        this.context = context;
        this.local = localManager;
        this.remote = remoteManager;
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ------------------------------------------------------------
    // Sync helpers
    // ------------------------------------------------------------
    public void syncSnapTasks() {
        Log.d("SnapTaskRepository", "syncSnapTasks: starting sync from remote");
        List<SnapTaskModels.Task> tasks = remote.getTasks();
        Log.d("SnapTaskRepository", "syncSnapTasks: fetched " + tasks.size() + " tasks from remote");
        for (SnapTaskModels.Task task : tasks) {
            Log.d("SnapTaskRepository", "syncSnapTasks: upserting task uid=" + task.getUid() + ", name=" + task.getName());
            local.upsertTask(task.getUid(), task.getName(), task.getPoints(), task.getDescription(), task.getCompleted());
        }
    }

    // ------------------------------------------------------------
    // Method delegation
    // ------------------------------------------------------------

    public boolean upsertTask(String uid, String name, int points,
                              String description, Boolean completed) {
        local.upsertTask(uid, name, points, description, completed);
        return true;
    }

    public Task getSnapTask(String uid, String name) {
        return local.getSnapTask(uid, name);
    }

    public List<SnapTaskModels.Task> getTasks() {
        return local.getTasks();
    }

    public boolean setTaskCompleted(String uid) {
        return local.setTaskCompleted(uid);
    }

    public Integer getSnapTaskScore() {
        return local.getSnapTaskScore();
    }

    public boolean upsertSnapTaskScore(int score) {
        return local.upsertSnapTaskScore(score);
    }

    public int addToSnapTaskScore(int delta) {
        return local.addToSnapTaskScore(delta);
    }

}
