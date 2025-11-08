package com.code.wlu.cp470.wellnest.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Frontend-facing repository:
 * - All data APIs delegate to the LOCAL manager (SQLite).
 * - Sync helpers use the REMOTE manager (Firestore) for push/pull.
 * <p>
 * Threading: Firestore calls are synchronous in your FirebaseUserManager (Tasks.await);
 * call sync methods off the main thread.
 */
public final class UserRepository implements UserInterface {

    private static final String PREFS = "user_repo_prefs";
    private static final String KEY_LAST_GLOBAL_PUSH_DAY = "last_global_push_epoch_day";

    private final UserInterface local;   // SQLite UserManager
    private final UserInterface remote;  // FirebaseUserManager
    private final SharedPreferences prefs;

    public UserRepository(Context context, UserInterface localManager, UserInterface remoteManager) {
        if (context == null) throw new IllegalArgumentException("context == null");
        if (localManager == null) throw new IllegalArgumentException("localManager == null");
        if (remoteManager == null) throw new IllegalArgumentException("remoteManager == null");
        this.local = localManager;
        this.remote = remoteManager;
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ------------------------------------------------------------
    // Sync helpers
    // ------------------------------------------------------------

    /**
     * Push local global score → Firestore at most once per (UTC-ish) day.
     */
    public boolean pushLocalGlobalScoreToCloud() {
        long todayEpochDay = System.currentTimeMillis() / 86_400_000L; // avoid java.time
        long last = prefs.getLong(KEY_LAST_GLOBAL_PUSH_DAY, Long.MIN_VALUE);
        if (last == todayEpochDay) return false; // already pushed today

        int localScore = local.getGlobalScore(); // 0 if missing by contract
        boolean ok = remote.setGlobalScore(localScore); // upsert current user’s score
        if (ok) {
            prefs.edit().putLong(KEY_LAST_GLOBAL_PUSH_DAY, todayEpochDay).apply();
        }
        return ok;
    }

    /**
     * Pull friends' scores from Firestore and upsert into local global_score.
     * Runs every time the user opens the app.
     *
     * @return number of friend rows upserted in local.
     */
    public int refreshFriendsScoresFromCloud() {
        List<UserInterface.Friend> friends = local.getFriends(); // normalized DTOs
        ArrayList<String> acceptedUids = new ArrayList<>();
        if (friends != null) {
            for (UserInterface.Friend f : friends) {
                if (f == null || f.uid == null) continue;
                // only accepted friends for leaderboard
                if (f.status == null || "accepted".equalsIgnoreCase(f.status)) {
                    acceptedUids.add(f.uid);
                }
            }
        }
        if (acceptedUids.isEmpty()) return 0;

        Map<String, Integer> cloudScores = remote.getGlobalScores(acceptedUids); // missing UIDs omitted
        int upserts = 0;
        if (cloudScores != null) {
            for (Map.Entry<String, Integer> e : cloudScores.entrySet()) {
                String uid = e.getKey();
                Integer score = e.getValue();
                if (uid == null || score == null) continue;
                // setGlobalScore(uid, …) is specified to create if missing (normalized contract)
                if (local.setGlobalScore(uid, score)) {
                    upserts++;
                }
            }
        }
        return upserts;
    }

    // ------------------------------------------------------------
    // UserInterface delegation (LOCAL only)
    // ------------------------------------------------------------

    // user_profile
    @Override
    public boolean upsertUserProfile(String uid, String name, String email) {
        return local.upsertUserProfile(uid, name, email);
    }

    @Override
    public boolean hasUserProfile(String uid, String email) {
        return local.hasUserProfile(uid, email);
    }

    public boolean firebaseHasUserProfile(String uid, String email) {
        return remote.hasUserProfile(uid, email);
    }

    @Override
    public String getUserName(String uid) {
        return local.getUserName(uid);
    }

    @Override
    public String getUserEmail(String uid) {
        return local.getUserEmail(uid);
    }

    @Override
    public UserProfile getUserProfile(String uid, String email) {
        return local.getUserProfile(uid, email);
    }

    // global score (reads/writes used by UI go to LOCAL)
    @Override
    public int getGlobalScore() {
        return local.getGlobalScore();
    }

    @Override
    public Integer getGlobalScore(String uid) {
        return local.getGlobalScore(uid);
    }

    @Override
    public Map<String, Integer> getGlobalScores(java.util.Collection<String> uids) {
        return local.getGlobalScores(uids);
    }

    @Override
    public List<ScoreEntry> listAllGlobalScores() {
        return local.listAllGlobalScores();
    }

    @Override
    public boolean createGlobalScore(int initialScore) {
        return local.createGlobalScore(initialScore);
    }

    @Override
    public boolean createGlobalScore(String uid, int initialScore) {
        return local.createGlobalScore(uid, initialScore);
    }

    @Override
    public boolean ensureGlobalScore(String uid) {
        return local.ensureGlobalScore(uid);
    }

    @Override
    public boolean setGlobalScore(int newScore) {
        return local.setGlobalScore(newScore);
    }

    @Override
    public boolean setGlobalScore(String uid, int newScore) {
        return local.setGlobalScore(uid, newScore);
    }

    @Override
    public int addToGlobalScore(int delta) {
        return local.addToGlobalScore(delta);
    }

    @Override
    public int addToGlobalScore(String uid, int delta) {
        return local.addToGlobalScore(uid, delta);
    }

    @Override
    public boolean deleteGlobalScore() {
        return local.deleteGlobalScore();
    }

    @Override
    public boolean deleteGlobalScore(String uid) {
        return local.deleteGlobalScore(uid);
    }

    @Override
    public int deleteGlobalScores(java.util.Collection<String> uids) {
        return local.deleteGlobalScores(uids);
    }

    // streak
    @Override
    public int getStreakCount() {
        return local.getStreakCount();
    }

    @Override
    public boolean setStreakCount(int newCount) {
        return local.setStreakCount(newCount);
    }

    @Override
    public int incrementStreak() {
        return local.incrementStreak();
    }

    @Override
    public boolean resetStreak() {
        return local.resetStreak();
    }

    // friends
    @Override
    public boolean upsertFriend(String friendUid, String friendName) {
        return local.upsertFriend(friendUid, friendName);
    }

    @Override
    public boolean removeFriend(String friendUid) {
        return local.removeFriend(friendUid);
    }

    @Override
    public boolean acceptFriend(String friendUid) {
        return local.acceptFriend(friendUid);
    }

    @Override
    public boolean denyFriend(String friendUid) {
        return local.denyFriend(friendUid);
    }

    @Override
    public boolean isFriend(String friendUid) {
        return local.isFriend(friendUid);
    }

    @Override
    public List<Friend> getFriends() {
        return local.getFriends();
    }

    // badges
    @Override
    public boolean addBadge(String badgeId) {
        return local.addBadge(badgeId);
    }

    @Override
    public boolean removeBadge(String badgeId) {
        return local.removeBadge(badgeId);
    }

    @Override
    public boolean hasBadge(String badgeId) {
        return local.hasBadge(badgeId);
    }

    @Override
    public List<String> listBadges() {
        return local.listBadges();
    }
}