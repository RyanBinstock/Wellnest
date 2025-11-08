package com.code.wlu.cp470.wellnest.ui.friends;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.viewmodel.FriendViewModel;

public class FriendCardFragment extends Fragment {
    Context context;
    private String uid;
    private String name;
    private int score;

    public FriendCardFragment(String uid, String name, int score) {
        this.uid = uid;
        this.name = name;
        this.score = score;

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friend_card, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = getContext();

        TextView nameTv = view.findViewById(R.id.name);
        TextView scoreTv = view.findViewById(R.id.score);
        ImageButton removeFriendButton = view.findViewById(R.id.remove_friend_button);

        nameTv.setText(name);
        scoreTv.setText(String.valueOf(score));

        removeFriendButton.setOnClickListener(v -> {
            FriendViewModel viewModel = new FriendViewModel(requireActivity().getApplication());
            boolean success = viewModel.removeFriend(uid);
            if (success) {
                Toast.makeText(context, "Friend Removed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to remove friend", Toast.LENGTH_SHORT).show();
            }
        });

    }
}
