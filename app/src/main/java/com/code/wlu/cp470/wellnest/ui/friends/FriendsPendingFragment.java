package com.code.wlu.cp470.wellnest.ui.friends;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.ui.effects.UiClickEffects;
import com.code.wlu.cp470.wellnest.viewmodel.FriendViewModel;

import java.util.List;

public class FriendsPendingFragment extends Fragment {
    Context context;
    private FriendViewModel viewModel;
    private RecyclerView recyclerView;

    public FriendsPendingFragment() { /* default */ }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_friends_pending, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = view.getContext();

        recyclerView = view.findViewById(R.id.pendingFriendsListRecyclerView);
        ImageButton backButton = view.findViewById(R.id.backButton);

        viewModel = new FriendViewModel(requireActivity().getApplication());
        FriendAdapter adapter = new FriendAdapter(context, List.of(), "pending", viewModel);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        viewModel.getPendingFriends().observe(getViewLifecycleOwner(), adapter::updateData);

        UiClickEffects.setOnClickWithPulse(backButton, R.raw.ui_click_effect, v -> {
            Navigation.findNavController(v).navigateUp();
        });
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
