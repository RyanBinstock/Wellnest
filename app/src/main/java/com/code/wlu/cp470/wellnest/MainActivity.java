package com.code.wlu.cp470.wellnest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.code.wlu.cp470.wellnest.data.SnapTaskRepository;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.SnapTaskManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseSnapTaskManager;
import com.code.wlu.cp470.wellnest.utils.MusicService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PREFS = "main_activity_prefs";
    private SharedPreferences prefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host);
        NavController navController = navHostFragment.getNavController();

        // Synchronization logic goes here
        prefs = this.getSharedPreferences(PREFS, MODE_PRIVATE);

        WellnestDatabaseHelper dbHelper = new WellnestDatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

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
