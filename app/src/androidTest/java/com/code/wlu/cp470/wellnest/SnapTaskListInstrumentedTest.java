package com.code.wlu.cp470.wellnest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.matcher.IntentMatchers;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.SnapTaskManager;
import com.code.wlu.cp470.wellnest.ui.snaptask.SnapTaskActivity;
import com.code.wlu.cp470.wellnest.ui.snaptask.SnapTaskDetailActivity;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for the SnapTask list screen ({@link SnapTaskActivity}).
 *
 * Scenarios:
 * - List displays tasks with correct completion state and score.
 * - Tapping a task row triggers navigation to the detail screen with the correct arguments.
 */
@RunWith(AndroidJUnit4.class)
public class SnapTaskListInstrumentedTest {

    private Context context;
    private WellnestDatabaseHelper helper;
    private SQLiteDatabase db;
    private SnapTaskManager snapTaskManager;
    private ActivityScenario<SnapTaskActivity> scenario;

    private static final String UID_INCOMPLETE = "task_list_incomplete";
    private static final String UID_COMPLETE = "task_list_complete";

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
        context = ApplicationProvider.getApplicationContext();
        helper = new WellnestDatabaseHelper(context);
        db = helper.getWritableDatabase();
        helper.cleanDatabase(db);

        snapTaskManager = new SnapTaskManager(db);

        // Seed score so the scorecard has a deterministic value.
        snapTaskManager.upsertSnapTaskScore(42);

        // Seed one incomplete and one completed SnapTask.
        snapTaskManager.upsertTask(
                UID_INCOMPLETE,
                "List Incomplete Task",
                10,
                "Description incomplete",
                false
        );
        snapTaskManager.upsertTask(
                UID_COMPLETE,
                "List Completed Task",
                20,
                "Description complete",
                true
        );

        // Launch the SnapTaskActivity
        Intent intent = new Intent(context, SnapTaskActivity.class);
        scenario = ActivityScenario.launch(intent);

        // Give the fragment a moment to inflate and bind views.
        onView(isRoot()).perform(waitFor(200));
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
        if (helper != null && db != null) {
            helper.cleanDatabase(db);
        }
        if (db != null && db.isOpen()) {
            db.close();
        }
        if (helper != null) {
            helper.close();
        }
    }

    /**
     * The SnapTask list should display at least one task and render completed vs incomplete
     * states differently (subtitle text and points visibility), and show the current score.
     */
    @Test
    public void list_displaysTasksWithCorrectCompletionState_andScore() {
        // Score text reflects the value in the local DB.
        onView(withId(R.id.snap_task_score_text))
                .check(matches(withText("42")));

        // Ensure the incomplete task row is visible.
        onView(withId(R.id.snap_task_recycler_view))
                .perform(scrollTo(hasDescendant(withText("List Incomplete Task"))));

        onView(allOf(withId(R.id.task_title), withText("List Incomplete Task")))
                .check(matches(isDisplayed()));
        onView(allOf(
                withId(R.id.task_subtitle),
                withText(R.string.task_unfinished),
                hasSibling(withText("List Incomplete Task"))
        )).check(matches(isDisplayed()));
        // Points should be visible for incomplete tasks. Use a RecyclerView item-level
        // ViewAction so we only inspect the matched row, avoiding ambiguous matches
        // across multiple rows in the list.
        onView(withId(R.id.snap_task_recycler_view))
                .perform(actionOnItem(
                        hasDescendant(allOf(
                                withId(R.id.task_title),
                                withText("List Incomplete Task")
                        )),
                        new ViewAction() {
                            @Override
                            public Matcher<View> getConstraints() {
                                return isDisplayed();
                            }

                            @Override
                            public String getDescription() {
                                return "Check that task_points is VISIBLE for 'List Incomplete Task'";
                            }

                            @Override
                            public void perform(UiController uiController, View view) {
                                View points = view.findViewById(R.id.task_points);
                                assertNotNull("task_points should exist in the incomplete row", points);
                                assertEquals(View.VISIBLE, points.getVisibility());
                            }
                        }
                ));

        // Ensure the completed task row is visible.
        onView(withId(R.id.snap_task_recycler_view))
                .perform(scrollTo(hasDescendant(withText("List Completed Task"))));

        onView(allOf(withId(R.id.task_title), withText("List Completed Task")))
                .check(matches(isDisplayed()));
        onView(allOf(
                withId(R.id.task_subtitle),
                withText(R.string.task_finished),
                hasSibling(withText("List Completed Task"))
        )).check(matches(isDisplayed()));
        // Points should be hidden for completed tasks. Again, operate at the item level so
        // we only inspect the row whose title is 'List Completed Task'.
        onView(withId(R.id.snap_task_recycler_view))
                .perform(actionOnItem(
                        hasDescendant(allOf(
                                withId(R.id.task_title),
                                withText("List Completed Task")
                        )),
                        new ViewAction() {
                            @Override
                            public Matcher<View> getConstraints() {
                                return isDisplayed();
                            }

                            @Override
                            public String getDescription() {
                                return "Check that task_points is GONE for 'List Completed Task'";
                            }

                            @Override
                            public void perform(UiController uiController, View view) {
                                View points = view.findViewById(R.id.task_points);
                                assertNotNull("task_points should exist in the completed row", points);
                                assertEquals(View.GONE, points.getVisibility());
                            }
                        }
                ));
    }

    /**
     * Tapping a SnapTask row should launch the detail activity with the
     * correct intent extras (mode, uid, name, description, points, completed flag).
     */
    @Test
    public void clickTask_launchesDetailActivity_withCorrectExtras() {
        // Initialize Intents to intercept activity launches
        Intents.init();

        try {
            // Set up an ActivityResult stub for SnapTaskDetailActivity
            Intent resultData = new Intent();
            resultData.putExtra(SnapTaskDetailActivity.EXTRA_TASK_UID, UID_INCOMPLETE);
            resultData.putExtra(SnapTaskDetailActivity.EXTRA_TASK_COMPLETED, false);
            
            Instrumentation.ActivityResult result = new Instrumentation.ActivityResult(
                    Activity.RESULT_CANCELED, resultData);
            
            // Stub the activity launch to prevent actually starting it
            Intents.intending(IntentMatchers.hasComponent(SnapTaskDetailActivity.class.getName()))
                    .respondWith(result);

            // Click the incomplete task row
            onView(withId(R.id.snap_task_recycler_view))
                    .perform(actionOnItem(
                            hasDescendant(withText("List Incomplete Task")),
                            click()
                    ));

            // Wait for the click to process
            onView(isRoot()).perform(waitFor(200));

            // Verify that SnapTaskDetailActivity was started with correct extras
            Intents.intended(allOf(
                    IntentMatchers.hasComponent(SnapTaskDetailActivity.class.getName()),
                    IntentMatchers.hasExtra(SnapTaskDetailActivity.EXTRA_MODE, "before"),
                    IntentMatchers.hasExtra(SnapTaskDetailActivity.EXTRA_TASK_UID, UID_INCOMPLETE),
                    IntentMatchers.hasExtra(SnapTaskDetailActivity.EXTRA_TASK_NAME, "List Incomplete Task"),
                    IntentMatchers.hasExtra(SnapTaskDetailActivity.EXTRA_TASK_DESCRIPTION, "Description incomplete"),
                    IntentMatchers.hasExtra(SnapTaskDetailActivity.EXTRA_TASK_POINTS, 10),
                    IntentMatchers.hasExtra(SnapTaskDetailActivity.EXTRA_TASK_COMPLETED, false)
            ));
        } finally {
            Intents.release();
        }
    }
}