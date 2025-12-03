package com.code.wlu.cp470.wellnest.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.code.wlu.cp470.wellnest.data.SnapTaskModels.Task;
import com.code.wlu.cp470.wellnest.data.local.managers.SnapTaskManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseSnapTaskManager;

import java.time.LocalDate;
import java.util.List;

public class SnapTaskRepository {
    private static final String PREFS = "snapTask_repo_prefs";
    private static final String USER_REPO_PREFS = "user_repo_prefs";
    private static final String PREFS_UID = "uid";
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
        int newScore = local.addToSnapTaskScore(delta);

        // Immediately sync to Firebase after local update
        syncScoreToFirebaseAsync(newScore);

        return newScore;
    }

    /**
     * Async helper to sync the given score to Firebase.
     * Called whenever the local score is updated via addToSnapTaskScore().
     * Runs in a background thread to avoid blocking the caller.
     */
    private void syncScoreToFirebaseAsync(int score) {
        new Thread(() -> {
            try {
                // Get UID from UserRepository's SharedPreferences
                SharedPreferences userPrefs = context.getSharedPreferences(USER_REPO_PREFS, Context.MODE_PRIVATE);
                String uid = userPrefs.getString(PREFS_UID, null);

                if (uid == null || uid.isEmpty()) {
                    Log.w("SnapTaskRepository", "syncScoreToFirebaseAsync: uid is null/empty, skipping Firebase sync");
                    return;
                }

                // Push score to Firebase
                SnapTaskModels.SnapTaskScore scoreObj = new SnapTaskModels.SnapTaskScore(uid, score);
                boolean success = remote.upsertScore(scoreObj);
                Log.d("SnapTaskRepository", "syncScoreToFirebaseAsync: synced score=" + score
                        + " to Firebase for uid=" + uid + ", success=" + success);
            } catch (Exception e) {
                Log.e("SnapTaskRepository", "syncScoreToFirebaseAsync: failed to sync score to Firebase", e);
            }
        }).start();
    }

    // ------------------------------------------------------------
    // Remote micro-app score helpers (Firestore)
    // ------------------------------------------------------------

    public SnapTaskModels.SnapTaskScore getSnapTaskScoreRemote(String uid) {
        return remote.getScore(uid);
    }

    public boolean upsertSnapTaskScoreRemote(SnapTaskModels.SnapTaskScore snapTaskScore) {
        return remote.upsertScore(snapTaskScore);
    }

    // ------------------------------------------------------------
    // Once-daily micro-app score sync (local SQLite <-> Firestore)
    // ------------------------------------------------------------
    public void syncSnapTaskScoreOnceDaily() {
        Log.d("SnapTaskRepository", "syncSnapTaskScoreOnceDaily: === METHOD ENTERED ===");
        // Read uid from UserRepository's SharedPreferences
        SharedPreferences userPrefs = context.getSharedPreferences(USER_REPO_PREFS, Context.MODE_PRIVATE);
        String uid = userPrefs.getString(PREFS_UID, null);

        if (uid == null || uid.isEmpty()) {
            Log.w("SnapTaskRepository", "syncSnapTaskScoreOnceDaily: uid is null/empty in UserRepository prefs, skipping");
            return;
        }

        long todayEpochDay = LocalDate.now().toEpochDay();
        String key = "last_sync_snap_task_score_epoch_day_" + uid;
        long lastEpochDay = prefs.getLong(key, Long.MIN_VALUE);

        if (lastEpochDay == todayEpochDay) {
            Log.d("SnapTaskRepository", "syncSnapTaskScoreOnceDaily: already synced today for uid=" + uid
                    + " (epochDay=" + todayEpochDay + ")");
            return;
        }

        try {
            int localScore = 0;
            Integer localVal = getSnapTaskScore();
            if (localVal != null) {
                localScore = localVal;
            }

            SnapTaskModels.SnapTaskScore remoteScoreObj = getSnapTaskScoreRemote(uid);
            boolean remoteExists = remoteScoreObj != null;
            int remoteScore = remoteExists ? remoteScoreObj.getScore() : 0;

            Log.d("SnapTaskRepository", "syncSnapTaskScoreOnceDaily: localScore=" + localScore
                    + ", remoteScore=" + remoteScore + ", remoteExists=" + remoteExists);

            int finalScore = Math.max(localScore, remoteScore);

            if (finalScore != localScore) {
                boolean okLocal = upsertSnapTaskScore(finalScore);
                Log.d("SnapTaskRepository", "syncSnapTaskScoreOnceDaily: updated local score from "
                        + localScore + " to " + finalScore + ", ok=" + okLocal);
            }

            // Write to remote if: score changed OR remote document doesn't exist
            if (finalScore != remoteScore || !remoteExists) {
                SnapTaskModels.SnapTaskScore toPush =
                        new SnapTaskModels.SnapTaskScore(uid, finalScore);
                boolean okRemote = upsertSnapTaskScoreRemote(toPush);
                Log.d("SnapTaskRepository", "syncSnapTaskScoreOnceDaily: updated remote score from "
                        + remoteScore + " to " + finalScore + " (remoteExists=" + remoteExists + "), ok=" + okRemote);
            }

            prefs.edit().putLong(key, todayEpochDay).apply();
        } catch (Exception e) {
            Log.e("SnapTaskRepository", "syncSnapTaskScoreOnceDaily: failed for uid=" + uid, e);
        }
    }

}
