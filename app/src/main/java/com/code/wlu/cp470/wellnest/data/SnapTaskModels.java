package com.code.wlu.cp470.wellnest.data;

public class SnapTaskModels {
    public static final class Task {
        private String uid;
        private String name;
        private int points;
        private String description;
        private Boolean completed;

        public Task(String uid, String name, int points,
                    String description, Boolean completed) {
            this.uid = uid;
            this.name = name;
            this.points = points;
            this.description = description;
            this.completed = completed;
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

        public int getPoints() {
            return points;
        }

        public void setPoints(int points) {
            this.points = points;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Boolean getCompleted() {
            return completed;
        }

        public void setCompleted(Boolean completed) {
            this.completed = completed;
        }
    }

    public static final class SnapTaskScore {
        private String uid;
        private int score;

        public SnapTaskScore(String uid, int score) {
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
