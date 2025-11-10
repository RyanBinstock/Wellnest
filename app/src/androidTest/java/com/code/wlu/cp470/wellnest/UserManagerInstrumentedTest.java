package com.code.wlu.cp470.wellnest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.code.wlu.cp470.wellnest.data.UserModels.Friend;
import com.code.wlu.cp470.wellnest.data.UserModels.Score;
import com.code.wlu.cp470.wellnest.data.UserModels.UserProfile;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.UserManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class UserManagerInstrumentedTest {

    private Context context;
    private WellnestDatabaseHelper helper;
    private SQLiteDatabase db;
    private UserManager userManager;

    private static boolean containsFriendWith(List<Friend> friends, String uid, String name) {
        for (Friend f : friends) {
            if (uid.equals(f.getUid()) && name.equals(f.getName())) return true;
        }
        return false;
    }

    private static Friend findFriendByUid(List<Friend> friends, String uid) {
        for (Friend f : friends) {
            if (uid.equals(f.getUid())) return f;
        }
        return null;
    }

    // ------------------------------------------------------------
    // lifecycle
    // ------------------------------------------------------------

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        helper = new WellnestDatabaseHelper(context);
        db = helper.getWritableDatabase();
        helper.cleanDatabase(db);

        // UserManager constructor should also ensure singleton rows exist
        userManager = new UserManager(db);
    }

    @After
    public void tearDown() {
        helper.cleanDatabase(db);
        if (db != null && db.isOpen()) db.close();
        if (helper != null) helper.close();
    }

    // ------------------------------------------------------------
    // user_profile
    // ------------------------------------------------------------

    @Test
    public void upsertAndFetchUserProfile_byUidAndEmail() {
        String uid = "uid_123";
        String name = "Cameron";
        String email = "cam@example.com";

        assertTrue(userManager.upsertUserProfile(uid, name, email));

        // Direct getters
        assertEquals(name, userManager.getUserName(uid));
        assertEquals(email, userManager.getUserEmail(uid));

        // hasUserProfile: either UID or email should succeed; UID has priority if both are passed
        assertTrue(userManager.hasUserProfile(uid, null));
        assertTrue(userManager.hasUserProfile(null, email));
        assertTrue(userManager.hasUserProfile(uid, "wrong@other.com")); // UID prioritized

        // Structured getUserProfile: supports (uid) OR (email), prioritizes uid when both given
        UserProfile p1 = userManager.getUserProfile(uid, null);
        assertNotNull(p1);
        assertEquals(uid, p1.getUid());
        assertEquals(name, p1.getName());
        assertEquals(email, p1.getEmail());

        UserProfile p2 = userManager.getUserProfile(null, email);
        assertNotNull(p2);
        assertEquals(uid, p2.getUid());
        assertEquals(name, p2.getName());
        assertEquals(email, p2.getEmail());

        UserProfile p3 = userManager.getUserProfile(uid, "wrong@other.com");
        assertNotNull(p3);
        assertEquals(uid, p3.getUid());
    }

    @Test
    public void hasUserProfile_handlesNullsAndMissing() {
        assertFalse(userManager.hasUserProfile(null, null));
        assertNull(userManager.getUserProfile(null, null));
        assertNull(userManager.getUserName("missing"));
        assertNull(userManager.getUserEmail("missing"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void upsertUserProfile_rejectsEmptyUid() {
        userManager.upsertUserProfile("", "Name", "e@x.com");
    }

    // ------------------------------------------------------------
    // global_score (multi-row)
    // ------------------------------------------------------------

    @Test
    public void globalScore_singletonInitializedAndUpdates_self() {
        String me = "uid_me";
        assertTrue(userManager.upsertUserProfile(me, "Me", "me@x.com"));
        // No row yet for current user -> treated as 0
        assertEquals(0, userManager.getGlobalScore());

        assertTrue(userManager.setGlobalScore(10));
        assertEquals(10, userManager.getGlobalScore());

        int afterAdd = userManager.addToGlobalScore(5);
        assertEquals(15, afterAdd);
        assertEquals(15, userManager.getGlobalScore());

        // Negative delta allowed
        assertEquals(7, userManager.addToGlobalScore(-8));
        assertEquals(7, userManager.getGlobalScore());
    }

    @Test
    public void globalScore_crudForFriends_andBulkOps() {
        // Seed a current user so currentUid() is resolvable
        String me = "uid_me";
        assertTrue(userManager.upsertUserProfile(me, "Me", "me@x.com"));

        // Friends UIDs
        String f1 = "f_1";
        String f2 = "f_2";
        String f3 = "f_3";

        // Create rows (create returns false on duplicate)
        assertTrue(userManager.createGlobalScore(f1, 3));
        assertTrue(userManager.createGlobalScore(f2, 5));
        assertTrue(userManager.createGlobalScore(f3, 7));
        assertFalse(userManager.createGlobalScore(f1, 99)); // already exists

        // Ensure no-op if exists / create if missing
        assertFalse(userManager.ensureGlobalScore(f1)); // already there
        String f4 = "f_4";
        assertTrue(userManager.ensureGlobalScore(f4)); // created with 0
        assertEquals(Integer.valueOf(0), userManager.getGlobalScore(f4));

        // Read single/by-uid + add/set-by-uid
        assertEquals(Integer.valueOf(3), userManager.getGlobalScore(f1));
        assertEquals(6, userManager.addToGlobalScore(f1, 3));
        assertEquals(Integer.valueOf(6), userManager.getGlobalScore(f1));

        assertTrue(userManager.setGlobalScore(f2, 12));
        assertEquals(Integer.valueOf(12), userManager.getGlobalScore(f2));

        // Bulk read
        Map<String, Integer> m = userManager.getGlobalScores(Arrays.asList(f1, f2, f3, "missing"));
        assertEquals(3, m.size());
        assertEquals(Integer.valueOf(6), m.get(f1));
        assertEquals(Integer.valueOf(12), m.get(f2));
        assertEquals(Integer.valueOf(7), m.get(f3));

        // List all (should include all friend rows we created, possibly also self if created)
        List<Score> all = userManager.listAllGlobalScores();
        Set<String> seen = new HashSet<>();
        for (Score e : all) seen.add(e.getUid());
        assertTrue(seen.contains(f1));
        assertTrue(seen.contains(f2));
        assertTrue(seen.contains(f3));
        assertTrue(seen.contains(f4));

        // Deletes
        assertTrue(userManager.deleteGlobalScore(f3));
        assertNull(userManager.getGlobalScore(f3));

        int removed = userManager.deleteGlobalScores(Arrays.asList(f1, f2, "missing"));
        assertEquals(2, removed);
        assertNull(userManager.getGlobalScore(f1));
        assertNull(userManager.getGlobalScore(f2));

        // Self row delete should succeed even if not previously written
        assertTrue(userManager.setGlobalScore(4));
        assertEquals(4, userManager.getGlobalScore());
        assertTrue(userManager.deleteGlobalScore());
        assertEquals(0, userManager.getGlobalScore()); // treated as 0 when missing
    }

    // ------------------------------------------------------------
    // streak
    // ------------------------------------------------------------

    @Test
    public void streak_singletonInitializedAndMutates() {
        assertEquals(0, userManager.getStreakCount());

        assertTrue(userManager.setStreakCount(2));
        assertEquals(2, userManager.getStreakCount());

        assertEquals(3, userManager.incrementStreak());
        assertEquals(3, userManager.getStreakCount());

        assertTrue(userManager.resetStreak());
        assertEquals(0, userManager.getStreakCount());
    }

    // ------------------------------------------------------------
    // friends
    // ------------------------------------------------------------

    @Test
    public void friends_upsertListAcceptDenyRemove_andStatusUpdate() {
        String f1 = "friend_1";
        String f2 = "friend_2";
        String f3 = "friend_3";

        // upsert
        assertTrue(userManager.upsertFriend(f1, "Alice"));
        assertTrue(userManager.upsertFriend(f2, "Bob"));
        assertTrue(userManager.upsertFriend(f3, "Charlie"));

        // re-upsert should update name but preserve status
        assertTrue(userManager.acceptFriend(f2));
        assertTrue(userManager.upsertFriend(f2, "Bobby"));

        // isFriend
        assertTrue(userManager.isFriend(f1));
        assertTrue(userManager.isFriend(f2));
        assertTrue(userManager.isFriend(f3));
        userManager.setGlobalScore(f1, 0);
        userManager.setGlobalScore(f2, 0);
        userManager.setGlobalScore(f3, 0);


        // List
        List<Friend> friends = userManager.getFriends();
        assertEquals(3, friends.size());
        assertTrue(containsFriendWith(friends, f1, "Alice"));
        assertTrue(containsFriendWith(friends, f2, "Bobby"));   // updated name
        assertTrue(containsFriendWith(friends, f3, "Charlie"));

        // Deny removes
        assertTrue(userManager.denyFriend(f1));
        List<Friend> friends2 = userManager.getFriends();
        assertNull(findFriendByUid(friends2, f1));

        // Status checks
        Friend bob = findFriendByUid(friends2, f2);
        Friend charlie = findFriendByUid(friends2, f3);
        assertNotNull(bob);
        assertNotNull(charlie);
        assertEquals("accepted", bob.getStatus());
        assertEquals("pending", charlie.getStatus());

        // Remove
        assertTrue(userManager.removeFriend(f2));
        assertFalse(userManager.isFriend(f2));
        assertTrue(userManager.isFriend(f3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void upsertFriend_rejectsEmptyUid() {
        userManager.upsertFriend("", "Nobody");
    }

    // ------------------------------------------------------------
    // badges
    // ------------------------------------------------------------

    @Test
    public void badges_addIdempotentListRemove() {
        String b1 = "badge_first_login";
        String b2 = "badge_10_points";

        // add
        assertTrue(userManager.addBadge(b1));
        assertTrue(userManager.addBadge(b2));

        // idempotent insert (CONFLICT_IGNORE)
        assertFalse(userManager.addBadge(b1));

        // has/list
        assertTrue(userManager.hasBadge(b1));
        assertTrue(userManager.hasBadge(b2));
        List<String> badges = userManager.listBadges();
        assertTrue(badges.contains(b1));
        assertTrue(badges.contains(b2));

        // remove
        assertTrue(userManager.removeBadge(b1));
        assertFalse(userManager.hasBadge(b1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addBadge_rejectsNull() {
        userManager.addBadge(null);
    }
}
