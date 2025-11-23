package com.code.wlu.cp470.wellnest.ui.roamio;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowCompat;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.ui.components.WellnestProgressBar;
import com.code.wlu.cp470.wellnest.data.RoamioModels;
import com.code.wlu.cp470.wellnest.viewmodel.RoamioViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;

import java.util.Locale;

import eightbitlab.com.blurview.BlurTarget;
import eightbitlab.com.blurview.BlurView;

public class RoamioActivity extends AppCompatActivity {

    private static final String TAG = "RoamioActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final float COMPLETION_RADIUS_METERS = 50f;

    Context context;
    private TextView scoreText, walkTitle, walkDescription;
    private ImageView characterImage, infoButton;
    private Button primaryButton;
    private View difficultyDot1, difficultyDot2, difficultyDot3;

    private RoamioViewModel roamioViewModel;
    private RoamioModels.Walk currentWalk;
    private boolean walkStarted = false;
    private int currentDifficulty = 1;
    private FusedLocationProviderClient fusedLocationClient;

    private View loadingOverlay;
    private TextView loadingMessage;
    private WellnestProgressBar loadingProgressBar;
    private TextView progressPercentage;
    private Handler loadingMessageHandler;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Hide navigation bar and allow content behind the status bar
        View decorView = getWindow().getDecorView();

        // Reuse the existing fragment layout as Activity content to minimize changes
        setContentView(R.layout.activity_roamio);

        // Ensure layout extends behind system bars; do NOT use FULLSCREEN so status icons remain visible
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        // Enter transition (the caller fragment also applies this)
        try {
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } catch (Exception ignored) {
        }

        // Initialize views
        scoreText = findViewById(R.id.roamio_score_text);
        walkTitle = findViewById(R.id.walkTitle);
        walkDescription = findViewById(R.id.walkDescription);
        characterImage = findViewById(R.id.roamioCharacter);
        primaryButton = findViewById(R.id.walkButton);
        infoButton = findViewById(R.id.roamio_card_info_button);
        difficultyDot1 = findViewById(R.id.difficultyDot1);
        difficultyDot2 = findViewById(R.id.difficultyDot2);
        difficultyDot3 = findViewById(R.id.difficultyDot3);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize loading overlay
        loadingOverlay = findViewById(R.id.roamioLoadingOverlay);
        loadingMessage = loadingOverlay.findViewById(R.id.loadingMessage);
        loadingProgressBar = loadingOverlay.findViewById(R.id.loadingProgressBar);
        progressPercentage = loadingOverlay.findViewById(R.id.progressPercentage);
        loadingMessageHandler = new Handler(Looper.getMainLooper());

        // Initialize ViewModel
        roamioViewModel = new RoamioViewModel(getApplication());
        int score = roamioViewModel.getScore().getScore();
        scoreText.setText(String.valueOf(score));

        // Set character image based on score
        if (score < 500) {
            characterImage.setImageResource(R.drawable.puffin_baby);
        } else if (score < 1000) {
            characterImage.setImageResource(R.drawable.puffin_teen);
        } else if (score < 1500) {
            characterImage.setImageResource(R.drawable.puffin_adult);
        } else {
            characterImage.setImageResource(R.drawable.puffin_senior);
        }

        // Set up button click listener
        primaryButton.setOnClickListener(v -> handleWalkButtonClick());

        // Generate walk using current location and update UI with results
        // Show loading overlay with fade-in animation
        showLoadingOverlay();
        
        roamioViewModel.generateWalk(new RoamioViewModel.RoamioCallback<>() {
            @Override
            public void onSuccess(RoamioModels.Walk walk) {
                hideLoadingOverlay();
                currentWalk = walk;
                // Update UI with generated walk details
                walkTitle.setText(walk.getName());
                walkDescription.setText(walk.getStory());

                // Calculate and display difficulty
                currentDifficulty = calculateDifficulty(walk.getDistanceMeters());
                updateDifficultyDisplay(currentDifficulty);

                // Set initial button state
                primaryButton.setText("Start Walk");
                walkStarted = false;
            }

            @Override
            public void onError(String error) {
                hideLoadingOverlay();
                // Display error message and fallback content
                walkTitle.setText("Walk Generation Failed");
                walkDescription.setText(error);
                primaryButton.setEnabled(false);
            }

            @Override
            public void onProgress(int percent, String message) {
                updateLoadingProgress(percent, message);
            }
        });

