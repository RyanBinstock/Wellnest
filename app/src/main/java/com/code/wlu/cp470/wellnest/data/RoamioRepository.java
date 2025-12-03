package com.code.wlu.cp470.wellnest.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.code.wlu.cp470.wellnest.data.auth.AuthRepository;
import com.code.wlu.cp470.wellnest.data.local.managers.RoamioManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseRoamioManager;

import java.util.Locale;
import java.time.LocalDate;

public class RoamioRepository {

    private static final String PREFS = AuthRepository.PREFS;
    private static final String USER_REPO_PREFS = "user_repo_prefs";
    private static final String PREFS_UID = "uid";
    private static final String TAG = "RoamioRepository";
    private static final String KEY_LAST_SYNC_ROAMIO_SCORE_DAY_PREFIX = "last_sync_roamio_score_epoch_day_";
    private final Context context;
    private final SharedPreferences prefs;
    private final FirebaseRoamioManager remote;
    private final RoamioManager local;

    public RoamioRepository(Context context, RoamioManager localManager, FirebaseRoamioManager remoteManager) {
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

    public void syncScore() {
        String uid = prefs.getString("uid", null);
        Log.d(TAG, "syncScore: uid from prefs = " + uid);

        if (uid == null) {
            uid = "test_uid"; // Fallback for test environment
            Log.w(TAG, "syncScore: uid was null, using test fallback: " + uid);
        }

        syncScoreInternal(uid);
    }

    /**
     * Once-daily Roamio micro-app score sync between local SQLite and Firestore.
     * Reads the uid from UserRepository's SharedPreferences.
     * Uses the same "higher score wins" strategy as {@link #syncScore()},
     * but gated so it runs at most once per day per user.
     */
    public void syncRoamioScoreOnceDaily() {
        // Read uid from UserRepository's SharedPreferences
        SharedPreferences userPrefs = context.getSharedPreferences(USER_REPO_PREFS, Context.MODE_PRIVATE);
        String uid = userPrefs.getString(PREFS_UID, null);
        
        if (uid == null || uid.isEmpty()) {
            Log.w(TAG, "syncRoamioScoreOnceDaily: uid is null/empty in UserRepository prefs, skipping");
            return;
        }

        long todayEpochDay = LocalDate.now().toEpochDay();
        String key = KEY_LAST_SYNC_ROAMIO_SCORE_DAY_PREFIX + uid;
        long lastEpochDay = prefs.getLong(key, Long.MIN_VALUE);

        if (lastEpochDay == todayEpochDay) {
            Log.d(TAG, "syncRoamioScoreOnceDaily: already synced today for uid=" + uid
                    + " (epochDay=" + todayEpochDay + ")");
            return;
        }

        try {
            syncScoreInternal(uid);
            prefs.edit().putLong(key, todayEpochDay).apply();
        } catch (Exception e) {
            Log.e(TAG, "syncRoamioScoreOnceDaily: sync failed for uid=" + uid, e);
        }
    }

    private void syncScoreInternal(String uid) {
        RoamioModels.RoamioScore localScore = local.getRoamioScore();
        RoamioModels.RoamioScore remoteScoreObj = remote.getScore(uid);
        
        // Check if remote document actually exists (null means document doesn't exist)
        boolean remoteExists = remoteScoreObj != null;
        int remoteScoreValue = remoteExists ? remoteScoreObj.getScore() : 0;

        Log.d(TAG, String.format(Locale.US, "syncScoreInternal: BEFORE - localScore=%d (uid=%s), remoteScore=%d, remoteExists=%b",
                localScore.getScore(), localScore.getUid(),
                remoteScoreValue, remoteExists));

        if (remoteExists && remoteScoreValue > localScore.getScore()) {
            Log.d(TAG, "syncScoreInternal: BRANCH 1 - Remote higher, updating local from " + localScore.getScore() + " to " + remoteScoreValue);
            Log.d(TAG, "syncScoreInternal: remoteScore uid BEFORE setting: " + remoteScoreObj.getUid());
            Log.d(TAG, "syncScoreInternal: Calling local.upsertRoamioScore(" + remoteScoreValue + ") with INT only");
            Log.d(TAG, "syncScoreInternal: localScore uid (database row): " + localScore.getUid());

            boolean success = local.upsertRoamioScore(remoteScoreValue);
            Log.d(TAG, "syncScoreInternal: Local update result = " + success);

            // Verify the update persisted
            RoamioModels.RoamioScore verifyLocal = local.getRoamioScore();
            Log.d(TAG, "syncScoreInternal: Verification: local score after update = " + verifyLocal.getScore() + " (uid=" + verifyLocal.getUid() + ")");
            Log.d(TAG, "syncScoreInternal: Expected: " + remoteScoreValue + ", Actual: " + verifyLocal.getScore() + ", Match: " + (verifyLocal.getScore() == remoteScoreValue));
        } else if (localScore.getScore() > remoteScoreValue || !remoteExists) {
            // Write to remote if: local score is higher OR remote document doesn't exist
            Log.d(TAG, "syncScoreInternal: BRANCH 2 - Local higher or remote doesn't exist, updating remote from " + remoteScoreValue + " to " + localScore.getScore() + " (remoteExists=" + remoteExists + ")");
            Log.d(TAG, "syncScoreInternal: localScore uid BEFORE setting: " + localScore.getUid());
            localScore.setUid(uid);
            Log.d(TAG, "syncScoreInternal: localScore uid AFTER setting: " + localScore.getUid());
            Log.d(TAG, "syncScoreInternal: Calling remote.upsertScore() with FULL RoamioScore object");

            boolean success = remote.upsertScore(localScore);
            Log.d(TAG, "syncScoreInternal: Remote update result = " + success);

            // Verify the update persisted
            RoamioModels.RoamioScore verifyRemote = remote.getScore(uid);
            Log.d(TAG, "syncScoreInternal: Verification: remote score after update = " + (verifyRemote != null ? verifyRemote.getScore() : "null") + " (uid=" + (verifyRemote != null ? verifyRemote.getUid() : "null") + ")");
            Log.d(TAG, "syncScoreInternal: Expected: " + localScore.getScore() + ", Actual: " + (verifyRemote != null ? verifyRemote.getScore() : "null") + ", Match: " + (verifyRemote != null && verifyRemote.getScore() == localScore.getScore()));
        } else {
            Log.d(TAG, "syncScoreInternal: Scores are equal, no sync needed");
        }
    }

    // ------------------------------------------------------------
    // Method delegation
    // ------------------------------------------------------------
    public RoamioModels.RoamioScore getRoamioScore() {
        return local.getRoamioScore();
    }

    public boolean upsertRoamioScore(int score) {
        return local.upsertRoamioScore(score);
    }

    public RoamioModels.RoamioScore getRoamioScoreRemote(String uid) {
        return remote.getScore(uid);
    }

    public boolean upsertRoamioScoreRemote(RoamioModels.RoamioScore score) {
        return remote.upsertScore(score);
    }

    public void addToRoamioScore(int delta) {
        local.addToRoamioScore(delta);
        
        // Immediately sync to Firebase after local update
        syncScoreToFirebaseAsync();
    }
    
    /**
     * Async helper to sync the current local score to Firebase.
     * Called whenever the local score is updated via addToRoamioScore().
     * Runs in a background thread to avoid blocking the caller.
     */
    private void syncScoreToFirebaseAsync() {
        new Thread(() -> {
            try {
                // Get UID from UserRepository's SharedPreferences
                SharedPreferences userPrefs = context.getSharedPreferences(USER_REPO_PREFS, Context.MODE_PRIVATE);
                String uid = userPrefs.getString(PREFS_UID, null);
                
                if (uid == null || uid.isEmpty()) {
                    Log.w(TAG, "syncScoreToFirebaseAsync: uid is null/empty, skipping Firebase sync");
                    return;
                }
                
                // Get the current local score and push to Firebase
                RoamioModels.RoamioScore localScore = local.getRoamioScore();
                localScore.setUid(uid);
                
                boolean success = remote.upsertScore(localScore);
                Log.d(TAG, "syncScoreToFirebaseAsync: synced score=" + localScore.getScore()
                        + " to Firebase for uid=" + uid + ", success=" + success);
            } catch (Exception e) {
                Log.e(TAG, "syncScoreToFirebaseAsync: failed to sync score to Firebase", e);
            }
        }).start();
    }

    // ------------------------------------------------------------
    // AI Generation
    // ------------------------------------------------------------

    /**
     * Generates a walking recommendation using AI and location services.
     * <p>
     * This method automatically obtains the user's current location and
     * generates a personalized walk recommendation. It delegates to
     * {@link WellnestAiClient#pickWalkAndStory(Context)} which handles:
     * - Location permission checking
     * - Current location acquisition via GPS
     * - Reverse geocoding to get location name
     * - Weather data fetching
     * - AI-powered walk recommendation generation
     * - Geocoding of start and end addresses
     * - Distance calculation between start and end points
     * <p>
     * <b>Required Permissions:</b>
     * The calling activity/fragment must have already obtained location permissions:
     * - {@link android.Manifest.permission#ACCESS_FINE_LOCATION} or
     * - {@link android.Manifest.permission#ACCESS_COARSE_LOCATION}
     * <p>
     * <b>Error Handling:</b>
     * Returns null if:
     * - Location permissions are not granted
     * - Location cannot be obtained
     * - Geocoding fails for current location or walk addresses
     * - Network requests fail
     * - Distance calculation fails
     *
     * @param callback Optional callback for progress updates
     * @return A Walk object with all attributes set including calculated distance, or null if generation fails
     */
    public RoamioModels.Walk generateWalk(WellnestAiClient.ProgressCallback callback) {
        try {
            // Call WellnestAiClient which now returns a fully constructed Walk object
            RoamioModels.Walk walk = WellnestAiClient.pickWalkAndStory(context, callback);
            
            if (walk == null) {
                Log.e(TAG, "generateWalk: pickWalkAndStory returned null (likely permission, location, or geocoding issue)");
                return null;
            }
            
            Log.d(TAG, String.format(Locale.US, "generateWalk: Successfully generated walk '%s' with distance %.2f meters",
                    walk.getName(), walk.getDistanceMeters()));
            
            return walk;
        } catch (Exception e) {
            Log.e(TAG, "generateWalk failed", e);
            return null;
        }
    }
}
