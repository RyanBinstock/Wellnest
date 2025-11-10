package com.code.wlu.cp470.wellnest.viewmodel;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.lifecycle.AndroidViewModel;

import com.code.wlu.cp470.wellnest.data.UserModels.Friend;
import com.code.wlu.cp470.wellnest.data.UserRepository;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.UserManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseUserManager;

import java.util.ArrayList;
import java.util.List;

public class FriendViewModel extends AndroidViewModel {
    private final UserRepository repo;
    private final WellnestDatabaseHelper dbHelper;
    private final SQLiteDatabase db;

    public FriendViewModel(Application app) {
        super(app);
        Context context = app.getApplicationContext();

        this.dbHelper = new WellnestDatabaseHelper(context);
        this.db = dbHelper.getWritableDatabase();

        UserManager local = new UserManager(db);
        FirebaseUserManager remote = new FirebaseUserManager();

        this.repo = new UserRepository(context, local, remote);
    }

    public boolean addFriend(String email) {
        return repo.upsertFriend(null, email);
    }

    public boolean removeFriend(String uid) {
        return repo.removeFriend(uid);
    }

    public boolean acceptFriend(String uid) {
        return repo.acceptFriend(uid);
    }

    public boolean denyFriend(String uid) {
        return repo.denyFriend(uid);
    }

    public List<Friend> getFriends() {
        return repo.getFriends();
    }

    public int getFriendScore(String uid) {
        return repo.getGlobalScore(uid);
    }

    public List<Friend> getPendingFriends() {
        List<Friend> allFriends = getFriends();
        List<Friend> pendingFriends = new ArrayList<>();
        for (Friend friend : allFriends) {
            if (friend.getStatus().equals("pending")) {
                pendingFriends.add(friend);
            }
        }
        return pendingFriends;
    }

    public List<Friend> getAcceptedFriends() {
        List<Friend> allFriends = getFriends();
        List<Friend> acceptedFriends = new ArrayList<>();
        for (Friend friend : allFriends) {
            if (friend.getStatus().equals("accepted")) {
                acceptedFriends.add(friend);
            }
        }
        return acceptedFriends;
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        if (db != null && db.isOpen()) db.close();
        if (dbHelper != null) dbHelper.close();
    }
}
