package com.code.wlu.cp470.wellnest.ui.snaptask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.data.WellnestAiClient;
import com.code.wlu.cp470.wellnest.ui.components.WellnestProgressBar;
import com.code.wlu.cp470.wellnest.ui.effects.UiClickEffects;
import com.code.wlu.cp470.wellnest.ui.effects.UiProgressEffects;
import com.code.wlu.cp470.wellnest.viewmodel.SnapTaskViewModel;

import java.io.ByteArrayOutputStream;

public class SnapTaskDetailActivity extends AppCompatActivity {

    // Intent extras (aligned with previous Fragment argument keys)
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_TASK_UID = "taskUid";
    public static final String EXTRA_TASK_NAME = "taskName";
    public static final String EXTRA_TASK_DESCRIPTION = "taskDescription";
    public static final String EXTRA_TASK_POINTS = "taskPoints";
    public static final String EXTRA_TASK_COMPLETED = "taskCompleted";
    private static final String TAG = "SnapTaskDetailActivity";
    private static final String ACTIVE_COLOUR = "#4A74DF";
    private static final String INACTIVE_COLOUR = "#CBD1E0";
    private static final String ACTIVE_TEXT_COLOUR = "#FFFFFF";
    private static final String INACTIVE_TEXT_COLOUR = "#494949";

    // Data/state
    private SnapTaskViewModel snapTaskViewModel;
    private String mode; // "before" | "after"
    private String taskUid;
    private String taskName;
    private String taskDescription;
    private int taskPoints;
    private boolean taskCompleted;
    private byte[] beforeImage, afterImage;

    // UI elements
    private TextView taskNameView, beforeText, beforeInnerText, afterInnerText, afterText, primaryButtonText;
    private ImageView heroImage;
    private CardView exitButton, infoButton, primaryButton, beforeCard, afterCard;
    private View loadingOverlay;
    private WellnestProgressBar loadingProgressBar;

    public static Intent createIntent(Context context,
                                      String mode,
                                      String taskUid,
                                      String taskName,
                                      String taskDescription,
                                      int taskPoints,
                                      boolean taskCompleted) {
        Intent i = new Intent(context, SnapTaskDetailActivity.class);
        i.putExtra(EXTRA_MODE, mode);
        i.putExtra(EXTRA_TASK_UID, taskUid);
        i.putExtra(EXTRA_TASK_NAME, taskName);
        i.putExtra(EXTRA_TASK_DESCRIPTION, taskDescription);
        i.putExtra(EXTRA_TASK_POINTS, taskPoints);
        i.putExtra(EXTRA_TASK_COMPLETED, taskCompleted);
        return i;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Hide navigation bar and allow content behind the status bar
        View decorView = getWindow().getDecorView();

        // Reuse existing detail Fragment layout as Activity content
        setContentView(R.layout.fragment_snap_task_detail);

        // Ensure layout extends behind system bars; do NOT use FULLSCREEN so status icons remain visible
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );


        // Enter animation
        try {
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } catch (Exception ignored) {
        }

        initializeViewModel();
        if (!extractExtras(getIntent())) {
            setResult(Activity.RESULT_CANCELED);
            finishWithExitAnim();
            return;
        }

