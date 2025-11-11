package com.code.wlu.cp470.wellnest.viewmodel;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.code.wlu.cp470.wellnest.data.UserModels.Friend;
import com.code.wlu.cp470.wellnest.data.UserRepository;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.UserManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseUserManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FriendViewModel extends AndroidViewModel {
    private final UserRepository repo;
    private final WellnestDatabaseHelper dbHelper;
    private final SQLiteDatabase db;

    // Background executor so we don't hit DB on the main thread
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    // LiveData exposed to the UI
    private final MutableLiveData<List<Friend>> acceptedFriends = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<Friend>> pendingFriends = new MutableLiveData<>(Collections.emptyList());

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

    // --- Mutations: do work, then refresh lists ---
    public void addFriend(@NonNull String email) {
        io.execute(() -> {
            boolean ok = false;
            try {
                ok = repo.addFriend(email);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (ok) refreshFriends();
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

    // --- Internal: load from repo and split into lists ---
    private void refreshFriends() {
        io.execute(() -> {
            List<Friend> all = repo.getFriends();
            List<Friend> acc = new ArrayList<>();
            List<Friend> pen = new ArrayList<>();
            for (Friend f : all) {
                if ("accepted".equals(f.getStatus())) acc.add(f);
                else if ("pending".equals(f.getStatus())) pen.add(f);
            }
            acceptedFriends.postValue(acc);
            pendingFriends.postValue(pen);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        io.shutdown();
        if (db != null && db.isOpen()) db.close();
        if (dbHelper != null) dbHelper.close();
    }
}
