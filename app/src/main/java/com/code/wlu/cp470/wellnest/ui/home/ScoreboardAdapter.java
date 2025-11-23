package com.code.wlu.cp470.wellnest.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.data.UserModels.Friend;

import java.util.List;

public class ScoreboardAdapter extends RecyclerView.Adapter<ScoreboardAdapter.MyViewHolder> {

    private final Context context;
    private final List<Friend> friends;

    public ScoreboardAdapter(Context context, List<Friend> friends) {
        this.context = context;
        this.friends = friends;
    }

    @NonNull
    @Override
    public ScoreboardAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_scoreboard_row, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScoreboardAdapter.MyViewHolder holder, int position) {
        Friend friend = friends.get(position);
        holder.nameText.setText(friend.getName());
        holder.scoreText.setText(String.valueOf(friend.getScore()));
        holder.rankNumber.setText(String.valueOf(position + 1));

        // Set rank icon based on position
        if (position == 0) {
            holder.rankIcon.setImageResource(R.drawable.star_icon_first);
        } else if (position == 1) {
            holder.rankIcon.setImageResource(R.drawable.star_icon_second);
        } else if (position == 2) {
            holder.rankIcon.setImageResource(R.drawable.star_icon_third);
        } else {
            holder.rankIcon.setImageResource(R.drawable.star_icon_other);
        }
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView rankNumber, scoreText, nameText;
        ImageView rankIcon;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            rankNumber = itemView.findViewById(R.id.rankNumber);
            scoreText = itemView.findViewById(R.id.textScore);
            nameText = itemView.findViewById(R.id.textName);
            rankIcon = itemView.findViewById(R.id.rankText); // ID in xml is rankText for the ImageView
        }
    }
}