        bindViews();
        wireUi();
    }

    private void initializeViewModel() {
        snapTaskViewModel = new ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())
        ).get(SnapTaskViewModel.class);
    }

    private boolean extractExtras(Intent intent) {
        if (intent == null) return false;
        mode = intent.getStringExtra(EXTRA_MODE);
        if (mode == null) mode = "before";
        taskUid = intent.getStringExtra(EXTRA_TASK_UID);
        taskName = intent.getStringExtra(EXTRA_TASK_NAME);
        taskDescription = intent.getStringExtra(EXTRA_TASK_DESCRIPTION);
        taskPoints = intent.getIntExtra(EXTRA_TASK_POINTS, 0);
        taskCompleted = intent.getBooleanExtra(EXTRA_TASK_COMPLETED, false);
        return taskUid != null && taskName != null && taskDescription != null;
    }

    private void bindViews() {
        heroImage = findViewById(R.id.snap_task_detail_hero);
        taskNameView = findViewById(R.id.snap_task_detail_top_name);
        beforeInnerText = findViewById(R.id.before_text);
        afterInnerText = findViewById(R.id.after_text);
        beforeText = findViewById(R.id.acceptance_criteria_text_before);
        afterText = findViewById(R.id.acceptance_criteria_text_after);
        beforeCard = findViewById(R.id.snap_task_cardview_before);
        afterCard = findViewById(R.id.snap_task_cardview_after);
        exitButton = findViewById(R.id.snap_task_detail_exit);
        infoButton = findViewById(R.id.snap_task_detail_info);
        primaryButton = findViewById(R.id.snap_task_primary_button_card);
        primaryButtonText = findViewById(R.id.snap_task_primary_button_text);
        loadingOverlay = findViewById(R.id.snap_task_loading_overlay);
        loadingProgressBar = findViewById(R.id.snap_task_loading_progress);
    }

    private void wireUi() {
        // Task name
        taskNameView.setText(taskName);

        // Split description by literal '\n' delimiter (not actual newline)
        String[] descriptionParts = taskDescription.split("\\\\n");
        String description = "";
        String beforeCriteria = "";
        String afterCriteria = "";

        if (descriptionParts.length >= 1 && descriptionParts[0] != null) {
            description = descriptionParts[0].trim();
        }
        if (descriptionParts.length >= 2 && descriptionParts[1] != null) {
            beforeCriteria = descriptionParts[1].trim();
        }
        if (descriptionParts.length >= 3 && descriptionParts[2] != null) {
            afterCriteria = descriptionParts[2].trim();
        }

        beforeText.setText(beforeCriteria.isEmpty() ? "No before criteria specified" : beforeCriteria);
        afterText.setText(afterCriteria.isEmpty() ? "No after criteria specified" : afterCriteria);

        updateUIForMode();

        final String finalDescription = description;
        UiClickEffects.setOnClickWithPulse(infoButton, R.raw.ui_click_effect, v -> {
            Log.d(TAG, "Info button clicked");
            showInfoDialog(finalDescription);
        });

        UiClickEffects.setOnClickWithPulse(exitButton, R.raw.ui_click_effect, v -> {
            Log.d(TAG, "Exit button clicked");
            setResult(Activity.RESULT_CANCELED);
            finishWithExitAnim();
        });

        UiClickEffects.setOnClickWithPulse(primaryButton, R.raw.ui_click_effect, v -> {
            Log.d(TAG, "Primary button clicked - mode: " + mode);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if ("before".equals(mode)) {
                startActivityForResult(intent, 1001);
            } else {
                startActivityForResult(intent, 1002);
            }
        });
    }

    private void updateUIForMode() {
        if ("before".equals(mode)) {
            beforeCard.setCardBackgroundColor(Color.parseColor(ACTIVE_COLOUR));
            afterCard.setCardBackgroundColor(Color.parseColor(INACTIVE_COLOUR));
            primaryButtonText.setText(R.string.start_task);
            heroImage.setImageResource(R.drawable.snap_task_detail_hero_1);
            beforeInnerText.setTextColor(Color.parseColor(ACTIVE_TEXT_COLOUR));
            afterInnerText.setTextColor(Color.parseColor(INACTIVE_TEXT_COLOUR));
        } else {
            afterCard.setCardBackgroundColor(Color.parseColor(ACTIVE_COLOUR));
            beforeCard.setCardBackgroundColor(Color.parseColor(INACTIVE_COLOUR));
            primaryButtonText.setText(R.string.end_task);
            heroImage.setImageResource(R.drawable.snap_task_detail_hero_2);
            beforeInnerText.setTextColor(Color.parseColor(INACTIVE_TEXT_COLOUR));
            afterInnerText.setTextColor(Color.parseColor(ACTIVE_TEXT_COLOUR));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap photo = (Bitmap) extras.get("data");
                if (photo != null) {
                    photo = downscale(photo, 480);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    photo.compress(Bitmap.CompressFormat.JPEG, 70, stream);

                    if (requestCode == 1001) {
                        this.beforeImage = stream.toByteArray();
                        this.mode = "after";
                        updateUIForMode();
                    } else if (requestCode == 1002) {
                        this.afterImage = stream.toByteArray();
                        evaluateTask();
                    }
                }
            }
        }
    }

    private void evaluateTask() {
        Log.d(TAG, "evaluateTask() called; beforeImage=" +
                (beforeImage == null ? "null" : beforeImage.length) +
                ", afterImage=" +
                (afterImage == null ? "null" : afterImage.length));

        if (beforeImage == null || afterImage == null) {
            Log.e(TAG, "Missing images for evaluation");
            showFailureDialog();
            return;
        }

        showLoadingOverlay();

        new Thread(() -> {
            String verdictLocal = "fail";
            try {
                verdictLocal = WellnestAiClient.evaluateSnapTask(taskDescription, beforeImage, afterImage);
                Log.d(TAG, "evaluateTask() got verdict from AI (background): " + verdictLocal);
            } catch (Exception e) {
                Log.e(TAG, "Error during evaluateSnapTask in background thread", e);
            }

            final String finalVerdict = verdictLocal;
            runOnUiThread(() -> {
                hideLoadingOverlay();
                if ("pass".equals(finalVerdict)) {
                    handleTaskCompletionSuccess();
                } else {
                    showFailureDialog();
                }
            });
        }).start();
    }

    private Bitmap downscale(Bitmap src, int maxSize) {
        int width = src.getWidth();
        int height = src.getHeight();
        float scale = Math.min(
                (float) maxSize / width,
                (float) maxSize / height
        );
        int newW = Math.round(width * scale);
        int newH = Math.round(height * scale);
        return Bitmap.createScaledBitmap(src, newW, newH, true);
    }

    private void showInfoDialog(String description) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_snap_task_info, null);
        TextView descriptionText = dialogView.findViewById(R.id.taskDescriptionText);
        Button okButton = dialogView.findViewById(R.id.okButton);

        String formattedDescription = taskDescription.replace("\\n", "\n");
        descriptionText.setText(formattedDescription);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        UiClickEffects.setOnClickWithPulse(okButton, R.raw.ui_click_effect, v -> dialog.dismiss());
        dialog.show();
    }

    private void showLoadingOverlay() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.VISIBLE);
        }
        if (loadingProgressBar != null) {
            loadingProgressBar.setIndeterminate(true);
            UiProgressEffects.pulseIndeterminate(loadingProgressBar);
        }
    }

    private void hideLoadingOverlay() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.GONE);
        }
        if (loadingProgressBar != null) {
            UiProgressEffects.stopPulseIndeterminate(loadingProgressBar);
        }
    }

    private void handleTaskCompletionSuccess() {
        try {
            if (snapTaskViewModel != null && taskUid != null && !taskUid.isEmpty()) {
                if (!taskCompleted) {
                    snapTaskViewModel.completeTaskAndApplyScore(taskUid, taskPoints);
                    taskCompleted = true;
                } else {
                    snapTaskViewModel.completeTaskAndApplyScore(taskUid, 0);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating local SnapTask completion state", e);
        }
        showSuccessDialog();
    }

    private void showSuccessDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_snap_task_success, null);
        TextView pointsEarnedText = dialogView.findViewById(R.id.pointsEarnedText);
        Button continueButton = dialogView.findViewById(R.id.continueButton);

        pointsEarnedText.setText("You earned " + taskPoints + " points!");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();

        UiClickEffects.setOnClickWithPulse(continueButton, R.raw.happy_ping, v -> {
            dialog.dismiss();
            Intent result = new Intent();
            result.putExtra(EXTRA_TASK_UID, taskUid);
            result.putExtra(EXTRA_TASK_COMPLETED, true);
            setResult(Activity.RESULT_OK, result);
            finishWithExitAnim();
        });

        dialog.show();
    }

    private void showFailureDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_snap_task_failure, null);
        Button tryAgainButton = dialogView.findViewById(R.id.tryAgainButton);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();

        UiClickEffects.setOnClickWithPulse(tryAgainButton, R.raw.ui_click_effect, v -> {
            dialog.dismiss();
            setResult(Activity.RESULT_CANCELED);
            finishWithExitAnim();
        });

        dialog.show();
    }

    private void finishWithExitAnim() {
        finish();
        try {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        try {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        } catch (Exception ignored) {
        }
    }
}