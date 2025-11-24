package com.code.wlu.cp470.wellnest.data.local.managers;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.code.wlu.cp470.wellnest.data.UserModels.Friend;
import com.code.wlu.cp470.wellnest.data.UserModels.Score;
import com.code.wlu.cp470.wellnest.data.UserModels.UserProfile;
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
public final class UserManager {
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
     * Returns current user’s UID.
     */
    public String currentUid() {
        try (Cursor cursor = db.query(
                UserContract.UserProfile.TABLE,
                new String[]{UserContract.UserProfile.Col.UID},
                null, null, null, null, null
        )) {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            } else {
                throw new IllegalStateException("No current user");
            }
        }
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
    public UserProfile getUserProfile(String uid, String email) {
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

            return new UserProfile(
                    c.getString(0), // uid
                    c.getString(1), // name
                    c.getString(2)  // email (may be null)
            );

        } finally {
            if (c != null) c.close();
        }
    }

    public boolean deleteUserProfile() {
        int rows = db.delete(
                UserContract.UserProfile.TABLE,
                null,
                null
        );
        return rows > 0;
    }

    // ----------------------------------------------------------------------
    // global_score (multiple rows keyed by uid)
    // ----------------------------------------------------------------------

    private Integer queryScoreByUid(String uid) {
        return queryInt(
                UserContract.GlobalScore.TABLE,
                UserContract.GlobalScore.Col.SCORE,
                UserContract.GlobalScore.Col.UID + "=?",
                new String[]{uid}
        );
    }

    private boolean upsertScore(String uid, int score) {
        // Try UPDATE first; if no row, INSERT.
        ContentValues cv = new ContentValues();
        cv.put(UserContract.GlobalScore.Col.SCORE, score);
        int rows = db.update(
                UserContract.GlobalScore.TABLE,
                cv,
                UserContract.GlobalScore.Col.UID + "=?",
                new String[]{uid}
        );
        if (rows > 0) return true;

        cv.put(UserContract.GlobalScore.Col.UID, uid);
        long id = db.insert(UserContract.GlobalScore.TABLE, null, cv);
        return id != -1;
    }

    // --- READ (current user) ---
    public int getGlobalScore() {
        Integer val = queryScoreByUid(currentUid());
        return val != null ? val : 0;
    }

    // --- READ (by uid) ---
    public Integer getGlobalScore(String uid) {
        return queryScoreByUid(uid);
    }

    // --- CREATE (current user) ---
    public boolean createGlobalScore(int initialScore) {
        ContentValues cv = new ContentValues();
        cv.put(UserContract.GlobalScore.Col.UID, currentUid());
        cv.put(UserContract.GlobalScore.Col.SCORE, initialScore);
        long id = db.insert(UserContract.GlobalScore.TABLE, null, cv);
        return id != -1;
    }

    // --- CREATE (by uid) ---
    public boolean createGlobalScore(String uid, int initialScore) {
        ContentValues cv = new ContentValues();
        cv.put(UserContract.GlobalScore.Col.UID, uid);
        cv.put(UserContract.GlobalScore.Col.SCORE, initialScore);
        long id = db.insert(UserContract.GlobalScore.TABLE, null, cv);
        return id != -1;
    }

    // --- ENSURE ---
    public boolean ensureGlobalScore(String uid) {
        // create with 0 if missing
        if (queryScoreByUid(uid) != null) return false;
        ContentValues cv = new ContentValues();
        cv.put(UserContract.GlobalScore.Col.UID, uid);
        cv.put(UserContract.GlobalScore.Col.SCORE, 0);
        return db.insert(UserContract.GlobalScore.TABLE, null, cv) != -1;
    }

// --- UPDATE (current user) ---

    /**
     * Sets global score to an absolute value (>= 0 suggested).
     */
    public boolean setGlobalScore(int newScore) {
        return upsertScore(currentUid(), newScore);
    }

    // --- UPDATE (by uid) ---
    public boolean setGlobalScore(String uid, int newScore) {
        return upsertScore(uid, newScore);
    }

// --- ADD (current user) ---

    /**
     * Adds delta (can be negative). Returns the new score.
     */
    public int addToGlobalScore(int delta) {
        return addToGlobalScore(currentUid(), delta);
    }

