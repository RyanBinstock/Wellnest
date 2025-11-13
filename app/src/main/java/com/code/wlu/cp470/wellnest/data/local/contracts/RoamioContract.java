package com.code.wlu.cp470.wellnest.data.local.contracts;

public class RoamioContract {
    private RoamioContract() {}

    // =========================
    //  roamio_score  (singleton row)
    // =========================

    public static final class Roamio_Score {
        private Roamio_Score() {}

        public static final String TABLE = "roamio_score";

        public static final class Col {
            public static final String UID = "id";        // INTEGER PK, forced to 1
            public static final String SCORE = "score";   // INTEGER (active score)
        }

        public static final String SQL_CREATE =
                "CREATE TABLE " + TABLE + "(" +
                        Col.UID + " INTEGER PRIMARY KEY CHECK(" + Col.UID + "=1), " +
                        Col.SCORE + " INTEGER NOT NULL DEFAULT 0" +
                        ")";
    }

    // =========================
    //  walk_session
    // =========================
    public static final class Walk_Sessions {
        private Walk_Sessions() {}

        public static final String TABLE = "walk_sessions";

        public static final class Col {
            public static final String UID = "id";                // TEXT PK
            public static final String STARTED_AT = "startedAt";  // INTEGER
            public static final String ENDED_AT = "endedAt";      // INTEGER
            public static final String STEPS = "steps";           //steps INTEGER
            public static final String DISTANCE_METERS =
                    "distanceMeters";                             // REAL (floating point number)
            public static final String POINTS_AWARDED =
                    "pointsAwarded";                              // INTEGER
            public static final String STATUS = "status";         // TEXT ("COMPLETED" or "CANCELLED")
        }

        public static final String SQL_CREATE =
                "CREATE TABLE " + TABLE + "(" +
                        Col.UID + " TEXT PRIMARY KEY, " +
                        Col.STARTED_AT + " INTEGER NOT NULL, " +
                        Col.ENDED_AT + " INTEGER, " +
                        Col.STEPS + " INTEGER DEFAULT 0, " +
                        Col.DISTANCE_METERS + " REAL DEFAULT 0," +
                        Col.POINTS_AWARDED + " INTEGER NOT NULL, " +
                        Col.STATUS + " TEXT CHECK(" + Col.STATUS + " IN ('COMPLETED', 'CANCELLED'))" +
                        ")";

        public static final String SQL_INDEXES =
                "CREATE INDEX IF NOT EXISTS idx_walk_hist_time ON " + TABLE +
                        "(" + Col.ENDED_AT + ")";
    }

    // =========================
    //  current_walk
    // =========================
    public static final class Current_Walk {
        private Current_Walk() {}

        public static final String TABLE = "current_walk";

        public static final class Col {
            public static final String UID = "uid";
            public static final String STATUS = "status";
            public static final String STARTED_AT = "startedAt";
            public static final String START_STEP_COUNT
                    = "startStepCount";
            public static final String START_ELAPSED_REALTIME_MS
                    = "startElapsedRealtimeMs";
            public static final String LAST_UPDATED_MS
                    = "lastUpdatedMs";
            public static final String LAST_KNOWN_STEPS
                    = "lastKnownSteps";
            public static final String LAST_KNOWN_DISTANCE_METERS
                    = "lastKnownDistanceMeters";
        }

        public static final String SQL_CREATE =
                "CREATE TABLE " + TABLE + "(" +
                        Col.UID + " TEXT PRIMARY KEY, " +
                        Col.STATUS + " TEXT NOT NULL CHECK(" + Col.STATUS + " IN ('ACTIVE', 'PAUSED')), " +
                        Col.STARTED_AT + " INTEGER NOT NULL, " +
                        Col.START_STEP_COUNT + " INTEGER, " +
                        Col.START_ELAPSED_REALTIME_MS + " INTEGER, " +
                        Col.LAST_UPDATED_MS + " INTEGER, " +
                        Col.LAST_KNOWN_STEPS + " INTEGER, " +
                        Col.LAST_KNOWN_DISTANCE_METERS + " REAL" +
                        ")";

        public static final String SQL_INDEXES =
                "CREATE INDEX IF NOT EXISTS idx_current_walk_updated ON " + TABLE +
                        "(" + Col.LAST_UPDATED_MS + ")";
    }
}
