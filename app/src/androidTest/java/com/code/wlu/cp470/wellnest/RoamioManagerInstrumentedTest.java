package com.code.wlu.cp470.wellnest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.code.wlu.cp470.wellnest.data.RoamioModels;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.RoamioManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for RoamioManager covering local database CRUD operations
 * for Roamio scores.
 * 
 * Tests the singleton score pattern where a single row (uid=1) holds the user's
 * Roamio score in the local SQLite database.
 */
@RunWith(AndroidJUnit4.class)
public class RoamioManagerInstrumentedTest {

    private Context context;
    private WellnestDatabaseHelper helper;
    private SQLiteDatabase db;
    private RoamioManager manager;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        helper = new WellnestDatabaseHelper(context);
        db = helper.getWritableDatabase();
        helper.cleanDatabase(db);
        manager = new RoamioManager(db);
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
    // Constructor and Initialization Tests
    // ============================================================

    /**
     * Test that RoamioManager constructor initializes singleton row
     * with default score of 0.
     */
    @Test
    public void testConstructor_initializesSingletonRow() {
        RoamioModels.RoamioScore score = manager.getRoamioScore();
        assertNotNull("Score should not be null", score);
        assertEquals("Initial score should be 0", 0, score.getScore());
        assertNotNull("UID should not be null", score.getUid());
    }

    /**
     * Test that RoamioManager throws exception when constructed with null database.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullDatabase_throwsException() {
        new RoamioManager(null);
    }

    // ============================================================
    // getRoamioScore Tests
    // ============================================================

    /**
     * Test that getRoamioScore returns the singleton score row.
     */
    @Test
    public void testGetRoamioScore_returnsSingletonScore() {
        RoamioModels.RoamioScore score = manager.getRoamioScore();
        assertNotNull("Score should not be null", score);
        assertEquals("Score should be 0 by default", 0, score.getScore());
    }

    /**
     * Test that getRoamioScore returns updated score after upsert.
     */
    @Test
    public void testGetRoamioScore_afterUpsert_returnsUpdatedScore() {
        manager.upsertRoamioScore(150);
        
        RoamioModels.RoamioScore score = manager.getRoamioScore();
        assertNotNull("Score should not be null", score);
        assertEquals("Score should be 150", 150, score.getScore());
    }

    /**
     * Test that multiple calls to getRoamioScore return consistent results.
     */
    @Test
    public void testGetRoamioScore_multipleCalls_consistentResults() {
        manager.upsertRoamioScore(100);
        
        RoamioModels.RoamioScore score1 = manager.getRoamioScore();
        RoamioModels.RoamioScore score2 = manager.getRoamioScore();
        
        assertEquals("Both scores should be equal", score1.getScore(), score2.getScore());
        assertEquals("Both scores should be 100", 100, score1.getScore());
    }

    // ============================================================
    // upsertRoamioScore Tests
    // ============================================================

    /**
     * Test that upsertRoamioScore creates a new score successfully.
     */
    @Test
    public void testUpsertRoamioScore_createsScore() {
        boolean result = manager.upsertRoamioScore(50);
        assertTrue("Upsert should succeed", result);
        
        RoamioModels.RoamioScore score = manager.getRoamioScore();
        assertEquals("Score should be 50", 50, score.getScore());
    }

    /**
     * Test that upsertRoamioScore updates an existing score.
     */
    @Test
    public void testUpsertRoamioScore_updatesExistingScore() {
        manager.upsertRoamioScore(100);
        RoamioModels.RoamioScore score1 = manager.getRoamioScore();
        assertEquals("First score should be 100", 100, score1.getScore());
        
        boolean result = manager.upsertRoamioScore(200);
        assertTrue("Update should succeed", result);
        
        RoamioModels.RoamioScore score2 = manager.getRoamioScore();
        assertEquals("Updated score should be 200", 200, score2.getScore());
    }

    /**
     * Test that upsertRoamioScore can set score to zero.
     */
    @Test
    public void testUpsertRoamioScore_zeroValue() {
        manager.upsertRoamioScore(100);
        boolean result = manager.upsertRoamioScore(0);
        assertTrue("Upsert to 0 should succeed", result);
        
        RoamioModels.RoamioScore score = manager.getRoamioScore();
        assertEquals("Score should be 0", 0, score.getScore());
    }

    /**
     * Test that upsertRoamioScore handles negative values.
     */
    @Test
    public void testUpsertRoamioScore_negativeValue() {
        boolean result = manager.upsertRoamioScore(-50);
        assertTrue("Upsert with negative value should succeed", result);
        
        RoamioModels.RoamioScore score = manager.getRoamioScore();
        assertEquals("Score should be -50", -50, score.getScore());
    }

