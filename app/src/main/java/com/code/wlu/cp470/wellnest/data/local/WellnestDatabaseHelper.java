package com.code.wlu.cp470.wellnest.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.code.wlu.cp470.wellnest.data.local.contracts.ActivityJarContract;
import com.code.wlu.cp470.wellnest.data.local.contracts.RoamioContract;
import com.code.wlu.cp470.wellnest.data.local.contracts.SnapTaskContract;
import com.code.wlu.cp470.wellnest.data.local.contracts.UserContract;

public class WellnestDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "wellnest.db";
    private static final int DATABASE_VERSION = 2;

    public WellnestDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // USER PROFILE DOMAIN
        db.execSQL(UserContract.UserProfile.SQL_CREATE);
        db.execSQL(UserContract.GlobalScore.SQL_CREATE);
        db.execSQL(UserContract.Streak.SQL_CREATE);
        db.execSQL(UserContract.Friends.SQL_CREATE);
        db.execSQL(UserContract.Badges.SQL_CREATE);
        db.execSQL(UserContract.Friends.SQL_INDEXES);

        // SNAP TASK DOMAIN
        db.execSQL(SnapTaskContract.Tasks.SQL_CREATE);
        db.execSQL(SnapTaskContract.SnapTask_Score.SQL_CREATE);

        //ROAMIO DOMAIN
        db.execSQL(RoamioContract.Walk_Sessions.SQL_CREATE);
        db.execSQL(RoamioContract.Current_Walk.SQL_CREATE);
        db.execSQL(RoamioContract.Roamio_Score.SQL_CREATE);
        db.execSQL(RoamioContract.Walk_Sessions.SQL_INDEXES);
        db.execSQL(RoamioContract.Current_Walk.SQL_INDEXES);

        //ACTIVITY JAR DOMAIN
        db.execSQL(ActivityJarContract.Activity_Jar_Score.SQL_CREATE);
        db.execSQL(ActivityJarContract.ActivityJarCache.SQL_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(ActivityJarContract.ActivityJarCache.SQL_CREATE);
        }
        // For future versions, add more if blocks or switch case
        // For development, if you want to wipe data on every upgrade, you can keep the old logic,
        // but typically onUpgrade should migrate data.
        // The previous implementation wiped everything, which is fine for dev but maybe not what we want long term.
        // However, to respect the existing pattern if it was intended for dev:
        /*
        // for development, just wipe the data
        db.beginTransaction();
        try {
            db.execSQL("DROP TABLE IF EXISTS " + UserContract.Badges.TABLE);
            // ... drop all other tables ...
            onCreate(db); // recreate tables
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        */
    }

    /**
     * This is a method used for testing to wipe the database of all tables
     *
     * @param db
     */
    public void cleanDatabase(SQLiteDatabase db) {
        db.beginTransaction();
        try (android.database.Cursor c = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_%' AND name NOT LIKE 'sqlite_%'", null)) {
            while (c.moveToNext()) {
                String table = c.getString(0);
                db.execSQL("DROP TABLE IF EXISTS " + table);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        onCreate(db);
    }
}
