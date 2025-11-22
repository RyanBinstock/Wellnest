package com.code.wlu.cp470.wellnest.ui.friends;

import android.content.Context;
import android.util.Log;
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

import java.util.ArrayList;
import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.MyViewHolder> {

    private final Context context;
    private final String mode;
    private final FriendViewModel viewModel;

    // Always mutable, adapter-owned list
    private final List<Friend> items = new ArrayList<>();

    public FriendAdapter(Context context, List<Friend> initial, String mode, FriendViewModel viewModel) {
        this.context = context;
        this.mode = mode;
        this.viewModel = viewModel;
        if (initial != null) items.addAll(initial); // defensive copy
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.fragment_friend_card, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Friend friend = items.get(position);
        holder.name.setText(friend.getName());
        holder.score.setText(String.valueOf(friend.getScore()));

        // Remove
        UiClickEffects.setOnClickWithPulse(holder.remove_friend_button, R.raw.ui_click_effect, v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            String uid = items.get(pos).getUid();
            viewModel.removeFriend(uid);
            Log.d("FriendAdapter", "Removed friend with uid: " + uid);
            // Let LiveData observer drive the UI refresh. Don't mutate here to avoid race/stale pos.
        });

        if ("pending".equals(mode)) {
            holder.accept_friend_button.setVisibility(View.VISIBLE);
            holder.score.setVisibility(View.INVISIBLE);
            UiClickEffects.setOnClickWithPulse(holder.accept_friend_button, R.raw.happy_ping, v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                String uid = items.get(pos).getUid();
                viewModel.acceptFriend(uid);
                // Again, let LiveData update the list (it should move this item out of "pending").
            });
        } else {
            holder.accept_friend_button.setVisibility(View.INVISIBLE);
            holder.score.setVisibility(View.VISIBLE);
        }
    }

    public void updateData(List<Friend> newFriends) {
        items.clear();
        if (newFriends != null) items.addAll(newFriends); // always mutable
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        final TextView name, score;
        final ImageButton accept_friend_button, remove_friend_button;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.friend_name);
            score = itemView.findViewById(R.id.friend_score);
            accept_friend_button = itemView.findViewById(R.id.accept_friend_button);
            remove_friend_button = itemView.findViewById(R.id.remove_friend_button);
        }
    }
}