    /**
     * Test that upsertRoamioScore handles large values.
     */
    @Test
    public void testUpsertRoamioScore_largeValue() {
        boolean result = manager.upsertRoamioScore(999999);
        assertTrue("Upsert with large value should succeed", result);
        
        RoamioModels.RoamioScore score = manager.getRoamioScore();
        assertEquals("Score should be 999999", 999999, score.getScore());
    }

    /**
     * Test multiple consecutive upserts maintain data consistency.
     */
    @Test
    public void testUpsertRoamioScore_multipleConsecutiveUpserts() {
        manager.upsertRoamioScore(10);
        assertEquals("Score should be 10", 10, manager.getRoamioScore().getScore());
        
        manager.upsertRoamioScore(20);
        assertEquals("Score should be 20", 20, manager.getRoamioScore().getScore());
        
        manager.upsertRoamioScore(30);
        assertEquals("Score should be 30", 30, manager.getRoamioScore().getScore());
        
        manager.upsertRoamioScore(40);
        assertEquals("Score should be 40", 40, manager.getRoamioScore().getScore());
    }

    // ============================================================
    // addToRoamioScore Tests
    // ============================================================

    /**
     * Test that addToRoamioScore increments the score correctly.
     */
    @Test
    public void testAddToRoamioScore_incrementsScore() {
        manager.upsertRoamioScore(0);
        
        manager.addToRoamioScore(50);
        assertEquals("Score should be 50 after +50", 50, manager.getRoamioScore().getScore());
        
        manager.addToRoamioScore(30);
        assertEquals("Score should be 80 after +30", 80, manager.getRoamioScore().getScore());
    }

    /**
     * Test that addToRoamioScore with zero delta is a no-op.
     */
    @Test
    public void testAddToRoamioScore_zeroDelta() {
        manager.upsertRoamioScore(100);
        
        manager.addToRoamioScore(0);
        assertEquals("Score should remain 100 after +0", 100, manager.getRoamioScore().getScore());
    }

    /**
     * Test that addToRoamioScore can decrement with negative delta.
     */
    @Test
    public void testAddToRoamioScore_negativeDelta() {
        manager.upsertRoamioScore(100);
        
        manager.addToRoamioScore(-25);
        assertEquals("Score should be 75 after -25", 75, manager.getRoamioScore().getScore());
    }

    /**
     * Test that addToRoamioScore creates initial row if missing.
     * This tests the fallback behavior when the singleton row doesn't exist.
     */
    @Test
    public void testAddToRoamioScore_createsMissingRow() {
        // Clean database to remove singleton row
        helper.cleanDatabase(db);
        
        // addToRoamioScore should create row starting at the delta value
        manager.addToRoamioScore(75);
        
        RoamioModels.RoamioScore score = manager.getRoamioScore();
        assertEquals("Score should be 75 after creating missing row", 75, score.getScore());
    }

    /**
     * Test typical walk completion point awards (300, 500, 800).
     */
    @Test
    public void testAddToRoamioScore_walkCompletionPoints() {
        manager.upsertRoamioScore(0);
        
        // Easy walk: 300 points
        manager.addToRoamioScore(300);
        assertEquals("Score should be 300 after easy walk", 300, manager.getRoamioScore().getScore());
        
        // Medium walk: 500 points
        manager.addToRoamioScore(500);
        assertEquals("Score should be 800 after medium walk", 800, manager.getRoamioScore().getScore());
        
        // Hard walk: 800 points
        manager.addToRoamioScore(800);
        assertEquals("Score should be 1600 after hard walk", 1600, manager.getRoamioScore().getScore());
    }

    /**
     * Test multiple small increments accumulate correctly.
     */
    @Test
    public void testAddToRoamioScore_multipleSmallIncrements() {
        manager.upsertRoamioScore(0);
        
        for (int i = 1; i <= 10; i++) {
            manager.addToRoamioScore(10);
            assertEquals("Score should be " + (i * 10), i * 10, manager.getRoamioScore().getScore());
        }
    }

    /**
     * Test large increment values.
     */
    @Test
    public void testAddToRoamioScore_largeIncrement() {
        manager.upsertRoamioScore(1000);
        
        manager.addToRoamioScore(50000);
        assertEquals("Score should be 51000", 51000, manager.getRoamioScore().getScore());
    }

    // ============================================================
    // Data Persistence Tests
    // ============================================================

    /**
     * Test that scores persist across manager instances.
     */
    @Test
    public void testScorePersistence_acrossInstances() {
        manager.upsertRoamioScore(250);
        
        // Create new manager instance with same database
        RoamioManager newManager = new RoamioManager(db);
        RoamioModels.RoamioScore score = newManager.getRoamioScore();
        
        assertEquals("Persisted score should be 250", 250, score.getScore());
    }

