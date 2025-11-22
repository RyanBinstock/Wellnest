package com.code.wlu.cp470.wellnest.data;

public class ActivityJarModels {
    public static final class ActivityJarScore {
        private String uid;
        private int score;

        public ActivityJarScore(String uid, int score) {
            this.uid = uid;
            this.score = score;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }
    }
}
