package com.code.wlu.cp470.wellnest.viewmodel;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.lifecycle.AndroidViewModel;

import com.code.wlu.cp470.wellnest.data.UserInterface;
import com.code.wlu.cp470.wellnest.data.UserRepository;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.UserManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseUserManager;

public class FriendViewModel extends AndroidViewModel {
    private final UserRepository repo;
    private final WellnestDatabaseHelper dbHelper;
    private final SQLiteDatabase db;

    public FriendViewModel(Application app) {
        super(app);
        Context context = app.getApplicationContext();

        this.dbHelper = new WellnestDatabaseHelper(context);
        this.db = dbHelper.getWritableDatabase();

        UserInterface local = new UserManager(db);
        UserInterface remote = new FirebaseUserManager();

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

    @Override
    protected void onCleared() {
        super.onCleared();
        if (db != null && db.isOpen()) db.close();
        if (dbHelper != null) dbHelper.close();
    }
}
