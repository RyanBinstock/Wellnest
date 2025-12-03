package com.code.wlu.cp470.wellnest;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.code.wlu.cp470.wellnest.data.ActivityJarModels;
import com.code.wlu.cp470.wellnest.data.ActivityJarRepository;
import com.code.wlu.cp470.wellnest.data.RoamioModels;
import com.code.wlu.cp470.wellnest.data.RoamioRepository;
import com.code.wlu.cp470.wellnest.data.SnapTaskModels;
import com.code.wlu.cp470.wellnest.data.SnapTaskRepository;
import com.code.wlu.cp470.wellnest.data.UserRepository;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.ActivityJarManager;
import com.code.wlu.cp470.wellnest.data.local.managers.RoamioManager;
import com.code.wlu.cp470.wellnest.data.local.managers.SnapTaskManager;
import com.code.wlu.cp470.wellnest.data.local.managers.UserManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseRoamioManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseSnapTaskManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseUserManager;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Instrumented tests for data synchronization logic using REAL Firebase managers.
 * Tests cover two main scenarios:
 * 1. Initial Sign-In Sync (Remote → Local)
 * 2. Daily Sync (Local → Remote)
 * 
 * NOTE: These tests interact with real Firebase Firestore. Each test run creates
 * a unique test user to avoid conflicts between test runs.
 */
@RunWith(AndroidJUnit4.class)
public class DataSyncInstrumentedTest {

    private static final String TAG = "DataSyncInstrumentedTest";
    private static final String DEFAULT_TEST_NAME = "Test User";
    private static final String DEFAULT_TEST_EMAIL = "testuser@example.com";
    private static final String USER_REPO_PREFS = "user_repo_prefs";
    private static final String PREFS_UID = "uid";
    private static final String SNAP_TASK_REPO_PREFS = "snapTask_repo_prefs";
    private static final String ROAMIO_REPO_PREFS = "auth_repository_prefs";
    private static final String ACTIVITY_JAR_REPO_PREFS = "activityJar_repo_prefs";

    // Unique test UID generated per test run to avoid Firebase conflicts
    private String testUid;

    private Context context;
    private WellnestDatabaseHelper helper;
    private SQLiteDatabase db;
    private UserRepository userRepo;
    private SnapTaskRepository snapTaskRepo;
    private RoamioRepository roamioRepo;
    private ActivityJarRepository activityJarRepo;
    private UserManager localUserManager;
    private SnapTaskManager snapTaskManager;
    private RoamioManager localRoamioManager;
    private ActivityJarManager activityJarManager;
    
    // Real Firebase managers
    private FirebaseUserManager firebaseUserManager;
    private FirebaseSnapTaskManager firebaseSnapTaskManager;
    private FirebaseRoamioManager firebaseRoamioManager;
    
    private FirebaseFirestore firestore;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        helper = new WellnestDatabaseHelper(context);
        db = helper.getWritableDatabase();
        helper.cleanDatabase(db);

        // Generate unique test UID for this test run
        testUid = "sync_test_user_" + System.currentTimeMillis();

        // Initialize local managers
        localUserManager = new UserManager(db);
        snapTaskManager = new SnapTaskManager(db);
        localRoamioManager = new RoamioManager(db);
        activityJarManager = new ActivityJarManager(db);

        // Initialize real Firebase managers
        firebaseUserManager = new FirebaseUserManager();
        firebaseSnapTaskManager = new FirebaseSnapTaskManager();
        firebaseRoamioManager = new FirebaseRoamioManager();
        firestore = FirebaseFirestore.getInstance();

        // Create test user profile in Firebase
        boolean userCreated = firebaseUserManager.upsertUserProfile(testUid, DEFAULT_TEST_NAME, DEFAULT_TEST_EMAIL);
        if (!userCreated) {
            throw new RuntimeException("Failed to create test user in Firebase");
        }

        // Create local user profile
        localUserManager.upsertUserProfile(testUid, DEFAULT_TEST_NAME, DEFAULT_TEST_EMAIL);

