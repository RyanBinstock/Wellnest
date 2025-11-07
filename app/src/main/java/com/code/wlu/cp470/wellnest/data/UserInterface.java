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
    // global_score  (from UserManager: getGlobalScore, setGlobalScore, addToGlobalScore)
    // ----------------------------------------------------------------------

    int getGlobalScore();

    /**
     * Sets global score to an absolute value (>= 0 suggested).
     */
    boolean setGlobalScore(int newScore);

    /**
     * Adds delta (can be negative). Returns the new score.
     */
    int addToGlobalScore(int delta);

    // ----------------------------------------------------------------------
    // streak  (from UserManager: getStreakCount, setStreakCount, incrementStreak, resetStreak)
    // ----------------------------------------------------------------------

    int getStreakCount();

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

    // ----------------------------------------------------------------------
    // friends  (from UserManager: upsertFriend, removeFriend, acceptFriend, denyFriend, isFriend, getFriends)
    // ----------------------------------------------------------------------

    /**
     * Upsert a friend by their Firebase UID.
     */
    boolean upsertFriend(String friendUid, String friendName);

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

    // ----------------------------------------------------------------------
    // badges  (from UserManager: addBadge, removeBadge, hasBadge, listBadges)
    // ----------------------------------------------------------------------

    /**
     * Adds a badge id (no-op if already present). Returns true if inserted.
     */
    boolean addBadge(String badgeId);

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
        public final String status; // e.g., "accepted"

        public Friend(String uid, String name, String status) {
            this.uid = uid;
            this.name = name;
            this.status = status;
        }
    }
}