package com.code.wlu.cp470.wellnest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.code.wlu.cp470.wellnest.data.RoamioModels;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseRoamioManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for FirebaseRoamioManager covering remote score operations
 * and conflict resolution with Firestore.
 * 
 * NOTE: These tests interact with Firebase Firestore. For CI/CD environments,
 * consider using Firebase Test Lab or mocking Firebase operations.
 * 
 * Tests use a test UID to avoid conflicts with real user data.
 */
@RunWith(AndroidJUnit4.class)
public class FirebaseRoamioManagerInstrumentedTest {

    private FirebaseRoamioManager manager;
    private static final String TEST_UID = "test_roamio_user_" + System.currentTimeMillis();
    private static final String TEST_UID_2 = "test_roamio_user_2_" + System.currentTimeMillis();

    @Before
    public void setUp() {
        manager = new FirebaseRoamioManager();
    }

    @After
    public void tearDown() {
        // Clean up test data in Firebase
        // Note: In a real scenario, you'd want to delete the test documents
        // For now, using timestamped UIDs avoids conflicts between test runs
    }

    // ============================================================
    // getScore Tests
    // ============================================================

    /**
     * Test that getScore returns a default score of 0 for a new user.
     */
    @Test
    public void testGetScore_newUser_returnsZero() {
        RoamioModels.RoamioScore score = manager.getScore(TEST_UID);
        
        assertNotNull("Score should not be null", score);
        assertEquals("New user score should be 0", 0, score.getScore());
        assertEquals("UID should match", TEST_UID, score.getUid());
    }

    /**
     * Test that getScore returns null for null UID.
     */
    @Test
    public void testGetScore_nullUid_returnsDefaultScore() {
        RoamioModels.RoamioScore score = manager.getScore(null);
        
        assertNotNull("Score should not be null even with null UID", score);
        assertEquals("Score should be 0 for null UID", 0, score.getScore());
    }

    /**
     * Test that getScore retrieves a previously upserted score.
     */
    @Test
    public void testGetScore_afterUpsert_returnsCorrectScore() {
        // Upsert a score
        RoamioModels.RoamioScore upsertScore = new RoamioModels.RoamioScore(TEST_UID, 250);
        boolean upsertResult = manager.upsertScore(upsertScore);
        assertTrue("Upsert should succeed", upsertResult);
        
        // Retrieve the score
        RoamioModels.RoamioScore retrievedScore = manager.getScore(TEST_UID);
        
        assertNotNull("Retrieved score should not be null", retrievedScore);
        assertEquals("Retrieved score should match upserted score", 250, retrievedScore.getScore());
        assertEquals("UID should match", TEST_UID, retrievedScore.getUid());
    }

    /**
     * Test that getScore for different users returns independent scores.
     */
    @Test
    public void testGetScore_differentUsers_independentScores() {
        // Set scores for two different users
        RoamioModels.RoamioScore score1 = new RoamioModels.RoamioScore(TEST_UID, 100);
        RoamioModels.RoamioScore score2 = new RoamioModels.RoamioScore(TEST_UID_2, 200);
        
        manager.upsertScore(score1);
        manager.upsertScore(score2);
        
        // Retrieve scores
        RoamioModels.RoamioScore retrieved1 = manager.getScore(TEST_UID);
        RoamioModels.RoamioScore retrieved2 = manager.getScore(TEST_UID_2);
        
        assertEquals("First user score should be 100", 100, retrieved1.getScore());
        assertEquals("Second user score should be 200", 200, retrieved2.getScore());
    }

    // ============================================================
    // upsertScore Tests
    // ============================================================

    /**
     * Test that upsertScore creates a new score document.
     */
    @Test
    public void testUpsertScore_createsNewScore() {
        RoamioModels.RoamioScore score = new RoamioModels.RoamioScore(TEST_UID, 150);
        
        boolean result = manager.upsertScore(score);
        assertTrue("Upsert should succeed", result);
        
        // Verify the score was created
        RoamioModels.RoamioScore retrieved = manager.getScore(TEST_UID);
        assertEquals("Score should be 150", 150, retrieved.getScore());
    }

