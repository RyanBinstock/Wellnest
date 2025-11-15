package com.code.wlu.cp470.wellnest.ui.snaptask;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.data.SnapTaskModels;
import com.code.wlu.cp470.wellnest.ui.effects.UiClickEffects;
import com.code.wlu.cp470.wellnest.viewmodel.SnapTaskViewModel;

import java.util.List;

public class SnapTaskActivity extends AppCompatActivity {

    public static final int REQUEST_TASK_DETAIL = 1001;

    private SnapTaskViewModel snapTaskViewModel;
    private RecyclerView recyclerView;
    private SnapTaskAdapter adapter;
    private TextView snapTaskScore;
    private ImageView character;
    private CardView backButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Reuse the existing fragment layout as Activity content to minimize changes
        setContentView(R.layout.fragment_snap_task);

        // Enter transition (the caller fragment also applies this)
        try {
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } catch (Exception ignored) { }

        initializeViewModel();
        bindViews();
        setupRecycler();
        refreshScoreAndCharacter();
        loadTasks();

        UiClickEffects.setOnClickWithPulse(backButton, R.raw.ui_click_effect, v -> {
            finish();
            try {
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            } catch (Exception ignored) { }
        });
    }

    private void initializeViewModel() {
        snapTaskViewModel = new ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())
        ).get(SnapTaskViewModel.class);
    }

    private void bindViews() {
        recyclerView = findViewById(R.id.snap_task_recycler_view);
        snapTaskScore = findViewById(R.id.snap_task_score_text);
        character = findViewById(R.id.snap_task_character);
        backButton = findViewById(R.id.snap_task_back_button);
    }

    private void setupRecycler() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadTasks() {
        List<SnapTaskModels.Task> tasks = snapTaskViewModel.getTasks();
        adapter = new SnapTaskAdapter(this, tasks, snapTaskViewModel);
        recyclerView.setAdapter(adapter);
    }

    private void refreshTasks() {
        List<SnapTaskModels.Task> tasks = snapTaskViewModel.getTasks();
        adapter = new SnapTaskAdapter(this, tasks, snapTaskViewModel);
        recyclerView.setAdapter(adapter);
    }

    private void refreshScoreAndCharacter() {
        int score = snapTaskViewModel.getScore();
        if (snapTaskScore != null) {
            snapTaskScore.setText(String.valueOf(score));
        }
        if (character != null) {
            if (score < 500) {
                character.setImageResource(R.drawable.puffin_baby);
            } else if (score < 1000) {
                character.setImageResource(R.drawable.puffin_teen);
            } else if (score < 1500) {
                character.setImageResource(R.drawable.puffin_adult);
            } else {
                character.setImageResource(R.drawable.puffin_senior);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TASK_DETAIL) {
            // After returning from detail, refresh score and list
            refreshScoreAndCharacter();
            refreshTasks();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        try {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        } catch (Exception ignored) { }
    }
}