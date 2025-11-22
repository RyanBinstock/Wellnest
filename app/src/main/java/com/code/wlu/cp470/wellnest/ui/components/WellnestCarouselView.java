package com.code.wlu.cp470.wellnest.ui.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.code.wlu.cp470.wellnest.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A custom FrameLayout that hosts a ViewPager2 with a specialized "stacked" carousel effect.
 * <p>
 * This component displays items in a horizontal carousel where the center item is prominent,
 * and side items are scaled down, faded, and optionally blurred. It supports auto-play
 * and various customization options via XML attributes.
 * </p>
 *
 * <h2>XML Usage</h2>
 * <pre>
 * <com.code.wlu.cp470.wellnest.ui.components.WellnestCarouselView
 *     android:id="@+id/carouselView"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:carousel_page_margin="-40dp"
 *     app:carousel_offscreen_limit="4"
 *     app:carousel_side_scale="0.85"
 *     app:carousel_side_alpha="0.7"
 *     app:carousel_blur_enabled="true"
 *     app:carousel_auto_play="false" />
 * </pre>
 *
 * <h2>Java Usage</h2>
 * <pre>
 * WellnestCarouselView carousel = findViewById(R.id.carouselView);
 * MyAdapter adapter = new MyAdapter(dataList);
 * carousel.setAdapter(adapter);
 * </pre>
 *
 * <h2>Adapter Requirements</h2>
 * <p>
 * <strong>CRITICAL:</strong> The root view of the item layout inflated by your adapter
 * MUST use <code>match_parent</code> for both width and height. If <code>wrap_content</code>
 * is used, the ViewPager2 may crash or layout incorrectly due to the transformation calculations.
 * </p>
 *
 * <h2>Customization Attributes</h2>
 * <ul>
 *     <li>{@code carousel_page_margin}: Controls the overlap between cards. Negative values create overlap. Default: -40dp.</li>
 *     <li>{@code carousel_offscreen_limit}: Number of pages to keep in memory on either side. Default: 4.</li>
 *     <li>{@code carousel_side_scale}: Scale factor for side items (0.0 - 1.0). Default: 0.85.</li>
 *     <li>{@code carousel_side_alpha}: Alpha/Opacity for side items (0.0 - 1.0). Default: 0.7.</li>
 *     <li>{@code carousel_blur_enabled}: Applies a blur effect to side items on Android 12+. Default: true.</li>
 *     <li>{@code carousel_auto_play}: Enables automatic scrolling. Default: false.</li>
 *     <li>{@code carousel_auto_play_delay}: Delay in ms for auto-play. Default: 3000ms.</li>
 *     <li>{@code carousel_effect}: Visual effect style (depth, stack, fade). Currently defaults to stack logic.</li>
 * </ul>
 */
public class WellnestCarouselView extends FrameLayout {

    private ViewPager2 viewPager;
    private int pageMargin;
    private int offscreenLimit;
    private float sideScale;
    private float sideAlpha;
    private boolean blurEnabled;
    private boolean autoPlay;
    private int autoPlayDelay;
    private int effectType; // 0: depth, 1: stack, 2: fade

    private Timer timer;
    private int currentPage = 0;
    private Handler handler = new Handler(Looper.getMainLooper());

