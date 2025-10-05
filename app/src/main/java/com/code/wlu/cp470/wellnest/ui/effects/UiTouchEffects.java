package com.code.wlu.cp470.wellnest.ui.effects;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

public final class UiTouchEffects {
    private UiTouchEffects() {}

    /** Adds press-in/out scaling that plays on ACTION_DOWN/UP; returns the listener so you can remove it. */
    @SuppressLint("ClickableViewAccessibility")
    public static View.OnTouchListener attachPressScale(View v, float pressedScale) {
        View.OnTouchListener l = (view, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().cancel();
                    view.animate().scaleX(pressedScale).scaleY(pressedScale)
                            .setDuration(80).setInterpolator(new AccelerateDecelerateInterpolator()).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f)
                            .setDuration(120).setInterpolator(new DecelerateInterpolator()).start();
                    break;
            }
            return false; // let clicks continue
        };
        v.setOnTouchListener(l);
        return l;
    }
}
