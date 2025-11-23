package com.code.wlu.cp470.wellnest.ui.activityjar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.data.ActivityJarModels;

import java.util.ArrayList;
import java.util.List;

public class ActivityCarouselAdapter extends RecyclerView.Adapter<ActivityCarouselAdapter.ActivityViewHolder> {

    private List<ActivityJarModels.Activity> activities = new ArrayList<>();
    private final OnActivityClickListener listener;

    public interface OnActivityClickListener {
        void onActivityClick(ActivityJarModels.Activity activity);
    }

    public ActivityCarouselAdapter(OnActivityClickListener listener) {
        this.listener = listener;
    }

    public void setActivities(List<ActivityJarModels.Activity> activities) {
        this.activities = activities;
        notifyDataSetChanged();
    }

    public ActivityJarModels.Activity getItem(int position) {
        if (position >= 0 && position < activities.size()) {
            return activities.get(position);
        }
        return null;
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity_card, parent, false);
        // Ensure match_parent for CarouselView
        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        ActivityJarModels.Activity activity = activities.get(position);
        holder.bind(activity, listener);
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        private final TextView emojiTv;
        private final TextView titleTv;
        private final TextView descriptionTv;
        private final ImageView bgImage;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            emojiTv = itemView.findViewById(R.id.emoji_tv);
            titleTv = itemView.findViewById(R.id.activityName_tv);
            descriptionTv = itemView.findViewById(R.id.description_tv);
            bgImage = itemView.findViewById(R.id.imgCardBackground);
        }

        public void bind(ActivityJarModels.Activity activity, OnActivityClickListener listener) {
            emojiTv.setText(activity.getEmoji());
            titleTv.setText(activity.getTitle());
            descriptionTv.setText(activity.getDescription());

            // Set background based on category if needed, or keep default
            // For now we keep the default blurred background from XML or set dynamically
            // bgImage.setImageResource(...);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onActivityClick(activity);
                }
            });
        }
    }
}