        // Initialize repositories with real Firebase managers
        userRepo = new UserRepository(context, localUserManager, firebaseUserManager);
        snapTaskRepo = new SnapTaskRepository(context, snapTaskManager, firebaseSnapTaskManager);
        roamioRepo = new RoamioRepository(context, localRoamioManager, firebaseRoamioManager);
        activityJarRepo = new ActivityJarRepository(context, activityJarManager, firebaseUserManager);

        seedUserRepoUid(testUid);
    }

    @After
    public void tearDown() {
        Log.d(TAG, "tearDown: Starting cleanup for testUid=" + testUid);
        
        // Clean up local database
        helper.cleanDatabase(db);
        if (db != null && db.isOpen()) db.close();
        if (helper != null) helper.close();
        
        // Clear shared preferences
        context.getSharedPreferences(USER_REPO_PREFS, Context.MODE_PRIVATE).edit().clear().apply();
        context.getSharedPreferences(SNAP_TASK_REPO_PREFS, Context.MODE_PRIVATE).edit().clear().apply();
        context.getSharedPreferences(ROAMIO_REPO_PREFS, Context.MODE_PRIVATE).edit().clear().apply();
        context.getSharedPreferences(ACTIVITY_JAR_REPO_PREFS, Context.MODE_PRIVATE).edit().clear().apply();

        // Clean up test user data from Firebase
        cleanupFirebaseTestData();
        
        Log.d(TAG, "tearDown: Completed cleanup for testUid=" + testUid);
    }

    /**
     * Cleans up all test data from Firebase for the current test user.
     * This includes all subcollections and the user document itself.
     *
     * Deletion order:
     * 1. microapp_scores subcollection documents (snap_task, roamio, activity_jar)
     * 2. friends subcollection (all documents)
     * 3. Parent user document
     */
    private void cleanupFirebaseTestData() {
        if (testUid == null || testUid.isEmpty()) {
            Log.w(TAG, "cleanupFirebaseTestData: testUid is null or empty, skipping cleanup");
            return;
        }

        Log.d(TAG, "cleanupFirebaseTestData: Starting Firebase cleanup for testUid=" + testUid);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Step 1: Delete microapp_scores subcollection documents
        deleteDocumentWithLogging(db, "users/" + testUid + "/microapp_scores/snap_task");
        deleteDocumentWithLogging(db, "users/" + testUid + "/microapp_scores/roamio");
        deleteDocumentWithLogging(db, "users/" + testUid + "/microapp_scores/activity_jar");
        
        // Step 2: Delete all documents in friends subcollection (if any exist)
        deleteSubcollection(db, "users/" + testUid + "/friends");
        
        // Step 3: Delete the parent user document
        deleteDocumentWithLogging(db, "users/" + testUid);
        
        // Step 4: Verify deletion was successful
        verifyUserDeleted(db, testUid);
        
        Log.d(TAG, "cleanupFirebaseTestData: Completed Firebase cleanup for testUid=" + testUid);
    }

    /**
     * Deletes a single Firestore document with proper logging and error handling.
     */
    private void deleteDocumentWithLogging(FirebaseFirestore db, String documentPath) {
        try {
            Log.d(TAG, "deleteDocumentWithLogging: Attempting to delete " + documentPath);
            Task<Void> deleteTask = db.document(documentPath).delete();
            Tasks.await(deleteTask, 10, TimeUnit.SECONDS);
            Log.d(TAG, "deleteDocumentWithLogging: Successfully deleted " + documentPath);
        } catch (ExecutionException e) {
            Log.w(TAG, "deleteDocumentWithLogging: Failed to delete " + documentPath + " - " + e.getMessage());
        } catch (InterruptedException e) {
            Log.w(TAG, "deleteDocumentWithLogging: Interrupted while deleting " + documentPath);
            Thread.currentThread().interrupt();
        } catch (TimeoutException e) {
            Log.w(TAG, "deleteDocumentWithLogging: Timeout while deleting " + documentPath);
        }
    }

    /**
     * Deletes all documents in a subcollection.
     */
    private void deleteSubcollection(FirebaseFirestore db, String collectionPath) {
        try {
            Log.d(TAG, "deleteSubcollection: Attempting to delete all documents in " + collectionPath);
            Task<QuerySnapshot> queryTask = db.collection(collectionPath).get();
            QuerySnapshot snapshot = Tasks.await(queryTask, 10, TimeUnit.SECONDS);
            
            if (snapshot != null && !snapshot.isEmpty()) {
                Log.d(TAG, "deleteSubcollection: Found " + snapshot.size() + " documents in " + collectionPath);
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    try {
                        Tasks.await(doc.getReference().delete(), 10, TimeUnit.SECONDS);
                        Log.d(TAG, "deleteSubcollection: Deleted " + doc.getReference().getPath());
                    } catch (Exception e) {
                        Log.w(TAG, "deleteSubcollection: Failed to delete " + doc.getReference().getPath() + " - " + e.getMessage());
                    }
                }
            } else {
                Log.d(TAG, "deleteSubcollection: No documents found in " + collectionPath);
            }
        } catch (ExecutionException e) {
            Log.w(TAG, "deleteSubcollection: Failed to query " + collectionPath + " - " + e.getMessage());
        } catch (InterruptedException e) {
            Log.w(TAG, "deleteSubcollection: Interrupted while querying " + collectionPath);
            Thread.currentThread().interrupt();
        } catch (TimeoutException e) {
            Log.w(TAG, "deleteSubcollection: Timeout while querying " + collectionPath);
        }
    }

    /**
     * Verifies that the user document was successfully deleted.
     */
    private void verifyUserDeleted(FirebaseFirestore db, String uid) {
        try {
            Task<DocumentSnapshot> getTask = db.collection("users").document(uid).get();
            DocumentSnapshot doc = Tasks.await(getTask, 10, TimeUnit.SECONDS);
            if (doc != null && doc.exists()) {
                Log.e(TAG, "verifyUserDeleted: WARNING - User document still exists for uid=" + uid);
            } else {
                Log.d(TAG, "verifyUserDeleted: Verified user document deleted for uid=" + uid);
            }
        } catch (Exception e) {
            Log.w(TAG, "verifyUserDeleted: Could not verify deletion for uid=" + uid + " - " + e.getMessage());
        }
    }

    private void seedUserRepoUid(String uid) {
        context.getSharedPreferences(USER_REPO_PREFS, Context.MODE_PRIVATE)
                .edit().putString(PREFS_UID, uid).apply();
    }

    private void clearAllSyncDates(String uid) {
        context.getSharedPreferences(SNAP_TASK_REPO_PREFS, Context.MODE_PRIVATE)
                .edit().remove("last_sync_snap_task_score_epoch_day_" + uid).apply();
        context.getSharedPreferences(ROAMIO_REPO_PREFS, Context.MODE_PRIVATE)
                .edit().remove("last_sync_roamio_score_epoch_day_" + uid).apply();
        context.getSharedPreferences(ACTIVITY_JAR_REPO_PREFS, Context.MODE_PRIVATE)
                .edit().remove("last_sync_activity_jar_score_epoch_day_" + uid).apply();
    }

    private void setSyncDatesToYesterday(String uid) {
        long yesterday = LocalDate.now().toEpochDay() - 1;
        context.getSharedPreferences(SNAP_TASK_REPO_PREFS, Context.MODE_PRIVATE)
                .edit().putLong("last_sync_snap_task_score_epoch_day_" + uid, yesterday).apply();
        context.getSharedPreferences(ROAMIO_REPO_PREFS, Context.MODE_PRIVATE)
                .edit().putLong("last_sync_roamio_score_epoch_day_" + uid, yesterday).apply();
        context.getSharedPreferences(ACTIVITY_JAR_REPO_PREFS, Context.MODE_PRIVATE)
                .edit().putLong("last_sync_activity_jar_score_epoch_day_" + uid, yesterday).apply();
    }

    private void setSyncDatesToToday(String uid) {
        long today = LocalDate.now().toEpochDay();
        context.getSharedPreferences(SNAP_TASK_REPO_PREFS, Context.MODE_PRIVATE)
                .edit().putLong("last_sync_snap_task_score_epoch_day_" + uid, today).apply();
        context.getSharedPreferences(ROAMIO_REPO_PREFS, Context.MODE_PRIVATE)
                .edit().putLong("last_sync_roamio_score_epoch_day_" + uid, today).apply();
        context.getSharedPreferences(ACTIVITY_JAR_REPO_PREFS, Context.MODE_PRIVATE)
                .edit().putLong("last_sync_activity_jar_score_epoch_day_" + uid, today).apply();
    }

    /**
     * Helper method to set snap task score in Firebase
     */
    private void setRemoteSnapTaskScore(int score) {
        firebaseSnapTaskManager.upsertScore(new SnapTaskModels.SnapTaskScore(testUid, score));
    }

    /**
     * Helper method to get snap task score from Firebase
     */
    private int getRemoteSnapTaskScore() {
        return firebaseSnapTaskManager.getScore(testUid).getScore();
    }

    /**
     * Helper method to set roamio score in Firebase
     */
    private void setRemoteRoamioScore(int score) {
        firebaseRoamioManager.upsertScore(new RoamioModels.RoamioScore(testUid, score));
    }

    /**
     * Helper method to get roamio score from Firebase
     */
    private int getRemoteRoamioScore() {
        return firebaseRoamioManager.getScore(testUid).getScore();
    }

    /**
     * Helper method to set activity jar score in Firebase
     */
    private void setRemoteActivityJarScore(int score) {
        firebaseUserManager.upsertActivityJarScore(new ActivityJarModels.ActivityJarScore(testUid, score));
    }

    /**
     * Helper method to get activity jar score from Firebase
     */
    private int getRemoteActivityJarScore() {
        return firebaseUserManager.getActivityJarScore(testUid).getScore();
    }

    /**
     * Helper method to set global score in Firebase
     */
    private void setRemoteGlobalScore(int score) {
        firebaseUserManager.setGlobalScore(testUid, score);
    }

    /**
     * Helper method to get global score from Firebase
     */
    private int getRemoteGlobalScore() {
        try {
            Integer score = firebaseUserManager.getGlobalScore(testUid);
            return score != null ? score : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Helper method to set streak in Firebase
     */
    private void setRemoteStreak(int streak) {
        firebaseUserManager.setStreak(testUid, streak);
    }

    /**
     * Helper method to get streak from Firebase
     */
    private int getRemoteStreak() {
        try {
            Integer streak = firebaseUserManager.getStreak(testUid);
            return streak != null ? streak : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // ============================================================
    // SCENARIO 1: Initial Sign-In Sync (Remote → Local)
    // ============================================================

    @Test
    public void initialSync_snapTaskScore_remoteHigher_updatesLocal() {
        snapTaskRepo.upsertSnapTaskScore(0);
        setRemoteSnapTaskScore(100);
        clearAllSyncDates(testUid);
        snapTaskRepo.syncSnapTaskScoreOnceDaily();
        assertEquals(100, snapTaskRepo.getSnapTaskScore().intValue());
    }

    @Test
    public void initialSync_roamioScore_remoteHigher_updatesLocal() {
        roamioRepo.upsertRoamioScore(0);
        setRemoteRoamioScore(200);
        clearAllSyncDates(testUid);
        roamioRepo.syncRoamioScoreOnceDaily();
        assertEquals(200, roamioRepo.getRoamioScore().getScore());
    }

    @Test
    public void initialSync_activityJarScore_remoteHigher_updatesLocal() {
        activityJarRepo.upsertActivityJarScore(0);
        setRemoteActivityJarScore(150);
        clearAllSyncDates(testUid);
        activityJarRepo.syncActivityJarScoreOnceDaily();
        assertEquals(150, activityJarRepo.getActivityJarScore().intValue());
    }

    @Test
    public void initialSync_globalScore_remoteHigher_updatesLocal() {
        userRepo.setGlobalScore(0);
        setRemoteGlobalScore(500);
        userRepo.syncGlobalScore();
        assertEquals(500, userRepo.getGlobalScore());
    }

    @Test
    public void initialSync_streak_remoteHigher_updatesLocal() {
        userRepo.setStreakCount(0);
        setRemoteStreak(7);
        userRepo.syncStreak();
        assertEquals(7, userRepo.getStreakCount());
    }

    @Test
    public void initialSync_allMicroAppScores_comprehensiveSync() {
        snapTaskRepo.upsertSnapTaskScore(0);
        roamioRepo.upsertRoamioScore(0);
        activityJarRepo.upsertActivityJarScore(0);
        userRepo.setGlobalScore(0);
        userRepo.setStreakCount(0);

        setRemoteSnapTaskScore(100);
        setRemoteRoamioScore(200);
        setRemoteActivityJarScore(150);
        setRemoteGlobalScore(450);
        setRemoteStreak(5);

        clearAllSyncDates(testUid);

        snapTaskRepo.syncSnapTaskScoreOnceDaily();
        roamioRepo.syncRoamioScoreOnceDaily();
        activityJarRepo.syncActivityJarScoreOnceDaily();
        userRepo.syncGlobalScore();
        userRepo.syncStreak();

        assertEquals(100, snapTaskRepo.getSnapTaskScore().intValue());
        assertEquals(200, roamioRepo.getRoamioScore().getScore());
        assertEquals(150, activityJarRepo.getActivityJarScore().intValue());
        assertEquals(450, userRepo.getGlobalScore());
        assertEquals(5, userRepo.getStreakCount());
    }

    @Test
    public void initialSync_globalScoreCalculation_fromMicroAppScores() {
        snapTaskRepo.upsertSnapTaskScore(100);
        roamioRepo.upsertRoamioScore(200);
        activityJarRepo.upsertActivityJarScore(150);

        int calculated = snapTaskRepo.getSnapTaskScore()
                + roamioRepo.getRoamioScore().getScore()
                + activityJarRepo.getActivityJarScore();
        userRepo.setGlobalScore(calculated);

        assertEquals(450, userRepo.getGlobalScore());
    }

    // ============================================================
    // SCENARIO 2: Daily Sync (Local → Remote)
    // ============================================================

    @Test
    public void dailySync_snapTaskScore_localHigher_updatesRemote() {
        snapTaskRepo.upsertSnapTaskScore(200);
        setRemoteSnapTaskScore(50);
        setSyncDatesToYesterday(testUid);
        snapTaskRepo.syncSnapTaskScoreOnceDaily();
        assertEquals(200, getRemoteSnapTaskScore());
        assertEquals(200, snapTaskRepo.getSnapTaskScore().intValue());
    }

    @Test
    public void dailySync_roamioScore_localHigher_updatesRemote() {
        roamioRepo.upsertRoamioScore(300);
        setRemoteRoamioScore(100);
        setSyncDatesToYesterday(testUid);
        roamioRepo.syncRoamioScoreOnceDaily();
        assertEquals(300, getRemoteRoamioScore());
        assertEquals(300, roamioRepo.getRoamioScore().getScore());
    }

    @Test
    public void dailySync_activityJarScore_localHigher_updatesRemote() {
        activityJarRepo.upsertActivityJarScore(250);
        setRemoteActivityJarScore(100);
        setSyncDatesToYesterday(testUid);
        activityJarRepo.syncActivityJarScoreOnceDaily();
        assertEquals(250, getRemoteActivityJarScore());
    }

    @Test
    public void dailySync_globalScore_localHigher_updatesRemote() {
        userRepo.setGlobalScore(600);
        setRemoteGlobalScore(300);
        userRepo.syncGlobalScore();
        assertEquals(600, getRemoteGlobalScore());
    }

    @Test
    public void dailySync_streak_localHigher_updatesRemote() {
        userRepo.setStreakCount(10);
        setRemoteStreak(3);
        userRepo.syncStreak();
        assertEquals(10, getRemoteStreak());
    }

    @Test
    public void dailySync_alreadySyncedToday_skipsSync() {
        snapTaskRepo.upsertSnapTaskScore(100);
        setRemoteSnapTaskScore(50);
        setSyncDatesToToday(testUid);
        snapTaskRepo.syncSnapTaskScoreOnceDaily();
        assertEquals(100, snapTaskRepo.getSnapTaskScore().intValue());
        // Remote should remain unchanged since sync was skipped
        assertEquals(50, getRemoteSnapTaskScore());
    }

    @Test
    public void dailySync_allMicroAppScores_comprehensiveSync() {
        snapTaskRepo.upsertSnapTaskScore(200);
        roamioRepo.upsertRoamioScore(300);
        activityJarRepo.upsertActivityJarScore(150);
        userRepo.setGlobalScore(650);
        userRepo.setStreakCount(10);

        setRemoteSnapTaskScore(50);
        setRemoteRoamioScore(100);
        setRemoteActivityJarScore(50);
        setRemoteGlobalScore(200);
        setRemoteStreak(3);

        setSyncDatesToYesterday(testUid);

        snapTaskRepo.syncSnapTaskScoreOnceDaily();
        roamioRepo.syncRoamioScoreOnceDaily();
        activityJarRepo.syncActivityJarScoreOnceDaily();
        userRepo.syncGlobalScore();
        userRepo.syncStreak();

        assertEquals(200, getRemoteSnapTaskScore());
        assertEquals(300, getRemoteRoamioScore());
        assertEquals(150, getRemoteActivityJarScore());
        assertEquals(650, getRemoteGlobalScore());
        assertEquals(10, getRemoteStreak());
    }

    // ============================================================
    // Edge Cases & Error Handling
    // ============================================================

    @Test
    public void sync_equalScores_noChange() {
        snapTaskRepo.upsertSnapTaskScore(100);
        setRemoteSnapTaskScore(100);
        clearAllSyncDates(testUid);
        snapTaskRepo.syncSnapTaskScoreOnceDaily();
        assertEquals(100, snapTaskRepo.getSnapTaskScore().intValue());
        assertEquals(100, getRemoteSnapTaskScore());
    }

    @Test
    public void sync_nullUid_doesNotCrash() {
        context.getSharedPreferences(USER_REPO_PREFS, Context.MODE_PRIVATE)
                .edit().remove(PREFS_UID).apply();
        snapTaskRepo.syncSnapTaskScoreOnceDaily();
        roamioRepo.syncRoamioScoreOnceDaily();
        activityJarRepo.syncActivityJarScoreOnceDaily();
    }

    @Test
    public void sync_emptyUid_doesNotCrash() {
        context.getSharedPreferences(USER_REPO_PREFS, Context.MODE_PRIVATE)
                .edit().putString(PREFS_UID, "").apply();
        snapTaskRepo.syncSnapTaskScoreOnceDaily();
        roamioRepo.syncRoamioScoreOnceDaily();
        activityJarRepo.syncActivityJarScoreOnceDaily();
    }

    @Test
    public void sync_syncDatePersistence_recordsToday() {
        clearAllSyncDates(testUid);
        snapTaskRepo.syncSnapTaskScoreOnceDaily();

        long today = LocalDate.now().toEpochDay();
        long stored = context.getSharedPreferences(SNAP_TASK_REPO_PREFS, Context.MODE_PRIVATE)
                .getLong("last_sync_snap_task_score_epoch_day_" + testUid, -1);
        assertEquals(today, stored);
    }
}
