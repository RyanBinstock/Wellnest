package com.code.wlu.cp470.wellnest.data;

import android.database.sqlite.SQLiteDatabase;

import com.code.wlu.cp470.wellnest.data.local.managers.ActivityJarManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActivityJarRepository {

    private final ActivityJarManager activityJarManager;
    private final ExecutorService executor;

    public ActivityJarRepository(SQLiteDatabase db) {
        this.activityJarManager = new ActivityJarManager(db);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public interface ScoreCallback {
        void onScoreUpdated(int newScore);
        void onError(Exception e);
    }

    public void addScore(int points, ScoreCallback callback) {
        executor.execute(() -> {
            try {
                int newScore = activityJarManager.addToActivityJarScore(points);
                callback.onScoreUpdated(newScore);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void getScore(ScoreCallback callback) {
        executor.execute(() -> {
            try {
                int score = activityJarManager.getActivityJarScore();
                callback.onScoreUpdated(score);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}