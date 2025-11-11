package com.code.wlu.cp470.wellnest.data.local.managers;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.code.wlu.cp470.wellnest.data.SnapTaskModels.Task;
import com.code.wlu.cp470.wellnest.data.local.contracts.SnapTaskContract;

import java.util.ArrayList;
import java.util.List;

public class SnapTaskManager {
    private static final int SCORE_ROW_ID = 1; // singleton row key
    private final SQLiteDatabase db;

    public SnapTaskManager(SQLiteDatabase db) {
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
            ContentValues sts = new ContentValues();
            sts.put(SnapTaskContract.SnapTask_Score.Col.UID, SCORE_ROW_ID);
            sts.put(SnapTaskContract.SnapTask_Score.Col.SCORE, 0);
            db.insertWithOnConflict(
                    SnapTaskContract.SnapTask_Score.TABLE,
                    null,
                    sts,
                    SQLiteDatabase.CONFLICT_IGNORE
            );
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // ----------------------------------------------------------------------
    // Tasks (uid == task id)
    // ----------------------------------------------------------------------
    public boolean upsertTask(String uid, String name, int points,
                              String description, Boolean completed) {
        if (uid == null || uid.isEmpty())
            throw new IllegalArgumentException("task uid cannot be null/empty");

        ContentValues cv = new ContentValues();
        cv.put(SnapTaskContract.Tasks.Col.UID, uid);
        cv.put(SnapTaskContract.Tasks.Col.NAME, name);
        cv.put(SnapTaskContract.Tasks.Col.POINTS, points);
        cv.put(SnapTaskContract.Tasks.Col.DESCRIPTION, description);
        cv.put(SnapTaskContract.Tasks.Col.COMPLETED, (completed != null && completed) ? 1 : 0);

        int rows = db.update(
                SnapTaskContract.Tasks.TABLE,
                cv,
                SnapTaskContract.Tasks.Col.UID + "=?",
                new String[]{uid}
        );
        if (rows == 0) {
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

    /**
     * Get by task uid (preferred). If name given but uid empty, falls back to name.
     */
    public Task getSnapTask(String uid, String name) {
        Cursor c = null;
        try {
            String sel;
            String[] args;
            if (uid != null && !uid.isEmpty()) {
                sel = SnapTaskContract.Tasks.Col.UID + "=?";
                args = new String[]{uid};
            } else if (name != null && !name.isEmpty()) {
                sel = SnapTaskContract.Tasks.Col.NAME + "=?";
                args = new String[]{name};
            } else {
                return null;
            }

            c = db.query(
                    SnapTaskContract.Tasks.TABLE,
                    new String[]{
                            SnapTaskContract.Tasks.Col.UID,
                            SnapTaskContract.Tasks.Col.NAME,
                            SnapTaskContract.Tasks.Col.POINTS,
                            SnapTaskContract.Tasks.Col.DESCRIPTION,
                            SnapTaskContract.Tasks.Col.COMPLETED
                    },
                    sel, args, null, null, null
            );
            if (!c.moveToFirst()) return null;

            return new Task(
                    c.getString(0),
                    c.getString(1),
                    c.getInt(2),
                    c.getString(3),
                    c.getInt(4) != 0
            );
        } finally {
            if (c != null) c.close();
        }
    }

    public List<Task> getTasks() {
        List<Task> tasks = new ArrayList<>();
        Cursor c = null;
        try {
            c = db.query(
                    SnapTaskContract.Tasks.TABLE,
                    new String[]{
                            SnapTaskContract.Tasks.Col.UID,
                            SnapTaskContract.Tasks.Col.NAME,
                            SnapTaskContract.Tasks.Col.POINTS,
                            SnapTaskContract.Tasks.Col.DESCRIPTION,
                            SnapTaskContract.Tasks.Col.COMPLETED
                    },
                    null, null, null, null, null
            );
            while (c.moveToNext()) {
                tasks.add(new Task(
                        c.getString(0),
                        c.getString(1),
                        c.getInt(2),
                        c.getString(3),
                        c.getInt(4) != 0
                ));
            }
            return tasks;
        } finally {
            if (c != null) c.close();
        }
    }

    public boolean setTaskCompleted(String uid) {
        if (uid == null || uid.isEmpty())
            throw new IllegalArgumentException("task uid cannot be null/empty");
        ContentValues cv = new ContentValues();
        cv.put(SnapTaskContract.Tasks.Col.COMPLETED, true);
        return db.update(
                SnapTaskContract.Tasks.TABLE,
                cv,
                SnapTaskContract.Tasks.Col.UID + "=?",
                new String[]{uid}
        ) > 0;
    }

    // ----------------------------------------------------------------------
    // Singleton snapTask_score (row id/uid = 1)
    // ----------------------------------------------------------------------
    public Integer getSnapTaskScore() {
        Cursor c = null;
        try {
            c = db.query(
                    SnapTaskContract.SnapTask_Score.TABLE,
                    new String[]{SnapTaskContract.SnapTask_Score.Col.SCORE},
                    SnapTaskContract.SnapTask_Score.Col.UID + "=?",
                    new String[]{String.valueOf(SCORE_ROW_ID)},
                    null, null, null
            );
            if (!c.moveToFirst() || c.isNull(0)) return 0; // default 0
            return c.getInt(0);
        } finally {
            if (c != null) c.close();
        }
    }

    public boolean upsertSnapTaskScore(int score) {
        ContentValues cv = new ContentValues();
        cv.put(SnapTaskContract.SnapTask_Score.Col.SCORE, score);
        int rows = db.update(
                SnapTaskContract.SnapTask_Score.TABLE,
                cv,
                SnapTaskContract.SnapTask_Score.Col.UID + "=?",
                new String[]{String.valueOf(SCORE_ROW_ID)}
        );
        if (rows > 0) return true;

        cv.put(SnapTaskContract.SnapTask_Score.Col.UID, SCORE_ROW_ID);
        long id = db.insert(SnapTaskContract.SnapTask_Score.TABLE, null, cv);
        return id != -1L;
    }

    /**
     * Atomic add using UPDATE; inserts row if missing, returns new score.
     */
    public int addToSnapTaskScore(int delta) {
        db.beginTransaction();
        try {
            // UPDATE score = score + ?
            String sql = "UPDATE " + SnapTaskContract.SnapTask_Score.TABLE +
                    " SET " + SnapTaskContract.SnapTask_Score.Col.SCORE + " = " +
                    SnapTaskContract.SnapTask_Score.Col.SCORE + " + ? " +
                    " WHERE " + SnapTaskContract.SnapTask_Score.Col.UID + " = " + SCORE_ROW_ID;

            SQLiteStatement st = db.compileStatement(sql);
            st.bindLong(1, delta);
            int rows = st.executeUpdateDelete();

            if (rows == 0) { // row missing → insert starting at delta
                ContentValues cv = new ContentValues();
                cv.put(SnapTaskContract.SnapTask_Score.Col.UID, SCORE_ROW_ID);
                cv.put(SnapTaskContract.SnapTask_Score.Col.SCORE, delta);
                db.insertWithOnConflict(
                        SnapTaskContract.SnapTask_Score.TABLE,
                        null,
                        cv,
                        SQLiteDatabase.CONFLICT_IGNORE
                );
            }

            // Read back new value
            int newScore = getSnapTaskScore();
            db.setTransactionSuccessful();
            return newScore;
        } finally {
            db.endTransaction();
        }
    }
}
