package com.code.wlu.cp470.wellnest.ui.components;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.ui.effects.UiClickEffects;
import com.code.wlu.cp470.wellnest.ui.effects.UiProgressEffects;

/**
 * Demo fragment showcasing the WellnestProgressBar component and its features.
 * This fragment provides interactive controls to test various progress bar functionalities.
 */
public class WellnestProgressBarDemoFragment extends Fragment {
    private static final String TAG = "WellnestProgressDemo";
    
    // Progress bars
    private WellnestProgressBar progressDefault;
    private WellnestProgressBar progressLarge;
    private WellnestProgressBar progressSmall;
    private WellnestProgressBar progressAccent;
    private WellnestProgressBar progressSuccess;
    private WellnestProgressBar progressLight;
    private WellnestProgressBar progressIndeterminate;
    private WellnestProgressBar progressCustom;
    private WellnestProgressBar progressInteractive;
    
    // Control buttons
    private Button btnDecrease;
    private Button btnIncrease;
    private Button btnReset;
    private Button btnComplete;
    private Button btnPulse;
    private Button btnBounce;
    private Button btnIndeterminate;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sample_wellnest_progress_bar, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Starting initialization");
        
        // Initialize progress bars
        initProgressBars(view);
        
        // Initialize control buttons
        initControlButtons(view);
        
        // Setup button click listeners
        setupButtonListeners();
        
        // Start demo animations
        startDemoAnimations();
        
