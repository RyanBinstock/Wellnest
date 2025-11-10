package com.code.wlu.cp470.wellnest.ui.friends;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.data.UserModels.Friend;
import com.code.wlu.cp470.wellnest.viewmodel.FriendViewModel;

import java.util.List;

/**
 * Hosts the RecyclerView of friend cards.
 * Note: Friend does NOT contain a score.
 * We fetch (username, score) from FriendViewModel using the friend's uid.
 */
public class FriendsFragment extends Fragment {
    Context context;
    private FriendViewModel viewModel;
    private RecyclerView recyclerView;

    public FriendsFragment() { /* default */ }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = view.getContext();


        recyclerView = view.findViewById(R.id.friendsListRecyclerView);
        EditText searchEditText = view.findViewById(R.id.friendSearchEditText);
        ImageButton searchSendButton = view.findViewById(R.id.friendSearchSendButton);

//        UiClickEffects.setOnClickWithPulse(searchSendButton, v ->  {
//            String email = searchEditText.getText().toString();
//            String uid = viewModel.getFriendUidByEmail(email);
//            viewModel.
//        });

        viewModel = new FriendViewModel(requireActivity().getApplication());
        List<Friend> acceptedFriends = viewModel.getAcceptedFriends();
        FriendAdapter adapter = new FriendAdapter(context, acceptedFriends, "accepted", viewModel);
        for (Friend f : acceptedFriends) {
            Log.d("FriendsFragment", "Friend: " + f.getName() + ", Score: " + f.getScore());
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }
}
