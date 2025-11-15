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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.data.WellnestAiClient;
import com.code.wlu.cp470.wellnest.ui.components.WellnestProgressBar;
import com.code.wlu.cp470.wellnest.ui.effects.UiClickEffects;
import com.code.wlu.cp470.wellnest.ui.effects.UiProgressEffects;
import com.code.wlu.cp470.wellnest.viewmodel.SnapTaskViewModel;

import java.io.ByteArrayOutputStream;

public class SnapTaskDetailFragment extends Fragment {

    private static final String TAG = "SnapTaskDetailFragment";
    private static final String ACTIVE_COLOUR = "#4A74DF";
    private static final String INACTIVE_COLOUR = "#CBD1E0";
    private static final String ACTIVE_TEXT_COLOUR = "#FFFFFF";
    private static final String INACTIVE_TEXT_COLOUR = "#494949";
    private String mode; // "before" | "after"
    private String taskUid;
    private String taskName;
    private String taskDescription;
    private int taskPoints;
    private boolean taskCompleted;
    private byte[] beforeImage, afterImage;
    private Context context;

    // Data / state
    private SnapTaskViewModel snapTaskViewModel;

    // UI elements
    private TextView taskNameView, beforeText, beforeInnerText, afterInnerText, afterText, primaryButtonText;
    private ImageView heroImage;
    private CardView exitButton, infoButton, primaryButton, beforeCard, afterCard;
    private View loadingOverlay;
    private WellnestProgressBar loadingProgressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_snap_task_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = view.getContext();

        // Shared SnapTask data access
        snapTaskViewModel = new SnapTaskViewModel(requireActivity().getApplication());

        // Get arguments
        Bundle args = getArguments();
        if (args != null) {
            mode = args.getString("mode", "before");
            taskUid = args.getString("taskUid", "");
            taskName = args.getString("taskName", "");
            taskDescription = args.getString("taskDescription", "");
            taskPoints = args.getInt("taskPoints", 0);
            taskCompleted = args.getBoolean("taskCompleted", false);
        } else {
            Log.e(TAG, "No arguments received");
            // Navigate back if no arguments
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigateUp();
            return;
        }

        // Initialize UI elements
        heroImage = view.findViewById(R.id.snap_task_detail_hero);
        taskNameView = view.findViewById(R.id.snap_task_detail_top_name);
        beforeInnerText = view.findViewById(R.id.before_text);
        afterInnerText = view.findViewById(R.id.after_text);
        beforeText = view.findViewById(R.id.acceptance_criteria_text_before);
        afterText = view.findViewById(R.id.acceptance_criteria_text_after);
        beforeCard = view.findViewById(R.id.snap_task_cardview_before);
        afterCard = view.findViewById(R.id.snap_task_cardview_after);
        exitButton = view.findViewById(R.id.snap_task_detail_exit);
        infoButton = view.findViewById(R.id.snap_task_detail_info);
        primaryButton = view.findViewById(R.id.snap_task_primary_button_card);
        primaryButtonText = view.findViewById(R.id.snap_task_primary_button_text);
        loadingOverlay = view.findViewById(R.id.snap_task_loading_overlay);
        loadingProgressBar = view.findViewById(R.id.snap_task_loading_progress);

        // Set task name
        taskNameView.setText(taskName);

        // Split description by literal '\n' delimiter (not actual newline)
        String[] descriptionParts = taskDescription.split("\\\\n");
        String description = "";
        String beforeCriteria = "";
        String afterCriteria = "";

        // Handle edge cases with safe parsing
        if (descriptionParts.length >= 1 && descriptionParts[0] != null) {
            description = descriptionParts[0].trim(); // for the info button
        }
        if (descriptionParts.length >= 2 && descriptionParts[1] != null) {
            beforeCriteria = descriptionParts[1].trim();
        }
        if (descriptionParts.length >= 3 && descriptionParts[2] != null) {
            afterCriteria = descriptionParts[2].trim();
        }

        // Set the text only if not empty
        if (!beforeCriteria.isEmpty()) {
            beforeText.setText(beforeCriteria);
        } else {
            beforeText.setText("No before criteria specified");
        }

        if (!afterCriteria.isEmpty()) {
            afterText.setText(afterCriteria);
        } else {
            afterText.setText("No after criteria specified");
        }

        // Update UI based on mode
        updateUIForMode();

        final String finalDescription = description;
        UiClickEffects.setOnClickWithPulse(infoButton, R.raw.ui_click_effect, v -> {
            Log.d(TAG, "Info button clicked");
            showInfoDialog(finalDescription);
        });
        UiClickEffects.setOnClickWithPulse(exitButton, R.raw.ui_click_effect, v -> {
            Log.d(TAG, "Exit button clicked");
            // go back to SnapTaskFragment
            NavController navController = NavHostFragment.findNavController(SnapTaskDetailFragment.this);
            navController.navigateUp();
        });

