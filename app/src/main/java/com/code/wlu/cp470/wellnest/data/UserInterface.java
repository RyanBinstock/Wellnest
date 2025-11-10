package com.code.wlu.cp470.wellnest.data;

import java.util.List;

/**
 * Unified contract for all user-related data operations across local (SQLite) and remote (Firestore) sources.
 * <p>
 * NOTE ON NORMALIZATION:
 * - No DB-specific types (e.g., Cursor) are exposed. Lists/POJOs are used instead.
 * - Shapes are kept stable so both LocalUserManager (SQLite) and FirebaseUserManager (Firestore) can implement identically.
 */
public interface UserInterface {

    // ----------------------------------------------------------------------
    // user_profile  (from UserManager: upsertUserProfile, hasUserProfile, getUserName, getUserEmail, getUserProfile)
    // ----------------------------------------------------------------------

    /**
     * Upsert by Firebase UID. Returns true if insert or update affected a row.
     */
    boolean upsertUserProfile(String uid, String name, String email);

    /**
     * Returns true if a user_profile row exists with this UID or email.
     * If both uid and email are provided, uid is prioritized.
     */
    boolean hasUserProfile(String uid, String email);

    /**
     * Returns display name or null.
     */
    String getUserName(String uid);

    /**
     * Returns email or null.
     */
    String getUserEmail(String uid);

    /**
     * Convenience: load {uid, name, email} as a structured object or null if missing.
     * If both uid and email are provided, uid is prioritized.
     */
    UserProfile getUserProfile(String uid, String email);

// ----------------------------------------------------------------------
    // Global Score (user + friends), keyed by Firebase UID
    // ----------------------------------------------------------------------

    // ---- READ ----

    /**
     * Current user’s global score; returns 0 if missing (treat as not yet created).
     */
    int getGlobalScore();

    /**
     * Single user’s score by UID; returns null if no row exists.
     */
    Integer getGlobalScore(String uid);

    /**
     * Bulk read by UID; missing UIDs are omitted from the map.
     */
    java.util.Map<String, Integer> getGlobalScores(java.util.Collection<String> uids);

    /**
     * List all rows known locally. Useful for scoreboard UIs and syncing.
     */
    java.util.List<ScoreEntry> listAllGlobalScores();


    // ---- CREATE ----

    /**
     * Insert current user with initial score (fails if row exists).
     */
    boolean createGlobalScore(int initialScore);

    /**
     * Insert friend/user with initial score (fails if row exists).
     */
    boolean createGlobalScore(String uid, int initialScore);

    /**
     * Ensure a row exists with default 0 (no-op if present).
     */
    boolean ensureGlobalScore(String uid);


    // ---- UPDATE ----

    /**
     * Set current user’s score to an absolute value (>=0 suggested).
     * Returns true on success.
     */
    boolean setGlobalScore(int newScore);

    /**
     * Set specific user’s score to an absolute value (creates row if missing).
     * Returns true on success.
     */
    boolean setGlobalScore(String uid, int newScore);

    /**
     * Add delta to current user’s score (delta may be negative). Returns the new score.
     * Creates row if missing (starting from 0).
     */
    int addToGlobalScore(int delta);

    /**
     * Add delta to a user’s score (delta may be negative). Returns the new score.
     * Creates row if missing (starting from 0).
     */
    int addToGlobalScore(String uid, int delta);


    // ---- DELETE ----

    /**
     * Delete current user’s score row (returns true if a row was removed).
     */
    boolean deleteGlobalScore();

    /**
     * Delete a specific user’s score row (returns true if a row was removed).
     */
    boolean deleteGlobalScore(String uid);

    /**
     * Delete many rows by UID; returns number of rows removed.
     */
    int deleteGlobalScores(java.util.Collection<String> uids);


    // ---- Helper DTO ----

    int getStreakCount();

    // ----------------------------------------------------------------------
    // streak  (from UserManager: getStreakCount, setStreakCount, incrementStreak, resetStreak)
    // ----------------------------------------------------------------------

    /**
     * Sets streak to an absolute value (e.g., 0 to reset).
     */
    boolean setStreakCount(int newCount);

    /**
     * Increments streak by 1 and returns the new count.
     */
    int incrementStreak();

    /**
     * Resets streak to 0.
     */
    boolean resetStreak();

    /**
     * Upsert a friend by their Firebase UID.
     */
    boolean upsertFriend(String friendUid, String friendName);

    // ----------------------------------------------------------------------
    // friends  (from UserManager: upsertFriend, removeFriend, acceptFriend, denyFriend, isFriend, getFriends)
    // ----------------------------------------------------------------------

    /**
     * Remove a friend by UID. Returns true if a row was deleted.
     */
    boolean removeFriend(String friendUid);

    /**
     * Accept a friend by UID.
     */
    boolean acceptFriend(String friendUid);

    /**
     * Deny a friend by UID.
     */
    boolean denyFriend(String friendUid);

    /**
     * Returns true if this friend exists.
     */
    boolean isFriend(String friendUid);

    /**
     * Returns a list containing all the user's friends.
     */
    List<Friend> getFriends();

    /**
     * Adds a badge id (no-op if already present). Returns true if inserted.
     */
    boolean addBadge(String badgeId);

    // ----------------------------------------------------------------------
    // badges  (from UserManager: addBadge, removeBadge, hasBadge, listBadges)
    // ----------------------------------------------------------------------

    /**
     * Removes a badge.
     */
    boolean removeBadge(String badgeId);

    /**
     * Returns true if the user has this badge.
     */
    boolean hasBadge(String badgeId);

    /**
     * Lists all badge IDs (normalized from Cursor).
     */
    List<String> listBadges();

    // ----------------------------------------------------------------------
    // Normalized data models
    // ----------------------------------------------------------------------
    final class ScoreEntry {
        public final String uid;
        public final int score;

        public ScoreEntry(String uid, int score) {
            this.uid = uid;
            this.score = score;
        }
    }

    /**
     * Normalized user profile shape.
     */
    final class UserProfile {
        public final String uid;
        public final String name;
        public final String email;

        public UserProfile(String uid, String name, String email) {
            this.uid = uid;
            this.name = name;
            this.email = email;
        }
    }

    /**
     * Normalized friend shape (matches the fields used in UserManager).
     */
    final class Friend {
        public final String uid;
        public final String name;
        public final String status; // "pending" | "accepted"
        public int score;           // not final so we can set it during inner joins

        public Friend(String uid, String name, String status, int score) {
            this.uid = uid;
            this.name = name;
            this.status = status;
            this.score = score;
        }
    }
}