package com.code.wlu.cp470.wellnest.data.local.contracts;

public class SnapTaskContract {

    private SnapTaskContract() {}

    // =========================
    //  snapTask_score  (singleton row)
    // =========================
    public static final class SnapTask_Score {
        private SnapTask_Score() {}

        public static final String TABLE = "snapTask_score";

        public static final class Col {
            public static final String UID = "id";           // INTEGER PK, forced to 1
            public static final String SCORE =  "score";    // INTEGER (active score)
        }

        public static final String SQL_CREATE =
                "CREATE TABLE " + TABLE + "(" +
                        Col.UID + " INTEGER PRIMARY KEY CHECK(" + Col.UID + "=1), " +
                        Col.SCORE + " INTEGER NOT NULL DEFAULT 0" +
                        ")";

        public static final String SQL_INDEXES = ""; // none needed
    }


    // =========================
    //  tasks (stored user's tasks)
    // =========================
    public static final class Tasks {
        private Tasks() {}

        public static final String TABLE = "tasks";

        public static final class Col {
            public static final String UID = "uid";                  // TEXT PK
            public static final String NAME =  "name";              // TEXT
            public static final String POINTS = "points";            // INTEGER
            public static final String DESCRIPTION = "description";  // TEXT
            public static final String COMPLETED = "completed";      // BOOLEAN
        }

        // Default description column to empty string
        // and completed column to FALSE
        public static final String SQL_CREATE =
                "CREATE TABLE " + TABLE + "(" +
                        Col.UID + " TEXT PRIMARY KEY, " +
                        Col.NAME + " TEXT NOT NULL, " +
                        Col.POINTS + " INTEGER NOT NULL, " +
                        Col.DESCRIPTION + " TEXT DEFAULT '', " +
                        Col.COMPLETED + " BOOLEAN NOT NULL DEFAULT 0" + //0 = FALSE, 1 = TRUE
                        ")";

        public static final String SQL_INDEXES = ""; // none needed for the moment
    }
}
