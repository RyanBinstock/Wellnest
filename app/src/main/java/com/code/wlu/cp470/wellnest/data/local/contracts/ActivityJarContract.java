package com.code.wlu.cp470.wellnest.data.local.contracts;

public class ActivityJarContract {
    private ActivityJarContract() {}

    // =========================
    //  activtiy_jar_score  (singleton row)
    // =========================

    public static final class Activity_Jar_Score {
        private Activity_Jar_Score() {}

        public static final String TABLE = "activity_jar_score";

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
}
