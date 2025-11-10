package com.code.wlu.cp470.wellnest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.code.wlu.cp470.wellnest.data.UserInterface;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.UserManager;
import com.code.wlu.cp470.wellnest.ui.friends.FriendsFragment;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class FriendsFragmentInstrumentedTest {
    UserManager userManager;
    String[] uids = {"uid1", "uid2", "uid3", "uid4", "uid5", "uid6", "uid7", "uid8", "uid9", "uid10"};
    String[] names = {"Alice", "Bob", "Charlie", "David", "Eve", "Frank", "Grace", "Hank", "Ivy", "Jack"};
    int[] scores = {100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};
    List<UserInterface.Friend> testFriends = new ArrayList<>();
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
    public void setUp() {
        // 1) Get a valid Context
        context = androidx.test.core.app.ApplicationProvider.getApplicationContext();

        // 2) Open DB + seed
        helper = new WellnestDatabaseHelper(context);
        db = helper.getWritableDatabase();
        userManager = new UserManager(db);
        for (int i = 0; i < uids.length; i++) {
            String uid = uids[i];
            String name = names[i];
            int score = scores[i];
            userManager.upsertFriend(uid, name);
            userManager.setGlobalScore(uid, score);
            userManager.acceptFriend(uid);
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
}
