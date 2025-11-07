package com.code.wlu.cp470.wellnest.ui.effects;

import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;

/**
 * Utility class for applying gradient and shader effects to TextViews.
 * No need to rewrite gradient setup code in each fragment or activity.
 */
public final class UiTextEffects {

    private UiTextEffects() { /* static-only class */ }

    /**
     * Applies a vertical gradient (top → bottom) to a single TextView.
     *
     * @param tv            The TextView to apply the gradient to.
     * @param startColorRes Resource ID of the start color (top).
     * @param endColorRes   Resource ID of the end color (bottom).
     */
    public static void applyVerticalGradient(TextView tv,
                                             @ColorRes int startColorRes,
                                             @ColorRes int endColorRes) {
        if (tv == null) return;

        Context context = tv.getContext();
        int startColor = ContextCompat.getColor(context, startColorRes);
        int endColor = ContextCompat.getColor(context, endColorRes);

        Shader shader = new LinearGradient(
                0f, 0f, 0f, tv.getTextSize(),
                new int[]{startColor, endColor},
                null,
                Shader.TileMode.CLAMP
        );

        tv.getPaint().setShader(shader);
        tv.invalidate(); // ensure it redraws
    }

    /**
     * Applies a horizontal gradient (left → right) to a TextView.
     */
    public static void applyHorizontalGradient(TextView tv,
                                               @ColorRes int startColorRes,
                                               @ColorRes int endColorRes) {
        if (tv == null) return;

        Context context = tv.getContext();
        int startColor = ContextCompat.getColor(context, startColorRes);
        int endColor = ContextCompat.getColor(context, endColorRes);

        float textWidth = tv.getPaint().measureText(tv.getText().toString());
        Shader shader = new LinearGradient(
                0f, 0f, textWidth, 0f,
                new int[]{startColor, endColor},
                null,
                Shader.TileMode.CLAMP
        );

        tv.getPaint().setShader(shader);
        tv.invalidate();
    }

    /**
     * Applies a diagonal gradient (top-left → bottom-right) to a TextView.
     */
    public static void applyDiagonalGradient(TextView tv,
                                             @ColorRes int startColorRes,
                                             @ColorRes int endColorRes) {
        if (tv == null) return;

        Context context = tv.getContext();
        int startColor = ContextCompat.getColor(context, startColorRes);
        int endColor = ContextCompat.getColor(context, endColorRes);

        float textWidth = tv.getPaint().measureText(tv.getText().toString());
        float textHeight = tv.getTextSize();

        Shader shader = new LinearGradient(
                0f, 0f, textWidth, textHeight,
                new int[]{startColor, endColor},
                null,
                Shader.TileMode.CLAMP
        );

        tv.getPaint().setShader(shader);
        tv.invalidate();
    }

    /**
     * Clears any shader (removes the gradient) and restores normal text color rendering.
     */
    public static void clearGradient(TextView tv) {
        if (tv == null) return;
        tv.getPaint().setShader(null);
        tv.invalidate();
    }
}
