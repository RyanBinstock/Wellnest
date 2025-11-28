package com.code.wlu.cp470.wellnest.viewmodel;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.code.wlu.cp470.wellnest.data.UserModels.Friend;
import com.code.wlu.cp470.wellnest.data.UserRepository;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.UserManager;
import com.code.wlu.cp470.wellnest.data.model.FriendRequestResult;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseUserManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel responsible for exposing friend data and friend-request operations
 * to the UI (e.g., FriendFragment).
 * <p>
 * Handles:
 * - Loading friends from local DB (after syncing from Firebase when possible)
 * - Splitting friends into accepted vs pending lists
 * - Submitting friend requests and exposing the latest request state
 */
public class FriendViewModel extends AndroidViewModel {

    private static final String TAG = "FriendViewModel";

    // --- Data layer dependencies ---
    private final UserRepository userRepository;
    private final WellnestDatabaseHelper databaseHelper;
    private final SQLiteDatabase writableDatabase;

    // Single background thread for DB/network work (avoid blocking main thread)
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    // --- LiveData exposed to the UI ---
    /**
     * Friends whose status is "accepted".
     */
    private final MutableLiveData<List<Friend>> acceptedFriendsLiveData =
            new MutableLiveData<>(Collections.emptyList());

    /**
     * Friends whose status is "pending".
     */
    private final MutableLiveData<List<Friend>> pendingFriendsLiveData =
            new MutableLiveData<>(Collections.emptyList());

    /**
     * UI-facing state for the most recent friend-request attempt (addFriend()).
     */
    private final MutableLiveData<FriendRequestUiState> friendRequestStateLiveData =
            new MutableLiveData<>(FriendRequestUiState.idle());

    // --- Constructor / setup ---

    public FriendViewModel(@NonNull Application application) {
        super(application);
        Context appContext = application.getApplicationContext();

        // Local SQLite DB
        this.databaseHelper = new WellnestDatabaseHelper(appContext);
        this.writableDatabase = databaseHelper.getWritableDatabase();

        // Local + remote managers
        UserManager localUserManager = new UserManager(writableDatabase);
        FirebaseUserManager remoteUserManager = new FirebaseUserManager();
        this.userRepository = new UserRepository(appContext, localUserManager, remoteUserManager);

        // Initial load (sync from Firebase then populate LiveData)
        refreshFriends();
    }

    // --- LiveData getters for Fragment / UI ---

    public LiveData<List<Friend>> getAcceptedFriends() {
        return acceptedFriendsLiveData;
    }

    public LiveData<List<Friend>> getPendingFriends() {
        return pendingFriendsLiveData;
    }

    public LiveData<FriendRequestUiState> getFriendRequestState() {
        return friendRequestStateLiveData;
    }

    // --- Public operations: add / remove / accept / deny friends ---

