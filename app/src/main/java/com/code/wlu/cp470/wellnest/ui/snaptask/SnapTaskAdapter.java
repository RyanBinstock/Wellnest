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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.data.SnapTaskModels;
import com.code.wlu.cp470.wellnest.ui.effects.UiClickEffects;
import com.code.wlu.cp470.wellnest.viewmodel.SnapTaskViewModel;

import java.util.ArrayList;
import java.util.List;

public class SnapTaskAdapter extends RecyclerView.Adapter<SnapTaskAdapter.MyViewHolder> {
    private final Context context;
    private final SnapTaskViewModel viewModel;
    private final List<SnapTaskModels.Task> items = new ArrayList<>();
    private final String TAG = "SNAPTASK_ADAPTER";

    public SnapTaskAdapter(Context context, List<SnapTaskModels.Task> initial, SnapTaskViewModel viewModel) {
        this.context = context;
        this.viewModel = viewModel;
        if (initial != null) items.addAll(initial);
    }

    @NonNull
    @Override
    public SnapTaskAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_task_card, parent, false);
        return new SnapTaskAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SnapTaskAdapter.MyViewHolder holder, int position) {
        SnapTaskModels.Task task = items.get(position);
        holder.task_title.setText(task.getName());
        if (task.getCompleted()) {
            holder.task_bg.setImageResource(R.drawable.task_card_completed);
            holder.task_points.setVisibility(View.GONE);
            holder.task_star_icon.setVisibility(View.GONE);
            holder.task_subtitle.setText(R.string.task_finished);
        } else {
            holder.task_bg.setImageResource(R.drawable.task_card_incomplete);
            holder.task_points.setVisibility(View.VISIBLE);
            holder.task_points.setText(String.valueOf(task.getPoints()));
            holder.task_star_icon.setVisibility(View.VISIBLE);
            holder.task_subtitle.setText(R.string.task_unfinished);
        }
        UiClickEffects.setOnClickWithPulse(holder.itemView, R.raw.happy_ping, v -> {
            Log.d(TAG, "Position " + position + " clicked");

            // Navigate to SnapTaskDetailFragment with task data
            try {
                NavController navController = Navigation.findNavController(v);
                Bundle args = new Bundle();
                args.putString("mode", "before");
                args.putString("taskUid", task.getUid());
                args.putString("taskName", task.getName());
                args.putString("taskDescription", task.getDescription());
                args.putInt("taskPoints", task.getPoints());
                args.putBoolean("taskCompleted", task.getCompleted());

                navController.navigate(R.id.action_snapTask_to_detail, args);
            } catch (Exception e) {
                Log.e(TAG, "Navigation error: " + e.getMessage());
            }
        });

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        final TextView task_points, task_title, task_subtitle;
        final ImageView task_bg, task_star_icon;


        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            task_points = itemView.findViewById(R.id.task_points);
            task_title = itemView.findViewById(R.id.task_title);
            task_subtitle = itemView.findViewById(R.id.task_subtitle);
            task_bg = itemView.findViewById(R.id.task_bg);
            task_star_icon = itemView.findViewById(R.id.task_star_icon);
        }
    }
}
