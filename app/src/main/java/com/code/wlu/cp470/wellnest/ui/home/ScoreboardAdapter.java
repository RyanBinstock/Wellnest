package com.code.wlu.cp470.wellnest.ui.home;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.code.wlu.cp470.wellnest.R;

public class ScoreboardAdapter extends RecyclerView.Adapter<ScoreboardAdapter.MyViewHolder> {


//    public ScoreboardAdapter(Context context, List<...>) {

    //     TODO: Implement scoreboard adapter
//    }
    @NonNull
    @Override
    public ScoreboardAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull ScoreboardAdapter.MyViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView rankText, scoreText, nameText;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            rankText = itemView.findViewById(R.id.rankText);
            scoreText = itemView.findViewById(R.id.textScore);
            nameText = itemView.findViewById(R.id.textName);
        }
    }
}
