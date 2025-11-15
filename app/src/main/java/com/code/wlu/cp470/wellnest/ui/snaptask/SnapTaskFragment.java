package com.code.wlu.cp470.wellnest.ui.snaptask;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.data.SnapTaskModels;
import com.code.wlu.cp470.wellnest.ui.effects.UiClickEffects;
import com.code.wlu.cp470.wellnest.viewmodel.SnapTaskViewModel;

import java.util.List;

public class SnapTaskFragment extends Fragment {
    Context context;
    private SnapTaskViewModel snapTaskViewModel;
    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_snap_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        context = view.getContext();

        recyclerView = view.findViewById(R.id.snap_task_recycler_view);
        TextView snapTaskScore = view.findViewById(R.id.snap_task_score_text);
        ImageView character = view.findViewById(R.id.snap_task_character);
        CardView backButton = view.findViewById(R.id.snap_task_back_button);

        snapTaskViewModel = new SnapTaskViewModel(requireActivity().getApplication());

        int score = snapTaskViewModel.getScore();
        snapTaskScore.setText(String.valueOf(score));

        if (score < 500) {
            character.setImageResource(R.drawable.puffin_baby);
        } else if (score < 1000) {
            character.setImageResource(R.drawable.puffin_teen);
        } else if (score < 1500) {
            character.setImageResource(R.drawable.puffin_adult);
        } else {
            character.setImageResource(R.drawable.puffin_senior);
        }

        List<SnapTaskModels.Task> tasks = snapTaskViewModel.getTasks();
        SnapTaskAdapter adapter = new SnapTaskAdapter(context, tasks, snapTaskViewModel);
        Log.d("SnapTaskFragment", "logging tasks: " + tasks.size());
        for (SnapTaskModels.Task task : tasks) {
            Log.d("SnapTaskFragment", "Task: " + task.getName() + ", Points: " + task.getPoints());
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        UiClickEffects.setOnClickWithPulse(backButton, R.raw.ui_click_effect, v -> {
            NavController navController =
                    NavHostFragment.findNavController(SnapTaskFragment.this);
            navController.navigateUp();
        });
    }
}
