package com.code.wlu.cp470.wellnest.ui.welcome;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.ui.effects.UiClickEffects;
import com.code.wlu.cp470.wellnest.ui.effects.UiTextEffects;
import com.code.wlu.cp470.wellnest.ui.effects.UiTouchEffects;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

public class WelcomeFragment extends Fragment {

    private int current = 0;
    private List<Slide> slides;

    private ImageView carouselImage;
    private TextView carouselText;
    private LinearLayout dotsContainer;
    private View touchSurface; // large swipe area (carouselColumn)

    public WelcomeFragment() {
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Navigation.findNavController(requireView()).navigate(R.id.homeFragment);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Gradient title
        TextView gradientWellnest = view.findViewById(R.id.gradientWellnest);
        UiTextEffects.applyVerticalGradient(
                gradientWellnest,
                R.color.wl_welcome_gradient_start,
                R.color.wl_welcome_gradient_end);

        Button btnGetStarted = view.findViewById(R.id.btnGetStarted);
        if (btnGetStarted != null) {
            UiClickEffects.setOnClickWithPulse(btnGetStarted, v -> {
                var action = WelcomeFragmentDirections.actionWelcomeToAuth();
                action.setStartMode("signup");
                NavHostFragment.findNavController(this).navigate(action);
            });
        }

        UiTouchEffects.attachPressScale(btnGetStarted, 0.96f);

        // ------- Carousel wiring -------
        carouselImage = view.findViewById(R.id.carouselImage);
        carouselText = view.findViewById(R.id.carouselText);
        dotsContainer = view.findViewById(R.id.dotsContainer);
        // BIG swipe area = the whole carousel column (image + text + dots)
        touchSurface = view.findViewById(R.id.carouselColumn);

        // 4 slides (adjust drawables/strings to your assets)
        slides = Arrays.asList(
                new Slide(R.drawable.welcome_image_1, R.string.slide_1_text),
                new Slide(R.drawable.welcome_image_2, R.string.slide_2_text),
                new Slide(R.drawable.welcome_image_3, R.string.slide_3_text),
                new Slide(R.drawable.welcome_image_4, R.string.slide_4_text)
        );

        buildDots(slides.size());
        applySlide(0);

        // Drag-to-swipe
        setupDragSwipe();
    }

    // ---------- Carousel helpers ----------

    private void next() {
        current = (current + 1) % slides.size();
        applySlide(current);
    }

    private void prev() {
        current = (current - 1 + slides.size()) % slides.size();
        applySlide(current);
    }

    private void applySlide(int index) {
        Slide s = slides.get(index);
        carouselImage.setImageResource(s.imageRes);
        carouselText.setText(s.textRes);
        updateDots(index);
    }

    private void buildDots(int count) {
        dotsContainer.removeAllViews();
        final int sizeDp = 8;
        final int marginDp = 4;

        int sizePx = dp(sizeDp);
        int marginPx = dp(marginDp);

        for (int i = 0; i < count; i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
            lp.setMargins(marginPx, marginPx, marginPx, marginPx);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(R.drawable.dot_inactive);
            dotsContainer.addView(dot);
        }
    }

    private void updateDots(int activeIndex) {
        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            View dot = dotsContainer.getChildAt(i);
            dot.setBackgroundResource(i == activeIndex ? R.drawable.dot_active : R.drawable.dot_inactive);
        }
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    // ---------- Drag + Animated settle ----------

    @SuppressLint("ClickableViewAccessibility")
    private void setupDragSwipe() {
        final float[] downX = {0f};
        final boolean[] dragging = {false};

        touchSurface.setOnTouchListener((v, event) -> {
            float w = Math.max(1f, v.getWidth()); // avoid /0
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX[0] = event.getX();
                    dragging[0] = true;
                    // stop any running animations
                    carouselImage.animate().cancel();
                    carouselText.animate().cancel();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (!dragging[0]) return false;
                    float dx = event.getX() - downX[0];
                    // apply drag translation
                    carouselImage.setTranslationX(dx);
                    carouselText.setTranslationX(dx * 0.8f); // subtle parallax
                    float fade = 1f - Math.min(1f, Math.abs(dx) / w) * 0.3f;
                    carouselImage.setAlpha(fade);
                    carouselText.setAlpha(fade);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if (!dragging[0]) return false;
                    dragging[0] = false;

                    float dxUp = event.getX() - downX[0];
                    float threshold = w * 0.20f; // need ~20% drag to switch

                    if (Math.abs(dxUp) >= threshold) {
                        boolean toNext = dxUp < 0; // left swipe → next
                        animateOffAndSwap(toNext, w);
                    } else {
                        // not enough drag → spring back
                        springBack();
                    }
                    return true;
                }
            }
            return false;
        });
    }

    private void springBack() {
        ViewPropertyAnimator a1 = carouselImage.animate()
                .translationX(0f).alpha(1f)
                .setDuration(180)
                .setInterpolator(new DecelerateInterpolator());
        ViewPropertyAnimator a2 = carouselText.animate()
                .translationX(0f).alpha(1f)
                .setDuration(180)
                .setInterpolator(new DecelerateInterpolator());
        a1.start();
        a2.start();
    }

    private void animateOffAndSwap(boolean toNext, float width) {
        float off = toNext ? -width : width;

        // Animate current out
        carouselImage.animate()
                .translationX(off).alpha(0.6f)
                .setDuration(180)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        carouselText.animate()
                .translationX(off * 0.8f).alpha(0.6f)
                .setDuration(180)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    // update index + content
                    if (toNext) next();
                    else prev();

                    // place new content just off-screen on the other side
                    float startPos = -off;
                    carouselImage.setTranslationX(startPos);
                    carouselText.setTranslationX(startPos * 0.8f);
                    carouselImage.setAlpha(0.6f);
                    carouselText.setAlpha(0.6f);

                    // animate in
                    carouselImage.animate()
                            .translationX(0f).alpha(1f)
                            .setDuration(220)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                    carouselText.animate()
                            .translationX(0f).alpha(1f)
                            .setDuration(220)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                })
                .start();
    }

    // ---------- Model ----------
    private static class Slide {
        final int imageRes;
        final int textRes;

        Slide(int imageRes, int textRes) {
            this.imageRes = imageRes;
            this.textRes = textRes;
        }
    }
}
