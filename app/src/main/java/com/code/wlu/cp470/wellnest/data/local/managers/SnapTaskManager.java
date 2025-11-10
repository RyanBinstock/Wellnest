package com.code.wlu.cp470.wellnest.data.local.managers;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.code.wlu.cp470.wellnest.data.SnapTaskModels.Task;
import com.code.wlu.cp470.wellnest.data.local.contracts.SnapTaskContract;

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

    // ----------------------------------------------------------------------
    // tasks
    // ----------------------------------------------------------------------

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

    /** Convenience: load { uid, name, points, description, completed } as
     *  a structured object or null if missing.
     *  If both uid and name are given, uid is prioritized.
     */
    public Task getSnapTask(String uid, String name) {
        Cursor c = null;
        try {
            String selection = null;
            String[] selectionArgs = null;

            // Priority: UID > Name
            if (uid != null && !uid.isEmpty()) {
                selection = SnapTaskContract.Tasks.Col.UID + "=?";
                selectionArgs = new String[] {uid};
            } else if (name != null && !name.isEmpty()) {
                selection = SnapTaskContract.Tasks.Col.NAME + "=?";
                selectionArgs = new String[] {name};
            } else {
                // Neither provided --> nothing to query
                return null;
            }

            c = db.query(
                    SnapTaskContract.Tasks.TABLE,
                    new String[] {
                            SnapTaskContract.Tasks.Col.UID,
                            SnapTaskContract.Tasks.Col.NAME,
                            SnapTaskContract.Tasks.Col.POINTS,
                            SnapTaskContract.Tasks.Col.DESCRIPTION,
                            SnapTaskContract.Tasks.Col.COMPLETED
                    },
                    selection,
                    selectionArgs,
                    null, null, null
            );
            if (!c.moveToFirst()) return null; // no results --> return nothing

            return new Task(
                    c.getString(0),    // uid
                    c.getString(1),    // name
                    c.getInt(2),       // points
                    c.getString(3),    // description
                    c.getInt(4) != 0   // completed --> 0 = FALSE, 1 = TRUE
            );
        } finally {
            if (c != null) c.close();
        }
    }

    // ----------------------------------------------------------------------
    // snapTask_score
    // ----------------------------------------------------------------------

}
