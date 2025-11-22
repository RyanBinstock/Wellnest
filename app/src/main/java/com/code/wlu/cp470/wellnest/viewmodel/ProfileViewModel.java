package com.code.wlu.cp470.wellnest.viewmodel;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.code.wlu.cp470.wellnest.data.UserModels;
import com.code.wlu.cp470.wellnest.data.UserRepository;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.UserManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseUserManager;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileViewModel extends AndroidViewModel {

    private final UserRepository repo;
    private final WellnestDatabaseHelper dbHelper;
    private final SQLiteDatabase db;

    // Background executor so we don't hit DB on the main thread
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    // LiveData exposed to the UI
    private final MutableLiveData<List<UserModels.Friend>> acceptedFriends = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<UserModels.Friend>> pendingFriends = new MutableLiveData<>(Collections.emptyList());

    public ProfileViewModel(@NonNull Application app) {
        super(app);
        Context context = app.getApplicationContext();

        this.dbHelper = new WellnestDatabaseHelper(context);
        this.db = dbHelper.getWritableDatabase();

        UserManager local = new UserManager(db);
        FirebaseUserManager remote = new FirebaseUserManager();
        this.repo = new UserRepository(context, local, remote);
    }

    public String getName() {
        return repo.getUserName();
    }

    public String getEmail() {
        return repo.getUserEmail();
    }
}
