package com.code.wlu.cp470.wellnest.ui.snaptask;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.utils.ImageUtils;
import com.code.wlu.cp470.wellnest.network.OpenAIService;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class SnapTaskFragment extends Fragment {

    private EditText etTaskName;
    private ImageView ivBefore, ivAfter;
    private Button btnBefore, btnAfter, btnSubmit;
    private ProgressBar progress;
    private TextView tvStatus;

    private File beforeFile, afterFile;
    private Uri currentOutputUri;
    private boolean isCapturingBefore;

    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    public @Nullable View onCreateView(@NonNull LayoutInflater inflater,
                                       @Nullable ViewGroup container,
                                       @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_snaptask, container, false);

        etTaskName = v.findViewById(R.id.etTaskName);
        ivBefore   = v.findViewById(R.id.ivBefore);
        ivAfter    = v.findViewById(R.id.ivAfter);
        btnBefore  = v.findViewById(R.id.btnCaptureBefore);
        btnAfter   = v.findViewById(R.id.btnCaptureAfter);
        btnSubmit  = v.findViewById(R.id.btnSubmit);
        progress   = v.findViewById(R.id.progress);
        tvStatus   = v.findViewById(R.id.tvStatus);

        // Camera launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && currentOutputUri != null) {
                        try {
                            Bitmap down = ImageUtils.downscaleFromUri(requireContext(), currentOutputUri, 480);
                            File out = ImageUtils.saveTempCompressed(requireContext(), down,
                                    isCapturingBefore ? "before" : "after");
                            if (isCapturingBefore) {
                                beforeFile = out;
                                ivBefore.setImageBitmap(down);
                            } else {
                                afterFile = out;
                                ivAfter.setImageBitmap(down);
                            }
                            tvStatus.setText("Photo captured ✔");
                        } catch (IOException e) {
                            tvStatus.setText("Failed to process photo.");
                        }
                    } else {
                        tvStatus.setText("Capture cancelled.");
                    }
                });

        btnBefore.setOnClickListener(vw -> capture(true));
        btnAfter.setOnClickListener(vw -> capture(false));
        btnSubmit.setOnClickListener(vw -> submitForVerification());

        ensureCameraPermission();
        return v;
    }

    private void ensureCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (!isGranted)
                            Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
                    }).launch(Manifest.permission.CAMERA);
        }
    }

    private void capture(boolean before) {
        isCapturingBefore = before;
        try {
            File photoFile = File.createTempFile(before ? "before_" : "after_", ".jpg", requireContext().getCacheDir());
            currentOutputUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, currentOutputUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Cannot create temp file", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitForVerification() {
        String taskName = etTaskName.getText().toString().trim();
        if (TextUtils.isEmpty(taskName)) {
            etTaskName.setError("Enter activity name");
            return;
        }
        if (beforeFile == null || afterFile == null) {
            tvStatus.setText("Please capture both photos first.");
            return;
        }

        setBusy(true, "Verifying…");

        new Thread(() -> {
            try {
                boolean verified = OpenAIService.verifyBeforeAfter(taskName, beforeFile, afterFile);
                requireActivity().runOnUiThread(() -> {
                    if (verified) {
                        tvStatus.setText("Verified  — points awarded!");
                        cleanup();
                    } else {
                        tvStatus.setText("Not verified");
                    }
                    setBusy(false, null);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    tvStatus.setText("Network error. Try again later.");
                    setBusy(false, null);
                });
            }
        }).start();
    }

    private void cleanup() {
        if (beforeFile != null) beforeFile.delete();
        if (afterFile != null) afterFile.delete();
        ivBefore.setImageDrawable(null);
        ivAfter.setImageDrawable(null);
    }

    private void setBusy(boolean busy, @Nullable String msg) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!busy);
        btnBefore.setEnabled(!busy);
        btnAfter.setEnabled(!busy);
        if (msg != null) tvStatus.setText(msg);
    }
}
