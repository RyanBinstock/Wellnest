package com.code.wlu.cp470.wellnest.viewmodel;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.code.wlu.cp470.wellnest.data.SnapTaskModels;
import com.code.wlu.cp470.wellnest.data.SnapTaskRepository;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.SnapTaskManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseSnapTaskManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SnapTaskViewModel extends AndroidViewModel {

    private final SnapTaskRepository repo;
    private final WellnestDatabaseHelper dbHelper;
    private final SQLiteDatabase db;

    // Background executor so we don't hit DB on the main thread
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public SnapTaskViewModel(@NonNull Application app) {
        super(app);
        Context context = app.getApplicationContext();

        this.dbHelper = new WellnestDatabaseHelper(context);
        this.db = dbHelper.getWritableDatabase();

        SnapTaskManager local = new SnapTaskManager(db);
        FirebaseSnapTaskManager remote = new FirebaseSnapTaskManager();
        this.repo = new SnapTaskRepository(context, local, remote);
    }

    public List<SnapTaskModels.Task> getTasks() {
        return repo.getTasks();
    }
    
    public int getScore() {
        return repo.getSnapTaskScore();
    }

    /**
     * Mark a SnapTask as completed and add its points to the local SnapTask score.
     * This uses the existing repository/local manager methods to ensure consistency.
     */
    public void completeTaskAndApplyScore(String uid, int points) {
        if (uid == null || uid.isEmpty()) return;
        repo.setTaskCompleted(uid);
        if (points != 0) {
            repo.addToSnapTaskScore(points);
        }
    }
    
}

