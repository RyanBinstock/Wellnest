package com.code.wlu.cp470.wellnest.ui.components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.code.wlu.cp470.wellnest.R;

/**
 * Custom progress bar component for Wellnest app with smooth animations,
 * accessibility features, and both determinate and indeterminate modes.
 */
public class WellnestProgressBar extends View {
    
    // Constants
    private static final int DEFAULT_ANIMATION_DURATION = 300;
    private static final int INDETERMINATE_ANIMATION_DURATION = 1500;
    private static final float DEFAULT_CORNER_RADIUS_DP = 50f;
    private static final float DEFAULT_HEIGHT_DP = 28f;
    
    // Progress properties
    private int progress = 0;
    private int max = 100;
    private boolean isIndeterminate = false;
    
    // Animation
    private ValueAnimator progressAnimator;
    private ValueAnimator indeterminateAnimator;
    private float animatedProgress = 0f;
    private float indeterminateOffset = 0f;
    private int animationDuration = DEFAULT_ANIMATION_DURATION;
    
    // Drawing properties
    private Paint trackPaint;
    private Paint progressPaint;
    private Paint shadowPaint;
    private RectF trackRect;
    private RectF progressRect;
    
    // Colors (matching the design image)
    @ColorInt private int trackColor = 0xFFB3E5FC; // Light blue/cyan
    @ColorInt private int progressColor = 0xFF5B9BD5; // Darker blue
    @ColorInt private int shadowColor = 0x20000000; // Subtle shadow
    
    // Dimensions
    private float cornerRadius;
    private float progressHeight;
    
    // Accessibility
    private int lastAnnouncedProgress = -1;
    private static final int ACCESSIBILITY_ANNOUNCE_THRESHOLD = 10;
    
    public WellnestProgressBar(Context context) {
        super(context);
        init(context, null);
    }
    
