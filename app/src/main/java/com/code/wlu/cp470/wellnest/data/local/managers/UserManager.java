package com.code.wlu.cp470.wellnest.data.local.managers;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.code.wlu.cp470.wellnest.data.UserInterface;
import com.code.wlu.cp470.wellnest.data.local.contracts.UserContract;

import java.util.ArrayList;
import java.util.List;

/**
 * UserManager wraps reads/writes for:
 * - user_profile (singleton-by-usage, PK = Firebase UID)
 * - global_score (true singleton row with id=1)
 * - streak (true singleton row with id=1)
 * - friends (many)
 * - badges (many)
 * <p>
 * This version implements UserInterface with normalized return shapes.
 */
public final class UserManager implements UserInterface {
    private final SQLiteDatabase db;

    public UserManager(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("db cannot be null");
        this.db = db;
        ensureSingletons();
    }

    /**
     * Ensure singleton rows exist for global_score and streak.
     */
    private void ensureSingletons() {
        db.beginTransaction();
        try {
            // global_score -> id=1 default 0
            ContentValues gs = new ContentValues();
            gs.put(UserContract.GlobalScore.Col.ID, 1);
            gs.put(UserContract.GlobalScore.Col.SCORE, 0);
            db.insertWithOnConflict(
                    UserContract.GlobalScore.TABLE,
                    null,
                    gs,
                    SQLiteDatabase.CONFLICT_IGNORE
            );

            // streak -> id=1 default 0
            ContentValues st = new ContentValues();
            st.put(UserContract.Streak.Col.ID, 1);
            st.put(UserContract.Streak.Col.COUNT, 0);
            db.insertWithOnConflict(
                    UserContract.Streak.TABLE,
                    null,
                    st,
                    SQLiteDatabase.CONFLICT_IGNORE
            );

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // ----------------------------------------------------------------------
    // user_profile
    // ----------------------------------------------------------------------

    /**
     * Upsert by Firebase UID. Returns true if insert or update affected a row.
     */
    public boolean upsertUserProfile(String uid, String name, String email) {
        if (uid == null || uid.isEmpty())
            throw new IllegalArgumentException("uid cannot be null/empty");
        ContentValues cv = new ContentValues();
        cv.put(UserContract.UserProfile.Col.UID, uid);
        cv.put(UserContract.UserProfile.Col.NAME, name);
        cv.put(UserContract.UserProfile.Col.EMAIL, email);

        long id = db.insertWithOnConflict(
                UserContract.UserProfile.TABLE,
                null,
                cv,
                SQLiteDatabase.CONFLICT_REPLACE
        );
        return id != -1L;
    }

    /**
     * Returns true if a user_profile row exists with this UID or email.
     * If both uid and email are provided, uid is prioritized.
     */
    public boolean hasUserProfile(String uid, String email) {
        String where;
        String[] args;

        if (uid != null && !uid.isEmpty()) {
            where = UserContract.UserProfile.Col.UID + "=?";
            args = new String[]{uid};
        } else if (email != null && !email.isEmpty()) {
            where = UserContract.UserProfile.Col.EMAIL + "=?";
            args = new String[]{email};
        } else {
            return false;
        }

        Cursor c = null;
        try {
            c = db.query(
                    UserContract.UserProfile.TABLE,
                    new String[]{UserContract.UserProfile.Col.UID},
                    where, args, null, null, null
            );
            return c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Returns display name or null.
     */
    public String getUserName(String uid) {
        if (uid == null || uid.isEmpty()) return null;
        return queryString(
                UserContract.UserProfile.TABLE,
                UserContract.UserProfile.Col.NAME,
                UserContract.UserProfile.Col.UID + "=?",
                new String[]{uid}
        );
    }

    /**
     * Returns email or null.
     */
    public String getUserEmail(String uid) {
        if (uid == null || uid.isEmpty()) return null;
        return queryString(
                UserContract.UserProfile.TABLE,
                UserContract.UserProfile.Col.EMAIL,
                UserContract.UserProfile.Col.UID + "=?",
                new String[]{uid}
        );
    }

    /**
     * Convenience: load {uid, name, email} as a structured object or null if missing.
     * If both uid and email are provided, uid is prioritized.
     */
    public UserInterface.UserProfile getUserProfile(String uid, String email) {
        Cursor c = null;
        try {
            String selection = null;
            String[] selectionArgs = null;

            // Priority: UID > Email
            if (uid != null && !uid.isEmpty()) {
                selection = UserContract.UserProfile.Col.UID + "=?";
                selectionArgs = new String[]{uid};
            } else if (email != null && !email.isEmpty()) {
                selection = UserContract.UserProfile.Col.EMAIL + "=?";
                selectionArgs = new String[]{email};
            } else {
                // Neither provided → nothing to query
                return null;
            }

            c = db.query(
                    UserContract.UserProfile.TABLE,
                    new String[]{
                            UserContract.UserProfile.Col.UID,
                            UserContract.UserProfile.Col.NAME,
                            UserContract.UserProfile.Col.EMAIL
                    },
                    selection,
                    selectionArgs,
                    null, null, null
            );
            if (!c.moveToFirst()) return null;

            return new UserInterface.UserProfile(
                    c.getString(0), // uid
                    c.getString(1), // name
                    c.getString(2)  // email (may be null)
            );

        } finally {
            if (c != null) c.close();
        }
    }

    // ----------------------------------------------------------------------
    // global_score (id = 1)
    // ----------------------------------------------------------------------

    public int getGlobalScore() {
        Integer val = queryInt(
                UserContract.GlobalScore.TABLE,
                UserContract.GlobalScore.Col.SCORE,
                UserContract.GlobalScore.Col.ID + "=?",
                new String[]{"1"}
        );
        return val != null ? val : 0;
    }

    /**
     * Sets global score to an absolute value (>= 0 suggested).
     */
    public boolean setGlobalScore(int newScore) {
        ContentValues cv = new ContentValues();
        cv.put(UserContract.GlobalScore.Col.SCORE, newScore);
        int rows = db.update(
                UserContract.GlobalScore.TABLE,
                cv,
                UserContract.GlobalScore.Col.ID + "=?",
                new String[]{"1"}
        );
        return rows > 0;
    }

    /**
     * Adds delta (can be negative). Returns the new score.
     */
    public int addToGlobalScore(int delta) {
        db.beginTransaction();
        try {
            int current = getGlobalScore();
            int updated = current + delta;

            ContentValues cv = new ContentValues();
            cv.put(UserContract.GlobalScore.Col.SCORE, updated);
            int rows = db.update(
                    UserContract.GlobalScore.TABLE,
                    cv,
                    UserContract.GlobalScore.Col.ID + "=?",
                    new String[]{"1"}
            );
            if (rows == 0) throw new SQLException("Failed to update global score");
            db.setTransactionSuccessful();
            return updated;
        } finally {
            db.endTransaction();
        }
    }

    // ----------------------------------------------------------------------
    // streak (id = 1)
    // ----------------------------------------------------------------------

    public int getStreakCount() {
        Integer val = queryInt(
                UserContract.Streak.TABLE,
                UserContract.Streak.Col.COUNT,
                UserContract.Streak.Col.ID + "=?",
                new String[]{"1"}
        );
        return val != null ? val : 0;
    }

    /**
     * Sets streak to an absolute value (e.g., 0 to reset).
     */
    public boolean setStreakCount(int newCount) {
        ContentValues cv = new ContentValues();
        cv.put(UserContract.Streak.Col.COUNT, newCount);
        int rows = db.update(
                UserContract.Streak.TABLE,
                cv,
                UserContract.Streak.Col.ID + "=?",
                new String[]{"1"}
        );
        return rows > 0;
    }

    /**
     * Increments streak by 1 and returns the new count.
     */
    public int incrementStreak() {
        db.beginTransaction();
        try {
            int current = getStreakCount();
            int updated = current + 1;

            ContentValues cv = new ContentValues();
            cv.put(UserContract.Streak.Col.COUNT, updated);
            int rows = db.update(
                    UserContract.Streak.TABLE,
                    cv,
                    UserContract.Streak.Col.ID + "=?",
                    new String[]{"1"}
            );
            if (rows == 0) throw new SQLException("Failed to update streak");
            db.setTransactionSuccessful();
            return updated;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Resets streak to 0.
     */
    public boolean resetStreak() {
        return setStreakCount(0);
    }

    // ----------------------------------------------------------------------
    // friends
    // ----------------------------------------------------------------------

    /**
     * Upsert a friend by their Firebase UID.
     */
    public boolean upsertFriend(String friendUid, String friendName) {
        if (friendUid == null || friendUid.isEmpty())
            throw new IllegalArgumentException("friendUid cannot be null/empty");
        ContentValues cv = new ContentValues();
        cv.put(UserContract.Friends.Col.FRIEND_UID, friendUid);
        cv.put(UserContract.Friends.Col.FRIEND_NAME, friendName);
        cv.put(UserContract.Friends.Col.FRIEND_STATUS, "accepted"); // normalized status

        long id = db.insertWithOnConflict(
                UserContract.Friends.TABLE,
                null,
                cv,
                SQLiteDatabase.CONFLICT_REPLACE
        );
        return id != -1L;
    }

    /**
     * Remove a friend by UID. Returns true if a row was deleted.
     */
    public boolean removeFriend(String friendUid) {
        int rows = db.delete(
                UserContract.Friends.TABLE,
                UserContract.Friends.Col.FRIEND_UID + "=?",
                new String[]{friendUid}
        );
        return rows > 0;
    }

    /**
     * Accept a friend by UID.
     */
    public boolean acceptFriend(String friendUid) {
        ContentValues cv = new ContentValues();
        cv.put(UserContract.Friends.Col.FRIEND_STATUS, "accepted");
        int rows = db.update(
                UserContract.Friends.TABLE,
                cv,
                UserContract.Friends.Col.FRIEND_UID + "=?",
                new String[]{friendUid}
        );
        return rows > 0;
    }

    /**
     * Deny a friend by UID.
     */
    public boolean denyFriend(String friendUid) {
        ContentValues cv = new ContentValues();
        cv.put(UserContract.Friends.Col.FRIEND_STATUS, "denied");
        int rows = db.update(
                UserContract.Friends.TABLE,
                cv,
                UserContract.Friends.Col.FRIEND_UID + "=?",
                new String[]{friendUid}
        );
        return rows > 0;
    }

    /**
     * Returns true if this friend exists.
     */
    public boolean isFriend(String friendUid) {
        Cursor c = null;
        try {
            c = db.query(
                    UserContract.Friends.TABLE,
                    new String[]{UserContract.Friends.Col.FRIEND_UID},
                    UserContract.Friends.Col.FRIEND_UID + "=?",
                    new String[]{friendUid},
                    null, null, null
            );
            return c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Returns a list containing all the user's friends.
     */
    public List<UserInterface.Friend> getFriends() {
        List<UserInterface.Friend> friends = new ArrayList<>();
        Cursor c = null;
        try {
            c = db.query(
                    UserContract.Friends.TABLE,
                    new String[]{
                            UserContract.Friends.Col.FRIEND_UID,
                            UserContract.Friends.Col.FRIEND_NAME
                    },
                    null, null, null, null,
                    UserContract.Friends.Col.FRIEND_NAME + " COLLATE NOCASE ASC"
            );

            while (c.moveToNext()) {
                String uid = c.getString(
                        c.getColumnIndexOrThrow(UserContract.Friends.Col.FRIEND_UID));
                String name = c.getString(
                        c.getColumnIndexOrThrow(UserContract.Friends.Col.FRIEND_NAME));
                String status = c.getString(
                        c.getColumnIndexOrThrow(UserContract.Friends.Col.FRIEND_STATUS));
                friends.add(new UserInterface.Friend(uid, name, status));
            }
        } finally {
            if (c != null) c.close();
        }
        return friends;
    }

    // ----------------------------------------------------------------------
    // badges
    // ----------------------------------------------------------------------

    /**
     * Adds a badge id (no-op if already present). Returns true if inserted.
     */
    public boolean addBadge(String badgeId) {
        if (badgeId == null) throw new IllegalArgumentException("badgeId cannot be null");
        ContentValues cv = new ContentValues();
        cv.put(UserContract.Badges.Col.BADGE_ID, badgeId);
        long id = db.insertWithOnConflict(
                UserContract.Badges.TABLE,
                null,
                cv,
                SQLiteDatabase.CONFLICT_IGNORE
        );
        return id != -1L;
    }

    /**
     * Removes a badge.
     */
    public boolean removeBadge(String badgeId) {
        int rows = db.delete(
                UserContract.Badges.TABLE,
                UserContract.Badges.Col.BADGE_ID + "=?",
                new String[]{badgeId}
        );
        return rows > 0;
    }

    /**
     * Returns true if the user has this badge.
     */
    public boolean hasBadge(String badgeId) {
        Cursor c = null;
        try {
            c = db.query(
                    UserContract.Badges.TABLE,
                    new String[]{UserContract.Badges.Col.BADGE_ID},
                    UserContract.Badges.Col.BADGE_ID + "=?",
                    new String[]{badgeId},
                    null, null, null
            );
            return c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Lists all badge IDs (normalized; no Cursor exposed).
     */
    public List<String> listBadges() {
        List<String> badges = new ArrayList<>();
        Cursor c = null;
        try {
            c = db.query(
                    UserContract.Badges.TABLE,
                    new String[]{UserContract.Badges.Col.BADGE_ID},
                    null, null, null, null,
                    UserContract.Badges.Col.BADGE_ID + " ASC"
            );
            while (c.moveToNext()) {
                String id = c.getString(c.getColumnIndexOrThrow(UserContract.Badges.Col.BADGE_ID));
                badges.add(id);
            }
        } finally {
            if (c != null) c.close();
        }
        return badges;
    }

    // ----------------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------------

    private Integer queryInt(String table, String col, String where, String[] args) {
        Cursor c = null;
        try {
            c = db.query(table, new String[]{col}, where, args, null, null, null);
            if (!c.moveToFirst()) return null;
            if (c.isNull(0)) return null;
            return c.getInt(0);
        } finally {
            if (c != null) c.close();
        }
    }

    private String queryString(String table, String col, String where, String[] args) {
        Cursor c = null;
        try {
            c = db.query(table, new String[]{col}, where, args, null, null, null);
            if (!c.moveToFirst()) return null;
            return c.getString(0);
        } finally {
            if (c != null) c.close();
        }
    }
}
