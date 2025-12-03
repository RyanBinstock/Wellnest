package com.code.wlu.cp470.wellnest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.code.wlu.cp470.wellnest.data.ActivityJarRepository;
import com.code.wlu.cp470.wellnest.data.RoamioRepository;
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
import com.code.wlu.cp470.wellnest.utils.ActivityJarPrefetcher;
import com.code.wlu.cp470.wellnest.utils.MusicService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    /**
     * Broadcast action sent when global score sync completes. HomeFragment listens for this.
     */
    public static final String ACTION_SCORE_SYNC_COMPLETE = "com.code.wlu.cp470.wellnest.ACTION_SCORE_SYNC_COMPLETE";
    private static final String TAG = "MainActivity";
    private static final String PREFS = "main_activity_prefs";
    private static final String KEY_LAST_STREAK_DATE = "last_streak_epoch_day";
    private SharedPreferences prefs;
    private WellnestDatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private UserRepository userRepository;
    private SnapTaskRepository snapTaskRepository;
    private RoamioRepository roamioRepository;
    private ActivityJarRepository activityJarRepository;

    private FirebaseAuth firebaseAuth;
    private boolean userServicesInitialized = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Hide navigation bar and allow content behind the status bar
        View decorView = getWindow().getDecorView();

        // Use WindowInsetsControllerCompat for immersive nav hiding while keeping status bar visible
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), decorView);
        if (insetsController != null) {
            insetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
            // Hide only the navigation bar; keep status bar icons visible
            insetsController.hide(WindowInsetsCompat.Type.navigationBars());
            insetsController.show(WindowInsetsCompat.Type.statusBars());
        }

        // Ensure layout extends behind system bars; do NOT use FULLSCREEN so status icons remain visible
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host);
        NavController navController = navHostFragment.getNavController();

        // Synchronization logic goes here
        prefs = this.getSharedPreferences(PREFS, MODE_PRIVATE);

        dbHelper = new WellnestDatabaseHelper(this);
        db = dbHelper.getWritableDatabase();


        // Save date information
        long lastCheckMillis = prefs.getLong("last_check_date", 0);
        long nowMillis = System.currentTimeMillis();
        LocalDate lastDate = Instant.ofEpochMilli(lastCheckMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        LocalDate today = Instant.ofEpochMilli(nowMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        boolean newDay = !today.equals(lastDate);
        Log.d(TAG, "onCreate: lastCheckMillis=" + lastCheckMillis
                + ", lastDate=" + lastDate
                + ", today=" + today
                + ", newDay=" + newDay);

        if (newDay) {
            Log.d(TAG, "onCreate: new day detected, starting background sync of snap tasks");
            new Thread(() -> {
                SnapTaskManager snapTaskManager = new SnapTaskManager(db);
                FirebaseSnapTaskManager snapTaskRemoteManager = new FirebaseSnapTaskManager();
                SnapTaskRepository snapTaskRepository =
                        new SnapTaskRepository(getApplicationContext(), snapTaskManager, snapTaskRemoteManager);
                try {
                    snapTaskRepository.syncSnapTasks();
                    prefs.edit().putLong("last_check_date", nowMillis).apply();
                    Log.d(TAG, "onCreate: snap task sync finished, last_check_date updated=" + nowMillis);
                } catch (Exception e) {
                    Log.e(TAG, "onCreate: snap task sync failed", e);
                }
            }).start();
        } else {
            Log.d(TAG, "onCreate: same day as last check, skipping snap task sync");
        }

        // Prefetch Activity Jar activities in background
        new Thread(() -> {
            ActivityJarPrefetcher.prefetchActivities(getApplicationContext());
        }).start();

        // Initialize Firebase Auth and listen for authentication state
        firebaseAuth = FirebaseAuth.getInstance();
        Log.d(TAG, "onCreate: Firebase Auth initialized, checking current user");

        // Check if user is already authenticated
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "onCreate: User already authenticated, initializing user services");
            initializeUserDependentServices(today);
        } else {
            Log.d(TAG, "onCreate: No user authenticated yet, services will be initialized after login");
        }
    }

    /**
     * Initialize services that depend on an authenticated user.
     * This should only be called when a user is authenticated.
     */
    private void initializeUserDependentServices(LocalDate today) {
        // Prevent re-initialization
        if (userServicesInitialized) {
            Log.d(TAG, "initializeUserDependentServices: Services already initialized, skipping");
            return;
        }

        // Save UID immediately to SharedPreferences before sync methods run
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            SharedPreferences userRepoPrefs = getSharedPreferences("user_repo_prefs", MODE_PRIVATE);
            userRepoPrefs.edit().putString("uid", uid).apply();
            Log.d(TAG, "initializeUserDependentServices: Saved UID to SharedPreferences: " + uid);
        }

        Log.d(TAG, "initializeUserDependentServices: Starting user-dependent service initialization");

        try {
            // Create Repositories (this requires an authenticated user)
            userRepository = new UserRepository(this, new UserManager(db), new FirebaseUserManager());
            snapTaskRepository = new SnapTaskRepository(this, new SnapTaskManager(db), new FirebaseSnapTaskManager());
            roamioRepository = new RoamioRepository(this, new RoamioManager(db), new FirebaseRoamioManager());
            activityJarRepository = new ActivityJarRepository(
                    this,
                    new ActivityJarManager(db),
                    new FirebaseUserManager()
            );

            // Ensure local SQLite user profile exists before any score operations
            FirebaseUser currentUserForProfile = firebaseAuth.getCurrentUser();
            if (currentUserForProfile != null) {
                userRepository.upsertUserProfile(
                    currentUserForProfile.getUid(),
                    currentUserForProfile.getDisplayName() != null ? currentUserForProfile.getDisplayName() : "",
                    currentUserForProfile.getEmail() != null ? currentUserForProfile.getEmail() : ""
                );
                Log.d(TAG, "initializeUserDependentServices: Created local user profile for uid=" + currentUserForProfile.getUid());
            }

            // Run sync operations in background
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    Log.d(TAG, "[INIT] initializeUserDependentServices: Starting syncGlobalScore");

                    int snapScore = snapTaskRepository.getSnapTaskScore();
                    int roamioScore = roamioRepository.getRoamioScore().getScore();
                    int activityScore = activityJarRepository.getActivityJarScore();
                    Log.d(TAG, "[INIT] initializeUserDependentServices: snapScore=" + snapScore
                            + ", roamioScore=" + roamioScore
                            + ", activityScore=" + activityScore
                            + ", globalScore should be =" + (snapScore + roamioScore + activityScore));
                    int globalScore = snapScore + roamioScore + activityScore;
                    userRepository.setGlobalScore(globalScore);
                    Log.d(TAG, "[INIT] initializeUserDependentServices: setGlobalScore complete, starting actual syncs");

                    try {
                        Log.d(TAG, "[INIT] initializeUserDependentServices: calling syncGlobalScore()");
                        userRepository.syncGlobalScore();
                        Log.d(TAG, "[INIT] initializeUserDependentServices: syncGlobalScore() returned");
                    } catch (Exception e) {
                        Log.e(TAG, "[INIT] initializeUserDependentServices: syncGlobalScore FAILED", e);
                    }

                    try {
                        Log.d(TAG, "[INIT] initializeUserDependentServices: calling syncSnapTaskScoreOnceDaily()");
                        snapTaskRepository.syncSnapTaskScoreOnceDaily();
                        Log.d(TAG, "[INIT] initializeUserDependentServices: syncSnapTaskScoreOnceDaily() returned");
                    } catch (Exception e) {
                        Log.e(TAG, "[INIT] initializeUserDependentServices: syncSnapTaskScoreOnceDaily FAILED", e);
                    }

                    try {
                        Log.d(TAG, "[INIT] initializeUserDependentServices: calling syncRoamioScoreOnceDaily()");
                        roamioRepository.syncRoamioScoreOnceDaily();
                        Log.d(TAG, "[INIT] initializeUserDependentServices: syncRoamioScoreOnceDaily() returned");
                    } catch (Exception e) {
                        Log.e(TAG, "[INIT] initializeUserDependentServices: syncRoamioScoreOnceDaily FAILED", e);
                    }

                    try {
                        Log.d(TAG, "[INIT] initializeUserDependentServices: calling syncActivityJarScoreOnceDaily()");
                        activityJarRepository.syncActivityJarScoreOnceDaily();
                        Log.d(TAG, "[INIT] initializeUserDependentServices: syncActivityJarScoreOnceDaily() returned");
                    } catch (Exception e) {
                        Log.e(TAG, "[INIT] initializeUserDependentServices: syncActivityJarScoreOnceDaily FAILED", e);
                    }

                    Log.d(TAG, "[INIT] initializeUserDependentServices: syncStreak");

                    int streakAfterSync = 0;
                    boolean userDocExists = false;
                    
                    try {
                        Log.d(TAG, "[INIT] initializeUserDependentServices: calling syncStreak()");
                        userRepository.syncStreak();
                        Log.d(TAG, "[INIT] initializeUserDependentServices: syncStreak() returned");
                        
                        // DIAGNOSTIC LOG: Check streak after sync
                        streakAfterSync = userRepository.getStreakCount();
                        Log.d(TAG, "[INIT] DIAGNOSTIC - Streak count after sync: " + streakAfterSync);
                        
                        // Check if user document exists in Firebase
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            userDocExists = userRepository.userDocumentExists(firebaseUser.getUid());
                            Log.d(TAG, "[INIT] DIAGNOSTIC - User document exists in Firebase: " + userDocExists);
                        }
                        
                    } catch (Exception e) {
                        Log.e(TAG, "[INIT] initializeUserDependentServices: syncStreak FAILED", e);
                    }

                    Log.d(TAG, "[INIT] initializeUserDependentServices: syncGlobalScore completed");

                    // Streak tracking logic - FIXED VERSION
                    long lastStreakEpochDay = prefs.getLong(KEY_LAST_STREAK_DATE, 0);
                    long todayEpochDay = today.toEpochDay();

                    Log.d(TAG, "[INIT] === STREAK DEBUG START ===");
                    Log.d(TAG, "[INIT] initializeUserDependentServices: streak check - lastStreakEpochDay=" + lastStreakEpochDay
                            + ", todayEpochDay=" + todayEpochDay);
                    Log.d(TAG, "[INIT] initializeUserDependentServices: comparison result - sameDay=" + (lastStreakEpochDay == todayEpochDay)
                            + ", consecutive=" + (lastStreakEpochDay == todayEpochDay - 1));
                    
                    // DIAGNOSTIC LOG: Track if this is first-time device login
                    boolean isFirstTimeDeviceLogin = (lastStreakEpochDay == 0);
                    Log.d(TAG, "[INIT] DIAGNOSTIC - Is this first-time device login: " + isFirstTimeDeviceLogin);
                    Log.d(TAG, "[INIT] DIAGNOSTIC - Streak after sync: " + streakAfterSync + ", User doc exists: " + userDocExists);

                    if (lastStreakEpochDay == todayEpochDay) {
                        // Same day - already tracked today, do nothing
                        Log.d(TAG, "[INIT] initializeUserDependentServices: streak already tracked today, skipping");
                    } else if (streakAfterSync > 0) {
                        // We have a synced streak from Firebase, use it and update lastStreakEpochDay
                        Log.d(TAG, "[INIT] initializeUserDependentServices: Using synced streak from Firebase: " + streakAfterSync);
                        boolean writeSuccess = prefs.edit().putLong(KEY_LAST_STREAK_DATE, todayEpochDay).commit();
                        Log.d(TAG, "[INIT] initializeUserDependentServices: Updated lastStreakEpochDay to today for synced streak, writeSuccess=" + writeSuccess);
                    } else if (streakAfterSync == 0 && userDocExists) {
                        // Existing user with 0 streak, update lastStreakEpochDay to today but don't increment
                        Log.d(TAG, "[INIT] initializeUserDependentServices: Existing user with 0 streak, updating lastStreakEpochDay only");
                        boolean writeSuccess = prefs.edit().putLong(KEY_LAST_STREAK_DATE, todayEpochDay).commit();
                        Log.d(TAG, "[INIT] initializeUserDependentServices: Updated lastStreakEpochDay for existing user with 0 streak, writeSuccess=" + writeSuccess);
                    } else if (lastStreakEpochDay == todayEpochDay - 1) {
                        // Exactly yesterday - consecutive day, increment streak (only for truly new users)
                        Log.d(TAG, "[INIT] initializeUserDependentServices: BEFORE incrementStreak for consecutive day");
                        int streakBeforeIncrement = userRepository.getStreakCount();
                        int newStreak = userRepository.incrementStreak();
                        Log.d(TAG, "[INIT] DIAGNOSTIC - Streak before increment: " + streakBeforeIncrement + ", after increment: " + newStreak);
                        Log.d(TAG, "[INIT] initializeUserDependentServices: AFTER incrementStreak, newStreak=" + newStreak);
                        boolean writeSuccess = prefs.edit().putLong(KEY_LAST_STREAK_DATE, todayEpochDay).commit();
                        Log.d(TAG, "[INIT] initializeUserDependentServices: consecutive day, streak incremented to " + newStreak + ", writeSuccess=" + writeSuccess);
                        long verifyRead = prefs.getLong(KEY_LAST_STREAK_DATE, -1);
                        Log.d(TAG, "[INIT] initializeUserDependentServices: VERIFY READ after write - stored value=" + verifyRead + ", expected=" + todayEpochDay);
                    } else {
                        // More than 1 day ago OR never set (0) - reset and start fresh at 1 (only for truly new users)
                        Log.d(TAG, "[INIT] initializeUserDependentServices: BEFORE resetStreak");
                        int streakBeforeReset = userRepository.getStreakCount();
                        userRepository.resetStreak();
                        int streakAfterReset = userRepository.getStreakCount();
                        Log.d(TAG, "[INIT] DIAGNOSTIC - Streak before reset: " + streakBeforeReset + ", after reset: " + streakAfterReset);
                        Log.d(TAG, "[INIT] initializeUserDependentServices: AFTER resetStreak, BEFORE incrementStreak");
                        int newStreak = userRepository.incrementStreak();
                        Log.d(TAG, "[INIT] DIAGNOSTIC - Final streak after increment: " + newStreak);
                        Log.d(TAG, "[INIT] initializeUserDependentServices: AFTER incrementStreak, newStreak=" + newStreak);
                        boolean writeSuccess = prefs.edit().putLong(KEY_LAST_STREAK_DATE, todayEpochDay).commit();
                        Log.d(TAG, "[INIT] initializeUserDependentServices: streak reset and started fresh, new streak=" + newStreak + ", writeSuccess=" + writeSuccess);
                        long verifyRead = prefs.getLong(KEY_LAST_STREAK_DATE, -1);
                        Log.d(TAG, "[INIT] initializeUserDependentServices: VERIFY READ after write - stored value=" + verifyRead + ", expected=" + todayEpochDay);
                    }
                    Log.d(TAG, "[INIT] === STREAK DEBUG END ===");

                    userServicesInitialized = true;
                    Log.d(TAG, "[INIT] initializeUserDependentServices: All user-dependent services initialized successfully");

                    // Broadcast that score sync is complete so UI can refresh
                    Intent syncCompleteIntent = new Intent(ACTION_SCORE_SYNC_COMPLETE);
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(syncCompleteIntent);
                    Log.d(TAG, "[INIT] initializeUserDependentServices: Sent ACTION_SCORE_SYNC_COMPLETE broadcast");
                } catch (Exception e) {
                    Log.e(TAG, "[INIT] initializeUserDependentServices: EXCEPTION during initialization!", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "initializeUserDependentServices: Failed to create UserRepository", e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startService(new Intent(this, MusicService.class));

        // Add auth state listener to initialize services when user logs in
        if (firebaseAuth != null) {
            firebaseAuth.addAuthStateListener(firebaseAuth -> {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null && !userServicesInitialized) {
                    Log.d(TAG, "onStart: User authenticated, initializing services");
                    LocalDate today = Instant.ofEpochMilli(System.currentTimeMillis())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    initializeUserDependentServices(today);
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Intent i = new Intent(this, MusicService.class);
        i.setAction(MusicService.ACTION_PAUSE);
        startService(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent i = new Intent(this, MusicService.class);
        i.setAction(MusicService.ACTION_RESUME);
        startService(i);

        // Skip score update if repositories aren't initialized yet
        if (snapTaskRepository == null || roamioRepository == null ||
            activityJarRepository == null || userRepository == null) {
            Log.w(TAG, "onResume: Repositories not initialized yet, skipping score update");
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
                    try {
                        Log.d(TAG, "[RESUME] onResume: Starting score recalculation");

                        int snapScore = snapTaskRepository.getSnapTaskScore();
                        int roamioScore = roamioRepository.getRoamioScore().getScore();
                        int activityScore = activityJarRepository.getActivityJarScore();
                        Log.d(TAG, "[RESUME] onResume: snapScore=" + snapScore
                                + ", roamioScore=" + roamioScore
                                + ", activityScore=" + activityScore
                                + ", globalScore should be =" + (snapScore + roamioScore + activityScore));
                        int globalScore = snapScore + roamioScore + activityScore;
                        userRepository.setGlobalScore(globalScore);
                    } catch (Exception e) {
                        Log.e(TAG, "[RESUME] onResume: EXCEPTION during score recalculation!", e);
                    }
                }
        );
        executor.shutdown();
    }


    @Override
    protected void onStop() {
        super.onStop();
        stopService(new Intent(this, MusicService.class));
    }

}
