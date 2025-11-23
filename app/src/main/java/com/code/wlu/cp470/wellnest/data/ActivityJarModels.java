package com.code.wlu.cp470.wellnest.data;

public class ActivityJarModels {

    public static enum Category {
        Explore,
        Nightlife,
        Play,
        Cozy,
        Culture
    }

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

    public static final class Activity {
        private String category;
        private String emoji;
        private String title;
        private String description;
        private String address;
        private String[] tags;
        private String url;

        public Activity(String category, String emoji, String title, String description, String address, String[] tags, String url) {
            this.category = category;
            this.emoji = emoji;
            this.title = title;
            this.description = description;
            this.address = address;
            this.tags = tags;
            this.url = url;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getEmoji() {
            return emoji;
        }

        public void setEmoji(String emoji) {
            this.emoji = emoji;
        }


        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String[] getTags() {
            return tags;
        }

        public void setTags(String[] tags) {
            this.tags = tags;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
