package com.code.wlu.cp470.wellnest.data.local.managers;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.code.wlu.cp470.wellnest.data.RoamioModels.WalkSession;
import com.code.wlu.cp470.wellnest.data.RoamioModels.CurrentWalk;

import com.code.wlu.cp470.wellnest.data.local.contracts.RoamioContract;

import java.util.ArrayList;
import java.util.List;

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
    // Singleton roamio_score (row id/uid = 1)
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

    public int addToRoamioScore(int delta) {
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

            int newScore = getRoamioScore();
            db.setTransactionSuccessful();
            return newScore;
        } finally {
            db.endTransaction();
        }
    }


    // ----------------------------------------------------------------------
    // Walk Sessions
    // ----------------------------------------------------------------------
    public WalkSession getWalkSession(String uid) {
        Cursor c = null;
        try {
            String sel;
            String[] args;
            if (uid != null && !uid.isEmpty()) {
                sel = RoamioContract.Walk_Sessions.Col.UID + "=?";
                args = new String[]{uid};
            } else {
                return null;
            }

            c = db.query(
                    RoamioContract.Walk_Sessions.TABLE,
                    new String[]{
                            RoamioContract.Walk_Sessions.Col.UID,
                            RoamioContract.Walk_Sessions.Col.STARTED_AT,
                            RoamioContract.Walk_Sessions.Col.ENDED_AT,
                            RoamioContract.Walk_Sessions.Col.STEPS,
                            RoamioContract.Walk_Sessions.Col.DISTANCE_METERS,
                            RoamioContract.Walk_Sessions.Col.POINTS_AWARDED,
                            RoamioContract.Walk_Sessions.Col.STATUS,
                    },
                    sel, args, null, null, null
            );
            if (!c.moveToFirst()) return null;

            return new WalkSession(
                    c.getString(0),
                    c.getInt(1),
                    c.getInt(2),
                    c.getInt(3),
                    c.getFloat(4),
                    c.getInt(5),
                    c.getString(6)
            );
        } finally {
            if (c != null) c.close();
        }
    }

    public boolean upsertWalkSession(String uid, int startedAt, int endedAt,
                                     int steps, float distanceMeters,
                                     int pointsAwarded, String status) {
        if (uid == null || uid.isEmpty())
            throw new IllegalArgumentException("task uid cannot be null/empty");

        ContentValues cv = new ContentValues();
        cv.put(RoamioContract.Walk_Sessions.Col.UID, uid);
        cv.put(RoamioContract.Walk_Sessions.Col.STARTED_AT, startedAt);
        cv.put(RoamioContract.Walk_Sessions.Col.ENDED_AT, endedAt);
        cv.put(RoamioContract.Walk_Sessions.Col.STEPS, steps);
        cv.put(RoamioContract.Walk_Sessions.Col.DISTANCE_METERS, distanceMeters);
        cv.put(RoamioContract.Walk_Sessions.Col.POINTS_AWARDED, pointsAwarded);
        cv.put(RoamioContract.Walk_Sessions.Col.STATUS, status.toUpperCase());

        int rows = db.update(
                RoamioContract.Walk_Sessions.TABLE,
                cv,
                RoamioContract.Walk_Sessions.Col.UID + "=?",
                new String[]{uid}
        );
        if (rows == 0) {
            long id = db.insertWithOnConflict(
                    RoamioContract.Walk_Sessions.TABLE,
                    null,
                    cv,
                    SQLiteDatabase.CONFLICT_ABORT
            );
            return id != -1;
        }
        return true;
    }

    public List<WalkSession> getWalks() {
        List<WalkSession> walks = new ArrayList<>();
        Cursor c = null;
        try {
            c = db.query(
                    RoamioContract.Walk_Sessions.TABLE,
                    new String[]{
                            RoamioContract.Walk_Sessions.Col.UID,
                            RoamioContract.Walk_Sessions.Col.STARTED_AT,
                            RoamioContract.Walk_Sessions.Col.ENDED_AT,
                            RoamioContract.Walk_Sessions.Col.STEPS,
                            RoamioContract.Walk_Sessions.Col.DISTANCE_METERS,
                            RoamioContract.Walk_Sessions.Col.POINTS_AWARDED,
                            RoamioContract.Walk_Sessions.Col.STATUS,
                    },
                    null, null, null, null, null
            );
            while (c.moveToNext()) {
                walks.add(new WalkSession(
                        c.getString(0),
                        c.getInt(1),
                        c.getInt(2),
                        c.getInt(3),
                        c.getFloat(4),
                        c.getInt(5),
                        c.getString(6)

                ));
            }

            return walks;
        } finally {
            if (c != null) c.close();
        }
    }

    public boolean setWalkSessionStatus(String uid, String status) {
        if (uid == null || uid.isEmpty())
            throw new IllegalArgumentException("walk uid cannot be null/empty");

        if (!status.equalsIgnoreCase("CANCELLED")
                && !status.equalsIgnoreCase("COMPLETED") ) {
            throw new IllegalArgumentException(
                    "walk status must be set to 'CANCELLED' or 'COMPLETED'");
        }
        ContentValues cv = new ContentValues();
        cv.put(RoamioContract.Walk_Sessions.Col.STATUS, status.toUpperCase());
        return db.update(
                RoamioContract.Walk_Sessions.TABLE,
                cv,
                RoamioContract.Walk_Sessions.Col.UID + "=?",
                new String[]{uid}
        ) > 0;
    }

    public boolean cancelWalk(String uid) {
        return setWalkSessionStatus(uid, "CANCELLED");
    }

    public boolean completeWalk(String uid) {
        return setWalkSessionStatus(uid, "COMPLETED");
    }

    // ----------------------------------------------------------------------
    // Current Walk
    // ----------------------------------------------------------------------
    public CurrentWalk getCurrentWalk(String uid) {
        Cursor c = null;
        try {
            String sel;
            String[] args;
            if (uid != null && !uid.isEmpty()) {
                sel = RoamioContract.Current_Walk.Col.UID + "=?";
                args = new String[]{uid};
            } else {
                return null;
            }

            c = db.query(
                    RoamioContract.Current_Walk.TABLE,
                    new String[]{
                            RoamioContract.Current_Walk.Col.UID,
                            RoamioContract.Current_Walk.Col.STATUS,
                            RoamioContract.Current_Walk.Col.STARTED_AT,
                            RoamioContract.Current_Walk.Col.START_STEP_COUNT,
                            RoamioContract.Current_Walk.Col.START_ELAPSED_REALTIME_MS,
                            RoamioContract.Current_Walk.Col.LAST_UPDATED_MS,
                            RoamioContract.Current_Walk.Col.LAST_KNOWN_STEPS,
                            RoamioContract.Current_Walk.Col.LAST_KNOWN_DISTANCE_METERS,
                    },
                    sel, args, null, null, null
            );
            if (!c.moveToFirst()) return null;

            return new CurrentWalk(
                    c.getString(0),
                    c.getString(1),
                    c.getInt(2),
                    c.getInt(3),
                    c.getInt(4),
                    c.getInt(5),
                    c.getInt(6),
                    c.getFloat(7)
            );
        } finally {
            if (c != null) c.close();
        }
    }

    public boolean upsertCurrentWalk(String uid, String status, int startedAt,
                             int startStepCount, int startElapsedRealtimeMs,
                             int lastUpdatedMs, int lastKnownSteps, float lastKnownDistanceMeters) {
        if (uid == null || uid.isEmpty()) {
            throw new IllegalArgumentException("Walk uid cannot be null/empty");
        }

        ContentValues cv = new ContentValues();
        cv.put(RoamioContract.Current_Walk.Col.UID, uid);
        cv.put(RoamioContract.Current_Walk.Col.STATUS, status);
        cv.put(RoamioContract.Current_Walk.Col.STARTED_AT, startedAt);
        cv.put(RoamioContract.Current_Walk.Col.START_STEP_COUNT, startStepCount);
        cv.put(RoamioContract.Current_Walk.Col.START_ELAPSED_REALTIME_MS, startElapsedRealtimeMs);
        cv.put(RoamioContract.Current_Walk.Col.LAST_UPDATED_MS, lastUpdatedMs);
        cv.put(RoamioContract.Current_Walk.Col.LAST_KNOWN_STEPS, lastKnownSteps);
        cv.put(RoamioContract.Current_Walk.Col.LAST_KNOWN_DISTANCE_METERS, lastKnownDistanceMeters);

        int rows = db.update(
                RoamioContract.Current_Walk.TABLE,
                cv,
                RoamioContract.Current_Walk.Col.UID + "=?",
                new String[]{uid}
        );

        if (rows == 0) {
            long id = db.insertWithOnConflict(
                    RoamioContract.Current_Walk.TABLE,
                    null,
                    cv,
                    SQLiteDatabase.CONFLICT_ABORT
            );
            return id != -1;
        }

        return true;
    }

    public boolean setCurrentWalkStatus(String uid, String status) {
        if (uid == null || uid.isEmpty())
            throw new IllegalArgumentException("walk uid cannot be null/empty");

        if (!status.equalsIgnoreCase("ACTIVE")
            && !status.equalsIgnoreCase("PAUSED")) {
            throw new IllegalArgumentException("walk status must be set to 'ACTIVE' or 'PAUSED'");
        }

        ContentValues cv = new ContentValues();
        cv.put(RoamioContract.Current_Walk.Col.STATUS, status.toUpperCase());
        return db.update(
                RoamioContract.Current_Walk.TABLE,
                cv,
                RoamioContract.Current_Walk.Col.UID + "=?",
                new String[]{uid}
        ) > 0;
    }

    public boolean setWalkActive(String uid) {
        return setCurrentWalkStatus(uid, "ACTIVE");
    }

    public boolean setWalkPaused(String uid) {
        return setCurrentWalkStatus(uid, "PAUSED");
    }
}
