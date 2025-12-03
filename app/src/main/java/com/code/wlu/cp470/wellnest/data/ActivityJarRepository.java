package com.code.wlu.cp470.wellnest.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.code.wlu.cp470.wellnest.data.local.managers.ActivityJarManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseUserManager;

import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for ActivityJar microapp data.
 * Handles local SQLite operations and remote Firebase sync.
 */
public class ActivityJarRepository {

    private static final String TAG = "ActivityJarRepository";
    private static final String PREFS = "activityJar_repo_prefs";
    private static final String USER_REPO_PREFS = "user_repo_prefs";
    private static final String PREFS_UID = "uid";
    private static final String KEY_LAST_SYNC_ACTIVITY_JAR_SCORE_DAY_PREFIX = "last_sync_activity_jar_score_epoch_day_";

    private final ActivityJarManager activityJarManager;
    private final ExecutorService executor;
    private final Context context;
    private final SharedPreferences prefs;
    private final FirebaseUserManager remote;

    /**
     * Constructor for full dependency injection (recommended for production).
     */
    public ActivityJarRepository(Context context, ActivityJarManager localManager, FirebaseUserManager remoteManager) {
        if (context == null) throw new IllegalArgumentException("context == null");
        if (localManager == null) throw new IllegalArgumentException("localManager == null");
        if (remoteManager == null) throw new IllegalArgumentException("remoteManager == null");
        this.context = context.getApplicationContext();
        this.activityJarManager = localManager;
        this.remote = remoteManager;
        this.executor = Executors.newSingleThreadExecutor();
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /**
     * Legacy constructor for backwards compatibility (creates its own local manager).
     */
    public ActivityJarRepository(SQLiteDatabase db) {
        this.activityJarManager = new ActivityJarManager(db);
        this.executor = Executors.newSingleThreadExecutor();
        this.context = null;
        this.prefs = null;
        this.remote = null;
    }

    public void addScore(int points, ScoreCallback callback) {
        executor.execute(() -> {
            try {
                int newScore = activityJarManager.addToActivityJarScore(points);
                callback.onScoreUpdated(newScore);

                // Immediately sync to Firebase after local update
                syncScoreToFirebaseAsync(newScore);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    // ------------------------------------------------------------
    // Local SQLite operations
    // ------------------------------------------------------------

    /**
     * Async helper to sync the given score to Firebase.
     * Called whenever the local score is updated via addScore().
     * Already runs in executor thread, so just performs the sync directly.
     */
    private void syncScoreToFirebaseAsync(int score) {
        if (context == null || remote == null) {
            Log.w(TAG, "syncScoreToFirebaseAsync: context or remote is null (legacy constructor used), skipping");
            return;
        }

        try {
            // Get UID from UserRepository's SharedPreferences
            SharedPreferences userPrefs = context.getSharedPreferences(USER_REPO_PREFS, Context.MODE_PRIVATE);
            String uid = userPrefs.getString(PREFS_UID, null);

            if (uid == null || uid.isEmpty()) {
                Log.w(TAG, "syncScoreToFirebaseAsync: uid is null/empty, skipping Firebase sync");
                return;
            }

            // Push score to Firebase
            ActivityJarModels.ActivityJarScore scoreObj = new ActivityJarModels.ActivityJarScore(uid, score);
            boolean success = remote.upsertActivityJarScore(scoreObj);
            Log.d(TAG, "syncScoreToFirebaseAsync: synced score=" + score
                    + " to Firebase for uid=" + uid + ", success=" + success);
        } catch (Exception e) {
            Log.e(TAG, "syncScoreToFirebaseAsync: failed to sync score to Firebase", e);
        }
    }

    public void getScore(ScoreCallback callback) {
        executor.execute(() -> {
            try {
                int score = activityJarManager.getActivityJarScore();
                callback.onScoreUpdated(score);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public Integer getActivityJarScore() {
        return activityJarManager.getActivityJarScore();
    }

    public boolean upsertActivityJarScore(int score) {
        return activityJarManager.upsertRoamioScore(score);
    }

    /**
     * Fetches the ActivityJar score from Firebase for the given uid.
     * Returns a score object with 0 if fetch fails or user doesn't exist.
     */
    public ActivityJarModels.ActivityJarScore getActivityJarScoreRemote(String uid) {
        if (remote == null) {
            Log.w(TAG, "getActivityJarScoreRemote: remote manager is null (legacy constructor used)");
            return new ActivityJarModels.ActivityJarScore(uid, 0);
        }
        try {
            return remote.getActivityJarScore(uid);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get ActivityJar score for uid=" + uid, e);
            return new ActivityJarModels.ActivityJarScore(uid, 0);
        }
    }

    // ------------------------------------------------------------
    // Remote Firebase operations (via FirebaseUserManager)
    // ------------------------------------------------------------

    /**
     * Upserts (creates or updates) the ActivityJar score in Firebase.
     */
    public boolean upsertActivityJarScoreRemote(ActivityJarModels.ActivityJarScore activityJarScore) {
        if (remote == null) {
            Log.w(TAG, "upsertActivityJarScoreRemote: remote manager is null (legacy constructor used)");
            return false;
        }
        return remote.upsertActivityJarScore(activityJarScore);
    }

    /**
     * Once-daily sync of ActivityJar micro-app score between local SQLite and Firestore.
     * Reads the uid from UserRepository's SharedPreferences.
     * Uses a "never decrease" strategy by taking the max of local and
     * remote scores, then updating whichever side is behind. Gated so it runs at most
     * once per calendar day per uid.
     * <p>
     * Call from a background thread.
     */
    public void syncActivityJarScoreOnceDaily() {
        if (prefs == null || remote == null || context == null) {
            Log.w(TAG, "syncActivityJarScoreOnceDaily: prefs, remote, or context is null (legacy constructor used), skipping");
            return;
        }

        // Read uid from UserRepository's SharedPreferences
        SharedPreferences userPrefs = context.getSharedPreferences(USER_REPO_PREFS, Context.MODE_PRIVATE);
        String uid = userPrefs.getString(PREFS_UID, null);

        if (uid == null || uid.isEmpty()) {
            Log.w(TAG, "syncActivityJarScoreOnceDaily: uid is null/empty in UserRepository prefs, skipping");
            return;
        }

        long todayEpochDay = LocalDate.now().toEpochDay();
        String key = KEY_LAST_SYNC_ACTIVITY_JAR_SCORE_DAY_PREFIX + uid;
        long lastEpochDay = prefs.getLong(key, Long.MIN_VALUE);

        if (lastEpochDay == todayEpochDay) {
            Log.d(TAG, "syncActivityJarScoreOnceDaily: already synced today for uid=" + uid
                    + " (epochDay=" + todayEpochDay + ")");
            return;
        }

        try {
            int localScore = 0;
            Integer localVal = activityJarManager.getActivityJarScore();
            if (localVal != null) {
                localScore = localVal;
            }

            ActivityJarModels.ActivityJarScore remoteScoreObj = getActivityJarScoreRemote(uid);
            boolean remoteExists = remoteScoreObj != null;
            int remoteScore = remoteExists ? remoteScoreObj.getScore() : 0;

            Log.d(TAG, "syncActivityJarScoreOnceDaily: localScore=" + localScore
                    + ", remoteScore=" + remoteScore + ", remoteExists=" + remoteExists);

            int finalScore = Math.max(localScore, remoteScore);

            if (finalScore != localScore) {
                boolean okLocal = activityJarManager.upsertRoamioScore(finalScore);
                Log.d(TAG, "syncActivityJarScoreOnceDaily: updated local score from "
                        + localScore + " to " + finalScore + ", ok=" + okLocal);
            }

            // Write to remote if: score changed OR remote document doesn't exist
            if (finalScore != remoteScore || !remoteExists) {
                ActivityJarModels.ActivityJarScore toPush =
                        new ActivityJarModels.ActivityJarScore(uid, finalScore);
                boolean okRemote = upsertActivityJarScoreRemote(toPush);
                Log.d(TAG, "syncActivityJarScoreOnceDaily: updated remote score from "
                        + remoteScore + " to " + finalScore + " (remoteExists=" + remoteExists + "), ok=" + okRemote);
            }

            prefs.edit().putLong(key, todayEpochDay).apply();
        } catch (Exception e) {
            Log.e(TAG, "syncActivityJarScoreOnceDaily: failed for uid=" + uid, e);
        }
    }

    // ------------------------------------------------------------
    // Once-daily micro-app score sync (local SQLite <-> Firestore)
    // ------------------------------------------------------------

    public void shutdown() {
        executor.shutdown();
    }

    public interface ScoreCallback {
        void onScoreUpdated(int newScore);

        void onError(Exception e);
    }
}