        BlurTarget blurTarget = findViewById(R.id.roamio_bg_blurTarget);
        BlurView blurView = findViewById(R.id.roamio_blurView);
        blurView.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        blurView.setClipToOutline(true);
        blurView.setupWith(blurTarget)
                .setBlurRadius(5f);
    }

    /**
     * Calculates difficulty rating based on walk distance.
     *
     * @param distanceMeters Distance in meters
     * @return Difficulty level (1, 2, or 3)
     */
    private int calculateDifficulty(float distanceMeters) {
        if (distanceMeters < 1000) {
            return 1; // Easy - 300 points
        } else if (distanceMeters <= 2500) {
            return 2; // Medium - 500 points
        } else {
            return 3; // Hard - 800 points
        }
    }

    /**
     * Gets points to award based on difficulty level.
     *
     * @param difficulty Difficulty level (1, 2, or 3)
     * @return Points to award
     */
    private int getPointsForDifficulty(int difficulty) {
        switch (difficulty) {
            case 1:
                return 300;
            case 2:
                return 500;
            case 3:
                return 800;
            default:
                return 300;
        }
    }

    /**
     * Updates the difficulty indicator UI.
     *
     * @param difficulty Difficulty level (1, 2, or 3)
     */
    private void updateDifficultyDisplay(int difficulty) {
        int activeColor = 0xFF59C060; // Green
        int inactiveColor = 0xFFFFFFFF; // White

        difficultyDot1.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                difficulty >= 1 ? activeColor : inactiveColor));
        difficultyDot2.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                difficulty >= 2 ? activeColor : inactiveColor));
        difficultyDot3.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                difficulty >= 3 ? activeColor : inactiveColor));
    }

    /**
     * Handles walk button clicks - implements the state machine.
     */
    private void handleWalkButtonClick() {
        if (currentWalk == null) {
            Toast.makeText(this, "No walk available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!walkStarted) {
            // State 1: Start Walk - Launch Maps
            launchMapsNavigation();
            walkStarted = true;
            primaryButton.setText("Finish Walk");
        } else {
            // State 2: Finish Walk - Check location and complete
            checkLocationAndCompleteWalk();
        }
    }

    /**
     * Launches Google Maps with navigation to the walk location.
     */
    private void launchMapsNavigation() {
        try {
            String startAddress = currentWalk.getStartAddress();
            String endAddress = currentWalk.getEndAddress();

            String uri = String.format(Locale.US,
                    "https://www.google.com/maps/dir/?api=1&origin=%s&destination=%s&travelmode=walking",
                    Uri.encode(startAddress),
                    Uri.encode(endAddress));

            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                String fallbackUri = String.format(Locale.US,
                        "geo:0,0?q=%s", Uri.encode(endAddress));
                Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUri));
                startActivity(fallbackIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching maps", e);
            Toast.makeText(this, "Error launching navigation", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Checks if user is at destination and completes the walk.
     */
    private void checkLocationAndCompleteWalk() {
        // Check location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Get current location
        Task<Location> locationTask = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY, null);

        locationTask.addOnSuccessListener(location -> {
            if (location == null) {
                Toast.makeText(this, "Unable to get current location. Please try again.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            // Parse end location from walk
            float distance = calculateDistanceToDestination(location);

            if (distance <= COMPLETION_RADIUS_METERS) {
                // Within radius - complete the walk
                completeWalk();
            } else {
                // Not close enough
                String message = String.format(Locale.US,
                        "You're %.0f meters from the destination. Get within %d meters to complete!",
                        distance, (int) COMPLETION_RADIUS_METERS);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error getting location", e);
            Toast.makeText(this, "Error getting location. Please try again.",
                    Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Calculates distance to the walk destination.
     *
     * @param currentLocation Current user location
     * @return Distance in meters
     */
    private float calculateDistanceToDestination(Location currentLocation) {
        // For simplicity, we'll use a geocoding approach
        // In production, you might want to cache the destination coordinates
        try {
            android.location.Geocoder geocoder = new android.location.Geocoder(this, Locale.getDefault());
            java.util.List<android.location.Address> addresses = geocoder.getFromLocationName(
                    currentWalk.getEndAddress(), 1);

            if (addresses != null && !addresses.isEmpty()) {
                android.location.Address destAddress = addresses.get(0);
                float[] results = new float[1];
                Location.distanceBetween(
                        currentLocation.getLatitude(),
                        currentLocation.getLongitude(),
                        destAddress.getLatitude(),
                        destAddress.getLongitude(),
                        results);
                return results[0];
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating distance", e);
        }

        // If geocoding fails, return a large distance
        return Float.MAX_VALUE;
    }

    /**
     * Completes the walk and awards points.
     */
    private void completeWalk() {
        // Award points based on difficulty
        int points = getPointsForDifficulty(currentDifficulty);
        roamioViewModel.addToScore(points);

        // Mark walk as completed
        currentWalk.setCompleted(true);

        // Update UI
        int newScore = roamioViewModel.getScore().getScore();
        scoreText.setText(String.valueOf(newScore));

        // Show success message
        String message = String.format(Locale.US,
                "Walk completed! +%d points", points);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        // Reset button state
        primaryButton.setText("Walk Completed!");
        primaryButton.setEnabled(false);

        Log.d(TAG, String.format("Walk completed: %s, Points: %d, New Score: %d",
                currentWalk.getName(), points, newScore));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, try again
                checkLocationAndCompleteWalk();
            } else {
                Toast.makeText(this, "Location permission required to complete walk",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Show the loading overlay with fade-in animation.
     */
    private void showLoadingOverlay() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.VISIBLE);

            // Reset progress
            if (loadingProgressBar != null) {
                loadingProgressBar.setProgress(0);
            }
            if (progressPercentage != null) {
                progressPercentage.setText("0%");
            }
            if (loadingMessage != null) {
                loadingMessage.setText(R.string.roamio_loading_message);
            }
            
            // Apply fade-in animation
            loadingOverlay.setAlpha(0f);
            loadingOverlay.animate()
                .alpha(1f)
                .setDuration(400)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
        }
    }

    /**
     * Updates the loading progress UI.
     *
     * @param percent Progress percentage (0-100)
     * @param message Status message to display
     */
    private void updateLoadingProgress(int percent, String message) {
        if (loadingOverlay != null && loadingOverlay.getVisibility() == View.VISIBLE) {
            if (loadingProgressBar != null) {
                loadingProgressBar.setProgress(percent);
            }
            if (progressPercentage != null) {
                progressPercentage.setText(String.format(Locale.US, "%d%%", percent));
            }
            if (loadingMessage != null && message != null) {
                loadingMessage.setText(message);
            }
        }
    }

    /**
     * Hide the loading overlay with fade-out animation and clean up.
     */
    private void hideLoadingOverlay() {
        if (loadingOverlay != null && loadingOverlay.getVisibility() == View.VISIBLE) {
            // Ensure 100% completion
            if (loadingProgressBar != null) {
                loadingProgressBar.setProgress(100);
            }
            if (progressPercentage != null) {
                progressPercentage.setText("100%");
            }
            
            // Apply fade-out animation
            loadingOverlay.animate()
                .alpha(0f)
                .setDuration(300)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> {
                    // Cleanup after animation completes
                    loadingOverlay.setVisibility(View.GONE);
                    loadingOverlay.setAlpha(1f); // Reset alpha for next show
                })
                .start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