        UiClickEffects.setOnClickWithPulse(primaryButton, R.raw.ui_click_effect, v -> {
            Log.d(TAG, "Primary button clicked - mode: " + mode);
            // launch camera
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (mode.equals("before")) {
                startActivityForResult(intent, 1001);
            } else {
                startActivityForResult(intent, 1002);
            }
        });
    }

    private void updateUIForMode() {
        if (mode.equals("before")) {
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
                        // Store before image
                        this.beforeImage = stream.toByteArray();
                        // Switch to after mode
                        this.mode = "after";
                        // Update UI
                        updateUIForMode();
                    } else if (requestCode == 1002) {
                        // Store after image
                        this.afterImage = stream.toByteArray();
                        // Evaluate task
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

        // Show loading UI while the AI evaluation is in progress
        showLoadingOverlay();
        
        // Run network-bound AI call off the main thread to avoid NetworkOnMainThreadException
        new Thread(() -> {
            String verdictLocal = "fail";
            try {
                verdictLocal = WellnestAiClient.evaluateSnapTask(taskDescription, beforeImage, afterImage);
                Log.d(TAG, "evaluateTask() got verdict from AI (background): " + verdictLocal);
            } catch (Exception e) {
                Log.e(TAG, "Error during evaluateSnapTask in background thread", e);
            }
            
            // Post result back to UI thread for dialog/navigation
            if (!isAdded()) {
                Log.w(TAG, "Fragment not added when AI verdict returned, aborting UI update");
                return;
            }
            
            final String finalVerdict = verdictLocal;
            
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) {
                    Log.w(TAG, "Fragment not added on UI thread when updating UI, aborting");
                    return;
                }

                // Always hide loading UI once we have a result
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
        if (context == null) return;
        
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_snap_task_info, null);
        TextView descriptionText = dialogView.findViewById(R.id.taskDescriptionText);
        Button okButton = dialogView.findViewById(R.id.okButton);
        
        // Replace literal '\n' with actual line breaks for display
        String formattedDescription = taskDescription.replace("\\n", "\n");
        descriptionText.setText(formattedDescription);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        
        UiClickEffects.setOnClickWithPulse(okButton, R.raw.ui_click_effect, v -> {
            dialog.dismiss();
        });
        
        dialog.show();
    }

    private void showLoadingOverlay() {
        if (!isAdded()) return;

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

    /**
     * Handle the successful evaluation case by marking the task as completed
     * in the local database (and updating the SnapTask score) before showing
     * the success dialog.
     */
    private void handleTaskCompletionSuccess() {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not added when handling task completion success, aborting");
            return;
        }

        try {
            if (snapTaskViewModel != null && taskUid != null && !taskUid.isEmpty()) {
                // Avoid double-scoring if this task was already completed
                if (!taskCompleted) {
                    snapTaskViewModel.completeTaskAndApplyScore(taskUid, taskPoints);
                    taskCompleted = true;
                } else {
                    // Ensure the completed flag is persisted at least once
                    snapTaskViewModel.completeTaskAndApplyScore(taskUid, 0);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating local SnapTask completion state", e);
        }

        showSuccessDialog();
    }
    
    private void showSuccessDialog() {
        if (context == null) return;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_snap_task_success, null);
        TextView pointsEarnedText = dialogView.findViewById(R.id.pointsEarnedText);
        Button continueButton = dialogView.findViewById(R.id.continueButton);

        pointsEarnedText.setText("You earned " + taskPoints + " points!");

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);
        builder.setCancelable(false); // Prevent dismissing by tapping outside
        AlertDialog dialog = builder.create();

        UiClickEffects.setOnClickWithPulse(continueButton, R.raw.happy_ping, v -> {
            dialog.dismiss();
            // Navigate back to SnapTaskFragment
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigateUp();
        });

        dialog.show();
    }

    private void showFailureDialog() {
        if (context == null) return;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_snap_task_failure, null);
        Button tryAgainButton = dialogView.findViewById(R.id.tryAgainButton);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);
        builder.setCancelable(false); // Prevent dismissing by tapping outside
        AlertDialog dialog = builder.create();

        UiClickEffects.setOnClickWithPulse(tryAgainButton, R.raw.ui_click_effect, v -> {
            dialog.dismiss();
            // Navigate back to SnapTaskFragment
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigateUp();
        });

        dialog.show();
    }
}