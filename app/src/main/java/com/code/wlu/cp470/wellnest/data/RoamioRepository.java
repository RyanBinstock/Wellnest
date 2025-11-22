package com.code.wlu.cp470.wellnest.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.code.wlu.cp470.wellnest.data.auth.AuthRepository;
import com.code.wlu.cp470.wellnest.data.local.managers.RoamioManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseRoamioManager;

import java.util.List;

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
        if (uid == null) throw new IllegalStateException("No user logged in");
        int localScore = local.getRoamioScore();
        int remoteScore = remote.getScore(uid);
        if (localScore != remoteScore) {
            remote.upsertScore(uid, localScore);
        }
    }

    // ------------------------------------------------------------
    // Method delegation
    // ------------------------------------------------------------
    public int getRoamioScore() {
        return local.getRoamioScore();
    }

    public boolean upsertRoamioScore(int score) {
        return local.upsertRoamioScore(score);
    }

    public int addToRoamioScore(int delta) {
        return local.addToRoamioScore(delta);
    }

    public RoamioModels.WalkSession getWalkSession(String uid) {
        return local.getWalkSession(uid);
    }

    public boolean upsertWalkSession(String uid, int startedAt, int endedAt,
                                     int steps, float distanceMeters,
                                     int pointsAwarded) {
        return local.upsertWalkSession(uid, startedAt, endedAt, steps, distanceMeters, pointsAwarded);
    }

    public List<RoamioModels.WalkSession> getWalks() {
        return local.getWalks();
    }

    public boolean setWalkStatus(String uid, String status) {
        return local.setWalkStatus(uid, status);
    }

    public boolean cancelWalk(String uid) {
        return local.cancelWalk(uid);
    }

    public boolean completeWalk(String uid) {
        return local.completeWalk(uid);
    }
}
