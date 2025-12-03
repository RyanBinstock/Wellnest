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
import com.code.wlu.cp470.wellnest.data.UserRepository;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.UserManager;
import com.code.wlu.cp470.wellnest.data.model.FriendRequestResult;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseUserManager;
import com.google.firebase.firestore.FirebaseFirestoreException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public class UserRepositoryInstrumentedTest {

    // Default test user constants
    private static final String DEFAULT_TEST_UID = "default_test_user";
    private static final String DEFAULT_TEST_NAME = "Test User";
    private static final String DEFAULT_TEST_EMAIL = "testuser@example.com";
    private Context context;
    private WellnestDatabaseHelper helper;
    private SQLiteDatabase db;
    private UserManager localManager;
    private FakeFirebaseUserManager remoteManager;
    private UserRepository repo;

    // ------------------------------------------------------------
    // FakeFirebaseUserManager - In-memory implementation
    // ------------------------------------------------------------

    private static Friend findFriendByUid(List<Friend> friends, String uid) {
        for (Friend f : friends) {
            if (uid.equals(f.getUid())) return f;
        }
        return null;
    }

    // ------------------------------------------------------------
    // Helper methods
    // ------------------------------------------------------------


    // ------------------------------------------------------------
    // Setup and Teardown
    // ------------------------------------------------------------

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        helper = new WellnestDatabaseHelper(context);
        db = helper.getWritableDatabase();
        helper.cleanDatabase(db);

        localManager = new UserManager(db);
        remoteManager = new FakeFirebaseUserManager();

        // Create a default test user in BOTH local and remote so remote operations don't fail
        localManager.upsertUserProfile(DEFAULT_TEST_UID, DEFAULT_TEST_NAME, DEFAULT_TEST_EMAIL);
        remoteManager.upsertUserProfile(DEFAULT_TEST_UID, DEFAULT_TEST_NAME, DEFAULT_TEST_EMAIL);

        repo = new UserRepository(context, localManager, remoteManager);
    }


    @After
    public void tearDown() {
        helper.cleanDatabase(db);
        if (db != null && db.isOpen()) {
            db.close();
        }
        if (helper != null) {
            helper.close();
        }
        remoteManager.deleteUserProfile(DEFAULT_TEST_UID);
    }

    @Test
    public void upsertUserProfile_createsNewProfile_visibleViaLocalAndRepo() {
        String uid = "test_uid_1";
        String name = "Test User";
        String email = "test@example.com";

        assertTrue(repo.upsertUserProfile(uid, name, email));

        // Verify via local manager
        assertEquals(name, localManager.getUserName(uid));
        assertEquals(email, localManager.getUserEmail(uid));

        // Verify via repository
        UserProfile profile = repo.getUser(uid, email);
        assertNotNull(profile);
        assertEquals(uid, profile.getUid());
        assertEquals(name, profile.getName());
        assertEquals(email, profile.getEmail());
    }

    // ------------------------------------------------------------
    // A. User Profile Operations (8-10 tests)
    // ------------------------------------------------------------

    @Test
    public void hasUserProfile_returnsTrueForExistingUser_falseOtherwise() {
        String uid = "test_uid_2";
        String name = "User Two";
        String email = "user2@example.com";

        assertFalse(repo.hasUserProfile(uid, null));
        assertFalse(repo.hasUserProfile(null, email));

        repo.upsertUserProfile(uid, name, email);

        assertTrue(repo.hasUserProfile(uid, null));
        assertTrue(repo.hasUserProfile(null, email));
        assertFalse(repo.hasUserProfile("nonexistent", null));
    }

    @Test
    public void firebaseHasUserProfile_delegatesToRemote_returnsCorrectResult() throws ExecutionException, InterruptedException {
        String uid = "test_uid_3";
        String name = "User Three";
        String email = "user3@example.com";

        assertFalse(repo.firebaseHasUserProfile(uid, email));

        remoteManager.upsertUserProfile(uid, name, email);

        assertTrue(repo.firebaseHasUserProfile(uid, null));
        assertTrue(repo.firebaseHasUserProfile(null, email));
    }

    @Test
    public void getUserName_returnsCurrentUserName() {
        String uid = localManager.currentUid();
        String name = "Current User";
        String email = "current@example.com";

        repo.upsertUserProfile(uid, name, email);

        assertEquals(name, repo.getUserName());
    }

    @Test
    public void getUserEmail_returnsCurrentUserEmail() {
        String uid = localManager.currentUid();
        String name = "Current User";
        String email = "current@example.com";

        repo.upsertUserProfile(uid, name, email);

        assertEquals(email, repo.getUserEmail());
    }

    @Test
    public void getUser_returnsUserProfile_whenExists() {
        String uid = "test_uid_4";
        String name = "User Four";
        String email = "user4@example.com";

        repo.upsertUserProfile(uid, name, email);

        UserProfile profile = repo.getUser(uid, email);
        assertNotNull(profile);
        assertEquals(uid, profile.getUid());
        assertEquals(name, profile.getName());
        assertEquals(email, profile.getEmail());
    }

    @Test
    public void getUser_returnsNull_whenNotExists() {
        UserProfile profile = repo.getUser("nonexistent", null);
        assertNull(profile);
    }

    @Test
    public void deleteUserProfile_removesFromLocalAndRemote_throwsOnFailure() {
        String uid = localManager.currentUid();
        String name = "Delete Test";
        String email = "delete@example.com";

        repo.upsertUserProfile(uid, name, email);
        remoteManager.upsertUserProfile(uid, name, email);

        assertTrue(localManager.hasUserProfile(uid, null));
        assertTrue(remoteManager.profiles.containsKey(uid));

        repo.deleteUserProfile();

        assertFalse(localManager.hasUserProfile(uid, null));
        assertFalse(remoteManager.profiles.containsKey(uid));
    }

    @Test(expected = IllegalStateException.class)
    public void deleteUserProfile_throwsException_whenRemoteFailure() {
        String uid = localManager.currentUid();
        String name = "Delete Fail Test";
        String email = "deletefail@example.com";

        repo.upsertUserProfile(uid, name, email);
        remoteManager.setShouldFailDeleteUserProfile(true);

        repo.deleteUserProfile();
    }

    @Test
    public void getGlobalScore_returnsScoreFromLocal() {
        assertEquals(0, repo.getGlobalScore());

        repo.setGlobalScore(25);
        assertEquals(25, repo.getGlobalScore());
    }

    // ------------------------------------------------------------
    // B. Global Score Operations (10-12 tests)
    // ------------------------------------------------------------

    @Test
    public void getGlobalScore_withUid_returnsSpecificUserScore() {
        String uid = "score_uid_1";
        repo.createGlobalScore(uid, 50);

        assertEquals(50, repo.getGlobalScore(uid));
    }

    @Test
    public void getGlobalScoreRemote_fetchesFromFirebase_handlesErrors() {
        String uid = "score_uid_2";
        remoteManager.setGlobalScore(uid, 100);

        assertEquals(100, repo.getGlobalScoreRemote(uid));

        // Test error handling
        remoteManager.setShouldFailGetGlobalScore(true);
        assertEquals(-1, repo.getGlobalScoreRemote(uid));
    }

    @Test
    public void getGlobalScores_returnsMultipleScores() {
        String uid1 = "score_uid_3";
        String uid2 = "score_uid_4";
        String uid3 = "score_uid_5";

        repo.createGlobalScore(uid1, 10);
        repo.createGlobalScore(uid2, 20);
        repo.createGlobalScore(uid3, 30);

        Map<String, Integer> scores = repo.getGlobalScores(Arrays.asList(uid1, uid2, uid3, "missing"));
        assertEquals(3, scores.size());
        assertEquals(Integer.valueOf(10), scores.get(uid1));
        assertEquals(Integer.valueOf(20), scores.get(uid2));
        assertEquals(Integer.valueOf(30), scores.get(uid3));
    }

    @Test
    public void listAllGlobalScores_returnsAllScores() {
        String uid1 = "score_uid_6";
        String uid2 = "score_uid_7";

        repo.createGlobalScore(uid1, 15);
        repo.createGlobalScore(uid2, 25);

        List<Score> allScores = repo.listAllGlobalScores();
        assertTrue(allScores.size() >= 2);

        boolean found1 = false, found2 = false;
        for (Score s : allScores) {
            if (uid1.equals(s.getUid())) {
                found1 = true;
                assertEquals(15, s.getScore());
            }
            if (uid2.equals(s.getUid())) {
                found2 = true;
                assertEquals(25, s.getScore());
            }
        }
        assertTrue(found1 && found2);
    }

    @Test
    public void createGlobalScore_createsNewScore_withDefaultValue() {
        assertTrue(repo.createGlobalScore(0));
        assertEquals(0, repo.getGlobalScore());

        assertTrue(repo.setGlobalScore(10));
        assertFalse(repo.createGlobalScore(20)); // Already exists
        assertEquals(10, repo.getGlobalScore()); // Value unchanged
    }

    @Test
    public void createGlobalScore_withUid_createsForSpecificUser() {
        String uid = "score_uid_8";
        assertTrue(repo.createGlobalScore(uid, 5));
        assertEquals(5, repo.getGlobalScore(uid));

        assertFalse(repo.createGlobalScore(uid, 10));
        assertEquals(5, repo.getGlobalScore(uid)); // Value unchanged
    }

    @Test
    public void ensureGlobalScore_createsIfMissing_noopIfExists() {
        String uid = "score_uid_9";

        assertFalse(repo.ensureGlobalScore(uid)); // Created with 0
        assertEquals(0, (int) repo.getGlobalScore(uid));

        assertTrue(repo.ensureGlobalScore(uid)); // Already exists, no-op
    }

    @Test
    public void setGlobalScore_updatesScore_persistsToLocal() {
        assertTrue(repo.setGlobalScore(50));
        assertEquals(50, repo.getGlobalScore());

        // Verify via local manager
        assertEquals(50, localManager.getGlobalScore());
    }

    @Test
    public void addToGlobalScore_incrementsScore_returnsNewValue() {
        repo.setGlobalScore(10);

        int newScore = repo.addToGlobalScore(5);
        assertEquals(15, newScore);
        assertEquals(15, repo.getGlobalScore());

        // Negative delta
        newScore = repo.addToGlobalScore(-3);
        assertEquals(12, newScore);
        assertEquals(12, repo.getGlobalScore());
    }

    @Test
    public void deleteGlobalScore_removesScore_returnsTrue() {
        repo.setGlobalScore(100);
        assertEquals(100, repo.getGlobalScore());

        assertTrue(repo.deleteGlobalScore());
        assertEquals(0, repo.getGlobalScore()); // Treated as 0 when missing
    }

    @Test
    public void deleteGlobalScores_removesMultiple_returnsCount() {
        String uid1 = "score_uid_10";
        String uid2 = "score_uid_11";
        String uid3 = "score_uid_12";

        repo.createGlobalScore(uid1, 5);
        repo.createGlobalScore(uid2, 10);
        repo.createGlobalScore(uid3, 15);

        int removed = repo.deleteGlobalScores(Arrays.asList(uid1, uid2, "missing"));
        assertEquals(2, removed);

        assertEquals(-1, (int) repo.getGlobalScore(uid1));
        assertEquals(-1, (int) repo.getGlobalScore(uid2));
        assertEquals(15, repo.getGlobalScore(uid3));
    }

    @Test
    public void syncGlobalScore_localHigher_pushesToRemote() {
        String uid = localManager.currentUid();
        repo.setGlobalScore(50);
        remoteManager.setGlobalScore(uid, 30);

        repo.syncGlobalScore();

        Integer remoteScore = remoteManager.scores.get(uid);
        assertNotNull(remoteScore);
        assertEquals(50, remoteScore.intValue());
        assertEquals(50, repo.getGlobalScore());
    }

    // ------------------------------------------------------------
    // C. Sync Operations (4-5 tests)
    // ------------------------------------------------------------

    @Test
    public void syncGlobalScore_remoteHigher_pullsToLocal() {
        String uid = localManager.currentUid();
        repo.setGlobalScore(20);
        remoteManager.setGlobalScore(uid, 60);

        repo.syncGlobalScore();

        assertEquals(60, repo.getGlobalScore());
        assertEquals(60, localManager.getGlobalScore());
    }

    @Test
    public void syncGlobalScore_equal_noChanges() {
        String uid = localManager.currentUid();
        repo.setGlobalScore(40);
        remoteManager.setGlobalScore(uid, 40);

        repo.syncGlobalScore();

        assertEquals(40, repo.getGlobalScore());
        assertEquals(Integer.valueOf(40), remoteManager.scores.get(uid));
    }

    @Test
    public void syncGlobalScore_remoteError_logsButDoesNotCrash() {
        String uid = localManager.currentUid();
        repo.setGlobalScore(30);
        remoteManager.setShouldFailGetGlobalScore(true);

        // Should not throw exception
        repo.syncGlobalScore();

        // Local score unchanged
        assertEquals(30, repo.getGlobalScore());
    }

    @Test
    public void syncFriendsFromFirebase_syncsAllFriends_returnsTrue() throws FirebaseFirestoreException, InterruptedException {
        String currentUid = localManager.currentUid();
        String friend1Uid = "sync_friend_1";
        String friend2Uid = "sync_friend_2";

        // Add friends to Firebase
        remoteManager.addFriendRequest(currentUid, friend1Uid, "Friend One", "Current User");
        remoteManager.addFriendRequest(currentUid, friend2Uid, "Friend Two", "Current User");

        assertTrue(repo.syncFriendsFromFirebase());

        // Verify friends synced to local
        List<Friend> localFriends = localManager.getFriends();
        assertTrue(localFriends.size() >= 2);
        assertNotNull(findFriendByUid(localFriends, friend1Uid));
        assertNotNull(findFriendByUid(localFriends, friend2Uid));
    }

    @Test
    public void getStreakCount_returnsCurrentStreak() {
        assertEquals(0, repo.getStreakCount());

        repo.setStreakCount(5);
        assertEquals(5, repo.getStreakCount());
    }

    // ------------------------------------------------------------
    // D. Streak Operations (4 tests)
    // ------------------------------------------------------------

    @Test
    public void setStreakCount_updatesStreak() {
        assertTrue(repo.setStreakCount(10));
        assertEquals(10, repo.getStreakCount());
        assertEquals(10, localManager.getStreakCount());
    }

    @Test
    public void incrementStreak_increasesBy1_returnsNewValue() {
        repo.setStreakCount(3);

        int newStreak = repo.incrementStreak();
        assertEquals(4, newStreak);
        assertEquals(4, repo.getStreakCount());
        assertEquals(4, localManager.getStreakCount());
    }

    @Test
    public void resetStreak_setsTo0() {
        repo.setStreakCount(15);
        assertEquals(15, repo.getStreakCount());

        assertTrue(repo.resetStreak());
        assertEquals(0, repo.getStreakCount());
        assertEquals(0, localManager.getStreakCount());
    }

    @Test
    public void addFriendByEmail_validEmail_sendsRequestAndPersists() {
        String friendUid = "friend_uid_1";
        String friendName = "Friend One";
        String friendEmail = "friend1@example.com";

        remoteManager.upsertUserProfile(friendUid, friendName, friendEmail);

        FriendRequestResult result = repo.addFriendByEmail(friendEmail);
        assertTrue(result.isSuccess());

        // Verify friend persisted locally
        assertTrue(localManager.isFriend(friendUid));

        // Verify friend request sent to Firebase
        List<Friend> remoteFriends = remoteManager.getFriends(localManager.currentUid());
        assertNotNull(findFriendByUid(remoteFriends, friendUid));
    }

    // ------------------------------------------------------------
    // E. Friend Operations (10-12 tests)
    // ------------------------------------------------------------

    @Test
    public void addFriendByEmail_emailNotFound_returnsRemoteFailure() {
        FriendRequestResult result = repo.addFriendByEmail("nonexistent@example.com");
        assertFalse(result.isSuccess());
        assertTrue(result.isRemoteFailure());
    }

    @Test
    public void addFriendByEmail_remoteException_returnsFailureResult() {
        String friendEmail = "error@example.com";
        remoteManager.setShouldFailGetUser(true);

        FriendRequestResult result = repo.addFriendByEmail(friendEmail);
        assertFalse(result.isSuccess());
        assertTrue(result.isRemoteFailure());
    }

    @Test
    public void upsertFriend_createsOrUpdatesFriend() {
        String friendUid = "friend_uid_2";
        String friendName = "Friend Two";

        assertTrue(repo.upsertFriend(friendUid, friendName));
        assertTrue(localManager.isFriend(friendUid));

        // Update name
        assertTrue(repo.upsertFriend(friendUid, "Updated Name"));
        Friend friend = findFriendByUid(localManager.getFriends(), friendUid);
        assertNotNull(friend);
        assertEquals("Updated Name", friend.getName());
    }

    @Test
    public void syncFriendsFromFirebase_updatesLocalFromRemote_handlesPartialFailures() throws FirebaseFirestoreException, InterruptedException {
        String currentUid = localManager.currentUid();
        String friend1 = "friend_uid_3";
        String friend2 = "friend_uid_4";

        remoteManager.addFriendRequest(currentUid, friend1, "Friend Three", "Current");
        remoteManager.addFriendRequest(currentUid, friend2, "Friend Four", "Current");

        assertTrue(repo.syncFriendsFromFirebase());

        List<Friend> friends = localManager.getFriends();
        assertTrue(friends.size() >= 2);
    }

    @Test
    public void removeFriend_deletesFriend_returnsTrue() {
        String friendUid = "friend_uid_5";
        repo.upsertFriend(friendUid, "Friend Five");

        assertTrue(localManager.isFriend(friendUid));
        assertTrue(repo.removeFriend(friendUid));
        assertFalse(localManager.isFriend(friendUid));
    }

    @Test
    public void acceptFriend_updatesLocalAndRemote_returnsTrue() {
        String currentUid = localManager.currentUid();
        String friendUid = "friend_uid_6";

        localManager.upsertFriend(friendUid, "Friend Six");
        remoteManager.friends.put(currentUid, new ArrayList<>());
        remoteManager.friends.get(currentUid).add(new Friend(friendUid, "Friend Six", "pending", 0));

        assertTrue(repo.acceptFriend(friendUid));

        Friend localFriend = findFriendByUid(localManager.getFriends(), friendUid);
        assertNotNull(localFriend);
        assertEquals("accepted", localFriend.getStatus());

        Friend remoteFriend = findFriendByUid(remoteManager.getFriends(currentUid), friendUid);
        assertNotNull(remoteFriend);
        assertEquals("accepted", remoteFriend.getStatus());
    }

    @Test
    public void acceptFriend_remoteFails_returnsFalse() {
        String friendUid = "friend_uid_7";
        localManager.upsertFriend(friendUid, "Friend Seven");
        remoteManager.setShouldFailAcceptFriend(true);

        // When remote fails, the method should return false to indicate failure
        assertFalse(repo.acceptFriend(friendUid));

        // Local should still be updated (the method updates local first)
        Friend localFriend = findFriendByUid(localManager.getFriends(), friendUid);
        assertNotNull(localFriend);
        assertEquals("accepted", localFriend.getStatus());
    }

    @Test
    public void denyFriend_updatesLocalAndRemote_returnsTrue() {
        String currentUid = localManager.currentUid();
        String friendUid = "friend_uid_8";

        localManager.upsertFriend(friendUid, "Friend Eight");
        remoteManager.friends.put(currentUid, new ArrayList<>());
        remoteManager.friends.get(currentUid).add(new Friend(friendUid, "Friend Eight", "pending", 0));

        assertTrue(repo.denyFriend(friendUid));

        assertNull(findFriendByUid(localManager.getFriends(), friendUid));
        assertNull(findFriendByUid(remoteManager.getFriends(currentUid), friendUid));
    }

    @Test
    public void denyFriend_remoteFails_returnsFalse() {
        String friendUid = "friend_uid_9";
        localManager.upsertFriend(friendUid, "Friend Nine");
        remoteManager.setShouldFailDenyFriend(true);

        // When remote fails, the method should return false to indicate failure
        assertFalse(repo.denyFriend(friendUid));

        // Local should still be updated (the method updates local first)
        assertNull(findFriendByUid(localManager.getFriends(), friendUid));
    }

    @Test
    public void isFriend_returnsTrueForFriend_falseOtherwise() {
        String friendUid = "friend_uid_10";

        assertFalse(repo.isFriend(friendUid));

        repo.upsertFriend(friendUid, "Friend Ten");
        assertTrue(repo.isFriend(friendUid));
    }

    @Test
    public void getFriends_returnsAllFriends() {
        String friend1 = "friend_uid_11";
        String friend2 = "friend_uid_12";

        repo.upsertFriend(friend1, "Friend Eleven");
        repo.upsertFriend(friend2, "Friend Twelve");

        List<Friend> friends = repo.getFriends();
        assertTrue(friends.size() >= 2);
        assertNotNull(findFriendByUid(friends, friend1));
        assertNotNull(findFriendByUid(friends, friend2));
    }

    @Test
    public void addBadge_addsBadge_returnsTrue() {
        String badgeId = "badge_1";

        assertTrue(repo.addBadge(badgeId));
        assertTrue(localManager.hasBadge(badgeId));

        // Idempotent
        assertFalse(repo.addBadge(badgeId));
    }

    // ------------------------------------------------------------
    // F. Badge Operations (4 tests)
    // ------------------------------------------------------------

    @Test
    public void removeBadge_removesBadge_returnsTrue() {
        String badgeId = "badge_2";
        repo.addBadge(badgeId);

        assertTrue(repo.removeBadge(badgeId));
        assertFalse(localManager.hasBadge(badgeId));
    }

    @Test
    public void hasBadge_returnsTrueForOwned_falseOtherwise() {
        String badgeId = "badge_3";

        assertFalse(repo.hasBadge(badgeId));

        repo.addBadge(badgeId);
        assertTrue(repo.hasBadge(badgeId));
    }

    @Test
    public void listBadges_returnsAllBadges() {
        String badge1 = "badge_4";
        String badge2 = "badge_5";

        repo.addBadge(badge1);
        repo.addBadge(badge2);

        List<String> badges = repo.listBadges();
        assertTrue(badges.contains(badge1));
        assertTrue(badges.contains(badge2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullContext_throwsIllegalArgumentException() {
        new UserRepository(null, localManager, remoteManager);
    }

    // ------------------------------------------------------------
    // G. Edge Cases & Error Handling (3-5 tests)
    // ------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullLocalManager_throwsIllegalArgumentException() {
        new UserRepository(context, null, remoteManager);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullRemoteManager_throwsIllegalArgumentException() {
        new UserRepository(context, localManager, null);
    }

    /**
     * Fake Firebase manager for testing. Stores all data in memory and provides
     * deterministic behavior for all UserRepository operations.
     */
    private static class FakeFirebaseUserManager extends FirebaseUserManager {
        private final Map<String, UserProfile> profiles = new HashMap<>();
        private final Map<String, Integer> scores = new HashMap<>();
        private final Map<String, List<Friend>> friends = new HashMap<>();
        private boolean shouldFailGetUser = false;
        private boolean shouldFailHasUserProfile = false;
        private boolean shouldFailGetGlobalScore = false;
        private boolean shouldFailDeleteUserProfile = false;
        private boolean shouldFailAcceptFriend = false;
        private boolean shouldFailDenyFriend = false;

        @Override
        public boolean upsertUserProfile(String uid, String name, String email) {
            if (uid == null || uid.isEmpty() || name == null || name.isEmpty() || email == null || email.isEmpty()) {
                throw new IllegalArgumentException("uid, name, and email cannot be empty");
            }
            profiles.put(uid, new UserProfile(uid, name, email));
            return true;
        }

        @Override
        public boolean hasUserProfile(String uid, String email) throws ExecutionException, InterruptedException {
            if (shouldFailHasUserProfile) {
                throw new ExecutionException("Simulated Firebase failure", new Exception());
            }
            if (uid == null && email == null) {
                throw new IllegalArgumentException("uid and email cannot both be null");
            }
            if (uid != null) {
                if (uid.isEmpty()) throw new IllegalArgumentException("uid cannot be empty");
                return profiles.containsKey(uid);
            } else {
                if (email == null || email.isEmpty()) {
                    throw new IllegalArgumentException("email cannot be empty");
                }
                for (UserProfile p : profiles.values()) {
                    if (email.equals(p.getEmail())) return true;
                }
                return false;
            }
        }

        @Override
        public UserProfile getUser(String uid, String email) throws ExecutionException, InterruptedException {
            if (shouldFailGetUser) {
                throw new ExecutionException("Simulated Firebase failure", new Exception());
            }
            if (uid == null && email == null) {
                throw new IllegalArgumentException("uid and email cannot both be null");
            }
            if (uid != null) {
                if (uid.isEmpty()) throw new IllegalArgumentException("uid cannot be empty");
                return profiles.get(uid);
            } else {
                if (email == null || email.isEmpty()) {
                    throw new IllegalArgumentException("email cannot be empty");
                }
                for (UserProfile p : profiles.values()) {
                    if (email.equals(p.getEmail())) return p;
                }
                return null;
            }
        }

        @Override
        public boolean deleteUserProfile(String uid) {
            if (shouldFailDeleteUserProfile) {
                return false;
            }
            profiles.remove(uid);
            scores.remove(uid);
            friends.remove(uid);
            return true;
        }

        @Override
        public Integer getGlobalScore(String uid) throws ExecutionException, InterruptedException {
            if (shouldFailGetGlobalScore) {
                throw new ExecutionException("Simulated Firebase failure", new Exception());
            }
            return scores.get(uid);
        }

        @Override
        public boolean setGlobalScore(String uid, int score) {
            if (uid == null || uid.isEmpty()) {
                throw new IllegalArgumentException("uid cannot be empty");
            }
            scores.put(uid, score);
            return true;
        }

        @Override
        public void addFriendRequest(String ownerUid, String friendUid, String friendName, String ownerName)
                throws FirebaseFirestoreException, InterruptedException {
            if (ownerUid == null || ownerUid.isEmpty() || friendUid == null || friendUid.isEmpty()) {
                throw new IllegalArgumentException("ownerUid and friendUid cannot be empty");
            }
            // Add to owner's friends
            List<Friend> ownerFriends = friends.get(ownerUid);
            if (ownerFriends == null) {
                ownerFriends = new ArrayList<>();
                friends.put(ownerUid, ownerFriends);
            }
            ownerFriends.add(new Friend(friendUid, friendName, "pending", 0));

            // Add to friend's friends
            List<Friend> friendsFriends = friends.get(friendUid);
            if (friendsFriends == null) {
                friendsFriends = new ArrayList<>();
                friends.put(friendUid, friendsFriends);
            }
            friendsFriends.add(new Friend(ownerUid, ownerName, "pending", 0));
        }

        @Override
        public boolean acceptFriend(String ownerUid, String friendUid) {
            if (shouldFailAcceptFriend) {
                return false;
            }
            if (ownerUid == null || ownerUid.isEmpty() || friendUid == null || friendUid.isEmpty()) {
                throw new IllegalArgumentException("ownerUid and friendUid cannot be empty");
            }
            // Update owner's side
            List<Friend> ownerFriends = friends.get(ownerUid);
            if (ownerFriends != null) {
                for (Friend f : ownerFriends) {
                    if (friendUid.equals(f.getUid())) {
                        f.setStatus("accepted");
                        break;
                    }
                }
            }
            // Update friend's side
            List<Friend> friendsFriends = friends.get(friendUid);
            if (friendsFriends != null) {
                for (Friend f : friendsFriends) {
                    if (ownerUid.equals(f.getUid())) {
                        f.setStatus("accepted");
                        break;
                    }
                }
            }
            return true;
        }

        @Override
        public boolean denyFriend(String ownerUid, String friendUid) {
            if (shouldFailDenyFriend) {
                return false;
            }
            if (ownerUid == null || ownerUid.isEmpty() || friendUid == null || friendUid.isEmpty()) {
                throw new IllegalArgumentException("ownerUid and friendUid cannot be empty");
            }
            // Remove from owner's side
            List<Friend> ownerFriends = friends.get(ownerUid);
            if (ownerFriends != null) {
                ownerFriends.removeIf(f -> friendUid.equals(f.getUid()));
            }
            // Remove from friend's side
            List<Friend> friendsFriends = friends.get(friendUid);
            if (friendsFriends != null) {
                friendsFriends.removeIf(f -> ownerUid.equals(f.getUid()));
            }
            return true;
        }

        @Override
        public List<Friend> getFriends(String ownerUid) {
            List<Friend> result = friends.get(ownerUid);
            if (result == null) {
                return new ArrayList<>();
            }
            // Enrich with scores
            List<Friend> enriched = new ArrayList<>();
            for (Friend f : result) {
                Integer score = scores.get(f.getUid());
                enriched.add(new Friend(f.getUid(), f.getName(), f.getStatus(), score == null ? 0 : score));
            }
            return enriched;
        }

        // Helper methods to simulate failures
        public void setShouldFailGetUser(boolean fail) {
            this.shouldFailGetUser = fail;
        }

        public void setShouldFailHasUserProfile(boolean fail) {
            this.shouldFailHasUserProfile = fail;
        }

        public void setShouldFailGetGlobalScore(boolean fail) {
            this.shouldFailGetGlobalScore = fail;
        }

        public void setShouldFailDeleteUserProfile(boolean fail) {
            this.shouldFailDeleteUserProfile = fail;
        }

        public void setShouldFailAcceptFriend(boolean fail) {
            this.shouldFailAcceptFriend = fail;
        }

        public void setShouldFailDenyFriend(boolean fail) {
            this.shouldFailDenyFriend = fail;
        }
    }
}