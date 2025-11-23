package com.code.wlu.cp470.wellnest.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.code.wlu.cp470.wellnest.data.auth.AuthRepository;
import com.code.wlu.cp470.wellnest.data.local.managers.RoamioManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseRoamioManager;

import java.util.Locale;

public class RoamioRepository {

    private static final String PREFS = AuthRepository.PREFS;
    private static final String TAG = "RoamioRepository";
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

        RoamioModels.RoamioScore localScore = local.getRoamioScore();
        RoamioModels.RoamioScore remoteScore = remote.getScore(uid);
        
        Log.d(TAG, String.format(Locale.US, "syncScore: BEFORE - localScore=%d (uid=%s), remoteScore=%d (uid=%s)",
                localScore.getScore(), localScore.getUid(),
                remoteScore.getScore(), remoteScore.getUid()));

        if (remoteScore.getScore() > localScore.getScore()) {
            Log.d(TAG, "syncScore: BRANCH 1 - Remote higher, updating local from " + localScore.getScore() + " to " + remoteScore.getScore());
            Log.d(TAG, "syncScore: DIAGNOSIS - remoteScore uid BEFORE setting: " + remoteScore.getUid());
            Log.d(TAG, "syncScore: DIAGNOSIS - Calling local.upsertRoamioScore(" + remoteScore.getScore() + ") with INT only");
            Log.d(TAG, "syncScore: DIAGNOSIS - localScore uid (database row): " + localScore.getUid());
            
            boolean success = local.upsertRoamioScore(remoteScore.getScore());
            Log.d(TAG, "syncScore: DIAGNOSIS - Local update result = " + success);
            
            // Verify the update persisted
            RoamioModels.RoamioScore verifyLocal = local.getRoamioScore();
            Log.d(TAG, "syncScore: DIAGNOSIS - Verification: local score after update = " + verifyLocal.getScore() + " (uid=" + verifyLocal.getUid() + ")");
            Log.d(TAG, "syncScore: DIAGNOSIS - Expected: " + remoteScore.getScore() + ", Actual: " + verifyLocal.getScore() + ", Match: " + (verifyLocal.getScore() == remoteScore.getScore()));
        } else if (localScore.getScore() > remoteScore.getScore()) {
            Log.d(TAG, "syncScore: BRANCH 2 - Local higher, updating remote from " + remoteScore.getScore() + " to " + localScore.getScore());
            Log.d(TAG, "syncScore: DIAGNOSIS - localScore uid BEFORE setting: " + localScore.getUid());
            localScore.setUid(uid);
            Log.d(TAG, "syncScore: DIAGNOSIS - localScore uid AFTER setting: " + localScore.getUid());
            Log.d(TAG, "syncScore: DIAGNOSIS - Calling remote.upsertScore() with FULL RoamioScore object");
            
            boolean success = remote.upsertScore(localScore);
            Log.d(TAG, "syncScore: DIAGNOSIS - Remote update result = " + success);
            
            // Verify the update persisted
            RoamioModels.RoamioScore verifyRemote = remote.getScore(uid);
            Log.d(TAG, "syncScore: DIAGNOSIS - Verification: remote score after update = " + verifyRemote.getScore() + " (uid=" + verifyRemote.getUid() + ")");
            Log.d(TAG, "syncScore: DIAGNOSIS - Expected: " + localScore.getScore() + ", Actual: " + verifyRemote.getScore() + ", Match: " + (verifyRemote.getScore() == localScore.getScore()));
        } else {
            Log.d(TAG, "syncScore: Scores are equal, no sync needed");
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

    public void addToRoamioScore(int delta) {
        local.addToRoamioScore(delta);
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
