package com.code.wlu.cp470.wellnest;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.code.wlu.cp470.wellnest.data.SnapTaskRepository;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.SnapTaskManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseSnapTaskManager;

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


        SnapTaskManager snapTaskManager = new SnapTaskManager(db);
        FirebaseSnapTaskManager snapTaskRemoteManager = new FirebaseSnapTaskManager();
        SnapTaskRepository snapTaskRepository = new SnapTaskRepository(this, snapTaskManager, snapTaskRemoteManager);
        if (newDay) {
            snapTaskRepository.syncSnapTasks();
        }
    }
}