// --- ADD (by uid) ---

    /**
     * Adds delta (can be negative). Returns the new score.
     */
    public int addToGlobalScore(String uid, int delta) {
        db.beginTransaction();
        try {
            int current = 0;
            Integer got = queryScoreByUid(uid);
            if (got != null) current = got;
            int updated = current + delta;
            if (!upsertScore(uid, updated)) {
                throw new SQLException("Failed to upsert global score for uid=" + uid);
            }
            db.setTransactionSuccessful();
            return updated;
        } finally {
            db.endTransaction();
        }
    }

    // --- DELETE (current user) ---
    public boolean deleteGlobalScore() {
        int rows = db.delete(
                UserContract.GlobalScore.TABLE,
                UserContract.GlobalScore.Col.UID + "=?",
                new String[]{currentUid()}
        );
        return rows > 0;
    }

    // --- DELETE (by uid) ---
    public boolean deleteGlobalScore(String uid) {
        int rows = db.delete(
                UserContract.GlobalScore.TABLE,
                UserContract.GlobalScore.Col.UID + "=?",
                new String[]{uid}
        );
        return rows > 0;
    }

    // --- BULK READ (uids) ---
    public java.util.Map<String, Integer> getGlobalScores(java.util.Collection<String> uids) {
        java.util.Map<String, Integer> out = new java.util.HashMap<>();
        if (uids == null || uids.isEmpty()) return out;

        // Build IN (?, ?, ...)
        StringBuilder sb = new StringBuilder();
        sb.append(UserContract.GlobalScore.Col.UID).append(" IN (");
        String[] args = new String[uids.size()];
        int i = 0;
        for (String uid : uids) {
            if (i > 0) sb.append(',');
            sb.append('?');
            args[i++] = uid;
        }
        sb.append(')');

        Cursor c = null;
        try {
            c = db.query(
                    UserContract.GlobalScore.TABLE,
                    new String[]{UserContract.GlobalScore.Col.UID, UserContract.GlobalScore.Col.SCORE},
                    sb.toString(),
                    args, null, null, null
            );
            while (c.moveToNext()) {
                out.put(c.getString(0), c.getInt(1));
            }
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    // --- LIST ALL ---
    public java.util.List<Score> listAllGlobalScores() {
        java.util.List<Score> list = new java.util.ArrayList<>();
        Cursor c = null;
        try {
            c = db.query(
                    UserContract.GlobalScore.TABLE,
                    new String[]{UserContract.GlobalScore.Col.UID, UserContract.GlobalScore.Col.SCORE},
                    null, null, null, null,
                    UserContract.GlobalScore.Col.SCORE + " DESC"
            );
            while (c.moveToNext()) {
                list.add(new Score(c.getString(0), c.getInt(1)));
            }
        } finally {
            if (c != null) c.close();
        }
        return list;
    }

    // --- BULK DELETE ---
    public int deleteGlobalScores(java.util.Collection<String> uids) {
        if (uids == null || uids.isEmpty()) return 0;
        StringBuilder sb = new StringBuilder();
        sb.append(UserContract.GlobalScore.Col.UID).append(" IN (");
        String[] args = new String[uids.size()];
        int i = 0;
        for (String uid : uids) {
            if (i > 0) sb.append(',');
            sb.append('?');
            args[i++] = uid;
        }
        sb.append(')');
        return db.delete(UserContract.GlobalScore.TABLE, sb.toString(), args);
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
        return upsertFriend(friendUid, friendName, "pending");
    }

    /**
     * Upsert a friend by their Firebase UID with specific status (used for Firebase sync).
     */
    public boolean upsertFriend(String friendUid, String friendName, String status) {
        if (friendUid == null || friendUid.isEmpty()) {
            throw new IllegalArgumentException("friendUid is empty");
        }
        if (friendName == null) friendName = "";
        if (status == null) status = "pending";

        Log.d("UserManager", "upsertFriend: uid=" + friendUid + ", name=" + friendName + ", status=" + status);

        ContentValues cv = new ContentValues();
        cv.put(UserContract.Friends.Col.FRIEND_UID, friendUid);
        cv.put(UserContract.Friends.Col.FRIEND_NAME, friendName);
        cv.put(UserContract.Friends.Col.FRIEND_STATUS, status);

        // Use REPLACE to overwrite existing entries completely
        long id = db.insertWithOnConflict(
                UserContract.Friends.TABLE,
                null,
                cv,
                SQLiteDatabase.CONFLICT_REPLACE
        );

        boolean success = id != -1;
        
        // Ensure the friend has a global_score entry for the INNER JOIN in getFriends()
        if (success) {
            boolean scoreEnsured = ensureGlobalScore(friendUid);
            Log.d("UserManager", "upsertFriend: ensureGlobalScore for " + friendUid + " = " + scoreEnsured);
        }
        
        Log.d("UserManager", "upsertFriend result: " + success + " (id=" + id + ")");
        return success;
    }


    /**
     * Remove a friend by UID. Returns true if a row was deleted.
     */
    public boolean removeFriend(String friendUid) {
        Log.d("UserManager", "removeFriend(" + friendUid + ")");
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
        return removeFriend(friendUid);
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
    public List<Friend> getFriends() {
        List<Friend> friends = new ArrayList<>();
        String TABLENAME = UserContract.Friends.TABLE + " INNER JOIN " + UserContract.GlobalScore.TABLE +
                " ON " + UserContract.Friends.TABLE + "." + UserContract.Friends.Col.FRIEND_UID +
                " = " + UserContract.GlobalScore.TABLE + "." + UserContract.GlobalScore.Col.UID;
        Cursor c = null;
        try {
            c = db.query(
                    TABLENAME,
                    new String[]{
                            UserContract.Friends.Col.FRIEND_UID,
                            UserContract.Friends.Col.FRIEND_NAME,
                            UserContract.Friends.Col.FRIEND_STATUS,
                            UserContract.GlobalScore.Col.SCORE
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
                int score = c.getInt(
                        c.getColumnIndexOrThrow(UserContract.GlobalScore.Col.SCORE));
                friends.add(new Friend(uid, name, status, score));
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
