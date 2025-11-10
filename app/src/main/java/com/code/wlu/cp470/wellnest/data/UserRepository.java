package com.code.wlu.cp470.wellnest.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.code.wlu.cp470.wellnest.data.UserModels.Friend;
import com.code.wlu.cp470.wellnest.data.UserModels.Score;
import com.code.wlu.cp470.wellnest.data.UserModels.UserProfile;
import com.code.wlu.cp470.wellnest.data.local.managers.UserManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseUserManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Frontend-facing repository:
 * - All data APIs delegate to the LOCAL manager (SQLite).
 * - Sync helpers use the REMOTE manager (Firestore) for push/pull.
 * <p>
 * Threading: Firestore calls are synchronous in your FirebaseUserManager (Tasks.await);
 * call sync methods off the main thread.
 */
public final class UserRepository {

    private static final String PREFS = "user_repo_prefs";
    private static final String KEY_LAST_GLOBAL_PUSH_DAY = "last_global_push_epoch_day";

    private final UserManager local;   // SQLite UserManager
    private final FirebaseUserManager remote;  // FirebaseUserManager
    private final SharedPreferences prefs;

    public UserRepository(Context context, UserManager localManager, FirebaseUserManager remoteManager) {
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


    // ------------------------------------------------------------
    // UserInterface delegation (LOCAL only)
    // ------------------------------------------------------------

    // user_profile

    public boolean upsertUserProfile(String uid, String name, String email) {
        return local.upsertUserProfile(uid, name, email);
    }


    public boolean hasUserProfile(String uid, String email) {
        return local.hasUserProfile(uid, email);
    }

    public boolean firebaseHasUserProfile(String uid, String email) throws ExecutionException, InterruptedException {
        return remote.hasUserProfile(uid, email);
    }


    public String getUserName(String uid) {
        return local.getUserName(uid);
    }


    public String getUserEmail(String uid) {
        return local.getUserEmail(uid);
    }


    public UserProfile getUser(String uid, String email) {
        return local.getUserProfile(uid, email);
    }

    // global score (reads/writes used by UI go to LOCAL)

    public int getGlobalScore() {
        return local.getGlobalScore();
    }


    public Integer getGlobalScore(String uid) {
        return local.getGlobalScore(uid);
    }


    public Map<String, Integer> getGlobalScores(java.util.Collection<String> uids) {
        return local.getGlobalScores(uids);
    }


    public List<Score> listAllGlobalScores() {
        return local.listAllGlobalScores();
    }


    public boolean createGlobalScore(int initialScore) {
        return local.createGlobalScore(initialScore);
    }


    public boolean createGlobalScore(String uid, int initialScore) {
        return local.createGlobalScore(uid, initialScore);
    }


    public boolean ensureGlobalScore(String uid) {
        return local.ensureGlobalScore(uid);
    }


    public boolean setGlobalScore(int newScore) {
        return local.setGlobalScore(newScore);
    }


    public boolean setGlobalScore(String uid, int newScore) {
        return local.setGlobalScore(uid, newScore);
    }


    public int addToGlobalScore(int delta) {
        return local.addToGlobalScore(delta);
    }


    public int addToGlobalScore(String uid, int delta) {
        return local.addToGlobalScore(uid, delta);
    }


    public boolean deleteGlobalScore() {
        return local.deleteGlobalScore();
    }


    public boolean deleteGlobalScore(String uid) {
        return local.deleteGlobalScore(uid);
    }


    public int deleteGlobalScores(java.util.Collection<String> uids) {
        return local.deleteGlobalScores(uids);
    }

    // streak

    public int getStreakCount() {
        return local.getStreakCount();
    }


    public boolean setStreakCount(int newCount) {
        return local.setStreakCount(newCount);
    }


    public int incrementStreak() {
        return local.incrementStreak();
    }


    public boolean resetStreak() {
        return local.resetStreak();
    }

    // friends

    public boolean addFriendByEmail(String email) throws ExecutionException, InterruptedException {
        String ownerUid = local.currentUid();
        UserProfile profile = remote.getUser(null, email);
        String uid = profile.getUid();
        String name = profile.getName();
        boolean localSuccess = local.upsertFriend(uid, name);
        boolean remoteSuccess = remote.addFriendRequest(ownerUid, uid, name);
        return localSuccess && remoteSuccess;
    }


    public boolean upsertFriend(String friendUid, String friendName) {
        return local.upsertFriend(friendUid, friendName);
    }


    public boolean removeFriend(String friendUid) {
        return local.removeFriend(friendUid);
    }


    public boolean acceptFriend(String friendUid) {
        return local.acceptFriend(friendUid);
    }


    public boolean denyFriend(String friendUid) {
        return local.denyFriend(friendUid);
    }


    public boolean isFriend(String friendUid) {
        return local.isFriend(friendUid);
    }


    public List<Friend> getFriends() {
        return local.getFriends();
    }

    // badges

    public boolean addBadge(String badgeId) {
        return local.addBadge(badgeId);
    }


    public boolean removeBadge(String badgeId) {
        return local.removeBadge(badgeId);
    }


    public boolean hasBadge(String badgeId) {
        return local.hasBadge(badgeId);
    }


    public List<String> listBadges() {
        return local.listBadges();
    }
}