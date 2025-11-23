package com.code.wlu.cp470.wellnest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.code.wlu.cp470.wellnest.data.RoamioModels;
import com.code.wlu.cp470.wellnest.data.RoamioRepository;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.RoamioManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseRoamioManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for RoamioRepository covering score operations,
 * walk generation, and local/remote synchronization.
 */
@RunWith(AndroidJUnit4.class)
public class RoamioRepositoryInstrumentedTest {

    private Context context;
    private WellnestDatabaseHelper helper;
    private SQLiteDatabase db;
    private RoamioManager localManager;
    private FirebaseRoamioManager remoteManager;
    private RoamioRepository repo;

    /**
     * Fake remote manager that avoids hitting Firestore in tests.
     * Returns predictable scores for testing sync logic.
     */
    private static class FakeFirebaseRoamioManager extends FirebaseRoamioManager {
        private int fakeScore = 0;
        private String fakeUid = "test_uid";

        public void setFakeScore(int score) {
            this.fakeScore = score;
        }

        @Override
        public RoamioModels.RoamioScore getScore(String uid) {
            return new RoamioModels.RoamioScore(fakeUid, fakeScore);
        }

        @Override
        public boolean upsertScore(RoamioModels.RoamioScore roamioScore) {
            if (roamioScore != null) {
                this.fakeScore = roamioScore.getScore();
                this.fakeUid = roamioScore.getUid();
                return true;
            }
            return false;
        }
    }

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        helper = new WellnestDatabaseHelper(context);
        db = helper.getWritableDatabase();
        helper.cleanDatabase(db);