        Log.d(TAG, "onViewCreated: Initialization complete");
    }
    
    private void initProgressBars(View view) {
        progressDefault = view.findViewById(R.id.progressDefault);
        progressLarge = view.findViewById(R.id.progressLarge);
        progressSmall = view.findViewById(R.id.progressSmall);
        progressAccent = view.findViewById(R.id.progressAccent);
        progressSuccess = view.findViewById(R.id.progressSuccess);
        progressLight = view.findViewById(R.id.progressLight);
        progressIndeterminate = view.findViewById(R.id.progressIndeterminate);
        progressCustom = view.findViewById(R.id.progressCustom);
        progressInteractive = view.findViewById(R.id.progressInteractive);
    }
    
    private void initControlButtons(View view) {
        Log.d(TAG, "initControlButtons: Starting button initialization");
        
        btnDecrease = view.findViewById(R.id.btnDecrease);
        Log.d(TAG, "btnDecrease found: " + (btnDecrease != null));
        
        btnIncrease = view.findViewById(R.id.btnIncrease);
        Log.d(TAG, "btnIncrease found: " + (btnIncrease != null));
        
        btnReset = view.findViewById(R.id.btnReset);
        Log.d(TAG, "btnReset found: " + (btnReset != null));
        
        btnComplete = view.findViewById(R.id.btnComplete);
        Log.d(TAG, "btnComplete found: " + (btnComplete != null));
        
        btnPulse = view.findViewById(R.id.btnPulse);
        Log.d(TAG, "btnPulse found: " + (btnPulse != null));
        
        btnBounce = view.findViewById(R.id.btnBounce);
        Log.d(TAG, "btnBounce found: " + (btnBounce != null));
        
        btnIndeterminate = view.findViewById(R.id.btnIndeterminate);
        Log.d(TAG, "btnIndeterminate found: " + (btnIndeterminate != null));
        
        // Check if progressInteractive is initialized for button operations
        Log.d(TAG, "progressInteractive found: " + (progressInteractive != null));
    }
    
    private void setupButtonListeners() {
        Log.d(TAG, "setupButtonListeners: Starting listener setup");
        
        // Progress control buttons
        if (btnDecrease != null) {
            Log.d(TAG, "Setting up btnDecrease listener");
            UiClickEffects.setOnClickWithPulse(btnDecrease, R.raw.ui_click_effect, v -> {
                Log.d(TAG, "btnDecrease clicked");
                try {
                    int currentProgress = progressInteractive.getProgress();
                    int newProgress = Math.max(0, currentProgress - 10);
                    Log.d(TAG, "Decreasing progress from " + currentProgress + " to " + newProgress);
                    progressInteractive.setProgressAnimated(newProgress);
                    
                    // Check for milestone effects
                    checkMilestoneEffects(newProgress);
                } catch (Exception e) {
                    Log.e(TAG, "Error in btnDecrease click handler", e);
                }
            });
        } else {
            Log.e(TAG, "btnDecrease is null, cannot set listener");
        }
        
        if (btnIncrease != null) {
            Log.d(TAG, "Setting up btnIncrease listener");
            UiClickEffects.setOnClickWithPulse(btnIncrease, R.raw.ui_click_effect, v -> {
                Log.d(TAG, "btnIncrease clicked");
                try {
                    int currentProgress = progressInteractive.getProgress();
                    int newProgress = Math.min(100, currentProgress + 10);
                    Log.d(TAG, "Increasing progress from " + currentProgress + " to " + newProgress);
                    progressInteractive.setProgressAnimated(newProgress);
                    
                    // Check for milestone effects
                    checkMilestoneEffects(newProgress);
                } catch (Exception e) {
                    Log.e(TAG, "Error in btnIncrease click handler", e);
                }
            });
        } else {
            Log.e(TAG, "btnIncrease is null, cannot set listener");
        }
        
        if (btnReset != null) {
            Log.d(TAG, "Setting up btnReset listener");
            UiClickEffects.setOnClickWithPulse(btnReset, R.raw.ui_click_effect, v -> {
                Log.d(TAG, "btnReset clicked");
                try {
                    progressInteractive.setProgressAnimated(0);
                    progressInteractive.setIndeterminate(false);
                    Log.d(TAG, "Progress reset to 0");
                } catch (Exception e) {
                    Log.e(TAG, "Error in btnReset click handler", e);
                }
            });
        } else {
            Log.e(TAG, "btnReset is null, cannot set listener");
        }
        
        if (btnComplete != null) {
            Log.d(TAG, "Setting up btnComplete listener");
            UiClickEffects.setOnClickWithPulse(btnComplete, R.raw.ui_click_effect, v -> {
                Log.d(TAG, "btnComplete clicked");
                try {
                    progressInteractive.setProgressAnimated(100);
                    progressInteractive.postDelayed(() -> {
                        UiProgressEffects.celebrateCompletion(progressInteractive);
                    }, 300);
                    Log.d(TAG, "Progress set to 100");
                } catch (Exception e) {
                    Log.e(TAG, "Error in btnComplete click handler", e);
                }
            });
        } else {
            Log.e(TAG, "btnComplete is null, cannot set listener");
        }
        
        // Effect buttons
        if (btnPulse != null) {
            Log.d(TAG, "Setting up btnPulse listener");
            UiClickEffects.setOnClickWithPulse(btnPulse, 0, v -> {
                Log.d(TAG, "btnPulse clicked");
                try {
                    UiProgressEffects.pulseOnComplete(progressInteractive);
                } catch (Exception e) {
                    Log.e(TAG, "Error in btnPulse click handler", e);
                }
            });
        } else {
            Log.e(TAG, "btnPulse is null, cannot set listener");
        }
        
        if (btnBounce != null) {
            Log.d(TAG, "Setting up btnBounce listener");
            UiClickEffects.setOnClickWithPulse(btnBounce, 0, v -> {
                Log.d(TAG, "btnBounce clicked");
                try {
                    UiProgressEffects.rubberBandEffect(progressInteractive);
                } catch (Exception e) {
                    Log.e(TAG, "Error in btnBounce click handler", e);
                }
            });
        } else {
            Log.e(TAG, "btnBounce is null, cannot set listener");
        }
        
        if (btnIndeterminate != null) {
            Log.d(TAG, "Setting up btnIndeterminate listener");
            UiClickEffects.setOnClickWithPulse(btnIndeterminate, R.raw.ui_click_effect, v -> {
                Log.d(TAG, "btnIndeterminate clicked");
                try {
                    boolean isIndeterminate = progressInteractive.isIndeterminate();
                    progressInteractive.setIndeterminate(!isIndeterminate);
                    
                    if (!isIndeterminate) {
                        UiProgressEffects.pulseIndeterminate(progressInteractive);
                        btnIndeterminate.setText("Stop");
                        Log.d(TAG, "Set to indeterminate mode");
                    } else {
                        UiProgressEffects.stopPulseIndeterminate(progressInteractive);
                        btnIndeterminate.setText("Loading");
                        Log.d(TAG, "Stopped indeterminate mode");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in btnIndeterminate click handler", e);
                }
            });
        } else {
            Log.e(TAG, "btnIndeterminate is null, cannot set listener");
        }
        
        Log.d(TAG, "setupButtonListeners: Listener setup complete");
    }
    
    private void checkMilestoneEffects(int progress) {
        // Apply effects at milestones
        if (progress == 25 || progress == 50 || progress == 75) {
            UiProgressEffects.bounceOnMilestone(progressInteractive, progress);
        } else if (progress == 100) {
            UiProgressEffects.celebrateCompletion(progressInteractive);
        }
    }
    
    private void startDemoAnimations() {
        // Animate the default progress bar
        progressDefault.postDelayed(() -> {
            progressDefault.setProgressAnimated(65);
        }, 500);
        
        // Animate the large progress bar
        progressLarge.postDelayed(() -> {
            progressLarge.setProgressAnimated(45);
        }, 700);
        
        // Animate the small progress bar
        progressSmall.postDelayed(() -> {
            progressSmall.setProgressAnimated(80);
        }, 900);
        
        // Animate the accent progress bar
        progressAccent.postDelayed(() -> {
            progressAccent.setProgressAnimated(30);
        }, 1100);
        
        // Celebrate the success progress bar
        progressSuccess.postDelayed(() -> {
            UiProgressEffects.celebrateCompletion(progressSuccess);
        }, 1300);
        
        // Animate the light progress bar
        progressLight.postDelayed(() -> {
            progressLight.setProgressAnimated(55);
        }, 1500);
        
        // Apply shimmer to custom progress bar
        progressCustom.postDelayed(() -> {
            progressCustom.setProgressAnimated(75);
            UiProgressEffects.shimmerEffect(progressCustom);
        }, 1700);
        
        // Apply pulse to indeterminate progress
        UiProgressEffects.pulseIndeterminate(progressIndeterminate);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Clean up any running animations
        if (progressIndeterminate != null) {
            UiProgressEffects.stopPulseIndeterminate(progressIndeterminate);
        }
        if (progressCustom != null) {
            UiProgressEffects.stopShimmer(progressCustom);
        }
    }
    
    /**
     * Example method showing how to use the progress bar in a real scenario
     */
    private void simulateFileDownload() {
        // Reset progress
        progressInteractive.setProgress(0);
        progressInteractive.setIndeterminate(false);
        
        // Simulate download progress
        Runnable downloadSimulation = new Runnable() {
            private int progress = 0;
            
            @Override
            public void run() {
                if (progress <= 100) {
                    progressInteractive.setProgressAnimated(progress);
                    
                    // Check for milestones
                    checkMilestoneEffects(progress);
                    
                    progress += 5;
                    progressInteractive.postDelayed(this, 200);
                }
            }
        };
        
        progressInteractive.postDelayed(downloadSimulation, 500);
    }
    
    /**
     * Example method showing indeterminate loading followed by determinate progress
     */
    private void simulateDataLoading() {
        // Start with indeterminate loading
        progressInteractive.setIndeterminate(true);
        UiProgressEffects.pulseIndeterminate(progressInteractive);
        
        // After 2 seconds, switch to determinate progress
        progressInteractive.postDelayed(() -> {
            UiProgressEffects.stopPulseIndeterminate(progressInteractive);
            progressInteractive.setIndeterminate(false);
            progressInteractive.setProgress(0);
            
            // Start simulated progress
            simulateFileDownload();
        }, 2000);
    }
    
    /**
     * Example of using progress bar with ViewModel and LiveData
     */
    private void setupWithViewModel() {
        // This would typically be in your Fragment/Activity
        // Example:
        /*
        viewModel.getDownloadProgress().observe(getViewLifecycleOwner(), progress -> {
            progressBar.setProgressAnimated(progress);
            
            if (progress == 100) {
                UiProgressEffects.celebrateCompletion(progressBar);
            } else if (progress % 25 == 0 && progress > 0) {
                UiProgressEffects.bounceOnMilestone(progressBar, progress);
            }
        });
        
        viewModel.getLoadingState().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setIndeterminate(isLoading);
            if (isLoading) {
                UiProgressEffects.fadeIn(progressBar, 300);
            } else {
                UiProgressEffects.fadeOut(progressBar, 300);
            }
        });
        */
    }
}