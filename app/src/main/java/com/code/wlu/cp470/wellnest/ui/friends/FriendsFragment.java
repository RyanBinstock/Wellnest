package com.code.wlu.cp470.wellnest.ui.friends;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
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


        viewModel = new FriendViewModel(requireActivity().getApplication());
        FriendAdapter adapter = new FriendAdapter(context, List.of(), "accepted", viewModel);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        viewModel.getAcceptedFriends().observe(getViewLifecycleOwner(), adapter::updateData);

        UiClickEffects.setOnClickWithPulse(searchSendButton, R.raw.message_effect, v -> {
            String email = searchEditText.getText().toString();
            viewModel.addFriend(email);

            searchEditText.setText("");
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
}
