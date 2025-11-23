package com.code.wlu.cp470.wellnest.data;

public class RoamioModels {
    public static final class Walk {
        private String uid;
        private String name;
        private String story;
        private String start_address;
        private String end_address;
        private float distanceMeters;
        private boolean completed;


        public Walk(
                String uid,
                String name,
                String story,
                String start_address,
                String end_address,
                float distanceMeters,
                boolean completed) {
            this.uid = uid;
            this.name = name;
            this.story = story;
            this.start_address = start_address;
            this.end_address = end_address;
            this.distanceMeters = distanceMeters;
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

        public String getStory() {
            return story;
        }

        public void setStory(String story) {
            this.story = story;
        }

        public String getStartAddress() {
            return start_address;
        }

        public void setStartAddress(String address) {
            this.start_address = address;
        }

        public String getEndAddress() {
            return end_address;
        }

        public void setEndAddress(String address) {
            this.end_address = address;
        }

        public float getDistanceMeters() {
            return distanceMeters;
        }

        public void setDistanceMeters(float distanceMeters) {
            this.distanceMeters = distanceMeters;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
    }

    public static final class RoamioScore {

        private String uid;
        private int score;

        public RoamioScore(String uid, int score) {
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
