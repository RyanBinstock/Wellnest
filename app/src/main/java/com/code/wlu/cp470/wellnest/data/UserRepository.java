package com.code.wlu.cp470.wellnest.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.data.UserModels.Friend;
import com.code.wlu.cp470.wellnest.data.UserModels.Score;
import com.code.wlu.cp470.wellnest.data.UserModels.UserProfile;
import com.code.wlu.cp470.wellnest.data.local.managers.UserManager;
import com.code.wlu.cp470.wellnest.data.model.FriendRequestResult;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseUserManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.Collection;
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

    private static final String TAG = "UserRepository";
    private static final String PREFS = "user_repo_prefs";
    private static final String PREFS_UID = "uid";
    private static final String KEY_LAST_GLOBAL_PUSH_DAY = "last_global_push_epoch_day";

    private final UserManager local;   // SQLite UserManager
    private final FirebaseUserManager remote;  // FirebaseUserManager
    private final SharedPreferences prefs;
    private final Context context;
    private final Handler mainHandler;

    public UserRepository(Context context, UserManager localManager, FirebaseUserManager remoteManager) {
        if (context == null) throw new IllegalArgumentException("context == null");
        if (localManager == null) throw new IllegalArgumentException("localManager == null");
        if (remoteManager == null) throw new IllegalArgumentException("remoteManager == null");
        this.local = localManager;
        this.remote = remoteManager;
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.mainHandler = new Handler(Looper.getMainLooper());

        prefs.edit().putString(PREFS_UID, local.currentUid()).apply();
    }

    // ------------------------------------------------------------
    // Sync helpers
    // ------------------------------------------------------------

    public void syncGlobalScore() {
        String uid = prefs.getString(PREFS_UID, "-1");
        int localScore = local.getGlobalScore();
        int remoteScore = -1;
        try {
            remoteScore = remote.getGlobalScore(uid);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get global score for uid=" + uid, e);
        }

        if (remoteScore == -1 || localScore == -1) {
            Log.e(TAG, "Failed to get global score for uid=" + uid);
        }

        if (localScore > remoteScore) remote.setGlobalScore(uid, localScore);
        else if (localScore < remoteScore) local.setGlobalScore(uid, remoteScore);
    }


    // ------------------------------------------------------------
    // Method delegation
    // ------------------------------------------------------------

    public boolean upsertUserProfile(String uid, String name, String email) {
        return local.upsertUserProfile(uid, name, email);
    }


    public boolean hasUserProfile(String uid, String email) {
        return local.hasUserProfile(uid, email);
    }

    public boolean firebaseHasUserProfile(String uid, String email) throws ExecutionException, InterruptedException {
        return remote.hasUserProfile(uid, email);
    }


    public String getUserName() {
        String uid = local.currentUid();
        return local.getUserName(uid);
    }


    public String getUserEmail() {
        String uid = local.currentUid();
        return local.getUserEmail(uid);
    }


    public UserProfile getUser(String uid, String email) {
        return local.getUserProfile(uid, email);
    }

    public void deleteUserProfile() {
        String uid = local.currentUid();
        boolean localSuccess = local.deleteUserProfile();
        if (!localSuccess) {
            throw new IllegalStateException("Failed to delete local user profile");
        }
        boolean remoteSuccess = remote.deleteUserProfile(uid);
        if (!remoteSuccess) {
            throw new IllegalStateException("Failed to delete remote user profile");
        }
    }

    // global score (reads/writes used by UI go to LOCAL)

    public int getGlobalScore() {
        return local.getGlobalScore();
    }


    public int getGlobalScore(String uid) {
        return local.getGlobalScore(uid);
    }

    public int getGlobalScoreRemote(String uid) {
        int score = -1;
        try {
            score = remote.getGlobalScore(uid);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get global score for uid=" + uid, e);
        }
        return score;
    }


    public Map<String, Integer> getGlobalScores(Collection<String> uids) {
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


    public int deleteGlobalScores(Collection<String> uids) {
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

    public FriendRequestResult addFriendByEmail(String email) {
        try {
            String ownerUid = local.currentUid();
            String ownerName = local.getUserName(ownerUid);
            UserProfile profile = remote.getUser(null, email);
            if (profile == null) {
                postToast(R.string.friend_not_found);
                String message = context.getString(R.string.friend_not_found);
                return FriendRequestResult.remoteFailure(message, null);
            }
            return persistFriendAndSend(ownerUid, ownerName, profile.getUid(), profile.getName());
        } catch (ExecutionException e) {
            Log.e(TAG, "Failed to look up user by email", e);
            String message = e.getMessage();
            if (message == null || message.isEmpty()) {
                message = context.getString(R.string.unable_to_add_friend_try_again_later);
            }
            return FriendRequestResult.remoteFailure(message, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Friend lookup interrupted", e);
            String message = e.getMessage();
            if (message == null || message.isEmpty()) {
                message = context.getString(R.string.unable_to_add_friend_try_again_later);
            }
            return FriendRequestResult.remoteFailure(message, e);
        }
    }


    public boolean upsertFriend(String friendUid, String friendName) {
        return local.upsertFriend(friendUid, friendName);
    }

    public boolean upsertFriend(String friendUid, String friendName, String status) {
        return local.upsertFriend(friendUid, friendName, status);
    }

    /**
     * Syncs friends from Firebase to local database.
     * This method fetches the current user's friends from Firebase and updates the local database.
     *
     * @return true if sync was successful, false otherwise
     */
    public boolean syncFriendsFromFirebase() {
        try {
            Log.d(TAG, "syncFriendsFromFirebase: Starting sync");

            // Get current user's UID
            String currentUid = local.currentUid();
            if (currentUid == null || currentUid.isEmpty()) {
                Log.w(TAG, "syncFriendsFromFirebase: No current user UID available");
                return false;
            }

            Log.d(TAG, "syncFriendsFromFirebase: Current user UID = " + currentUid);

            // Fetch friends from Firebase
            List<Friend> firebaseFriends = remote.getFriends(currentUid);
            Log.d(TAG, "syncFriendsFromFirebase: Found " + firebaseFriends.size() + " friends in Firebase");

            int syncedCount = 0;
            int failedCount = 0;

            // Sync each friend to local database
            for (Friend friend : firebaseFriends) {
                try {
                    boolean success = local.upsertFriend(
                            friend.getUid(),
                            friend.getName(),
                            friend.getStatus()
                    );

                    if (success) {
                        syncedCount++;
                        Log.d(TAG, "syncFriendsFromFirebase: Synced friend " + friend.getName() +
                                " (" + friend.getUid() + ") with status " + friend.getStatus());
                    } else {
                        failedCount++;
                        Log.w(TAG, "syncFriendsFromFirebase: Failed to sync friend " + friend.getName() +
                                " (" + friend.getUid() + ")");
                    }
                } catch (Exception e) {
                    failedCount++;
                    Log.e(TAG, "syncFriendsFromFirebase: Error syncing friend " + friend.getName(), e);
                }
            }

            Log.i(TAG, "syncFriendsFromFirebase: Sync completed. " +
                    "Synced: " + syncedCount + ", Failed: " + failedCount + ", Total: " + firebaseFriends.size());

            return failedCount == 0; // Return true only if all synced successfully

        } catch (Exception e) {
            Log.e(TAG, "syncFriendsFromFirebase: Failed to sync friends from Firebase", e);
            return false;
        }
    }

    public FriendRequestResult addFriend(String email) {
        return addFriendByEmail(email);
    }


    public boolean removeFriend(String friendUid) {
        return local.removeFriend(friendUid);
    }


    public boolean acceptFriend(String friendUid) {
        Log.d(TAG, "acceptFriend: Starting accept friend process for friendUid=" + friendUid);

        // First update local database
        boolean localSuccess = local.acceptFriend(friendUid);
        Log.d(TAG, "acceptFriend: Local update result=" + localSuccess + " for friendUid=" + friendUid);

        if (!localSuccess) {
            Log.w(TAG, "acceptFriend: Local update failed for friendUid=" + friendUid);
            return false;
        }

        // Then update Firebase
        String currentUid = local.currentUid();
        if (currentUid == null || currentUid.isEmpty()) {
            Log.e(TAG, "acceptFriend: Current user UID is null or empty, cannot update Firebase");
            return localSuccess; // Return local success even if Firebase fails
        }

        try {
            boolean remoteSuccess = remote.acceptFriend(currentUid, friendUid);
            Log.d(TAG, "acceptFriend: Firebase update result=" + remoteSuccess + " for currentUid=" + currentUid + ", friendUid=" + friendUid);

            if (!remoteSuccess) {
                Log.w(TAG, "acceptFriend: Firebase update failed but local update succeeded for friendUid=" + friendUid);
            } else {
                Log.i(TAG, "acceptFriend: Successfully accepted friend request for friendUid=" + friendUid);
            }

            return remoteSuccess; // Return Firebase result since local already succeeded
        } catch (Exception e) {
            Log.e(TAG, "acceptFriend: Firebase update threw exception for friendUid=" + friendUid, e);
            return localSuccess; // Return local success even if Firebase fails with exception
        }
    }


    public boolean denyFriend(String friendUid) {
        Log.d(TAG, "denyFriend: Starting deny friend process for friendUid=" + friendUid);

        // First update local database
        boolean localSuccess = local.denyFriend(friendUid);
        Log.d(TAG, "denyFriend: Local update result=" + localSuccess + " for friendUid=" + friendUid);

        if (!localSuccess) {
            Log.w(TAG, "denyFriend: Local update failed for friendUid=" + friendUid);
            return false;
        }

        // Then update Firebase
        String currentUid = local.currentUid();
        if (currentUid == null || currentUid.isEmpty()) {
            Log.e(TAG, "denyFriend: Current user UID is null or empty, cannot update Firebase");
            return localSuccess; // Return local success even if Firebase fails
        }

        try {
            boolean remoteSuccess = remote.denyFriend(currentUid, friendUid);
            Log.d(TAG, "denyFriend: Firebase update result=" + remoteSuccess + " for currentUid=" + currentUid + ", friendUid=" + friendUid);

            if (!remoteSuccess) {
                Log.w(TAG, "denyFriend: Firebase update failed but local update succeeded for friendUid=" + friendUid);
            } else {
                Log.i(TAG, "denyFriend: Successfully denied friend request for friendUid=" + friendUid);
            }

            return remoteSuccess; // Return Firebase result since local already succeeded
        } catch (Exception e) {
            Log.e(TAG, "denyFriend: Firebase update threw exception for friendUid=" + friendUid, e);
            return localSuccess; // Return local success even if Firebase fails with exception
        }
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

    private void postToast(@StringRes int messageRes) {
        mainHandler.post(() ->
                Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
        );
    }

    @Nullable
    private FriendRequestResult ensurePlayServicesReady() {
        GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
        int status = availability.isGooglePlayServicesAvailable(context);
        if (status == ConnectionResult.SUCCESS) {
            return null;
        }
        String errorString = availability.getErrorString(status);
        Log.e(TAG, "Google Play Services unavailable for friend request. status=" + status + ", error=" + errorString);
        String friendlyError = context.getString(
                R.string.friend_request_play_services_unavailable,
                errorString == null ? "unknown" : errorString
        );
        return FriendRequestResult.playServicesUnavailable(friendlyError, null);
    }

    private FriendRequestResult persistFriendAndSend(String ownerUid,
                                                     String ownerName,
                                                     String friendUid,
                                                     String friendName) {
        FriendRequestResult availability = ensurePlayServicesReady();
        if (availability != null) {
            return availability;
        }

        try {
            boolean localSuccess = local.upsertFriend(friendUid, friendName);
            if (!localSuccess) {
                Exception failure = new IllegalStateException("local.upsertFriend returned false for uid=" + friendUid);
                return FriendRequestResult.localFailure(failure.getMessage(), failure);
            }
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage()
                    : context.getString(R.string.unable_to_add_friend_try_again_later);
            return FriendRequestResult.localFailure(message, e);
        }

        try {
            remote.addFriendRequest(ownerUid, friendUid, friendName, ownerName);
            String successMessage = context.getString(R.string.friend_request_sent);
            postToast(R.string.friend_request_sent);
            return FriendRequestResult.success(successMessage);
        } catch (FirebaseFirestoreException e) {
            Log.e(TAG, "Remote friend request failed", e);
            String message = e.getMessage() != null ? e.getMessage()
                    : context.getString(R.string.unable_to_add_friend_try_again_later);
            return FriendRequestResult.remoteFailure(message, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Remote friend request interrupted", e);
            String message = context.getString(R.string.unable_to_add_friend_try_again_later);
            return FriendRequestResult.remoteFailure(message, e);
        }
    }
}