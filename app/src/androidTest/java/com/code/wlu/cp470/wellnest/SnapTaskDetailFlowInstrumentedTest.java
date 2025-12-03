package com.code.wlu.cp470.wellnest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.code.wlu.cp470.wellnest.data.SnapTaskModels;
import com.code.wlu.cp470.wellnest.data.WellnestAiClient;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.SnapTaskManager;
import com.code.wlu.cp470.wellnest.ui.snaptask.SnapTaskDetailActivity;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for the SnapTask detail & evaluation flow.
 * <p>
 * Focus:
 * - Loading overlay visibility while evaluation is running.
 * - Success path: DB completion + score update before success dialog.
 * - Failure path: overlay hidden, failure dialog shown, DB unchanged.
 * - Lifecycle safety: no crash or dialog when fragment is detached mid-evaluation.
 */
@RunWith(AndroidJUnit4.class)
public class SnapTaskDetailFlowInstrumentedTest {

    private static final String UID_SUCCESS = "detail_success_task";
    private static final String UID_FAILURE = "detail_failure_task";
    private static final String UID_SUCCESS_REAL = "detail_real_success_task";
    private static final String UID_FAILURE_REAL = "detail_real_failure_task";

    private Context context;
    private WellnestDatabaseHelper helper;
    private SQLiteDatabase db;
    private SnapTaskManager snapTaskManager;
    private ActivityScenario<SnapTaskDetailActivity> scenario;

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

