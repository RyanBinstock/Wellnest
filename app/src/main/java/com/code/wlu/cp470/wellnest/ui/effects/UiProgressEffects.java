package com.code.wlu.cp470.wellnest.ui.effects;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.code.wlu.cp470.wellnest.ui.components.WellnestProgressBar;

/**
 * Utility class for applying visual effects to WellnestProgressBar.
 * Provides various animations to enhance user experience when progress changes.
 */
public final class UiProgressEffects {
    
    // Tag keys for storing animators - using unique integer IDs
    private static final int TAG_SHIMMER_ANIMATOR = 0x7f0a0001;
    private static final int TAG_PULSE_ANIMATOR = 0x7f0a0002;
    
    private UiProgressEffects() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Apply a pulse effect when progress reaches 100%.
     * The progress bar will scale up and down to draw attention to completion.
     * 
     * @param progressBar The WellnestProgressBar to animate
     */
    public static void pulseOnComplete(View progressBar) {
        if (progressBar == null) return;
        
        progressBar.animate().cancel();
        
        PropertyValuesHolder pvhScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.05f, 0.98f, 1f);
        PropertyValuesHolder pvhScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.05f, 0.98f, 1f);
        
        ObjectAnimator pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                progressBar, pvhScaleX, pvhScaleY);
        pulseAnimator.setDuration(600);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.start();
    }
    
    /**
     * Apply a shimmer effect to the progress bar.
     * Creates a subtle glowing animation that runs continuously.
     * 
     * @param bar The WellnestProgressBar to apply shimmer to
     */
    public static void shimmerEffect(WellnestProgressBar bar) {
        if (bar == null) return;
        
        ValueAnimator shimmerAnimator = ValueAnimator.ofFloat(0.8f, 1.0f);
        shimmerAnimator.setDuration(1500);
        shimmerAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        shimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
        shimmerAnimator.setRepeatMode(ValueAnimator.REVERSE);
        
        shimmerAnimator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            bar.setAlpha(alpha);
        });
        
        shimmerAnimator.start();
        
        // Store the animator as a tag so it can be cancelled later
        bar.setTag(TAG_SHIMMER_ANIMATOR, shimmerAnimator);
    }
    
    /**
     * Stop the shimmer effect on a progress bar.
     * 
     * @param bar The WellnestProgressBar to stop shimmer on
     */
    public static void stopShimmer(WellnestProgressBar bar) {
        if (bar == null) return;
        
        Object animator = bar.getTag(TAG_SHIMMER_ANIMATOR);
        if (animator instanceof ValueAnimator) {
            ((ValueAnimator) animator).cancel();
            bar.setAlpha(1.0f);
            bar.setTag(TAG_SHIMMER_ANIMATOR, null);
        }
    }
    
    /**
     * Apply a bounce effect when reaching a milestone.
     * The progress bar will bounce to celebrate reaching specific progress points.
     * 
     * @param bar The WellnestProgressBar to animate
     * @param milestone The milestone value (e.g., 25, 50, 75, 100)
     */
    public static void bounceOnMilestone(WellnestProgressBar bar, int milestone) {
        if (bar == null) return;
        
        // Only bounce if we're at the milestone
        if (bar.getProgress() != milestone) return;
        
        bar.animate().cancel();
        
        // Create a bounce animation
        bar.setTranslationY(0);
        ObjectAnimator bounceAnimator = ObjectAnimator.ofFloat(bar, "translationY", 0, -20, 0);
        bounceAnimator.setDuration(500);
        bounceAnimator.setInterpolator(new BounceInterpolator());
        bounceAnimator.start();
    }
    
    /**
     * Apply a subtle scale effect when progress changes.
     * Makes the progress bar slightly scale up when value increases.
     * 
     * @param bar The WellnestProgressBar to animate
     */
    public static void scaleOnProgress(WellnestProgressBar bar) {
        if (bar == null) return;
        
        bar.animate().cancel();
        bar.animate()
                .scaleX(1.02f)
                .scaleY(1.02f)
                .setDuration(100)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> bar.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .setInterpolator(new DecelerateInterpolator())
                        .start())
                .start();
    }
    
    /**
     * Apply a celebration effect when reaching 100%.
     * Combines pulse and color animation for a festive completion.
     * 
     * @param bar The WellnestProgressBar to celebrate
     */
    public static void celebrateCompletion(WellnestProgressBar bar) {
        if (bar == null || bar.getProgress() != bar.getMax()) return;
        
        // Save original color
        int originalColor = 0xFF5B9BD5; // Default blue color
        int celebrationColor = 0xFF4CAF50; // Green success color
        
        // Pulse effect
        pulseOnComplete(bar);
        
        // Color animation
        ValueAnimator colorAnimator = ValueAnimator.ofArgb(originalColor, celebrationColor, originalColor);
        colorAnimator.setDuration(1000);
        colorAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        colorAnimator.addUpdateListener(animation -> {
            int color = (int) animation.getAnimatedValue();
            bar.setProgressColor(color);
        });
        colorAnimator.start();
    }
    
    /**
     * Apply a loading pulse effect for indeterminate progress.
     * Creates a rhythmic pulsing effect while loading.
     * 
     * @param bar The WellnestProgressBar to animate
     */
    public static void pulseIndeterminate(WellnestProgressBar bar) {
        if (bar == null || !bar.isIndeterminate()) return;
        
        ValueAnimator pulseAnimator = ValueAnimator.ofFloat(1f, 0.85f, 1f);
        pulseAnimator.setDuration(1200);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        
        pulseAnimator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            bar.setScaleY(scale);
        });
        
        pulseAnimator.start();
        
        // Store the animator as a tag
        bar.setTag(TAG_PULSE_ANIMATOR, pulseAnimator);
    }
    
    /**
     * Stop the pulse effect on indeterminate progress.
     * 
     * @param bar The WellnestProgressBar to stop pulsing
     */
    public static void stopPulseIndeterminate(WellnestProgressBar bar) {
        if (bar == null) return;
        
        Object animator = bar.getTag(TAG_PULSE_ANIMATOR);
        if (animator instanceof ValueAnimator) {
            ((ValueAnimator) animator).cancel();
            bar.setScaleY(1.0f);
            bar.setTag(TAG_PULSE_ANIMATOR, null);
        }
    }
    
    /**
     * Apply a rubber band effect when progress reaches max.
     * Creates a stretchy, elastic animation.
     * 
     * @param bar The WellnestProgressBar to animate
     */
    public static void rubberBandEffect(WellnestProgressBar bar) {
        if (bar == null) return;
        
        PropertyValuesHolder pvhScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.15f, 0.9f, 1.05f, 1f);
        PropertyValuesHolder pvhScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.9f, 1.1f, 0.95f, 1f);
        
        ObjectAnimator rubberBandAnimator = ObjectAnimator.ofPropertyValuesHolder(
                bar, pvhScaleX, pvhScaleY);
        rubberBandAnimator.setDuration(800);
        rubberBandAnimator.setInterpolator(new OvershootInterpolator(1.5f));
        rubberBandAnimator.start();
    }
    
    /**
     * Apply a fade-in effect when showing the progress bar.
     * 
     * @param bar The WellnestProgressBar to fade in
     * @param duration Duration of the fade animation in milliseconds
     */
    public static void fadeIn(WellnestProgressBar bar, long duration) {
        if (bar == null) return;
        
        bar.setAlpha(0f);
        bar.setVisibility(View.VISIBLE);
        bar.animate()
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(null)
                .start();
    }
    
    /**
     * Apply a fade-out effect when hiding the progress bar.
     * 
     * @param bar The WellnestProgressBar to fade out
     * @param duration Duration of the fade animation in milliseconds
     */
    public static void fadeOut(WellnestProgressBar bar, long duration) {
        if (bar == null) return;
        
        bar.animate()
                .alpha(0f)
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        bar.setVisibility(View.GONE);
                    }
                })
                .start();
    }
    
    /**
     * Apply a slide-in effect from the left when showing progress.
     * 
     * @param bar The WellnestProgressBar to slide in
     */
    public static void slideInFromLeft(WellnestProgressBar bar) {
        if (bar == null) return;
        
        bar.setTranslationX(-bar.getWidth());
        bar.setVisibility(View.VISIBLE);
        bar.animate()
                .translationX(0)
                .setDuration(400)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
    
    /**
     * Apply a slide-out effect to the right when hiding progress.
     * 
     * @param bar The WellnestProgressBar to slide out
     */
    public static void slideOutToRight(WellnestProgressBar bar) {
        if (bar == null) return;
        
        bar.animate()
                .translationX(bar.getWidth())
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        bar.setVisibility(View.GONE);
                        bar.setTranslationX(0);
                    }
                })
                .start();
    }
    
    /**
     * Create a progress wave effect.
     * Makes the progress bar appear to have a wave moving through it.
     * 
     * @param bar The WellnestProgressBar to animate
     */
    public static void waveEffect(WellnestProgressBar bar) {
        if (bar == null) return;
        
        ValueAnimator waveAnimator = ValueAnimator.ofFloat(0f, 1f);
        waveAnimator.setDuration(2000);
        waveAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        waveAnimator.setRepeatCount(3);
        
        waveAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            float scale = 1f + (0.05f * (float) Math.sin(value * Math.PI * 2));
            bar.setScaleY(scale);
        });
        
        waveAnimator.start();
    }
}