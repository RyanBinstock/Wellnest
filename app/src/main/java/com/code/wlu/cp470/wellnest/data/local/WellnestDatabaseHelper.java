package com.code.wlu.cp470.wellnest.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.code.wlu.cp470.wellnest.data.local.contracts.UserContract;

public class WellnestDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "wellnest.db";
    private static final int DATABASE_VERSION = 1;

    public WellnestDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(UserContract.UserProfile.SQL_CREATE);
        db.execSQL(UserContract.GlobalScore.SQL_CREATE);
        db.execSQL(UserContract.Streak.SQL_CREATE);
        db.execSQL(UserContract.Friends.SQL_CREATE);
        db.execSQL(UserContract.Badges.SQL_CREATE);
        db.execSQL(UserContract.Friends.SQL_INDEXES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // for development, just wipe the data
        db.beginTransaction();
        try {
            db.execSQL("DROP TABLE IF EXISTS " + UserContract.Badges.TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + UserContract.Friends.TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + UserContract.Streak.TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + UserContract.GlobalScore.TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + UserContract.UserProfile.TABLE);

            onCreate(db); // recreate tables
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
