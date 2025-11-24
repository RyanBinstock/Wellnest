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
import com.code.wlu.cp470.wellnest.data.model.FriendRequestResult;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.UserManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseUserManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FriendViewModel extends AndroidViewModel {
    private static final String TAG = "FriendViewModel";

    private final UserRepository repo;
    private final WellnestDatabaseHelper dbHelper;
    private final SQLiteDatabase db;

    // Background executor so we don't hit DB on the main thread
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    // LiveData exposed to the UI
    private final MutableLiveData<List<Friend>> acceptedFriends = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<Friend>> pendingFriends = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<FriendRequestUiState> friendRequestState =
            new MutableLiveData<>(FriendRequestUiState.idle());

    public FriendViewModel(@NonNull Application app) {
        super(app);
        Context context = app.getApplicationContext();

        this.dbHelper = new WellnestDatabaseHelper(context);
        this.db = dbHelper.getWritableDatabase();

        UserManager local = new UserManager(db);
        FirebaseUserManager remote = new FirebaseUserManager();
        this.repo = new UserRepository(context, local, remote);

        refreshFriends(); // initial load
    }

    // --- Expose LiveData to Fragment ---
    public LiveData<List<Friend>> getAcceptedFriends() {
        return acceptedFriends;
    }

    public LiveData<List<Friend>> getPendingFriends() {
        return pendingFriends;
    }

    public LiveData<FriendRequestUiState> getFriendRequestState() {
        return friendRequestState;
    }

    // --- Mutations: do work, then refresh lists ---
    public void addFriend(@NonNull String email) {
        io.execute(() -> {
            FriendRequestResult result;
            try {
                result = repo.addFriend(email);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected repository failure while adding friend", e);
                String message = e.getMessage();
                if (message == null || message.isEmpty()) {
                    message = "Unexpected error";
                }
                result = FriendRequestResult.localFailure(message, e);
            }
            FriendRequestUiState uiState = FriendRequestUiState.fromResult(email, result);
            friendRequestState.postValue(uiState);
            Log.i(TAG, "Friend request attempt email=" + email + ", status=" + result.getStatus()
                    + ", message=" + result.getMessage());
            if (result.isSuccess()) refreshFriends();
        });
    }


    public boolean removeFriend(String uid) {
        boolean ok = repo.removeFriend(uid);
        if (ok) refreshFriends();
        return ok;
    }

    public boolean acceptFriend(String uid) {
        boolean ok = repo.acceptFriend(uid);
        if (ok) refreshFriends();
        return ok;
    }

    public boolean denyFriend(String uid) {
        boolean ok = repo.denyFriend(uid);
        if (ok) refreshFriends();
        return ok;
    }

    public String getUidFromEmail(String email) {
        return repo.getUser(null, email).getUid();
    }

    public int getFriendScore(String uid) {
        return repo.getGlobalScore(uid);
    }

    // --- Public: sync from Firebase and refresh friends ---
    public void syncAndRefreshFriends() {
        io.execute(() -> {
            Log.d(TAG, "syncAndRefreshFriends: Starting sync and refresh");
            
            // First sync from Firebase to local
            boolean syncSuccess = repo.syncFriendsFromFirebase();
            if (syncSuccess) {
                Log.d(TAG, "syncAndRefreshFriends: Firebase sync successful");
            } else {
                Log.w(TAG, "syncAndRefreshFriends: Firebase sync failed, continuing with local data");
            }
            
            // Then load local friends and update UI
            refreshFriendsFromLocal();
        });
    }

    // --- Internal: load from repo and split into lists ---
    private void refreshFriends() {
        io.execute(() -> {
            Log.d(TAG, "refreshFriends: Syncing from Firebase before loading local data");
            
            // Sync from Firebase first, then load local
            boolean syncSuccess = repo.syncFriendsFromFirebase();
            if (syncSuccess) {
                Log.d(TAG, "refreshFriends: Firebase sync successful");
            } else {
                Log.w(TAG, "refreshFriends: Firebase sync failed, continuing with local data");
            }
            
            refreshFriendsFromLocal();
        });
    }
    
    // --- Helper: load from local repo and split into lists ---
    private void refreshFriendsFromLocal() {
        List<Friend> all = repo.getFriends();
        List<Friend> acc = new ArrayList<>();
        List<Friend> pen = new ArrayList<>();
        
        Log.d(TAG, "refreshFriendsFromLocal: Processing " + all.size() + " friends from local database");
        
        for (Friend f : all) {
            if ("accepted".equals(f.getStatus())) {
                acc.add(f);
            } else if ("pending".equals(f.getStatus())) {
                pen.add(f);
            }
        }
        
        Log.d(TAG, "refreshFriendsFromLocal: Found " + acc.size() + " accepted friends, " + pen.size() + " pending friends");
        
        acceptedFriends.postValue(acc);
        pendingFriends.postValue(pen);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        io.shutdown();
        if (db != null && db.isOpen()) db.close();
        if (dbHelper != null) dbHelper.close();
    }

    /**
     * UI-facing representation of the latest friend request attempt.
     * Idle state is represented by {@code hasResult == false}.
     */
    public static class FriendRequestUiState {
        private final boolean hasResult;
        @Nullable
        private final FriendRequestResult.Status status;
        @Nullable
        private final String message;
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

        public static FriendRequestUiState idle() {
            return new FriendRequestUiState(false, null, null, null);
        }

        public static FriendRequestUiState fromResult(@NonNull String target,
                                                      @NonNull FriendRequestResult result) {
            return new FriendRequestUiState(true, result.getStatus(), result.getMessage(), target);
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
