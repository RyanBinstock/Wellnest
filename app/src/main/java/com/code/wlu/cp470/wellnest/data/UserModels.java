package com.code.wlu.cp470.wellnest.data;

public final class UserModels {
    public static final class Friend {
        private String uid;
        private String name;
        private String status;
        private int score;

        public Friend(String uid, String name, String status, int score) {
            this.uid = uid;
            this.name = name;
            this.status = status;
            this.score = score;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }
    }

    public static final class UserProfile {
        private String uid;
        private String name;
        private String email;

        public UserProfile(String uid, String name, String email) {
            this.uid = uid;
            this.name = name;
            this.email = email;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static final class Score {
        private String uid;
        private int score;

        public Score(String uid, int score) {
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
