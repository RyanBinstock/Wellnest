package com.code.wlu.cp470.wellnest.data.local.managers;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.code.wlu.cp470.wellnest.data.local.contracts.RoamioContract;
import com.code.wlu.cp470.wellnest.data.local.contracts.SnapTaskContract;

public class RoamioManager {
    private static final int SCORE_ROW_ID = 1;  // Singleton row key
    private final SQLiteDatabase db;

    public RoamioManager(SQLiteDatabase db) {
        this.db = db;
    }

    // ----------------------------------------------------------------------
    // Bootstrap singleton score row: uid/int id = 1, score = 0
    // (Keep using whatever column your Contract defines, assumed integer)
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
    // Singleton roamio_core (row id/uid = 1)
    // ----------------------------------------------------------------------
    public Integer getRoamioScore() {
        Cursor c = null;
        try {
            c = db.query(
                    RoamioContract.Roamio_Score.TABLE,
                    new String[]{RoamioContract.Roamio_Score.Col.SCORE},
                    RoamioContract.Roamio_Score.Col.UID + "=?",
                    new String[]{String.valueOf(SCORE_ROW_ID)},
                    null, null, null
            );
            if (!c.moveToFirst() || c.isNull(0)) return 0;
            return c.getInt(0);
        } finally {
            if (c!= null) c.close();
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

}