    /**
     * Test that upsertScore updates an existing score document.
     */
    @Test
    public void testUpsertScore_updatesExistingScore() {
        // Create initial score
        RoamioModels.RoamioScore initialScore = new RoamioModels.RoamioScore(TEST_UID, 100);
        manager.upsertScore(initialScore);
        
        // Update the score
        RoamioModels.RoamioScore updatedScore = new RoamioModels.RoamioScore(TEST_UID, 300);
        boolean result = manager.upsertScore(updatedScore);
        assertTrue("Update should succeed", result);
        
        // Verify the update
        RoamioModels.RoamioScore retrieved = manager.getScore(TEST_UID);
        assertEquals("Score should be updated to 300", 300, retrieved.getScore());
    }

    /**
     * Test that upsertScore with null score returns false.
     */
    @Test
    public void testUpsertScore_nullScore_returnsFalse() {
        boolean result = manager.upsertScore(null);
        assertFalse("Upsert with null score should fail", result);
    }

    /**
     * Test that upsertScore with null UID returns false.
     */
    @Test
    public void testUpsertScore_nullUid_returnsFalse() {
        RoamioModels.RoamioScore score = new RoamioModels.RoamioScore(null, 100);
        
        boolean result = manager.upsertScore(score);
        assertFalse("Upsert with null UID should fail", result);
    }

    /**
     * Test that upsertScore can set score to zero.
     */
    @Test
    public void testUpsertScore_zeroValue() {
        RoamioModels.RoamioScore score = new RoamioModels.RoamioScore(TEST_UID, 0);
        
        boolean result = manager.upsertScore(score);
        assertTrue("Upsert with zero should succeed", result);
        
        RoamioModels.RoamioScore retrieved = manager.getScore(TEST_UID);
        assertEquals("Score should be 0", 0, retrieved.getScore());
    }

    /**
     * Test that upsertScore handles negative values.
     */
    @Test
    public void testUpsertScore_negativeValue() {
        RoamioModels.RoamioScore score = new RoamioModels.RoamioScore(TEST_UID, -50);
        
        boolean result = manager.upsertScore(score);
        assertTrue("Upsert with negative value should succeed", result);
        
        RoamioModels.RoamioScore retrieved = manager.getScore(TEST_UID);
        assertEquals("Score should be -50", -50, retrieved.getScore());
    }

    /**
     * Test that upsertScore handles large values.
     */
    @Test
    public void testUpsertScore_largeValue() {
        RoamioModels.RoamioScore score = new RoamioModels.RoamioScore(TEST_UID, 999999);
        
        boolean result = manager.upsertScore(score);
        assertTrue("Upsert with large value should succeed", result);
        
        RoamioModels.RoamioScore retrieved = manager.getScore(TEST_UID);
        assertEquals("Score should be 999999", 999999, retrieved.getScore());
    }

    /**
     * Test typical walk completion point values (300, 500, 800).
     */
    @Test
    public void testUpsertScore_walkCompletionPoints() {
        // Easy walk: 300 points
        manager.upsertScore(new RoamioModels.RoamioScore(TEST_UID, 300));
        assertEquals("Score should be 300", 300, manager.getScore(TEST_UID).getScore());
        
        // Update to medium walk total: 800 points
        manager.upsertScore(new RoamioModels.RoamioScore(TEST_UID, 800));
        assertEquals("Score should be 800", 800, manager.getScore(TEST_UID).getScore());
        
        // Update to include hard walk: 1600 points
        manager.upsertScore(new RoamioModels.RoamioScore(TEST_UID, 1600));
        assertEquals("Score should be 1600", 1600, manager.getScore(TEST_UID).getScore());
    }

    // ============================================================
    // Conflict Resolution Tests
    // ============================================================

    /**
     * Test conflict resolution: higher score should be preserved.
     * This simulates the sync logic in RoamioRepository.
     */
    @Test
    public void testConflictResolution_higherScoreWins() {
        String conflictUid = TEST_UID + "_conflict";
        
        // Set initial remote score
        manager.upsertScore(new RoamioModels.RoamioScore(conflictUid, 100));
        
        // Simulate higher local score syncing to remote
        RoamioModels.RoamioScore remoteScore = manager.getScore(conflictUid);
        int localScore = 200;
        
        if (localScore > remoteScore.getScore()) {
            manager.upsertScore(new RoamioModels.RoamioScore(conflictUid, localScore));
        }
        
        RoamioModels.RoamioScore finalScore = manager.getScore(conflictUid);
        assertEquals("Higher score should win", 200, finalScore.getScore());
    }

