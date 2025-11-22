package com.code.wlu.cp470.wellnest.ui.activityjar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.data.ActivityJarModels;

import java.util.List;

public class ActivitiesPagerAdapter
        extends RecyclerView.Adapter<ActivitiesPagerAdapter.CardViewHolder> {

    public interface OnActivityClickListener {
        void onActivityClicked(ActivityJarModels.Activity activity);
    }

    private final LayoutInflater inflater;
    private final List<ActivityJarModels.Activity> activities;
    private final OnActivityClickListener clickListener;

    public ActivitiesPagerAdapter(Context context,
                                  List<ActivityJarModels.Activity> activities,
                                  OnActivityClickListener clickListener) {
        this.inflater = LayoutInflater.from(context);
        this.activities = activities;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_activity_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        ActivityJarModels.Activity activity = activities.get(position);

        holder.txtEmoji.setText(activity.emoji);
        holder.txtName.setText(activity.name);
        holder.txtDescription.setText(activity.description);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onActivityClicked(activity);
            }
        });
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgBackground;
        final TextView txtEmoji;
        final TextView txtName;
        final TextView txtDescription;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBackground = itemView.findViewById(R.id.imgCardBackground);
            txtEmoji = itemView.findViewById(R.id.emoji_tv);
            txtName = itemView.findViewById(R.id.activityName_tv);
            txtDescription = itemView.findViewById(R.id.description_tv);
        }
    }
}
