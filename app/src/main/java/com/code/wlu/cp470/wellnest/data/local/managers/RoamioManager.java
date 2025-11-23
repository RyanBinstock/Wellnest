package com.code.wlu.cp470.wellnest.data.local.managers;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.code.wlu.cp470.wellnest.data.RoamioModels;
import com.code.wlu.cp470.wellnest.data.local.contracts.RoamioContract;

public class RoamioManager {
    private static final int SCORE_ROW_ID = 1;  // Singleton row key
    private final SQLiteDatabase db;

    public RoamioManager(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("db cannot be null");
        this.db = db;
        ensureSingletonRows();
    }

    // ----------------------------------------------------------------------
    // Bootstrap singleton score row: uid/int id = 1, score = 0
    // ----------------------------------------------------------------------
    private void ensureSingletonRows() {
        db.beginTransaction();
        try {
            ContentValues rs = new ContentValues();
            rs.put(RoamioContract.Roamio_Score.Col.UID, SCORE_ROW_ID);
            rs.put(RoamioContract.Roamio_Score.Col.SCORE, 0);
            db.insertWithOnConflict(
                    RoamioContract.Roamio_Score.TABLE,
                    null,
                    rs,
                    SQLiteDatabase.CONFLICT_IGNORE
            );
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // ----------------------------------------------------------------------
    // Singleton roamio_score (row id/uid = 1)
    // ----------------------------------------------------------------------
    public RoamioModels.RoamioScore getRoamioScore() {
        RoamioModels.RoamioScore score = new RoamioModels.RoamioScore(String.valueOf(SCORE_ROW_ID), 0);
        Cursor c = null;
        try {
            c = db.query(
                    RoamioContract.Roamio_Score.TABLE,
                    new String[]{RoamioContract.Roamio_Score.Col.SCORE},
                    RoamioContract.Roamio_Score.Col.UID + "=?",
                    new String[]{String.valueOf(SCORE_ROW_ID)},
                    null, null, null
            );
            if (!c.moveToFirst() || c.isNull(0)) {
                score.setScore(0);
                return score;
            }
            score.setScore(c.getInt(0));
            return score;
        } finally {
            if (c != null) c.close();
        }

    }

    public boolean upsertRoamioScore(int score) {
        ContentValues cv = new ContentValues();
        cv.put(RoamioContract.Roamio_Score.Col.SCORE, score);
        int rows = db.update(
                RoamioContract.Roamio_Score.TABLE,
                cv,
                RoamioContract.Roamio_Score.Col.UID + "=?",
                new String[]{String.valueOf(SCORE_ROW_ID)}
        );
        if (rows > 0) return true;

        cv.put(RoamioContract.Roamio_Score.Col.UID, SCORE_ROW_ID);
        long id = db.insert(RoamioContract.Roamio_Score.TABLE, null, cv);
        return id != -1L;
    }

    public void addToRoamioScore(int delta) {
        db.beginTransaction();
        try {
            // UPDATE score = score + ?
            String sql = "UPDATE " + RoamioContract.Roamio_Score.TABLE +
                    " SET " + RoamioContract.Roamio_Score.Col.SCORE + " = " +
                    RoamioContract.Roamio_Score.Col.SCORE + " + ? " +
                    " WHERE " + RoamioContract.Roamio_Score.Col.UID + " = " + SCORE_ROW_ID;

            SQLiteStatement st = db.compileStatement(sql);
            st.bindLong(1, delta);
            int rows = st.executeUpdateDelete();

            if (rows == 0) { //Row missing -> insert starting at delta
                ContentValues cv = new ContentValues();
                cv.put(RoamioContract.Roamio_Score.Col.UID, SCORE_ROW_ID);
                cv.put(RoamioContract.Roamio_Score.Col.SCORE, delta);
                db.insertWithOnConflict(
                        RoamioContract.Roamio_Score.TABLE,
                        null,
                        cv,
                        SQLiteDatabase.CONFLICT_IGNORE
                );
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