    /**
     * Test that the singleton pattern maintains a single score row.
     */
    @Test
    public void testSingletonPattern_maintainsSingleRow() {
        manager.upsertRoamioScore(100);
        manager.upsertRoamioScore(200);
        manager.addToRoamioScore(50);
        
        // All operations should affect the same singleton row
        RoamioModels.RoamioScore score = manager.getRoamioScore();
        assertEquals("Final score should be 250", 250, score.getScore());
    }

    // ============================================================
    // Transaction and Concurrency Tests
    // ============================================================

    /**
     * Test that addToRoamioScore uses transactions correctly.
     * Multiple adds should all succeed.
     */
    @Test
    public void testAddToRoamioScore_transactionIntegrity() {
        manager.upsertRoamioScore(0);
        
        // Perform multiple adds which use transactions internally
        manager.addToRoamioScore(10);
        manager.addToRoamioScore(20);
        manager.addToRoamioScore(30);
        
        assertEquals("All additions should succeed", 60, manager.getRoamioScore().getScore());
    }

    /**
     * Test rapid successive operations maintain consistency.
     */
    @Test
    public void testRapidOperations_maintainConsistency() {
        manager.upsertRoamioScore(0);
        
        // Rapid operations
        for (int i = 0; i < 100; i++) {
            manager.addToRoamioScore(1);
        }
        
        assertEquals("All 100 additions should be counted", 100, manager.getRoamioScore().getScore());
    }

    // ============================================================
    // Edge Cases and Error Handling Tests
    // ============================================================

    /**
     * Test score overflow behavior (Java int max value).
     */
    @Test
    public void testScoreOverflow_nearIntMaxValue() {
        int nearMax = Integer.MAX_VALUE - 100;
        manager.upsertRoamioScore(nearMax);
        
        assertEquals("Score should be near max int", nearMax, manager.getRoamioScore().getScore());
        
        // Adding small amount should work
        manager.addToRoamioScore(50);
        assertEquals("Score should increment", nearMax + 50, manager.getRoamioScore().getScore());
    }

    /**
     * Test score underflow behavior (Java int min value).
     */
    @Test
    public void testScoreUnderflow_nearIntMinValue() {
        int nearMin = Integer.MIN_VALUE + 100;
        manager.upsertRoamioScore(nearMin);
        
        assertEquals("Score should be near min int", nearMin, manager.getRoamioScore().getScore());
        
        // Subtracting small amount should work
        manager.addToRoamioScore(-50);
        assertEquals("Score should decrement", nearMin - 50, manager.getRoamioScore().getScore());
    }

    /**
     * Test that operations after database cleanup still work.
     */
    @Test
    public void testOperations_afterDatabaseClean() {
        manager.upsertRoamioScore(100);
        helper.cleanDatabase(db);
        
        // Manager should be able to recreate singleton row
        manager.addToRoamioScore(50);
        assertEquals("Score should be 50 after clean", 50, manager.getRoamioScore().getScore());
    }

    // ============================================================
    // Integration Scenario Tests
    // ============================================================

    /**
     * Test realistic game scenario: user completes multiple walks over time.
     */
    @Test
    public void testRealisticScenario_multipleWalksOverTime() {
        // User starts fresh
        manager.upsertRoamioScore(0);
        assertEquals("Starting score should be 0", 0, manager.getRoamioScore().getScore());
        
        // Complete first easy walk (300 points)
        manager.addToRoamioScore(300);
        assertEquals("Score after first walk should be 300", 300, manager.getRoamioScore().getScore());
        
        // Complete medium walk (500 points)
        manager.addToRoamioScore(500);
        assertEquals("Score after second walk should be 800", 800, manager.getRoamioScore().getScore());
        
        // Complete another easy walk (300 points)
        manager.addToRoamioScore(300);
        assertEquals("Score after third walk should be 1100", 1100, manager.getRoamioScore().getScore());
        
        // Complete hard walk (800 points)
        manager.addToRoamioScore(800);
        assertEquals("Score after fourth walk should be 1900", 1900, manager.getRoamioScore().getScore());
        
        // Sync might adjust score (simulate remote having higher score)
        manager.upsertRoamioScore(2000);
        assertEquals("Score after sync should be 2000", 2000, manager.getRoamioScore().getScore());
    }

    /**
     * Test scenario where user resets or adjusts their score.
     */
    @Test
    public void testScenario_scoreReset() {
        // User has accumulated points
        manager.upsertRoamioScore(1500);
        assertEquals("Initial score should be 1500", 1500, manager.getRoamioScore().getScore());
        
        // Score reset to zero (e.g., new season, new challenge)
        manager.upsertRoamioScore(0);
        assertEquals("Score should be reset to 0", 0, manager.getRoamioScore().getScore());
        
        // User starts earning points again
        manager.addToRoamioScore(300);
        assertEquals("Score should be 300 after reset", 300, manager.getRoamioScore().getScore());
    }
}