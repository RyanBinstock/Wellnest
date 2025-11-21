package com.code.wlu.cp470.wellnest.ui.roamio;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.code.wlu.cp470.wellnest.R;

import eightbitlab.com.blurview.BlurTarget;
import eightbitlab.com.blurview.BlurView;

public class RoamioActivity extends AppCompatActivity {

    Context context;
    private TextView scoreText, walkTitle, walkDescription;
    private ImageView characterImage, infoButton;
    private Button primaryButton;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Hide navigation bar and allow content behind the status bar
        View decorView = getWindow().getDecorView();

        // Reuse the existing fragment layout as Activity content to minimize changes
        setContentView(R.layout.activity_roamio);

        // Ensure layout extends behind system bars; do NOT use FULLSCREEN so status icons remain visible
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        // Enter transition (the caller fragment also applies this)
        try {
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } catch (Exception ignored) {
        }

        scoreText = findViewById(R.id.roamio_score_text);
        walkTitle = findViewById(R.id.walkTitle);
        walkDescription = findViewById(R.id.walkDescription);
        characterImage = findViewById(R.id.roamioCharacter);
        primaryButton = findViewById(R.id.walkButton);
        infoButton = findViewById(R.id.roamio_card_info_button); // this is actually a textview because imageButton was acting weird

        // TODO: Grab score from repo and update scoreText
        int score = 1584;
        scoreText.setText(String.valueOf(score));

        // Set character image based on score
        if (score < 500) {
            characterImage.setImageResource(R.drawable.puffin_baby);
        } else if (score < 1000) {
            characterImage.setImageResource(R.drawable.puffin_teen);
        } else if (score < 1500) {
            characterImage.setImageResource(R.drawable.puffin_adult);
        } else {
            characterImage.setImageResource(R.drawable.puffin_senior);
        }

        // TODO: Grab walk from repo and update walkTitle and walkDescription
        walkTitle.setText("Templin Gardens - Fergus");
        walkDescription.setText("Lorem ipsum dolor sit amet");

        BlurTarget blurTarget = findViewById(R.id.roamio_bg_blurTarget);
        BlurView blurView = findViewById(R.id.roamio_blurView);
        blurView.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        blurView.setClipToOutline(true);
        blurView.setupWith(blurTarget)
                .setBlurRadius(5f);
    }
}
