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
            public static final String ID = "id";           // INTEGER PK, forced to 1
            public static final String SCORE =  "score";    // INTEGER (active score)
        }

        public static final String SQL_CREATE =
                "CREATE TABLE " + TABLE + "(" +
                        Col.ID + " INTEGER PRIMARY KEY CHECK(" + Col.ID + "=1), " +
                        Col.SCORE + " INTEGER NOT NULL DEFAULT 0" +
                        ")";

        public static final String SQL_INDEXES = ""; // none needed
    }



}