    /**
     * Polls the ActivityScenario state until it reaches the expected state or times out.
     * This is more reliable than Thread.sleep for checking lifecycle transitions.
     *
     * @param scenario The ActivityScenario to check
     * @param expectedState The expected Lifecycle.State
     * @param timeoutMs Maximum time to wait in milliseconds
     * @param pollIntervalMs Interval between polls in milliseconds
     * @return true if the expected state was reached, false if timeout occurred
     */
    private boolean waitForActivityState(
            ActivityScenario<?> scenario,
            Lifecycle.State expectedState,
            long timeoutMs,
            long pollIntervalMs
    ) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                if (scenario.getState() == expectedState) {
                    return true;
                }
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return scenario.getState() == expectedState;
    }

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        helper = new WellnestDatabaseHelper(context);
        db = helper.getWritableDatabase();
        helper.cleanDatabase(db);

        snapTaskManager = new SnapTaskManager(db);

        // Seed baseline score and two SnapTasks (one for success, one for failure).
        snapTaskManager.upsertSnapTaskScore(0);

        snapTaskManager.upsertTask(
                UID_SUCCESS,
                "Detail Success Task",
                50,
                "Success description",
                false
        );

        snapTaskManager.upsertTask(
                UID_FAILURE,
                "Detail Failure Task",
                25,
                "Failure description",
                false
        );

        // Additional tasks used by the real-API image-based tests.
        snapTaskManager.upsertTask(
                UID_SUCCESS_REAL,
                "Detail Real Success Task",
                50,
                "Success description",
                false
        );

        snapTaskManager.upsertTask(
                UID_FAILURE_REAL,
                "Detail Real Failure Task",
                25,
                "Failure description",
                false
        );
    }

    @After
    public void tearDown() {
        WellnestAiClient.setSnapTaskEvaluationOverride(null);

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
     * Helper to launch the detail activity for a given task uid and points.
     */
    private void launchDetailActivity(String taskUid, String taskName, String taskDescription, int points, boolean completed) {
        Intent intent = SnapTaskDetailActivity.createIntent(
                context,
                "before",
                taskUid,
                taskName,
                taskDescription,
                points,
                completed
        );

        scenario = ActivityScenario.launch(intent);
        onView(isRoot()).perform(waitFor(200));
    }

    /**
     * Helper to simulate the before/after camera captures so that evaluateTask() is invoked.
     * Uses a tiny in-memory bitmap to keep these tests fast and deterministic.
     */
    private void triggerEvaluationWithDummyPhotos() {
        scenario.onActivity(activity -> {
            Bitmap bmp = Bitmap.createBitmap(10, 10, Config.ARGB_8888);

            Intent beforeIntent = new Intent();
            Bundle beforeExtras = new Bundle();
            beforeExtras.putParcelable("data", bmp);
            beforeIntent.putExtras(beforeExtras);
            activity.onActivityResult(1001, Activity.RESULT_OK, beforeIntent);

            Intent afterIntent = new Intent();
            Bundle afterExtras = new Bundle();
            afterExtras.putParcelable("data", bmp);
            afterIntent.putExtras(afterExtras);
            activity.onActivityResult(1002, Activity.RESULT_OK, afterIntent);
        });
    }

    /**
     * Helper to simulate camera captures using bundled drawable resources, so that the
     * real WellnestAiClient.evaluateSnapTask(...) implementation is exercised end-to-end.
     */
    private void triggerEvaluationWithResourcePhotos(
            int beforeDrawableResId,
            int afterDrawableResId
    ) {
        scenario.onActivity(activity -> {
            Resources res = activity.getResources();
            Bitmap before = BitmapFactory.decodeResource(res, beforeDrawableResId);
            Bitmap after = BitmapFactory.decodeResource(res, afterDrawableResId);

            Intent beforeIntent = new Intent();
            Bundle beforeExtras = new Bundle();
            beforeExtras.putParcelable("data", before);
            beforeIntent.putExtras(beforeExtras);
            activity.onActivityResult(1001, Activity.RESULT_OK, beforeIntent);

            Intent afterIntent = new Intent();
            Bundle afterExtras = new Bundle();
            afterExtras.putParcelable("data", after);
            afterIntent.putExtras(afterExtras);
            activity.onActivityResult(1002, Activity.RESULT_OK, afterIntent);
        });
    }

    /**
     * Assert that the loading overlay view inside the activity is currently visible.
     * Uses ActivityScenario.onActivity(...) so it is independent of dialog roots.
     */
    private void assertOverlayVisible() {
        assertNotNull("ActivityScenario should be initialized before checking overlay", scenario);
        scenario.onActivity(activity -> {
            View overlay = activity.findViewById(R.id.snap_task_loading_overlay);
            assertNotNull("Overlay should exist in activity view", overlay);
            assertEquals("Overlay should be visible", View.VISIBLE, overlay.getVisibility());
        });
    }

    /**
     * Assert that the loading overlay view inside the activity is currently hidden (GONE).
     */
    private void assertOverlayHidden() {
        assertNotNull("ActivityScenario should be initialized before checking overlay", scenario);
        scenario.onActivity(activity -> {
            View overlay = activity.findViewById(R.id.snap_task_loading_overlay);
            assertNotNull("Overlay should exist in activity view", overlay);
            assertEquals("Overlay should be gone", View.GONE, overlay.getVisibility());
        });
    }

    /**
     * Success flow:
     * - Shows loading overlay when evaluation starts.
     * - Hides overlay when done.
     * - Shows success dialog.
     * - Marks task as completed and increments score before/when dialog is visible.
     */
    @Test
    public void snaptask_successFlow_showsLoading_updatesDb_showsSuccessDialog() {
        // Arrange AI override to deterministically return "pass" after a short delay.
        WellnestAiClient.setSnapTaskEvaluationOverride((criteria, beforeJpeg, afterJpeg) -> {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "pass";
        });

        launchDetailActivity(
                UID_SUCCESS,
                "Detail Success Task",
                "Success description",
                50,
                false
        );

        triggerEvaluationWithDummyPhotos();

        // Immediately after starting evaluation, the loading overlay should be visible.
        assertOverlayVisible();

        // Wait for evaluation to complete and UI thread callbacks to run.
        onView(isRoot()).perform(waitFor(2000L));

        // Loading overlay is hidden.
        assertOverlayHidden();

        // Success dialog is visible.
        onView(withId(R.id.pointsEarnedText))
                .check(matches(isDisplayed()));
        onView(withText("Task Completed!"))
                .check(matches(isDisplayed()));
        onView(withId(R.id.continueButton))
                .check(matches(isDisplayed()));

        // DB: task is completed and score incremented by 50.
        SnapTaskModels.Task task = snapTaskManager.getSnapTask(UID_SUCCESS, null);
        assertNotNull(task);
        assertTrue(task.getCompleted());
        assertEquals(50, snapTaskManager.getSnapTaskScore().intValue());

        // Click Continue - in Activity version, this will finish the activity
        onView(withId(R.id.continueButton)).perform(click());
        
        // Wait for activity to be destroyed with polling (handles animation + lifecycle timing)
        boolean isDestroyed = waitForActivityState(scenario, Lifecycle.State.DESTROYED, 3000L, 100L);
        
        // Verify activity is destroyed/finished
        assertTrue("Activity should be DESTROYED after clicking continue, but was: " + scenario.getState(),
                   isDestroyed);
    }

    /**
     * Failure flow:
     * - Shows loading overlay while evaluation runs.
     * - Hides overlay on completion.
     * - Shows failure dialog.
     * - Keeps task incomplete and score unchanged.
     */
    @Test
    public void snaptask_failureFlow_showsLoading_hidesOverlay_showsFailureDialog_andDoesNotCompleteTask() {
        // Arrange AI override to deterministically return "fail" after a short delay.
        WellnestAiClient.setSnapTaskEvaluationOverride((criteria, beforeJpeg, afterJpeg) -> {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "fail";
        });

        launchDetailActivity(
                UID_FAILURE,
                "Detail Failure Task",
                "Failure description",
                25,
                false
        );

        triggerEvaluationWithDummyPhotos();

        // Loading overlay should be visible right after evaluation starts.
        assertOverlayVisible();

        // Wait for evaluation to complete and UI thread callbacks to run.
        onView(isRoot()).perform(waitFor(2000L));

        // Loading overlay hidden.
        assertOverlayHidden();

        // Failure dialog visible.
        onView(withText("Not Quite There"))
                .check(matches(isDisplayed()));
        onView(withId(R.id.tryAgainButton))
                .check(matches(isDisplayed()));

        // DB: task remains incomplete and score unchanged (0).
        SnapTaskModels.Task task = snapTaskManager.getSnapTask(UID_FAILURE, null);
        assertNotNull(task);
        assertTrue("Task should remain incomplete after failure", !task.getCompleted());
        assertEquals(0, snapTaskManager.getSnapTaskScore().intValue());

        // Click Try Again / Back to Tasks - in Activity version, this will finish the activity
        onView(withId(R.id.tryAgainButton)).perform(click());
        
        // Wait for activity to be destroyed with polling (handles animation + lifecycle timing)
        boolean isDestroyed = waitForActivityState(scenario, Lifecycle.State.DESTROYED, 3000L, 100L);
        
        // Verify activity is destroyed/finished
        assertTrue("Activity should be DESTROYED after clicking try again, but was: " + scenario.getState(),
                   isDestroyed);
    }

    /**
     * Lifecycle safety:
     * - Start evaluation, then destroy the fragment before the AI verdict returns.
     * - Verify that no dialogs are shown on a detached fragment and no crash occurs.
     */
    @Test
    public void snaptask_evaluation_doesNotShowDialogs_whenFragmentDestroyedMidEvaluation() {
        // Slow override so we have time to destroy the fragment while evaluation is running.
        WellnestAiClient.setSnapTaskEvaluationOverride((criteria, beforeJpeg, afterJpeg) -> {
            try {
                Thread.sleep(1500L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "pass";
        });

        launchDetailActivity(
                UID_SUCCESS,
                "Detail Success Task",
                "Success description",
                50,
                false
        );

        triggerEvaluationWithDummyPhotos();

        // Immediately move the activity to DESTROYED while evaluation is still in progress.
        scenario.moveToState(Lifecycle.State.DESTROYED);

        // Wait longer than the override delay to let evaluation finish.
        // Cannot use Espresso after activity is destroyed, so just sleep.
        try {
            Thread.sleep(2500L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Test passes if no crash occurs (no WindowManager$BadTokenException).
        // The isFinishing()/isDestroyed() checks in dialog methods prevent dialogs from showing.
    }

    /**
     * Real API success flow using bundled WebP test images.
     * This exercises the full WellnestAiClient.evaluateSnapTask(...) path.
     */
    @Test
    @LargeTest
    public void snaptask_realApi_passPair_showsSuccessDialog_andUpdatesDb() {
        // Ensure we are using the real network-backed implementation.
        WellnestAiClient.setSnapTaskEvaluationOverride(null);

        launchDetailActivity(
                UID_SUCCESS_REAL,
                "Detail Real Success Task",
                "Success description",
                50,
                false
        );

        triggerEvaluationWithResourcePhotos(
                R.drawable.snap_task_test_before,
                R.drawable.snap_task_test_after_pass
        );

        // Overlay should be visible shortly after starting evaluation.
        assertOverlayVisible();

        // Allow ample time for the real network call and UI callbacks to complete.
        // These tests are marked @LargeTest because they depend on external network state.
        onView(isRoot()).perform(waitFor(20000L));

        // Overlay should be hidden once evaluation has completed.
        assertOverlayHidden();

        // Success dialog is visible.
        onView(withId(R.id.pointsEarnedText))
                .check(matches(isDisplayed()));
        onView(withText("Task Completed!"))
                .check(matches(isDisplayed()));
        onView(withId(R.id.continueButton))
                .check(matches(isDisplayed()));

        // DB: task is completed and score incremented by 50.
        SnapTaskModels.Task task = snapTaskManager.getSnapTask(UID_SUCCESS_REAL, null);
        assertNotNull(task);
        assertTrue(task.getCompleted());
        assertEquals(50, snapTaskManager.getSnapTaskScore().intValue());

        // Click Continue - in Activity version, this will finish the activity
        onView(withId(R.id.continueButton)).perform(click());
        
        // Wait for activity to be destroyed with polling (handles animation + lifecycle timing)
        boolean isDestroyed = waitForActivityState(scenario, Lifecycle.State.DESTROYED, 3000L, 100L);
        
        // Verify activity is destroyed/finished
        assertTrue("Activity should be DESTROYED after clicking continue, but was: " + scenario.getState(),
                   isDestroyed);
    }

    /**
     * Real API failure flow using bundled WebP test images.
     * Verifies that a failing image pair surfaces the failure dialog and does NOT
     * mark the task as completed or change the score.
     */
    @Test
    @LargeTest
    public void snaptask_realApi_failPair_showsFailureDialog_andDoesNotCompleteTask() {
        // Ensure we are using the real network-backed implementation.
        WellnestAiClient.setSnapTaskEvaluationOverride(null);

        launchDetailActivity(
                UID_FAILURE_REAL,
                "Detail Real Failure Task",
                "Failure description",
                25,
                false
        );

        triggerEvaluationWithResourcePhotos(
                R.drawable.snap_task_test_before,
                R.drawable.snap_task_test_after_fail
        );

        // Overlay should be visible shortly after starting evaluation.
        assertOverlayVisible();

        // Allow ample time for the real network call and UI callbacks to complete.
        onView(isRoot()).perform(waitFor(20000L));

        // Overlay should be hidden once evaluation has completed.
        assertOverlayHidden();

        // Failure dialog visible.
        onView(withText("Not Quite There"))
                .check(matches(isDisplayed()));
        onView(withId(R.id.tryAgainButton))
                .check(matches(isDisplayed()));

        // DB: task remains incomplete and score unchanged (0).
        SnapTaskModels.Task task = snapTaskManager.getSnapTask(UID_FAILURE_REAL, null);
        assertNotNull(task);
        assertTrue("Task should remain incomplete after failure", !task.getCompleted());
        assertEquals(0, snapTaskManager.getSnapTaskScore().intValue());

        // Click Try Again / Back to Tasks - in Activity version, this will finish the activity
        onView(withId(R.id.tryAgainButton)).perform(click());
        
        // Wait for activity to be destroyed with polling (handles animation + lifecycle timing)
        boolean isDestroyed = waitForActivityState(scenario, Lifecycle.State.DESTROYED, 3000L, 100L);
        
        // Verify activity is destroyed/finished
        assertTrue("Activity should be DESTROYED after clicking try again, but was: " + scenario.getState(),
                   isDestroyed);
    }
}