    /**
     * Test conflict resolution: remote higher score is preserved.
     */
    @Test
    public void testConflictResolution_remoteHigherWins() {
        String conflictUid = TEST_UID + "_conflict2";
        
        // Set higher remote score
        manager.upsertScore(new RoamioModels.RoamioScore(conflictUid, 500));
        
        // Simulate lower local score attempting to sync
        RoamioModels.RoamioScore remoteScore = manager.getScore(conflictUid);
        int localScore = 300;
        
        if (localScore > remoteScore.getScore()) {
            manager.upsertScore(new RoamioModels.RoamioScore(conflictUid, localScore));
        } else {
            // Remote is higher, don't update
        }
        
        RoamioModels.RoamioScore finalScore = manager.getScore(conflictUid);
        assertEquals("Remote higher score should be preserved", 500, finalScore.getScore());
    }

    // ============================================================
    // Data Persistence Tests
    // ============================================================

    /**
     * Test that scores persist across manager instances.
     */
    @Test
    public void testScorePersistence_acrossInstances() {
        RoamioModels.RoamioScore score = new RoamioModels.RoamioScore(TEST_UID, 450);
        manager.upsertScore(score);
        
        // Create new manager instance
        FirebaseRoamioManager newManager = new FirebaseRoamioManager();
        RoamioModels.RoamioScore retrieved = newManager.getScore(TEST_UID);
        
        assertEquals("Score should persist across instances", 450, retrieved.getScore());
    }

    /**
     * Test multiple consecutive upserts maintain data consistency.
     */
    @Test
    public void testMultipleConsecutiveUpserts_maintainConsistency() {
        String multiUid = TEST_UID + "_multi";
        
        manager.upsertScore(new RoamioModels.RoamioScore(multiUid, 100));
        assertEquals("Score should be 100", 100, manager.getScore(multiUid).getScore());
        
        manager.upsertScore(new RoamioModels.RoamioScore(multiUid, 200));
        assertEquals("Score should be 200", 200, manager.getScore(multiUid).getScore());
        
        manager.upsertScore(new RoamioModels.RoamioScore(multiUid, 300));
        assertEquals("Score should be 300", 300, manager.getScore(multiUid).getScore());
        
        manager.upsertScore(new RoamioModels.RoamioScore(multiUid, 400));
        assertEquals("Score should be 400", 400, manager.getScore(multiUid).getScore());
    }

    // ============================================================
    // Integration Scenario Tests
    // ============================================================

    /**
     * Test realistic multi-device sync scenario.
     */
    @Test
    public void testRealisticScenario_multiDeviceSync() {
        String syncUid = TEST_UID + "_sync";
        
        // Device 1 completes walk, sets score to 300
        manager.upsertScore(new RoamioModels.RoamioScore(syncUid, 300));
        
        // Device 2 reads score
        RoamioModels.RoamioScore device2Score = manager.getScore(syncUid);
        assertEquals("Device 2 should read 300", 300, device2Score.getScore());
        
        // Device 2 completes walk, updates to 800
        manager.upsertScore(new RoamioModels.RoamioScore(syncUid, 800));
        
        // Device 1 reads updated score
        RoamioModels.RoamioScore device1UpdatedScore = manager.getScore(syncUid);
        assertEquals("Device 1 should read updated 800", 800, device1UpdatedScore.getScore());
    }

    /**
     * Test scenario where user completes multiple walks and syncs.
     */
    @Test
    public void testScenario_progressivewalkCompletion() {
        String walkUid = TEST_UID + "_walks";
        
        // User starts fresh
        RoamioModels.RoamioScore initialScore = manager.getScore(walkUid);
        assertEquals("Initial score should be 0", 0, initialScore.getScore());
        
        // Complete first easy walk (300 points)
        manager.upsertScore(new RoamioModels.RoamioScore(walkUid, 300));
        assertEquals("Score after first walk should be 300", 
                300, manager.getScore(walkUid).getScore());
        
        // Complete medium walk (total 800 points)
        manager.upsertScore(new RoamioModels.RoamioScore(walkUid, 800));
        assertEquals("Score after second walk should be 800", 
                800, manager.getScore(walkUid).getScore());
        
        // Complete hard walk (total 1600 points)
        manager.upsertScore(new RoamioModels.RoamioScore(walkUid, 1600));
        assertEquals("Score after third walk should be 1600", 
                1600, manager.getScore(walkUid).getScore());
    }

