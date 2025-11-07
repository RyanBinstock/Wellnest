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

import com.code.wlu.cp470.wellnest.data.UserInterface;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.UserManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class UserManagerInstrumentedTest {

    private Context context;
    private WellnestDatabaseHelper helper;
    private SQLiteDatabase db;
    private UserManager userManager;

    private static boolean containsFriendWith(List<UserInterface.Friend> friends, String uid, String name) {
        for (UserInterface.Friend f : friends) {
            if (uid.equals(f.uid) && name.equals(f.name)) return true;
        }
        return false;
    }

    private static UserInterface.Friend findFriendByUid(List<UserInterface.Friend> friends, String uid) {
        for (UserInterface.Friend f : friends) {
            if (uid.equals(f.uid)) return f;
        }
        return null;
    }

    // ------------------------------------------------------------
    // user_profile
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
        UserInterface.UserProfile p1 = userManager.getUserProfile(uid, null);
        assertNotNull(p1);
        assertEquals(uid, p1.uid);
        assertEquals(name, p1.name);
        assertEquals(email, p1.email);

        UserInterface.UserProfile p2 = userManager.getUserProfile(null, email);
        assertNotNull(p2);
        assertEquals(uid, p2.uid);
        assertEquals(name, p2.name);
        assertEquals(email, p2.email);

        UserInterface.UserProfile p3 = userManager.getUserProfile(uid, "wrong@other.com");
        assertNotNull(p3);
        assertEquals(uid, p3.uid);
    }

    // ------------------------------------------------------------
    // global_score
    // ------------------------------------------------------------

    @Test
    public void hasUserProfile_handlesNullsAndMissing() {
        assertFalse(userManager.hasUserProfile(null, null));
        assertNull(userManager.getUserProfile(null, null));
        assertNull(userManager.getUserName("missing"));
        assertNull(userManager.getUserEmail("missing"));
    }

    // ------------------------------------------------------------
    // streak
    // ------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void upsertUserProfile_rejectsEmptyUid() {
        userManager.upsertUserProfile("", "Name", "e@x.com");
    }

    // ------------------------------------------------------------
    // friends
    // ------------------------------------------------------------

    @Test
    public void globalScore_singletonInitializedAndUpdates() {
        // Constructor should have created id=1 row at 0
        assertEquals(0, userManager.getGlobalScore());

        assertTrue(userManager.setGlobalScore(10));
        assertEquals(10, userManager.getGlobalScore());

        int afterAdd = userManager.addToGlobalScore(5);
        assertEquals(15, afterAdd);
        assertEquals(15, userManager.getGlobalScore());

        // Add negative delta
        assertEquals(7, userManager.addToGlobalScore(-8));
        assertEquals(7, userManager.getGlobalScore());
    }

    @Test
    public void streak_singletonInitializedAndMutates() {
        // Constructor should have created id=1 row at 0
        assertEquals(0, userManager.getStreakCount());

        assertTrue(userManager.setStreakCount(2));
        assertEquals(2, userManager.getStreakCount());

        assertEquals(3, userManager.incrementStreak());
        assertEquals(3, userManager.getStreakCount());

        assertTrue(userManager.resetStreak());
        assertEquals(0, userManager.getStreakCount());
    }

    // ------------------------------------------------------------
    // badges
    // ------------------------------------------------------------

    @Test
    public void friends_upsertListAcceptDenyRemove() {
        String f1 = "friend_1";
        String f2 = "friend_2";
        String f3 = "friend_3";

        // "upsert"
        assertTrue(userManager.upsertFriend(f1, "Alice"));
        assertTrue(userManager.upsertFriend(f2, "Bob"));
        assertTrue(userManager.upsertFriend(f3, "Charlie"));

        // isFriend
        assertTrue(userManager.isFriend(f1));
        assertTrue(userManager.isFriend(f2));
        assertTrue(userManager.isFriend(f3));

        // List and sort check (order by name ASC, case-insensitive)
        List<UserInterface.Friend> friends = userManager.getFriends();
        assertEquals(3, friends.size());
        // Names present
        assertTrue(containsFriendWith(friends, f1, "Alice"));
        assertTrue(containsFriendWith(friends, f2, "Bob"));
        assertTrue(containsFriendWith(friends, f3, "Charlie"));

        // Deny one friend; accept the other; verify status if exposed by the Friend model
        assertTrue(userManager.denyFriend(f1));
        assertTrue(userManager.acceptFriend(f2));

        // Re-load and check statuses if available
        List<UserInterface.Friend> friends2 = userManager.getFriends();
        UserInterface.Friend alice = findFriendByUid(friends2, f1);
        UserInterface.Friend bob = findFriendByUid(friends2, f2);
        UserInterface.Friend charlie = findFriendByUid(friends2, f3);
        assertNull(alice);
        assertNotNull(bob);
        assertNotNull(charlie);

        assertEquals("accepted", bob.status);
        assertEquals("pending", charlie.status);


        // Remove one friend
        assertTrue(userManager.removeFriend(f2));
        assertFalse(userManager.isFriend(f2));

        // Remaining friend still present
        assertTrue(userManager.isFriend(f3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void upsertFriend_rejectsEmptyUid() {
        userManager.upsertFriend("", "Nobody");
    }

    // ------------------------------------------------------------
    // helpers
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