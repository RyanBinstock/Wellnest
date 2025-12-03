package com.code.wlu.cp470.wellnest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.code.wlu.cp470.wellnest.data.UserModels.Friend;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.UserManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseUserManager;
import com.code.wlu.cp470.wellnest.ui.friends.FriendsFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class FriendsFragmentInstrumentedTest {
    UserManager userManager;
    String[] uids = {"uid1", "uid2", "uid3", "uid4", "uid5", "uid6", "uid7", "uid8", "uid9", "uid10"};
    String[] names = {"Alice", "Bob", "Charlie", "David", "Eve", "Frank", "Grace", "Hank", "Ivy", "Jack"};
    int[] scores = {100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};
    List<Friend> testFriends = new ArrayList<>();
    private Context context;
    private FragmentScenario<FriendsFragment> scenario;
    private WellnestDatabaseHelper helper;
    private SQLiteDatabase db;

    public static ViewAction waitFor(long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "wait for " + millis + " milliseconds";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadForAtLeast(millis);
            }
        };
    }

    @Before
    public void setUp() throws InterruptedException {
        // 1) Get a valid Context
        context = androidx.test.core.app.ApplicationProvider.getApplicationContext();

        // Wait for Firebase anonymous auth to complete
        CountDownLatch authLatch = new CountDownLatch(1);
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener(r -> {
                    Log.d("Auth", "anon ok, uid=" + r.getUser().getUid());
                    authLatch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e("Auth", "anon failed", e);
                    authLatch.countDown();
                });
        
        // Wait up to 10 seconds for auth to complete
        authLatch.await(10, TimeUnit.SECONDS);

        // 2) Open DB + seed
        helper = new WellnestDatabaseHelper(context);
        db = helper.getWritableDatabase();
        userManager = new UserManager(db);
        
        // CRITICAL: Create a user_profile row so that currentUid() doesn't throw
        // Use a test UID that will be used for the current user context
        userManager.upsertUserProfile("testCurrentUser", "Test User", "test@test.com");
        
        for (int i = 0; i < uids.length; i++) {
            String uid = uids[i];
            String name = names[i];
            int score = scores[i];
            userManager.upsertFriend(uid, name);
            userManager.setGlobalScore(uid, score);
            userManager.acceptFriend(uid);
        }
        
        // Force database writes to be visible to other connections by closing and reopening
        // This flushes the WAL (Write-Ahead Log) and ensures data visibility
        db.close();
        helper.close();
        helper = new WellnestDatabaseHelper(context);
        db = helper.getWritableDatabase();
        userManager = new UserManager(db);
        
        // Verify data was persisted correctly
        List<Friend> friends = userManager.getFriends();
        Log.d("TestSetup", "Friends in DB after reopen: " + friends.size());
        for (Friend f : friends) {
            Log.d("TestSetup", "Friend: " + f.getName() + " (" + f.getUid() + ") status=" + f.getStatus());
        }
        
        // 3) Launch the fragment
        FragmentFactory factory = new FragmentFactory() {
            @NonNull
            @Override
            public Fragment instantiate(@NonNull ClassLoader cl, @NonNull String className) {
                if (className.equals(FriendsFragment.class.getName())) {
                    return new FriendsFragment();
                }
                return super.instantiate(cl, className);
            }
        };

        scenario = FragmentScenario.launchInContainer(
                FriendsFragment.class,
                /* args */ null,
                R.style.Theme_Wellnest,
                factory
        );
        
        // Wait for the ViewModel's async loading to complete
        // The FriendViewModel loads data on a background thread and makes
        // Firebase network calls that can take several seconds
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            Log.e("Test", "Sleep interrupted", e);
        }
    }

    @After
    public void tearDown() {
        if (scenario != null) scenario.close();
        helper.cleanDatabase(db);
        if (db != null && db.isOpen()) db.close();
        if (helper != null) helper.close();
    }

    @Test
    public void testLayoutInflatesWithAllFriends() {
        for (int i = 0; i < uids.length; i++) {
            String name = names[i];
            String score = String.valueOf(scores[i]);
            onView(withId(R.id.friendsListRecyclerView))
                    .perform(scrollTo(hasDescendant(withText(name))));
            onView(withId(R.id.friendsListRecyclerView))
                    .check(matches(hasDescendant(withText(name))));
            onView(withId(R.id.friendsListRecyclerView))
                    .check(matches(hasDescendant(withText((score)))));
        }
    }

    @Test
    public void testFriendRemoval() {
        int positionToRemove = 0;
        String uidToRemove = uids[positionToRemove];
        String nameToRemove = names[positionToRemove];

        onView(withId(R.id.friendsListRecyclerView))
                .check(matches(hasDescendant(withText(nameToRemove)))); // ensure present before

        onView(allOf(withId(R.id.remove_friend_button),
                hasSibling(withText(nameToRemove))))
                .perform(click());

        // Wait for the ViewModel's refreshFriends() to complete
        // This includes Firebase sync + UI animation (~290ms click animation + ~1s refresh)
        onView(isRoot()).perform(waitFor(2500));
        onView(withText(nameToRemove)).check(doesNotExist());

        // if you really need the DB assertion:
        assert !userManager.isFriend(uidToRemove);
    }

    @Test
    public void testAddFriend() {
        String newFriendEmail = "randomemail@domain.com";
        String newFriendUid = "NewUID";
        String newFriendName = "NewFriend";

        FirebaseUserManager firebaseUserManager = new FirebaseUserManager();
        firebaseUserManager.upsertUserProfile(newFriendUid, newFriendName, newFriendEmail);

        onView(withId(R.id.friendSearchEditText)).perform(typeText(newFriendEmail));
        onView(withId(R.id.friendSearchSendButton)).perform(click());
        onView(isRoot()).perform(waitFor(1500));

        assert userManager.isFriend(newFriendUid);

        // ---------- CLEANUP ----------
        // Delete the test user document (and its subcollections if you like)
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(newFriendUid).delete();
    }

}
