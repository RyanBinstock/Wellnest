package com.code.wlu.cp470.wellnest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.code.wlu.cp470.wellnest.data.SnapTaskRepository;
import com.code.wlu.cp470.wellnest.data.UserRepository;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.SnapTaskManager;
import com.code.wlu.cp470.wellnest.data.local.managers.UserManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseSnapTaskManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseUserManager;
import com.code.wlu.cp470.wellnest.utils.ActivityJarPrefetcher;
import com.code.wlu.cp470.wellnest.utils.MusicService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PREFS = "main_activity_prefs";
    private SharedPreferences prefs;
    private WellnestDatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private UserRepository userRepository;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Hide navigation bar and allow content behind the status bar
        View decorView = getWindow().getDecorView();

        // Use WindowInsetsControllerCompat for immersive nav hiding while keeping status bar visible
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), decorView);
        if (insetsController != null) {
            insetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
            // Hide only the navigation bar; keep status bar icons visible
            insetsController.hide(WindowInsetsCompat.Type.navigationBars());
            insetsController.show(WindowInsetsCompat.Type.statusBars());
        }

        // Ensure layout extends behind system bars; do NOT use FULLSCREEN so status icons remain visible
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host);
        NavController navController = navHostFragment.getNavController();

        // Synchronization logic goes here
        prefs = this.getSharedPreferences(PREFS, MODE_PRIVATE);

        dbHelper = new WellnestDatabaseHelper(this);
        db = dbHelper.getWritableDatabase();


        // Save date information
        long lastCheckMillis = prefs.getLong("last_check_date", 0);
        long nowMillis = System.currentTimeMillis();
        LocalDate lastDate = Instant.ofEpochMilli(lastCheckMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        LocalDate today = Instant.ofEpochMilli(nowMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        boolean newDay = !today.equals(lastDate);
        Log.d(TAG, "onCreate: lastCheckMillis=" + lastCheckMillis
                + ", lastDate=" + lastDate
                + ", today=" + today
                + ", newDay=" + newDay);

        if (newDay) {
            Log.d(TAG, "onCreate: new day detected, starting background sync of snap tasks");
            new Thread(() -> {
                SnapTaskManager snapTaskManager = new SnapTaskManager(db);
                FirebaseSnapTaskManager snapTaskRemoteManager = new FirebaseSnapTaskManager();
                SnapTaskRepository snapTaskRepository =
                        new SnapTaskRepository(getApplicationContext(), snapTaskManager, snapTaskRemoteManager);
                try {
                    snapTaskRepository.syncSnapTasks();
                    prefs.edit().putLong("last_check_date", nowMillis).apply();
                    Log.d(TAG, "onCreate: snap task sync finished, last_check_date updated=" + nowMillis);
                } catch (Exception e) {
                    Log.e(TAG, "onCreate: snap task sync failed", e);
                }
            }).start();
        } else {
            Log.d(TAG, "onCreate: same day as last check, skipping snap task sync");
        }

        // Prefetch Activity Jar activities in background
        new Thread(() -> {
            ActivityJarPrefetcher.prefetchActivities(getApplicationContext());
        }).start();

        userRepository = new UserRepository(this, new UserManager(db), new FirebaseUserManager());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            userRepository.syncGlobalScore();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        startService(new Intent(this, MusicService.class));
    }

    @Override
    protected void onPause() {
        super.onPause();
        Intent i = new Intent(this, MusicService.class);
        i.setAction(MusicService.ACTION_PAUSE);
        startService(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent i = new Intent(this, MusicService.class);
        i.setAction(MusicService.ACTION_RESUME);
        startService(i);
    }


    @Override
    protected void onStop() {
        super.onStop();
        stopService(new Intent(this, MusicService.class));
    }

}