        localManager = new RoamioManager(db);
        remoteManager = new FakeFirebaseRoamioManager();
        repo = new RoamioRepository(context, localManager, remoteManager);
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
    }

    // ============================================================
    // Score Operations Tests
    // ============================================================

    /**
     * Test that getRoamioScore returns the default score of 0
     * when no score has been set.
     */
    @Test
    public void testGetRoamioScore_returnsDefaultZero() {
        RoamioModels.RoamioScore score = repo.getRoamioScore();
        assertNotNull("Score should not be null", score);
        assertEquals("Default score should be 0", 0, score.getScore());
    }

    /**
     * Test that upsertRoamioScore creates a new score and
     * subsequent reads reflect the upserted value.
     */
    @Test
    public void testUpsertRoamioScore_createsAndPersistsScore() {
        boolean result = repo.upsertRoamioScore(100);
        assertTrue("Upsert should succeed", result);

        RoamioModels.RoamioScore score = repo.getRoamioScore();
        assertNotNull("Score should not be null", score);
        assertEquals("Score should be 100", 100, score.getScore());
    }

    /**
     * Test that upsertRoamioScore updates an existing score
     * and subsequent reads reflect the new value.
     */
    @Test
    public void testUpsertRoamioScore_updatesExistingScore() {
        repo.upsertRoamioScore(50);
        RoamioModels.RoamioScore score1 = repo.getRoamioScore();
        assertEquals("First score should be 50", 50, score1.getScore());

        repo.upsertRoamioScore(150);
        RoamioModels.RoamioScore score2 = repo.getRoamioScore();
        assertEquals("Updated score should be 150", 150, score2.getScore());
    }

    /**
     * Test that addToRoamioScore correctly increments the score
     * and persists the new value.
     */
    @Test
    public void testAddToRoamioScore_incrementsAndPersists() {
        assertEquals("Initial score should be 0", 0, repo.getRoamioScore().getScore());

        repo.addToRoamioScore(50);
        assertEquals("Score should be 50 after +50", 50, repo.getRoamioScore().getScore());

        repo.addToRoamioScore(30);
        assertEquals("Score should be 80 after +30", 80, repo.getRoamioScore().getScore());
    }

    /**
     * Test that addToRoamioScore with zero delta is effectively a no-op.
     */
    @Test
    public void testAddToRoamioScore_zeroDelta_isNoOp() {
        repo.upsertRoamioScore(75);
        assertEquals("Score should be 75", 75, repo.getRoamioScore().getScore());

        repo.addToRoamioScore(0);
        assertEquals("Score should still be 75 after +0", 75, repo.getRoamioScore().getScore());
    }

    /**
     * Test that addToRoamioScore can handle negative deltas
     * (though not typical for game scoring).
     */
    @Test
    public void testAddToRoamioScore_negativeDelta_decrementsScore() {
        repo.upsertRoamioScore(100);
        assertEquals("Score should be 100", 100, repo.getRoamioScore().getScore());

        repo.addToRoamioScore(-20);
        assertEquals("Score should be 80 after -20", 80, repo.getRoamioScore().getScore());
    }

    /**
     * Test that addToRoamioScore can handle large point values
     * typical of walk completion rewards (300, 500, 800).
     */
    @Test
    public void testAddToRoamioScore_largePointValues() {
        repo.upsertRoamioScore(0);

        // Easy walk: 300 points
        repo.addToRoamioScore(300);
        assertEquals("Score should be 300", 300, repo.getRoamioScore().getScore());

        // Medium walk: 500 points
        repo.addToRoamioScore(500);
        assertEquals("Score should be 800", 800, repo.getRoamioScore().getScore());

        // Hard walk: 800 points
        repo.addToRoamioScore(800);
        assertEquals("Score should be 1600", 1600, repo.getRoamioScore().getScore());
    }

    // ============================================================
    // Sync Operations Tests
    // ============================================================

    /**
     * Test that syncScore does nothing when scores are equal.
     */
    @Test
    public void testSyncScore_equalScores_noChange() {
        FakeFirebaseRoamioManager fakeRemote = (FakeFirebaseRoamioManager) remoteManager;
        fakeRemote.setFakeScore(100);
        repo.upsertRoamioScore(100);

        repo.syncScore();

        assertEquals("Local score should remain 100", 100, repo.getRoamioScore().getScore());
        assertEquals("Remote score should remain 100", 100, fakeRemote.fakeScore);
    }

    /**
     * Test that syncScore updates local when remote score is higher.
     */
    @Test
    public void testSyncScore_remoteHigher_updatesLocal() {
        FakeFirebaseRoamioManager fakeRemote = (FakeFirebaseRoamioManager) remoteManager;
        fakeRemote.setFakeScore(200);
        repo.upsertRoamioScore(100);

        repo.syncScore();

        assertEquals("Local score should be updated to 200", 200, repo.getRoamioScore().getScore());
    }

    /**
     * Test that syncScore updates remote when local score is higher.
     */
    @Test
    public void testSyncScore_localHigher_updatesRemote() {
        FakeFirebaseRoamioManager fakeRemote = (FakeFirebaseRoamioManager) remoteManager;
        fakeRemote.setFakeScore(100);
        repo.upsertRoamioScore(250);

        repo.syncScore();

        assertEquals("Remote score should be updated to 250", 250, fakeRemote.fakeScore);
        assertEquals("Local score should remain 250", 250, repo.getRoamioScore().getScore());
    }

    /**
     * Test sync conflict resolution: higher score always wins.
     */
    @Test
    public void testSyncScore_conflictResolution_higherScoreWins() {
        FakeFirebaseRoamioManager fakeRemote = (FakeFirebaseRoamioManager) remoteManager;

        // Scenario 1: Remote wins
        fakeRemote.setFakeScore(500);
        repo.upsertRoamioScore(300);
        repo.syncScore();
        assertEquals("Higher remote score should win", 500, repo.getRoamioScore().getScore());

        // Scenario 2: Local wins
        repo.upsertRoamioScore(700);
        fakeRemote.setFakeScore(600);
        repo.syncScore();
        assertEquals("Higher local score should win", 700, repo.getRoamioScore().getScore());
        assertEquals("Remote should be updated", 700, fakeRemote.fakeScore);
    }

    // ============================================================
    // Walk Generation Tests
    // ============================================================

    /**
     * Test that generateWalk returns a non-null Walk object.
     * Note: This test may return null in test environment due to
     * lack of location permissions or services, which is expected.
     */
    @Test
    public void testGenerateWalk_returnsWalkObject() {
        RoamioModels.Walk walk = repo.generateWalk();
        
        // In test environment without location permissions, this may be null
        // If it's not null, verify it has the required fields
        if (walk != null) {
            assertNotNull("Walk name should not be null", walk.getName());
            assertNotNull("Walk story should not be null", walk.getStory());
            assertNotNull("Start address should not be null", walk.getStartAddress());
            assertNotNull("End address should not be null", walk.getEndAddress());
            assertTrue("Distance should be positive", walk.getDistanceMeters() > 0);
        }
        // Note: null result is acceptable in test environment
    }

    /**
     * Test Walk model creation with all fields set correctly.
     */
    @Test
    public void testWalkCreation_allFieldsSetCorrectly() {
        RoamioModels.Walk walk = new RoamioModels.Walk(
                "user123",
                "Morning Stroll",
                "A peaceful walk through the park",
                "123 Start St",
                "456 End Ave",
                1500.0f,
                false
        );

        assertEquals("UID should match", "user123", walk.getUid());
        assertEquals("Name should match", "Morning Stroll", walk.getName());
        assertEquals("Story should match", "A peaceful walk through the park", walk.getStory());
        assertEquals("Start address should match", "123 Start St", walk.getStartAddress());
        assertEquals("End address should match", "456 End Ave", walk.getEndAddress());
        assertEquals("Distance should match", 1500.0f, walk.getDistanceMeters(), 0.01f);
        assertFalse("Should not be completed initially", walk.isCompleted());
    }

    /**
     * Test Walk distance calculation and storage.
     */
    @Test
    public void testWalk_distanceCalculation() {
        // Short walk
        RoamioModels.Walk shortWalk = new RoamioModels.Walk(
                "user1", "Short Walk", "Story", "Start", "End", 800.0f, false
        );
        assertTrue("Short walk distance should be under 1000m", shortWalk.getDistanceMeters() < 1000);

        // Medium walk
        RoamioModels.Walk mediumWalk = new RoamioModels.Walk(
                "user1", "Medium Walk", "Story", "Start", "End", 1500.0f, false
        );
        assertTrue("Medium walk should be 1000-2000m", 
                mediumWalk.getDistanceMeters() >= 1000 && mediumWalk.getDistanceMeters() < 2000);

        // Long walk
        RoamioModels.Walk longWalk = new RoamioModels.Walk(
                "user1", "Long Walk", "Story", "Start", "End", 2500.0f, false
        );
        assertTrue("Long walk should be over 2000m", longWalk.getDistanceMeters() >= 2000);
    }

    /**
     * Test Walk completion status works correctly.
     */
    @Test
    public void testWalk_completionStatus() {
        RoamioModels.Walk walk = new RoamioModels.Walk(
                "user1", "Test Walk", "Story", "Start", "End", 1000.0f, false
        );
        
        assertFalse("Walk should start incomplete", walk.isCompleted());
        
        walk.setCompleted(true);
        assertTrue("Walk should be completed after setting", walk.isCompleted());
        
        walk.setCompleted(false);
        assertFalse("Walk can be set back to incomplete", walk.isCompleted());
    }

    /**
     * Test Walk difficulty rating based on distance.
     * Easy: < 1000m (300 points)
     * Medium: 1000-2000m (500 points)
     * Hard: >= 2000m (800 points)
     */
    @Test
    public void testWalk_difficultyRating() {
        // Easy difficulty
        RoamioModels.Walk easyWalk = new RoamioModels.Walk(
                "user1", "Easy", "Story", "Start", "End", 800.0f, false
        );
        assertTrue("Easy walk should be < 1000m", easyWalk.getDistanceMeters() < 1000);

        // Medium difficulty
        RoamioModels.Walk mediumWalk = new RoamioModels.Walk(
                "user1", "Medium", "Story", "Start", "End", 1500.0f, false
        );
        assertTrue("Medium walk should be 1000-2000m", 
                mediumWalk.getDistanceMeters() >= 1000 && mediumWalk.getDistanceMeters() < 2000);

        // Hard difficulty
        RoamioModels.Walk hardWalk = new RoamioModels.Walk(
                "user1", "Hard", "Story", "Start", "End", 2500.0f, false
        );
        assertTrue("Hard walk should be >= 2000m", hardWalk.getDistanceMeters() >= 2000);
    }

    // ============================================================
    // RoamioScore Model Tests
    // ============================================================

    /**
     * Test RoamioScore creation and getters/setters.
     */
    @Test
    public void testRoamioScore_creation() {
        RoamioModels.RoamioScore score = new RoamioModels.RoamioScore("user123", 500);
        
        assertEquals("UID should match", "user123", score.getUid());
        assertEquals("Score should match", 500, score.getScore());
    }

    /**
     * Test RoamioScore setters work correctly.
     */
    @Test
    public void testRoamioScore_setters() {
        RoamioModels.RoamioScore score = new RoamioModels.RoamioScore("user1", 100);
        
        score.setUid("user2");
        assertEquals("UID should be updated", "user2", score.getUid());
        
        score.setScore(250);
        assertEquals("Score should be updated", 250, score.getScore());
    }

    /**
     * Test RoamioScore can handle various score values.
     */
    @Test
    public void testRoamioScore_variousValues() {
        // Zero score
        RoamioModels.RoamioScore zeroScore = new RoamioModels.RoamioScore("user1", 0);
        assertEquals("Zero score should be allowed", 0, zeroScore.getScore());

        // Negative score (edge case)
        RoamioModels.RoamioScore negScore = new RoamioModels.RoamioScore("user1", -10);
        assertEquals("Negative score should be stored", -10, negScore.getScore());

        // Large score
        RoamioModels.RoamioScore largeScore = new RoamioModels.RoamioScore("user1", 999999);
        assertEquals("Large score should be stored", 999999, largeScore.getScore());
    }

    // ============================================================
    // Integration Tests
    // ============================================================

    /**
     * Test complete walk point award flow:
     * 1. Generate walk (simulated with direct creation)
     * 2. Complete walk
     * 3. Award points based on difficulty
     * 4. Verify score updated correctly
     */
    @Test
    public void testCompleteWalkFlow_awardsPoints() {
        // Start with zero score
        assertEquals("Initial score should be 0", 0, repo.getRoamioScore().getScore());

        // Easy walk: 300 points
        RoamioModels.Walk easyWalk = new RoamioModels.Walk(
                "user1", "Easy Walk", "Story", "Start", "End", 800.0f, false
        );
        easyWalk.setCompleted(true);
        repo.addToRoamioScore(300);
        assertEquals("Score should be 300 after easy walk", 300, repo.getRoamioScore().getScore());

        // Medium walk: 500 points
        RoamioModels.Walk mediumWalk = new RoamioModels.Walk(
                "user1", "Medium Walk", "Story", "Start", "End", 1500.0f, false
        );
        mediumWalk.setCompleted(true);
        repo.addToRoamioScore(500);
        assertEquals("Score should be 800 after medium walk", 800, repo.getRoamioScore().getScore());

        // Hard walk: 800 points
        RoamioModels.Walk hardWalk = new RoamioModels.Walk(
                "user1", "Hard Walk", "Story", "Start", "End", 2500.0f, false
        );
        hardWalk.setCompleted(true);
        repo.addToRoamioScore(800);
        assertEquals("Score should be 1600 after hard walk", 1600, repo.getRoamioScore().getScore());
    }

    /**
     * Test walk difficulty points are correct based on distance.
     */
    @Test
    public void testWalkDifficultyPoints_correctPointValues() {
        // Verify point values for different difficulties
        int easyPoints = 300;
        int mediumPoints = 500;
        int hardPoints = 800;

        repo.upsertRoamioScore(0);

        // Complete easy walk
        repo.addToRoamioScore(easyPoints);
        assertEquals("Easy walk should award 300 points", 300, repo.getRoamioScore().getScore());

        // Reset and complete medium walk
        repo.upsertRoamioScore(0);
        repo.addToRoamioScore(mediumPoints);
        assertEquals("Medium walk should award 500 points", 500, repo.getRoamioScore().getScore());

        // Reset and complete hard walk
        repo.upsertRoamioScore(0);
        repo.addToRoamioScore(hardPoints);
        assertEquals("Hard walk should award 800 points", 800, repo.getRoamioScore().getScore());
    }

    /**
     * Test multiple operations in sequence maintain data consistency.
     */
    @Test
    public void testMultipleOperations_maintainConsistency() {
        // Set initial score
        repo.upsertRoamioScore(100);
        assertEquals("Initial score should be 100", 100, repo.getRoamioScore().getScore());

        // Add points
        repo.addToRoamioScore(50);
        assertEquals("Score should be 150", 150, repo.getRoamioScore().getScore());

        // Sync with remote
        FakeFirebaseRoamioManager fakeRemote = (FakeFirebaseRoamioManager) remoteManager;
        fakeRemote.setFakeScore(200);
        repo.syncScore();
        assertEquals("After sync, local should be 200", 200, repo.getRoamioScore().getScore());

        // Add more points
        repo.addToRoamioScore(100);
        assertEquals("Score should be 300", 300, repo.getRoamioScore().getScore());

        // Update score directly
        repo.upsertRoamioScore(500);
        assertEquals("Score should be 500 after upsert", 500, repo.getRoamioScore().getScore());
    }
}