    /**
     * Submit a friend request by email.
     * Runs on a background thread, then:
     * - posts FriendRequestUiState to LiveData
     * - refreshes friend lists on success
     */
    public void addFriend(@NonNull String friendEmail) {
        ioExecutor.execute(() -> {
            FriendRequestResult result;

            try {
                result = userRepository.addFriend(friendEmail);
            } catch (Exception exception) {
                Log.e(TAG, "Unexpected repository failure while adding friend", exception);
                String errorMessage = exception.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "Unexpected error";
                }
                result = FriendRequestResult.localFailure(errorMessage, exception);
            }

            // Map result to a UI-friendly state object
            FriendRequestUiState uiState =
                    FriendRequestUiState.fromResult(friendEmail, result);
            friendRequestStateLiveData.postValue(uiState);

            Log.i(
                    TAG,
                    "Friend request attempt email=" + friendEmail
                            + ", status=" + result.getStatus()
                            + ", message=" + result.getMessage()
            );

            if (result.isSuccess()) {
                refreshFriends();
            }
        });
    }

    /**
     * Remove an existing friend by uid. Refreshes lists on success.
     */
    public boolean removeFriend(String friendUid) {
        boolean success = userRepository.removeFriend(friendUid);
        if (success) {
            refreshFriends();
        }
        return success;
    }

    /**
     * Accept a pending friend request by uid. Refreshes lists on success.
     */
    public boolean acceptFriend(String friendUid) {
        boolean success = userRepository.acceptFriend(friendUid);
        if (success) {
            refreshFriends();
        }
        return success;
    }

    /**
     * Deny a pending friend request by uid. Refreshes lists on success.
     */
    public boolean denyFriend(String friendUid) {
        boolean success = userRepository.denyFriend(friendUid);
        if (success) {
            refreshFriends();
        }
        return success;
    }

    /**
     * Helper to resolve a user's uid from their email address.
     */
    public String getUidFromEmail(String email) {
        return userRepository.getUser(null, email).getUid();
    }

    /**
     * Get a friend's global score by uid.
     */
    public int getFriendScore(String friendUid) {
        return userRepository.getGlobalScore(friendUid);
    }

    // --- Public: explicit sync + refresh ---

    /**
     * Explicitly trigger a sync from Firebase, then reload local friends
     * and update LiveData. Runs on background thread.
     */
    public void syncAndRefreshFriends() {
        ioExecutor.execute(() -> {
            Log.d(TAG, "syncAndRefreshFriends: starting sync and refresh");

            boolean syncSuccess = userRepository.syncFriendsFromFirebase();
            if (syncSuccess) {
                Log.d(TAG, "syncAndRefreshFriends: Firebase sync successful");
            } else {
                Log.w(TAG, "syncAndRefreshFriends: Firebase sync failed, continuing with local data");
            }

            refreshFriendsFromLocal();
        });
    }

    // --- Internal: sync (Firebase) + load (local) ---

    /**
     * Syncs from Firebase first, then reloads friend lists from local DB.
     * Runs entirely on background thread.
     */
    private void refreshFriends() {
        ioExecutor.execute(() -> {
            Log.d(TAG, "refreshFriends: syncing from Firebase before loading local data");

            boolean syncSuccess = userRepository.syncFriendsFromFirebase();

            List<Friend> friends = userRepository.getFriends();
            for (Friend f : friends) {
                String uid = f.getUid();
                int localScore = userRepository.getGlobalScore(uid);
                int remoteScore = userRepository.getGlobalScoreRemote(uid);
                if (remoteScore == -1 || localScore == -1)
                    Log.e(TAG, "refreshFriends: Failed to get global score for uid=" + uid);
                if (localScore != remoteScore) {
                    int newScore = Math.max(localScore, remoteScore);
                    userRepository.setGlobalScore(uid, newScore);
                }

            }
            if (syncSuccess) {
                Log.d(TAG, "refreshFriends: Firebase sync successful");
            } else {
                Log.w(TAG, "refreshFriends: Firebase sync failed, continuing with local data");
            }
            
            refreshFriendsFromLocal();
        });
    }

    /**
     * Loads all friends from local repository and splits them into
     * accepted vs pending lists, then posts them to LiveData.
     */
    private void refreshFriendsFromLocal() {
        List<Friend> allFriends = userRepository.getFriends();
        List<Friend> acceptedFriends = new ArrayList<>();
        List<Friend> pendingFriends = new ArrayList<>();

        Log.d(TAG, "refreshFriendsFromLocal: processing " + allFriends.size() + " friends from local database");

        for (Friend friend : allFriends) {
            String status = friend.getStatus();
            if ("accepted".equals(status)) {
                acceptedFriends.add(friend);
            } else if ("pending".equals(status)) {
                pendingFriends.add(friend);
            }
        }

        Log.d(TAG, "refreshFriendsFromLocal: found "
                + acceptedFriends.size() + " accepted friends, "
                + pendingFriends.size() + " pending friends");

        acceptedFriendsLiveData.postValue(acceptedFriends);
        pendingFriendsLiveData.postValue(pendingFriends);
    }

    // --- Lifecycle cleanup ---

    @Override
    protected void onCleared() {
        super.onCleared();
        ioExecutor.shutdown();

        if (writableDatabase != null && writableDatabase.isOpen()) {
            writableDatabase.close();
        }
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }

    // --- UI state class for friend-request feedback ---

    /**
     * UI-facing representation of the latest friend request attempt.
     * Idle state is represented by {@code hasResult == false}.
     */
    public static class FriendRequestUiState {

        /**
         * True if we have a result to show (success or failure).
         * False means "no active result / idle".
         */
        private final boolean hasResult;

        @Nullable
        private final FriendRequestResult.Status status;

        @Nullable
        private final String message;

        /**
         * Email (or other identifier) we attempted to add as a friend.
         */
        @Nullable
        private final String target;

        private FriendRequestUiState(boolean hasResult,
                                     @Nullable FriendRequestResult.Status status,
                                     @Nullable String message,
                                     @Nullable String target) {
            this.hasResult = hasResult;
            this.status = status;
            this.message = message;
            this.target = target;
        }

        /**
         * Idle / cleared state: nothing to show.
         */
        public static FriendRequestUiState idle() {
            return new FriendRequestUiState(false, null, null, null);
        }

        /**
         * Build a UI state from a repository result and the target email.
         */
        public static FriendRequestUiState fromResult(@NonNull String target,
                                                      @NonNull FriendRequestResult result) {
            return new FriendRequestUiState(
                    true,
                    result.getStatus(),
                    result.getMessage(),
                    target
            );
        }

        public boolean hasResult() {
            return hasResult;
        }

        @Nullable
        public FriendRequestResult.Status getStatus() {
            return status;
        }

        @Nullable
        public String getMessage() {
            return message;
        }

        @Nullable
        public String getTarget() {
            return target;
        }
    }
}
