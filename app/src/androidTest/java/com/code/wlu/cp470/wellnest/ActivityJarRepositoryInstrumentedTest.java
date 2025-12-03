package com.code.wlu.cp470.wellnest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.code.wlu.cp470.wellnest.data.ActivityJarModels;
import com.code.wlu.cp470.wellnest.data.ActivityJarRepository;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.ActivityJarManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseUserManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Instrumented tests for ActivityJarRepository.
 * Tests local score operations and remote Firebase sync functionality.
 */
@RunWith(AndroidJUnit4.class)
public class ActivityJarRepositoryInstrumentedTest {

    private static final String USER_REPO_PREFS = "user_repo_prefs";
    private static final String PREFS_UID = "uid";

    private Context context;
    private WellnestDatabaseHelper helper;
    private SQLiteDatabase db;
    private ActivityJarManager localManager;
    private FakeFirebaseUserManager remoteManager;
    private ActivityJarRepository repo;

    /**
     * Fake Firebase manager for testing. Stores ActivityJar scores in memory
     * and provides deterministic behavior for repository operations.
     */
    private static class FakeFirebaseUserManager extends FirebaseUserManager {
        private final Map<String, Integer> activityJarScores = new HashMap<>();

        @Override
        public boolean upsertActivityJarScore(ActivityJarModels.ActivityJarScore activityJarScore) {
            if (activityJarScore == null) {
                throw new IllegalArgumentException("activityJarScore == null");
            }
            String uid = activityJarScore.getUid();
            if (uid == null || uid.isEmpty()) {
                throw new IllegalArgumentException("activityJarScore.uid is null or empty");
            }
            activityJarScores.put(uid, activityJarScore.getScore());
            return true;
        }

        @Override
        public ActivityJarModels.ActivityJarScore getActivityJarScore(String uid) {
            Integer score = activityJarScores.get(uid);
            return new ActivityJarModels.ActivityJarScore(uid, score == null ? 0 : score);
        }

        public void setFakeScore(String uid, int score) {
            activityJarScores.put(uid, score);
        }

        public int getFakeScore(String uid) {
            Integer score = activityJarScores.get(uid);
            return score == null ? 0 : score;
        }

        public void clearScores() {
            activityJarScores.clear();
        }
    }

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        helper = new WellnestDatabaseHelper(context);
        db = helper.getWritableDatabase();
        helper.cleanDatabase(db);

