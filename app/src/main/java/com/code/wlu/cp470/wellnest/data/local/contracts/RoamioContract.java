package com.code.wlu.cp470.wellnest.data.local.contracts;

public class RoamioContract {
    private RoamioContract() {
    }

    // =========================
    //  roamio_score  (singleton row)
    // =========================

    public static final class Roamio_Score {
        public static final String TABLE = "roamio_score";
        public static final String SQL_CREATE =
                "CREATE TABLE " + TABLE + "(" +
                        Col.UID + " INTEGER PRIMARY KEY CHECK(" + Col.UID + "=1), " +
                        Col.SCORE + " INTEGER NOT NULL DEFAULT 0" +
                        ")";

        private Roamio_Score() {
        }

        public static final class Col {
            public static final String UID = "id";        // INTEGER PK, forced to 1
            public static final String SCORE = "score";   // INTEGER (active score)
        }
    }

    // =========================
    //  walk_sessions (completed walks)
    // =========================

    public static final class Walk_Sessions {
        public static final String TABLE = "walk_sessions";

        public static final String SQL_CREATE =
                "CREATE TABLE " + TABLE + "(" +
                        Col._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        Col.UID + " TEXT NOT NULL, " +
                        Col.STARTED_AT + " INTEGER NOT NULL, " +
                        Col.ENDED_AT + " INTEGER NOT NULL, " +
                        Col.STEPS + " INTEGER NOT NULL DEFAULT 0, " +
                        Col.DISTANCE_METERS + " REAL NOT NULL DEFAULT 0.0, " +
                        Col.POINTS_AWARDED + " INTEGER NOT NULL DEFAULT 0, " +
                        Col.STATUS + " TEXT NOT NULL DEFAULT 'COMPLETED'" +
                        ")";

        public static final String SQL_INDEXES =
                "CREATE INDEX idx_walk_sessions_uid ON " + TABLE + "(" + Col.UID + "); " +
                "CREATE INDEX idx_walk_sessions_ended_at ON " + TABLE + "(" + Col.ENDED_AT + ");";

        private Walk_Sessions() {
        }

        public static final class Col {
            public static final String _ID = "_id";
            public static final String UID = "uid";
            public static final String STARTED_AT = "started_at";
            public static final String ENDED_AT = "ended_at";
            public static final String STEPS = "steps";
            public static final String DISTANCE_METERS = "distance_meters";
            public static final String POINTS_AWARDED = "points_awarded";
            public static final String STATUS = "status";
        }
    }

    // =========================
    //  current_walk (active walk state)
    // =========================

    public static final class Current_Walk {
        public static final String TABLE = "current_walk";

        public static final String SQL_CREATE =
                "CREATE TABLE " + TABLE + "(" +
                        Col._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        Col.UID + " TEXT NOT NULL, " +
                        Col.STATUS + " TEXT NOT NULL DEFAULT 'INACTIVE', " +
                        Col.STARTED_AT + " INTEGER NOT NULL, " +
                        Col.START_STEP_COUNT + " INTEGER NOT NULL DEFAULT 0, " +
                        Col.START_ELAPSED_REALTIME_MS + " INTEGER NOT NULL DEFAULT 0, " +
                        Col.LAST_UPDATED_MS + " INTEGER NOT NULL DEFAULT 0, " +
                        Col.LAST_KNOWN_STEPS + " INTEGER NOT NULL DEFAULT 0, " +
                        Col.LAST_KNOWN_DISTANCE_METERS + " REAL NOT NULL DEFAULT 0.0" +
                        ")";

        public static final String SQL_INDEXES =
                "CREATE INDEX idx_current_walk_uid ON " + TABLE + "(" + Col.UID + ");";

        private Current_Walk() {
        }

        public static final class Col {
            public static final String _ID = "_id";
            public static final String UID = "uid";
            public static final String STATUS = "status";
            public static final String STARTED_AT = "started_at";
            public static final String START_STEP_COUNT = "start_step_count";
            public static final String START_ELAPSED_REALTIME_MS = "start_elapsed_realtime_ms";
            public static final String LAST_UPDATED_MS = "last_updated_ms";
            public static final String LAST_KNOWN_STEPS = "last_known_steps";
            public static final String LAST_KNOWN_DISTANCE_METERS = "last_known_distance_meters";
        }
    }
}
