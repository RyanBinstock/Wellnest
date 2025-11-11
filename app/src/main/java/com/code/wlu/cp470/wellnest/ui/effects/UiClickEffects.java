package com.code.wlu.cp470.wellnest.ui.effects;

import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

public final class UiClickEffects {
    private UiClickEffects() {
    }

    /**
     * Wrap a click action with a short pulse animation + debounce.
     */
    public static void setOnClickWithPulse(View v, View.OnClickListener action) {
        v.setOnClickListener(view -> {
            if (!view.isEnabled()) return;
            view.setEnabled(false);
            view.animate().cancel();
            view.animate()
                    .scaleX(0.94f).scaleY(0.94f)
                    .setDuration(80)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> view.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(130)
                            .setInterpolator(new DecelerateInterpolator())
                            .withEndAction(() -> {
                                try {
                                    action.onClick(view);
                                } finally {
                                    view.setEnabled(true);
                                }
                            }).start()
                    ).start();
        });
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