    public WellnestCarouselView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public WellnestCarouselView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public WellnestCarouselView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.view_wellnest_carousel, this, true);
        viewPager = findViewById(R.id.viewPager);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WellnestCarouselView);
            // Default page margin to -40dp equivalent if not set, to ensure overlap
            int defaultMargin = (int) (context.getResources().getDisplayMetrics().density * -40);
            pageMargin = a.getDimensionPixelSize(R.styleable.WellnestCarouselView_carousel_page_margin, defaultMargin);
            offscreenLimit = a.getInt(R.styleable.WellnestCarouselView_carousel_offscreen_limit, 4);
            sideScale = a.getFloat(R.styleable.WellnestCarouselView_carousel_side_scale, 0.85f);
            sideAlpha = a.getFloat(R.styleable.WellnestCarouselView_carousel_side_alpha, 0.7f);
            blurEnabled = a.getBoolean(R.styleable.WellnestCarouselView_carousel_blur_enabled, true);
            autoPlay = a.getBoolean(R.styleable.WellnestCarouselView_carousel_auto_play, false);
            autoPlayDelay = a.getInt(R.styleable.WellnestCarouselView_carousel_auto_play_delay, 3000);
            effectType = a.getInt(R.styleable.WellnestCarouselView_carousel_effect, 0);
            a.recycle();
        } else {
            // Defaults
            pageMargin = (int) (context.getResources().getDisplayMetrics().density * -40);
            offscreenLimit = 4;
            sideScale = 0.85f;
            sideAlpha = 0.7f;
            blurEnabled = true;
            autoPlayDelay = 3000;
        }

        setupViewPager();
    }

    private void setupViewPager() {
        viewPager.setOffscreenPageLimit(offscreenLimit);

        // Remove default clipping to allow side items to be visible
        this.setClipChildren(false);
        this.setClipToPadding(false);
        viewPager.setClipToPadding(false);
        viewPager.setClipChildren(false);
        // We need to set clipChildren false on the internal RecyclerView as well
        View child = viewPager.getChildAt(0);
        if (child instanceof RecyclerView) {
            ((ViewGroup) child).setClipChildren(false);
            child.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }

        // We handle margin/translation inside CarouselPageTransformer now to support overlap + scaling
        viewPager.setPageTransformer(new CarouselPageTransformer());

        if (autoPlay) {
            startAutoPlay();
        }
    }

    /**
     * Sets the adapter for the internal ViewPager2.
     *
     * @param adapter The RecyclerView.Adapter to use. Ensure item views use match_parent.
     */
    public void setAdapter(RecyclerView.Adapter adapter) {
        viewPager.setAdapter(adapter);
    }

    /**
     * Sets the number of pages that should be retained to either side of the current page.
     * Higher limits result in smoother scrolling but consume more memory.
     *
     * @param limit The number of pages to keep offscreen.
     */
    public void setOffscreenPageLimit(int limit) {
        this.offscreenLimit = limit;
        viewPager.setOffscreenPageLimit(limit);
    }

    /**
     * Returns the internal ViewPager2 instance for advanced configuration.
     *
     * @return The underlying ViewPager2.
     */
    public ViewPager2 getViewPager() {
        return viewPager;
    }

    private void startAutoPlay() {
        if (timer != null) return;
        final Runnable update = () -> {
            if (viewPager.getAdapter() != null) {
                int count = viewPager.getAdapter().getItemCount();
                if (count > 0) {
                    currentPage = (viewPager.getCurrentItem() + 1) % count;
                    viewPager.setCurrentItem(currentPage, true);
                }
            }
        };

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(update);
            }
        }, autoPlayDelay, autoPlayDelay);
    }

    /**
     * Stops the auto-play timer if it is running.
     * Call this in onPause or onDestroy to prevent memory leaks.
     */
    public void stopAutoPlay() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAutoPlay();
    }

    /**
     * A PageTransformer that applies scaling, alpha fading, Z-translation, and X-translation
     * to create a 3D stacked carousel effect.
     */
    private class CarouselPageTransformer implements ViewPager2.PageTransformer {
        @Override
        public void transformPage(@NonNull View page, float position) {
            // Calculate absolute position to determine distance from center
            // position is 0 for center, -1 for left, 1 for right, etc.
            float absPos = Math.abs(position);

            // --- SCALING ---
            // Linearly interpolate scale between 1.0 (center) and sideScale (at position +/- 1)
            // Formula: 1 - (distance * (1 - minScale))
            float scale = 1f - (absPos * (1f - sideScale));
            // Ensure we don't scale below the minimum side scale
            scale = Math.max(sideScale, scale);

            page.setScaleY(scale);
            page.setScaleX(scale);

            // --- ALPHA FADE ---
            // Similar linear interpolation for opacity
            float alpha = 1f - (absPos * (1f - sideAlpha));
            alpha = Math.max(sideAlpha, alpha);
            page.setAlpha(alpha);

            // --- Z-INDEX ORDERING ---
            // Ensure the center item is always on top of side items.
            // Negative translationZ pushes side items "back" into the screen.
            page.setTranslationZ(-absPos);

            // --- HORIZONTAL TRANSLATION (STACKING) ---
            // This creates the "stack of cards" look.
            // Without this, items would just sit next to each other (or with standard margin).
            // We pull side items inward towards the center.
            float width = page.getWidth();
            // overlapFactor determines how much of the side card is hidden behind the center card.
            // 0.70f means 70% of the card width is overlapped.
            float overlapFactor = 0.70f;
            float translationX = -position * (width * overlapFactor);
            
            page.setTranslationX(translationX);

            // --- BLUR EFFECT ---
            if (blurEnabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (absPos > 0.01f) { // Apply blur if not center
                        float radius = absPos * 15f; // Dynamic blur radius (increased for stronger effect)
                        radius = Math.min(radius, 30f); // Cap radius
                        page.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));
                    } else {
                        page.setRenderEffect(null);
                    }
                }
                // For lower APIs, the alpha dimming (already applied above) serves as the "blur" substitute visually.
            }
        }
    }
}