        localManager = new ActivityJarManager(db);
        remoteManager = new FakeFirebaseUserManager();
        repo = new ActivityJarRepository(context, localManager, remoteManager);
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
        // Clean up SharedPreferences
        context.getSharedPreferences("activityJar_repo_prefs", Context.MODE_PRIVATE)
                .edit().clear().apply();
        // Clean up UserRepository prefs to avoid test pollution
        context.getSharedPreferences(USER_REPO_PREFS, Context.MODE_PRIVATE)
                .edit().clear().apply();
    }

    /**
     * Helper method to seed the uid in UserRepository's SharedPreferences
     */
    private void seedUserRepoUid(String uid) {
        context.getSharedPreferences(USER_REPO_PREFS, Context.MODE_PRIVATE)
                .edit().putString(PREFS_UID, uid).apply();
    }

    // ============================================================
    // Remote Score Operations Tests
    // ============================================================

    @Test
    public void getActivityJarScoreRemote_returnsScoreFromRemote() {
        String uid = "activityjar_uid_1";
        remoteManager.upsertActivityJarScore(new ActivityJarModels.ActivityJarScore(uid, 42));

        ActivityJarModels.ActivityJarScore score = repo.getActivityJarScoreRemote(uid);
        assertNotNull(score);
        assertEquals(uid, score.getUid());
        assertEquals(42, score.getScore());
    }

    @Test
    public void upsertActivityJarScoreRemote_writesToRemote() {
        String uid = "activityjar_uid_2";
        ActivityJarModels.ActivityJarScore score = new ActivityJarModels.ActivityJarScore(uid, 99);

        assertTrue(repo.upsertActivityJarScoreRemote(score));

        ActivityJarModels.ActivityJarScore retrieved = repo.getActivityJarScoreRemote(uid);
        assertNotNull(retrieved);
        assertEquals(99, retrieved.getScore());
    }

    // ============================================================
    // Local Score Operations Tests
    // ============================================================

    @Test
    public void getActivityJarScore_returnsLocalScore() {
        // Initial score should be 0 or null
        Integer initialScore = repo.getActivityJarScore();
        assertTrue("Initial score should be 0", initialScore == null || initialScore == 0);

        // Upsert a score
        repo.upsertActivityJarScore(50);
        
        Integer score = repo.getActivityJarScore();
        assertNotNull(score);
        assertEquals(50, score.intValue());
    }

    @Test
    public void upsertActivityJarScore_persistsLocally() {
        assertTrue(repo.upsertActivityJarScore(75));
        
        Integer score = repo.getActivityJarScore();
        assertNotNull(score);
        assertEquals(75, score.intValue());
    }

    // ============================================================
    // Once-Daily Score Sync Tests
    // ============================================================

    /**
     * Test that syncActivityJarScoreOnceDaily syncs scores when due (past sync date).
     * Verifies that max(local, remote) reconciliation and date update occur.
     */
    @Test
    public void syncActivityJarScoreOnceDaily_whenDue_syncsScoresAndRecordsDate() {
        String testUid = "activityjar_sync_test_1";

        // Pre-seed UserRepository's SharedPreferences with test uid
        seedUserRepoUid(testUid);

        // Clear any existing sync date by setting it to a past day
        SharedPreferences prefs = context.getSharedPreferences("activityJar_repo_prefs", Context.MODE_PRIVATE);
        String key = "last_sync_activity_jar_score_epoch_day_" + testUid;
        prefs.edit().putLong(key, LocalDate.now().toEpochDay() - 1).apply();

        // Set up: local=10, remote=50 => expected final=50
        repo.upsertActivityJarScore(10);
        remoteManager.setFakeScore(testUid, 50);

        // Act
        repo.syncActivityJarScoreOnceDaily();

        // Assert: sync date should be updated to today
        long storedEpochDay = prefs.getLong(key, Long.MIN_VALUE);
        assertEquals("Sync date should be today", LocalDate.now().toEpochDay(), storedEpochDay);

        // Assert: remote should have the reconciled score (max of 10 and 50 = 50)
        ActivityJarModels.ActivityJarScore remoteScore = repo.getActivityJarScoreRemote(testUid);
        assertNotNull("Remote score should exist", remoteScore);
        assertEquals("Remote score should be max(10,50)=50", 50, remoteScore.getScore());
    }

    /**
     * Test that syncActivityJarScoreOnceDaily is skipped when already synced today.
     */
    @Test
    public void syncActivityJarScoreOnceDaily_whenAlreadySyncedToday_skipsSync() {
        String testUid = "activityjar_sync_test_2";

        // Pre-seed UserRepository's SharedPreferences with test uid
        seedUserRepoUid(testUid);

        // Set up: local=5, remote=999
        repo.upsertActivityJarScore(5);
        remoteManager.setFakeScore(testUid, 999);

        // Set sync date to today - this should prevent sync
        SharedPreferences prefs = context.getSharedPreferences("activityJar_repo_prefs", Context.MODE_PRIVATE);
        String key = "last_sync_activity_jar_score_epoch_day_" + testUid;
        prefs.edit().putLong(key, LocalDate.now().toEpochDay()).apply();

        // Act
        repo.syncActivityJarScoreOnceDaily();

        // The sync date should remain at today (unchanged)
        long storedEpochDay = prefs.getLong(key, Long.MIN_VALUE);
        assertEquals("Sync date should still be today", LocalDate.now().toEpochDay(), storedEpochDay);

        // Local score should remain unchanged (not synced to 999)
        Integer localScore = repo.getActivityJarScore();
        assertNotNull("Local score should exist", localScore);
        assertEquals("Local score should remain 5 (no sync)", 5, localScore.intValue());
    }

    /**
     * Test that syncActivityJarScoreOnceDaily handles local > remote correctly.
     * When local is higher, remote should be updated.
     */
    @Test
    public void syncActivityJarScoreOnceDaily_localHigher_updatesRemote() {
        String testUid = "activityjar_sync_test_3";

        // Pre-seed UserRepository's SharedPreferences with test uid
        seedUserRepoUid(testUid);

        // Clear sync date
        SharedPreferences prefs = context.getSharedPreferences("activityJar_repo_prefs", Context.MODE_PRIVATE);
        String key = "last_sync_activity_jar_score_epoch_day_" + testUid;
        prefs.edit().remove(key).apply();

        // Set up: local=100, remote=30 => expected final=100
        repo.upsertActivityJarScore(100);
        remoteManager.setFakeScore(testUid, 30);

        // Act
        repo.syncActivityJarScoreOnceDaily();

        // Assert: remote should be updated to 100
        assertEquals("Remote score should be updated to 100", 100, remoteManager.getFakeScore(testUid));
    }

    /**
     * Test that syncActivityJarScoreOnceDaily handles null/empty uid gracefully.
     */
    @Test
    public void syncActivityJarScoreOnceDaily_nullUid_doesNotCrash() {
        // Clear the uid from UserRepository prefs to simulate null/empty uid
        context.getSharedPreferences(USER_REPO_PREFS, Context.MODE_PRIVATE)
                .edit().remove(PREFS_UID).apply();
        
        // Should not throw, just return early
        repo.syncActivityJarScoreOnceDaily();
        
        // Test with empty uid
        context.getSharedPreferences(USER_REPO_PREFS, Context.MODE_PRIVATE)
                .edit().putString(PREFS_UID, "").apply();
        repo.syncActivityJarScoreOnceDaily();
        // No assertions needed - just verifying no crash
    }
}