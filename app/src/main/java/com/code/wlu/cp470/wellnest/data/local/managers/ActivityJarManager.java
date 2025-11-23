package com.code.wlu.cp470.wellnest.data.local.managers;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.code.wlu.cp470.wellnest.data.local.contracts.ActivityJarContract;

public class ActivityJarManager {
    private static final int SCORE_ROW_ID = 1;

    private final SQLiteDatabase db;

    public ActivityJarManager(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("db cannot be null");
        this.db = db;
        ensureSingletonRows();
    }

    // ----------------------------------------------------------------------
    // Bootstrap singleton score row: uid/int id = 1, score = 0
    // (Keep using whatever column your Contract defines, assumed integer)
    // ----------------------------------------------------------------------
    private void ensureSingletonRows() {
        db.beginTransaction();
        try {
            ContentValues ajs = new ContentValues();
            ajs.put(ActivityJarContract.Activity_Jar_Score.Col.UID, SCORE_ROW_ID);
            ajs.put(ActivityJarContract.Activity_Jar_Score.Col.SCORE, 0);
            db.insertWithOnConflict(
                    ActivityJarContract.Activity_Jar_Score.TABLE,
                    null,
                    ajs,
                    SQLiteDatabase.CONFLICT_IGNORE
            );
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public Integer getActivityJarScore() {
        Cursor c = null;
        try {
            c = db.query(
                    ActivityJarContract.Activity_Jar_Score.TABLE,
                    new String[]{ActivityJarContract.Activity_Jar_Score.Col.SCORE},
                    ActivityJarContract.Activity_Jar_Score.Col.UID + "=?",
                    new String[]{String.valueOf(SCORE_ROW_ID)},
                    null, null, null
            );
            if (!c.moveToFirst() || c.isNull(0)) return 0;
            return c.getInt(0);
        } finally {
            if (c != null) c.close();
        }
    }

    public boolean upsertRoamioScore(int score) {
        ContentValues cv = new ContentValues();
        cv.put(ActivityJarContract.Activity_Jar_Score.Col.SCORE, score);
        int rows = db.update(
                ActivityJarContract.Activity_Jar_Score.TABLE,
                cv,
                ActivityJarContract.Activity_Jar_Score.Col.UID + "=?",
                new String[]{String.valueOf(SCORE_ROW_ID)}
        );
        if (rows > 0) return true;

        cv.put(ActivityJarContract.Activity_Jar_Score.Col.UID, SCORE_ROW_ID);
        long id = db.insert(
                ActivityJarContract.Activity_Jar_Score.TABLE,
                null,
                cv
        );
        return id != -1L;
    }

    public int addToActivityJarScore(int delta) {
        db.beginTransaction();
        try {
            String sql = "UPDATE " + ActivityJarContract.Activity_Jar_Score.TABLE +
                    " SET " + ActivityJarContract.Activity_Jar_Score.Col.SCORE + " = " +
                    ActivityJarContract.Activity_Jar_Score.Col.SCORE + " + ? " +
                    " WHERE " + ActivityJarContract.Activity_Jar_Score.Col.UID + " = " + SCORE_ROW_ID;

            SQLiteStatement st = db.compileStatement(sql);
            st.bindLong(1, delta);
            int rows = st.executeUpdateDelete();

            if (rows == 0) {
                ContentValues cv = new ContentValues();
                cv.put(ActivityJarContract.Activity_Jar_Score.Col.UID, SCORE_ROW_ID);
                cv.put(ActivityJarContract.Activity_Jar_Score.Col.SCORE, delta);
                db.insertWithOnConflict(
                        ActivityJarContract.Activity_Jar_Score.TABLE,
                        null,
                        cv,
                        SQLiteDatabase.CONFLICT_IGNORE
                );
            }

            int newScore = getActivityJarScore();
            db.setTransactionSuccessful();
            return newScore;
        } finally {
            db.endTransaction();
        }
    }
}
