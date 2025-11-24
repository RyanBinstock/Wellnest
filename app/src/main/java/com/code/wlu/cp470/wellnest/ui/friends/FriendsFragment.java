package com.code.wlu.cp470.wellnest.ui.friends;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.ui.effects.UiClickEffects;
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
    private TextView pendingFriendsNotificationNumber;

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
        ImageButton pendingFriendsFragmentButton = view.findViewById(R.id.pendingFriendsFragmentButton);
        pendingFriendsNotificationNumber = view.findViewById(R.id.pendingFriendsNotificationNumber);


        viewModel = new FriendViewModel(requireActivity().getApplication());
        FriendAdapter adapter = new FriendAdapter(context, List.of(), "accepted", viewModel);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        viewModel.getAcceptedFriends().observe(getViewLifecycleOwner(), adapter::updateData);

        viewModel.getPendingFriends().observe(getViewLifecycleOwner(), pendingFriends -> {
            if (pendingFriends != null && !pendingFriends.isEmpty()) {
                pendingFriendsNotificationNumber.setVisibility(View.VISIBLE);
                pendingFriendsNotificationNumber.setText(String.valueOf(pendingFriends.size()));
            } else {
                pendingFriendsNotificationNumber.setVisibility(View.GONE);
            }
        });

        UiClickEffects.setOnClickWithPulse(searchSendButton, R.raw.message_effect, v -> {
            String email = searchEditText.getText().toString();
            viewModel.addFriend(email);

            searchEditText.setText("");
        });

        UiClickEffects.setOnClickWithPulse(pendingFriendsFragmentButton, R.raw.ui_click_effect, v -> {
            Navigation.findNavController(v).navigate(R.id.action_friends_to_pending);
        });

        FragmentManager fm = getChildFragmentManager();
        com.code.wlu.cp470.wellnest.ui.nav.NavFragment navFragment = new com.code.wlu.cp470.wellnest.ui.nav.NavFragment();
        Bundle navArgs = new Bundle();
        navArgs.putString("page", "friends");
        navFragment.setArguments(navArgs);
        fm.beginTransaction()
                .replace(R.id.friends_navbar_container, navFragment)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Sync friends from Firebase when fragment becomes visible
        if (viewModel != null) {
            viewModel.syncAndRefreshFriends();
        }
    }
}
