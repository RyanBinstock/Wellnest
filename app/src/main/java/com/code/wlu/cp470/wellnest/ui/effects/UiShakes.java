package com.code.wlu.cp470.wellnest.ui.effects;

import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.CycleInterpolator;

public final class UiShakes {
    private UiShakes() {
    }

    /**
     * Quick horizontal shake (e.g., form error).
     */
    public static void shakeX(View v) {
        ObjectAnimator a = ObjectAnimator.ofFloat(v, "translationX", 0, 12, -12, 8, -8, 4, -4, 0);
        a.setDuration(320);
        a.setInterpolator(new CycleInterpolator(1));
        a.start();
    }
}