    public WellnestProgressBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }
    
    public WellnestProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }
    
    private void init(Context context, AttributeSet attrs) {
        // Set layer type for hardware acceleration
        setLayerType(LAYER_TYPE_HARDWARE, null);
        
        // Default dimensions
        float density = context.getResources().getDisplayMetrics().density;
        cornerRadius = DEFAULT_CORNER_RADIUS_DP * density;
        progressHeight = DEFAULT_HEIGHT_DP * density;
        
        // Load custom attributes if available
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WellnestProgressBar);
            try {
                progress = a.getInteger(R.styleable.WellnestProgressBar_wl_progress, 0);
                max = a.getInteger(R.styleable.WellnestProgressBar_wl_max, 100);
                isIndeterminate = a.getBoolean(R.styleable.WellnestProgressBar_wl_indeterminate, false);
                progressColor = a.getColor(R.styleable.WellnestProgressBar_wl_progressColor, progressColor);
                trackColor = a.getColor(R.styleable.WellnestProgressBar_wl_trackColor, trackColor);
                cornerRadius = a.getDimension(R.styleable.WellnestProgressBar_wl_cornerRadius, cornerRadius);
                progressHeight = a.getDimension(R.styleable.WellnestProgressBar_wl_progressHeight, progressHeight);
                animationDuration = a.getInteger(R.styleable.WellnestProgressBar_wl_animationDuration, DEFAULT_ANIMATION_DURATION);
            } finally {
                a.recycle();
            }
        }
        
        // Initialize paints
        initPaints();
        
        // Initialize rectangles
        trackRect = new RectF();
        progressRect = new RectF();
        
        // Setup accessibility
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setContentDescription(getProgressDescription());
        
        // Start indeterminate animation if needed
        if (isIndeterminate) {
            startIndeterminateAnimation();
        }
    }
    
    private void initPaints() {
        // Track paint
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setColor(trackColor);
        trackPaint.setStyle(Paint.Style.FILL);
        
        // Progress paint
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(progressColor);
        progressPaint.setStyle(Paint.Style.FILL);
        
        // Shadow paint
        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(shadowColor);
        shadowPaint.setStyle(Paint.Style.FILL);
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = Math.round(progressHeight);
        
        // Add padding for shadow
        height += getPaddingTop() + getPaddingBottom() + 4; // 4dp for shadow
        
        setMeasuredDimension(width, height);
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateRects();
    }
    
    private void updateRects() {
        float shadowOffset = 2;
        float left = getPaddingLeft();
        float top = getPaddingTop();
        float right = getWidth() - getPaddingRight();
        float bottom = getHeight() - getPaddingBottom() - shadowOffset;
        
        trackRect.set(left, top, right, bottom);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw shadow for elevation effect
        canvas.save();
        canvas.translate(0, 2);
        canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, shadowPaint);
        canvas.restore();
        
        // Draw track
        canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackPaint);
        
        // Draw progress
        if (isIndeterminate) {
            drawIndeterminateProgress(canvas);
        } else {
            drawDeterminateProgress(canvas);
        }
    }
    
    private void drawDeterminateProgress(Canvas canvas) {
        if (animatedProgress <= 0) return;
        
        float progressWidth = (trackRect.width() * animatedProgress) / max;
        progressRect.set(
            trackRect.left,
            trackRect.top,
            trackRect.left + progressWidth,
            trackRect.bottom
        );
        
        canvas.drawRoundRect(progressRect, cornerRadius, cornerRadius, progressPaint);
    }
    
    private void drawIndeterminateProgress(Canvas canvas) {
        float trackWidth = trackRect.width();
        float progressWidth = trackWidth * 0.3f; // 30% of track width for indeterminate bar
        
        float left = trackRect.left + (indeterminateOffset * trackWidth);
        
        // Handle wrapping
        if (left + progressWidth > trackRect.right) {
            // Draw first part
            progressRect.set(left, trackRect.top, trackRect.right, trackRect.bottom);
            canvas.drawRoundRect(progressRect, cornerRadius, cornerRadius, progressPaint);
            
            // Draw wrapped part
            float wrappedWidth = (left + progressWidth) - trackRect.right;
            progressRect.set(trackRect.left, trackRect.top, trackRect.left + wrappedWidth, trackRect.bottom);
            canvas.drawRoundRect(progressRect, cornerRadius, cornerRadius, progressPaint);
        } else {
            progressRect.set(left, trackRect.top, left + progressWidth, trackRect.bottom);
            canvas.drawRoundRect(progressRect, cornerRadius, cornerRadius, progressPaint);
        }
    }
    
    // Public methods
    
    public void setProgress(int progress) {
        setProgress(progress, false);
    }
    
    public void setProgressAnimated(int progress) {
        setProgress(progress, true);
    }
    
    private void setProgress(int progress, boolean animate) {
        if (isIndeterminate) {
            setIndeterminate(false);
        }
        
        progress = Math.max(0, Math.min(progress, max));
        
        if (this.progress != progress) {
            int oldProgress = this.progress;
            this.progress = progress;
            
            if (animate) {
                animateProgressChange(oldProgress, progress);
            } else {
                animatedProgress = progress;
                invalidate();
            }
            
            // Accessibility announcement
            announceProgressChange(oldProgress, progress);
            
            // Update content description
            setContentDescription(getProgressDescription());
        }
    }
    
    private void animateProgressChange(float from, float to) {
        if (progressAnimator != null && progressAnimator.isRunning()) {
            progressAnimator.cancel();
        }
        
        progressAnimator = ValueAnimator.ofFloat(from, to);
        progressAnimator.setDuration(animationDuration);
        progressAnimator.setInterpolator(new FastOutSlowInInterpolator());
        progressAnimator.addUpdateListener(animation -> {
            animatedProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        progressAnimator.start();
    }
    
    public void setIndeterminate(boolean indeterminate) {
        if (this.isIndeterminate != indeterminate) {
            this.isIndeterminate = indeterminate;
            
            if (indeterminate) {
                startIndeterminateAnimation();
            } else {
                stopIndeterminateAnimation();
                animatedProgress = progress;
            }
            
            setContentDescription(getProgressDescription());
            invalidate();
        }
    }
    
    private void startIndeterminateAnimation() {
        if (indeterminateAnimator != null && indeterminateAnimator.isRunning()) {
            return;
        }
        
        indeterminateAnimator = ValueAnimator.ofFloat(0f, 1f);
        indeterminateAnimator.setDuration(INDETERMINATE_ANIMATION_DURATION);
        indeterminateAnimator.setInterpolator(new FastOutSlowInInterpolator());
        indeterminateAnimator.setRepeatCount(ValueAnimator.INFINITE);
        indeterminateAnimator.setRepeatMode(ValueAnimator.RESTART);
        indeterminateAnimator.addUpdateListener(animation -> {
            indeterminateOffset = (float) animation.getAnimatedValue();
            invalidate();
        });
        indeterminateAnimator.start();
    }
    
    private void stopIndeterminateAnimation() {
        if (indeterminateAnimator != null) {
            indeterminateAnimator.cancel();
            indeterminateAnimator = null;
        }
        indeterminateOffset = 0f;
    }
    
    public void setMax(int max) {
        if (this.max != max && max > 0) {
            this.max = max;
            
            // Adjust progress if needed
            if (progress > max) {
                setProgress(max);
            }
            
            invalidate();
            setContentDescription(getProgressDescription());
        }
    }
    
    public void setProgressColor(@ColorInt int color) {
        if (this.progressColor != color) {
            this.progressColor = color;
            progressPaint.setColor(color);
            invalidate();
        }
    }
    
    public void setTrackColor(@ColorInt int color) {
        if (this.trackColor != color) {
            this.trackColor = color;
            trackPaint.setColor(color);
            invalidate();
        }
    }
    
    public void setCornerRadius(float radius) {
        if (this.cornerRadius != radius) {
            this.cornerRadius = radius;
            invalidate();
        }
    }
    
    public void setAnimationDuration(int duration) {
        this.animationDuration = duration;
    }
    
    // Getters
    
    public int getProgress() {
        return progress;
    }
    
    public int getMax() {
        return max;
    }
    
    public boolean isIndeterminate() {
        return isIndeterminate;
    }
    
    // Accessibility methods
    
    private void announceProgressChange(int oldProgress, int newProgress) {
        // Only announce significant changes
        int oldPercentage = (oldProgress * 100) / max;
        int newPercentage = (newProgress * 100) / max;
        
        if (Math.abs(newPercentage - oldPercentage) >= ACCESSIBILITY_ANNOUNCE_THRESHOLD ||
            newPercentage == 0 || newPercentage == 100) {
            
            if (newPercentage != lastAnnouncedProgress) {
                lastAnnouncedProgress = newPercentage;
                announceForAccessibility(getProgressDescription());
            }
        }
    }
    
    private String getProgressDescription() {
        if (isIndeterminate) {
            return "Loading in progress";
        } else {
            int percentage = (progress * 100) / max;
            if (percentage == 100) {
                return "Loading complete";
            } else {
                return "Loading " + percentage + " percent complete";
            }
        }
    }
    
    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(WellnestProgressBar.class.getName());
    }
    
    // Lifecycle methods
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isIndeterminate) {
            startIndeterminateAnimation();
        }
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        
        // Clean up animators
        if (progressAnimator != null) {
            progressAnimator.cancel();
            progressAnimator = null;
        }
        
        stopIndeterminateAnimation();
    }
}