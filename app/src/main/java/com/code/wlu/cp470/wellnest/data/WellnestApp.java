package com.code.wlu.cp470.wellnest.data;

import android.app.Application;

import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;

public final class WellnestApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        // Open DB so Database Inspector can see it
        WellnestDatabaseHelper helper = new WellnestDatabaseHelper(getApplicationContext());
        helper.getWritableDatabase(); // ensures wellnest.db is created/opened
    }
}
