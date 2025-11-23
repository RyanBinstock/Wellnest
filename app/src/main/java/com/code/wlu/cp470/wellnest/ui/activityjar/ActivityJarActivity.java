package com.code.wlu.cp470.wellnest.ui.activityjar;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.viewmodel.ActivityJarViewModel;

import java.text.NumberFormat;
import java.util.Locale;

public class ActivityJarActivity extends AppCompatActivity {

    private ActivityJarViewModel viewModel;
    private TextView txtScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Hide navigation bar and allow content behind the status bar
        View decorView = getWindow().getDecorView();

        // Reuse the existing fragment layout as Activity content to minimize changes
        setContentView(R.layout.activity_activity_jar);

        // Ensure layout extends behind system bars; do NOT use FULLSCREEN so status icons remain visible
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        try {
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } catch (Exception ignored) {
        }

        txtScore = findViewById(R.id.txtScore);
        viewModel = new ViewModelProvider(this).get(ActivityJarViewModel.class);

        viewModel.getScore().observe(this, score -> {
            if (score != null) {
                updateScoreWithAnimation(score);
            }
        });

        ImageView btnExplore = findViewById(R.id.btnExplore);
        ImageView btnNightlife = findViewById(R.id.btnNightlife);
        ImageView btnPlay = findViewById(R.id.btnPlay);
        ImageView btnCozy = findViewById(R.id.btnCozy);
        ImageView btnCulture = findViewById(R.id.btnCulture);
        btnExplore.setOnClickListener(v -> openSelection(0));   // Explore card
        btnNightlife.setOnClickListener(v -> openSelection(1)); // Nightlife card
        btnPlay.setOnClickListener(v -> openSelection(2));      // Play card
        btnCozy.setOnClickListener(v -> openSelection(3));      // Cozy card
        btnCulture.setOnClickListener(v -> openSelection(4));   // Culture card

        CardView btnHome = findViewById(R.id.activity_jar_back_button);
        btnHome.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

    }

    private void openSelection(int startIndex) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.activity_jar_root, activityJarSelection.newInstance(startIndex))
                .addToBackStack(null)
                .commit();
    }

    private void updateScoreWithAnimation(int newScore) {
        txtScore.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    txtScore.setText(NumberFormat.getNumberInstance(Locale.US).format(newScore));
                    txtScore.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start();
                })
                .start();
    }
}