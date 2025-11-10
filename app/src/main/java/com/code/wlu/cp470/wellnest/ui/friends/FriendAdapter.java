package com.code.wlu.cp470.wellnest.ui.friends;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.data.UserModels.Friend;
import com.code.wlu.cp470.wellnest.ui.effects.UiClickEffects;
import com.code.wlu.cp470.wellnest.viewmodel.FriendViewModel;

import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.MyViewHolder> {

    Context context;
    List<Friend> friendsList;
    String mode;
    FriendViewModel viewModel;

    public FriendAdapter(Context context, List<Friend> friendsList, String mode, FriendViewModel viewModel) {
        // Take the list of friends as an input
        this.context = context;
        this.friendsList = friendsList;
        this.mode = mode;
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public FriendAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // This method inflates the friend card layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.fragment_friend_card, parent, false);
        return new FriendAdapter.MyViewHolder(context, view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendAdapter.MyViewHolder holder, int position) {
        // This method assigns values to the views in the friend card layout
        Friend friend = friendsList.get(position);
        String uid = friend.getUid();
        holder.name.setText(friendsList.get(position).getName());
        holder.score.setText(String.valueOf(friendsList.get(position).getScore()));

        UiClickEffects.setOnClickWithPulse(holder.remove_friend_button, v -> {
            viewModel.removeFriend(uid);
            friendsList.remove(position);
            notifyItemRemoved(position);
        });

        if (mode.equals("pending")) {
            holder.accept_friend_button.setVisibility(View.VISIBLE);
            holder.score.setVisibility(View.INVISIBLE);
            UiClickEffects.setOnClickWithPulse(holder.accept_friend_button, v -> {
                viewModel.acceptFriend(uid);
                notifyItemChanged(position);
            });
        } else {
            holder.accept_friend_button.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return friendsList == null ? 0 : friendsList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // Grabs views from the friend card layout file and assigns them to variables
        // Its kind of like an onCreate() method
        TextView name, score;
        ImageButton accept_friend_button, remove_friend_button;

        public MyViewHolder(Context context, @NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.friend_name);
            score = itemView.findViewById(R.id.friend_score);
            accept_friend_button = itemView.findViewById(R.id.accept_friend_button);
            remove_friend_button = itemView.findViewById(R.id.remove_friend_button);
        }
    }
}
