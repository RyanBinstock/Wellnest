package com.code.wlu.cp470.wellnest.data;

public class RoamioModels {
    public static final class WalkSession {
        private String uid;
        private int startedAt;
        private int endedAt;
        private int steps;
        private float distanceMeters;
        private int pointsAwarded;
        private String status;

        public WalkSession(
                String uid,
                int startedAt,
                int endedAt,
                int steps,
                float distanceMeters,
                int pointsAwarded,
                String status) {
            this.uid = uid;
            this.startedAt = startedAt;
            this.endedAt = endedAt;
            this.steps = steps;
            this.distanceMeters = distanceMeters;
            this.pointsAwarded = pointsAwarded;
            this.status = status;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public int getStartedAt() {
            return startedAt;
        }

        public void setStartedAt(int startedAt) {
            this.startedAt = startedAt;
        }

        public int getEndedAt() {
            return endedAt;
        }

        public void setEndedAt(int endedAt) {
            this.endedAt = endedAt;
        }

        public int getSteps() {
            return steps;
        }

        public void setSteps(int steps) {
            this.steps = steps;
        }

        public float getDistanceMeters() {
            return distanceMeters;
        }

        public void setDistanceMeters(float distanceMeters) {
            this.distanceMeters = distanceMeters;
        }

        public int getPointsAwarded() {
            return pointsAwarded;
        }

        public void setPointsAwarded(int pointsAwarded) {
            this.pointsAwarded = pointsAwarded;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus() {
            this.status = status;
        }
    }

    public static final class CurrentWalk {
        private String uid;
        private String status;
        private int startedAt;
        private int startStepCount;
        private int startElapsedRealtimeMs;
        private int lastUpdatedMs;
        private int lastKnownSteps;
        private float lastKnownDistanceMeters;    // Last known distance in meters

        public CurrentWalk(
            String uid,
            String status,
            int startedAt,
            int startStepCount,
            int startElapsedRealtimeMs,
            int lastUpdatedMs,
            int lastKnownSteps,
            float lastKnownDistanceMeters) {
            this.uid = uid;
            this.status = status;
            this.startedAt = startedAt;
            this.startStepCount = startStepCount;
            this.startElapsedRealtimeMs = startElapsedRealtimeMs;
            this.lastUpdatedMs = lastUpdatedMs;
            this.lastKnownSteps = lastKnownSteps;
            this.lastKnownDistanceMeters = lastKnownDistanceMeters;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getStartedAt() {
            return startedAt;
        }

        public void setStartedAt(int startedAt) {
            this.startedAt = startedAt;
        }

        public int getStartStepCount() {
            return startStepCount;
        }

        public void setStartStepCount(int startStepCount) {
            this.startStepCount = startStepCount;
        }

        public int getStartElapsedRealtimeMs() {
            return startElapsedRealtimeMs;
        }

        public void setStartElapsedRealtimeMs(int startElapsedRealtimeMs) {
            this.startElapsedRealtimeMs = startElapsedRealtimeMs;
        }

        public int getLastUpdatedMs() {
            return lastUpdatedMs;
        }

        public void setLastUpdatedMs(int lastUpdatedMs) {
            this.lastUpdatedMs = lastUpdatedMs;
        }

        public int getLastKnownSteps() {
            return lastKnownSteps;
        }

        public void setLastKnownSteps(int lastKnownSteps) {
            this.lastKnownSteps = lastKnownSteps;
        }

        public float getLastKnownDistanceMeters() {
            return lastKnownDistanceMeters;
        }

        public void setLastKnownDistanceM(int lastKnownDistanceMeters) {
            this.lastKnownDistanceMeters = lastKnownDistanceMeters;
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
