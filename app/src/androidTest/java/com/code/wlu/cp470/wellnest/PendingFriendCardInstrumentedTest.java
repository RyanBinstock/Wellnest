package com.code.wlu.cp470.wellnest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

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
import com.code.wlu.cp470.wellnest.ui.friends.PendingFriendCardFragment;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class PendingFriendCardInstrumentedTest {
    String uid = "random-Uid";
    String name = "Alice";
    int score = 100;
    UserManager userManager;
    private Context context;
    private FragmentScenario<PendingFriendCardFragment> scenario;
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
        userManager.upsertFriend(uid, name);
        userManager.setGlobalScore(uid, score);

        // 3) Launch the fragment
        FragmentFactory factory = new FragmentFactory() {
            @NonNull
            @Override
            public Fragment instantiate(@NonNull ClassLoader cl, @NonNull String className) {
                if (className.equals(PendingFriendCardFragment.class.getName())) {
                    return new PendingFriendCardFragment(uid, name, score);
                }
                return super.instantiate(cl, className);
            }
        };

        scenario = FragmentScenario.launchInContainer(
                PendingFriendCardFragment.class,
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
    public void testInflateWithData() {
        onView(withId(R.id.name)).check(matches(withText(name)));
        onView(withId(R.id.score)).check(matches(withText(String.valueOf(score))));
    }

    @Test
    public void testRemoveFriendSuccess() {
        onView(withId(R.id.remove_friend_button)).perform(click());
        onView(isRoot()).perform(waitFor(400));
        List<UserInterface.Friend> friends = userManager.getFriends();
        assertEquals(0, friends.size());
    }

    @Test
    public void testAcceptFriendSuccess() {
        onView(withId(R.id.accept_friend_button)).perform(click());
        onView(isRoot()).perform(waitFor(400));
        List<UserInterface.Friend> friends = userManager.getFriends();
        assertEquals(1, friends.size());
        assertEquals("accepted", friends.get(0).status);

    }

}
