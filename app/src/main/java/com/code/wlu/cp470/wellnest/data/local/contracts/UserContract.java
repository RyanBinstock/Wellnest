package com.code.wlu.cp470.wellnest.data.local.contracts;

public final class UserContract {
    private UserContract() {
    }

    // =========================
    //  user_profile  (singleton by usage; PK = Firebase UID)
    // =========================
    public static final class UserProfile {
        public static final String TABLE = "user_profile";
        // TEXT PRIMARY KEY to mirror Firebase UID; WITHOUT ROWID saves space
        public static final String SQL_CREATE =
                "CREATE TABLE " + TABLE + " (" +
                        Col.UID + " TEXT PRIMARY KEY, " +
                        Col.NAME + " TEXT NOT NULL, " +
                        Col.EMAIL + " TEXT" +
                        ") WITHOUT ROWID";

        private UserProfile() {
        }

        public static final class Col {
            public static final String UID = "uid";          // TEXT PK (Firebase UID)
            public static final String NAME = "name";         // TEXT
            public static final String EMAIL = "email";        // TEXT
        }
    }

    // =========================
    //  global_score  (singleton row)
    // =========================
    public static final class GlobalScore {
        public static final String TABLE = "global_score";
        // CHECK(id=1) enforces single row; default score = 0
        public static final String SQL_CREATE =
                "CREATE TABLE " + TABLE + " (" +
                        Col.ID + " INTEGER PRIMARY KEY CHECK(" + Col.ID + "=1), " +
                        Col.SCORE + " INTEGER NOT NULL DEFAULT 0" +
                        ")";
        public static final String SQL_INDEXES = ""; // none needed

        private GlobalScore() {
        }

        public static final class Col {
            public static final String ID = "id";           // INTEGER PK, forced to 1
            public static final String SCORE = "score";        // INTEGER
        }
    }

    // =========================
    //  streak  (singleton row)
    // =========================
    public static final class Streak {
        public static final String TABLE = "streak";
        public static final String SQL_CREATE =
                "CREATE TABLE " + TABLE + " (" +
                        Col.ID + " INTEGER PRIMARY KEY CHECK(" + Col.ID + "=1), " +
                        Col.COUNT + " INTEGER NOT NULL DEFAULT 0" +
                        ")";
        public static final String SQL_INDEXES = ""; // none needed

        private Streak() {
        }

        public static final class Col {
            public static final String ID = "id";          // INTEGER PK, forced to 1
            public static final String COUNT = "count";       // INTEGER (active streak)
        }
    }

    // =========================
    //  friends  (friend list; Firebase UID + name)
    // =========================
    public static final class Friends {
        public static final String TABLE = "friends";
        public static final String SQL_CREATE =
                "CREATE TABLE " + TABLE + " (" +
                        Col.FRIEND_UID + " TEXT PRIMARY KEY, " +
                        Col.FRIEND_NAME + " TEXT NOT NULL, " +
                        Col.FRIEND_STATUS + " TEXT DEFAULT 'pending'" +
                        ") WITHOUT ROWID";
        public static final String SQL_INDEXES =
                "CREATE INDEX IF NOT EXISTS idx_friend_name ON " + TABLE + "(" + Col.FRIEND_NAME + ")";

        private Friends() {
        }

        public static final class Col {
            public static final String FRIEND_UID = "friend_uid";  // TEXT PK (their Firebase UID)
            public static final String FRIEND_NAME = "friend_name"; // TEXT (pulled from Firebase)
            public static final String FRIEND_STATUS = "friend_status"; // TEXT
        }
    }

    // =========================
    //  badges  (earned badge ids; from Firebase)
    // =========================
    public static final class Badges {
        public static final String TABLE = "badges";
        public static final String SQL_CREATE =
                "CREATE TABLE " + TABLE + " (" +
                        Col.BADGE_ID + " TEXT PRIMARY KEY" +
                        ") WITHOUT ROWID";
        public static final String SQL_INDEXES = ""; // none needed

        private Badges() {
        }

        public static final class Col {
            public static final String BADGE_ID = "badge_uid";  // TEXT PK (matches Firebase badge id)
        }
    }
}