    /**
     * Test edge case: rapid successive updates.
     */
    @Test
    public void testRapidSuccessiveUpdates_lastWriteWins() {
        String rapidUid = TEST_UID + "_rapid";
        
        // Perform rapid updates
        manager.upsertScore(new RoamioModels.RoamioScore(rapidUid, 100));
        manager.upsertScore(new RoamioModels.RoamioScore(rapidUid, 200));
        manager.upsertScore(new RoamioModels.RoamioScore(rapidUid, 300));
        manager.upsertScore(new RoamioModels.RoamioScore(rapidUid, 400));
        manager.upsertScore(new RoamioModels.RoamioScore(rapidUid, 500));
        
        // Last write should win
        RoamioModels.RoamioScore finalScore = manager.getScore(rapidUid);
        assertEquals("Last write (500) should win", 500, finalScore.getScore());
    }

    // ============================================================
    // Error Handling Tests
    // ============================================================

    /**
     * Test that getScore handles network errors gracefully.
     * Note: This test will succeed even if network is available,
     * as it just verifies the method doesn't throw exceptions.
     */
    @Test
    public void testGetScore_handlesNetworkErrorsGracefully() {
        try {
            RoamioModels.RoamioScore score = manager.getScore(TEST_UID + "_network");
            assertNotNull("Score should not be null even if network fails", score);
            // Should return default score (0) on error
        } catch (Exception e) {
            // Method should not throw exceptions
            assertTrue("getScore should not throw exceptions", false);
        }
    }

    /**
     * Test that upsertScore handles failures gracefully.
     */
    @Test
    public void testUpsertScore_handlesFailuresGracefully() {
        try {
            // Even with potential network issues, method should not throw
            RoamioModels.RoamioScore score = new RoamioModels.RoamioScore(TEST_UID + "_error", 100);
            boolean result = manager.upsertScore(score);
            // Result may be true or false, but should not throw
            assertNotNull("Result should be boolean", result);
        } catch (Exception e) {
            // Method should not throw exceptions
            assertTrue("upsertScore should not throw exceptions", false);
        }
    }

    // ============================================================
    // Boundary Tests
    // ============================================================

    /**
     * Test score at integer maximum value.
     */
    @Test
    public void testScore_atIntMaxValue() {
        RoamioModels.RoamioScore maxScore = new RoamioModels.RoamioScore(
                TEST_UID + "_max", Integer.MAX_VALUE);
        
        boolean result = manager.upsertScore(maxScore);
        assertTrue("Upsert with max int should succeed", result);
        
        RoamioModels.RoamioScore retrieved = manager.getScore(TEST_UID + "_max");
        assertEquals("Score should be Integer.MAX_VALUE", Integer.MAX_VALUE, retrieved.getScore());
    }

    /**
     * Test score at integer minimum value.
     */
    @Test
    public void testScore_atIntMinValue() {
        RoamioModels.RoamioScore minScore = new RoamioModels.RoamioScore(
                TEST_UID + "_min", Integer.MIN_VALUE);
        
        boolean result = manager.upsertScore(minScore);
        assertTrue("Upsert with min int should succeed", result);
        
        RoamioModels.RoamioScore retrieved = manager.getScore(TEST_UID + "_min");
        assertEquals("Score should be Integer.MIN_VALUE", Integer.MIN_VALUE, retrieved.getScore());
    }

    /**
     * Test UID with special characters.
     */
    @Test
    public void testScore_specialCharactersInUid() {
        String specialUid = TEST_UID + "_special_!@#$%";
        RoamioModels.RoamioScore score = new RoamioModels.RoamioScore(specialUid, 100);
        
        boolean result = manager.upsertScore(score);
        assertTrue("Upsert with special chars should succeed", result);
        
        RoamioModels.RoamioScore retrieved = manager.getScore(specialUid);
        assertEquals("Score should be retrieved correctly", 100, retrieved.getScore());
    }
}