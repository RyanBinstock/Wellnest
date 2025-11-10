package com.code.wlu.cp470.wellnest.data.local.managers;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.code.wlu.cp470.wellnest.data.local.contracts.SnapTaskContract;
import com.code.wlu.cp470.wellnest.data.local.contracts.UserContract;

public class SnapTaskManager {
    private final SQLiteDatabase db;

    public SnapTaskManager(SQLiteDatabase db) {
        this.db = db;
    }

    // ----------------------------------------------------------------------
    // Bootstrap: make sure snapTask_score (id=1) exists
    // ----------------------------------------------------------------------
    private void ensureSingletonRows() {
        db.beginTransaction();
        try {
            // snapTask_score -> id=1 default 0
            ContentValues sts = new ContentValues();
            sts.put(SnapTaskContract.SnapTask_Score.Col.ID, 1);
            sts.put(SnapTaskContract.SnapTask_Score.Col.SCORE, 0);
            db.insertWithOnConflict(
                    SnapTaskContract.SnapTask_Score.TABLE,
                    null,
                    sts,
                    SQLiteDatabase.CONFLICT_IGNORE
            );
        } finally {
            db.endTransaction();
        }
    }

    // Upsert by Firebase UID. Returns true if insert or update affected a row.
    public boolean upsertTask(String uid, String name, int points,
                              String description, Boolean completed) {
        if (uid == null) throw new IllegalArgumentException("uid cannot be null");

        ContentValues cv = new ContentValues();
        cv.put(SnapTaskContract.Tasks.Col.UID, uid);
        cv.put(SnapTaskContract.Tasks.Col.NAME, name);
        cv.put(SnapTaskContract.Tasks.Col.POINTS, points);
        cv.put(SnapTaskContract.Tasks.Col.DESCRIPTION, description);
        cv.put(SnapTaskContract.Tasks.Col.COMPLETED, completed);

        // Try update first
        int rows = db.update(
                SnapTaskContract.Tasks.TABLE,
                cv,
                SnapTaskContract.Tasks.Col.UID + "=?",
                new String[] { uid }
        );

        if (rows == 0) { //Row didn't exist, we must insert
            long id = db.insertWithOnConflict(
                    SnapTaskContract.Tasks.TABLE,
                    null,
                    cv,
                    SQLiteDatabase.CONFLICT_ABORT
            );
            return id != -1L;
        }

        return true;
    }


}
