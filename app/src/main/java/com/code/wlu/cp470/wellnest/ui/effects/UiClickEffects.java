package com.code.wlu.cp470.wellnest.ui.effects;

import android.media.MediaPlayer;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

public final class UiClickEffects {
    private static final String TAG = "UiClickEffects";
    
    private UiClickEffects() {
    }

    /**
     * Wrap a click action with a short pulse animation + debounce.
     * <p>
     * set soundEffect to 0 for no sound effect
     */
    public static void setOnClickWithPulse(View v, int soundEffect, View.OnClickListener action) {
        Log.d(TAG, "setOnClickWithPulse: Setting up click listener for view: " + v.getClass().getSimpleName());
        
        if (v == null) {
            Log.e(TAG, "setOnClickWithPulse: View is null, cannot set listener");
            return;
        }
        
        if (action == null) {
            Log.e(TAG, "setOnClickWithPulse: Action is null, cannot set listener");
            return;
        }
        
        v.setOnClickListener(view -> {
            Log.d(TAG, "Click detected on: " + view.getClass().getSimpleName());
            
            if (!view.isEnabled()) {
                Log.w(TAG, "View is not enabled, ignoring click");
                return;
            }
            
            view.setEnabled(false);
            Log.d(TAG, "View disabled temporarily to prevent double-clicks");
            
            // Handle sound effect
            if (soundEffect != 0) {
                try {
                    Log.d(TAG, "Creating MediaPlayer for sound effect: " + soundEffect);
                    MediaPlayer mp = MediaPlayer.create(view.getContext(), soundEffect);
                    
                    if (mp != null) {
                        mp.start();
                        mp.setOnCompletionListener(player -> {
                            Log.d(TAG, "Sound effect completed, releasing MediaPlayer");
                            player.release();
                        });
                    } else {
                        Log.e(TAG, "Failed to create MediaPlayer for resource: " + soundEffect);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception while playing sound effect", e);
                }
            } else {
                Log.d(TAG, "No sound effect requested (soundEffect = 0)");
            }
            
            // Animate and execute action
            view.animate().cancel();
            Log.d(TAG, "Starting pulse animation");
            
            view.animate()
                    .scaleX(0.94f).scaleY(0.94f)
                    .setDuration(80)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        Log.d(TAG, "First animation phase complete, starting second phase");
                        view.animate()
                                .scaleX(1f).scaleY(1f)
                                .setDuration(130)
                                .setInterpolator(new DecelerateInterpolator())
                                .withEndAction(() -> {
                                    Log.d(TAG, "Animation complete, executing click action");
                                    try {
                                        action.onClick(view);
                                        Log.d(TAG, "Click action executed successfully");
                                    } catch (Exception e) {
                                        Log.e(TAG, "Exception in click action", e);
                                    } finally {
                                        view.setEnabled(true);
                                        Log.d(TAG, "View re-enabled");
                                    }
                                }).start();
                    }).start();
        });
        
        Log.d(TAG, "Click listener setup complete for: " + v.getClass().getSimpleName());
    }

    /**
     * Simple bounce-in on demand (e.g., after success).
     */
    public static void bounceOnce(View v) {
        v.animate().cancel();
        v.setScaleX(0.9f);
        v.setScaleY(0.9f);
        v.animate().scaleX(1f).scaleY(1f)
                .setDuration(160)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
}
