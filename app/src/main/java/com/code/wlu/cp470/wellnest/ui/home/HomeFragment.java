package com.code.wlu.cp470.wellnest.ui.home;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Guideline;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.code.wlu.cp470.wellnest.MainActivity;
import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.data.UserModels.Friend;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.ActivityJarManager;
import com.code.wlu.cp470.wellnest.data.local.managers.RoamioManager;
import com.code.wlu.cp470.wellnest.data.local.managers.SnapTaskManager;
import com.code.wlu.cp470.wellnest.data.local.managers.UserManager;
import com.code.wlu.cp470.wellnest.ui.effects.UiClickEffects;
import com.code.wlu.cp470.wellnest.viewmodel.FriendViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment {

    private FriendViewModel friendViewModel;

    // Class fields for score refresh on resume
    private TextView scoreText;
    private String uid;
    private UserManager userManager;
    private SnapTaskManager snapTaskManager;
    private ActivityJarManager activityJarManager;
    private RoamioManager roamioManager;

    /**
     * BroadcastReceiver that listens for score sync completion from MainActivity.
     * When a fresh install login happens, the global score sync runs async and this receiver
     * ensures the UI refreshes once the sync completes.
     */
    private final BroadcastReceiver scoreSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            android.util.Log.d("HomeFragment", "Received ACTION_SCORE_SYNC_COMPLETE broadcast, refreshing score");
            // Re-open database connection to get fresh synced data
            if (getActivity() != null) {
                WellnestDatabaseHelper dbHelper = new WellnestDatabaseHelper(getActivity().getApplicationContext());
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                userManager = new UserManager(db);
                snapTaskManager = new SnapTaskManager(db);
                activityJarManager = new ActivityJarManager(db);
                roamioManager = new RoamioManager(db);
                refreshGlobalScore();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Register for score sync completion broadcasts
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(scoreSyncReceiver, new IntentFilter(MainActivity.ACTION_SCORE_SYNC_COMPLETE));

        friendViewModel = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication())).get(FriendViewModel.class);

        ImageView friendsTxt = view.findViewById(R.id.friendsScoreboardTxt);
        friendsTxt.setVisibility(GONE);
        scoreText = view.findViewById(R.id.scoreText);
        ImageView bgOval = view.findViewById(R.id.bgOval);
        ImageView chevron = view.findViewById(R.id.chevron);

        WellnestDatabaseHelper dbHelper = new WellnestDatabaseHelper(getActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        userManager = new UserManager(db);
        snapTaskManager = new SnapTaskManager(db);
        activityJarManager = new ActivityJarManager(db);
        roamioManager = new RoamioManager(db);

        uid = userManager.currentUid();

        // Calculate and display the global score
        refreshGlobalScore();

        // Display streak counter
        ImageView streakIcon = view.findViewById(R.id.imageView);
        TextView streakCounter = view.findViewById(R.id.streakCounter);
        int streakCount = userManager.getStreakCount();
        streakIcon.setVisibility(View.VISIBLE);
        streakCounter.setVisibility(View.VISIBLE);
        streakCounter.setText(String.valueOf(streakCount));

        // Add modular micro app cards to home fragment
        FragmentManager fm = getChildFragmentManager();
        ViewGroup container = view.findViewById(R.id.cardsContainer);

        addCardView(container,
                "SnapTask", "Snappy",
                R.drawable.microappcard_snaptask,
                "wellnest://snaptask");

        addCardView(container,
                "ActivityJar", "Zippy",
                R.drawable.microappcard_activityjar,
                "wellnest://activityjar");

        addCardView(container,
                "Roamio", "Rico",
                R.drawable.microappcard_roamio,
                "wellnest://roamio");

        // Add navbar to bottom
        com.code.wlu.cp470.wellnest.ui.nav.NavFragment navFragment = new com.code.wlu.cp470.wellnest.ui.nav.NavFragment();
        Bundle navArgs = new Bundle();
        navArgs.putString("page", "home");
        navFragment.setArguments(navArgs);
        fm.beginTransaction()
                .replace(R.id.home_navbar_container, navFragment)
                .commit();

        FrameLayout leaderboardContainer = view.findViewById(R.id.leaderboardContainer);
        leaderboardContainer.setVisibility(GONE);

        // Inflate scoreboard layout into container
        View scoreboardView = getLayoutInflater().inflate(R.layout.view_scoreboard, leaderboardContainer, false);
        leaderboardContainer.addView(scoreboardView);

        RecyclerView scoreboardRecyclerView = scoreboardView.findViewById(R.id.scoreboard_recycler_view);
        scoreboardRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        friendViewModel.getAcceptedFriends().observe(getViewLifecycleOwner(), friends -> {
            List<Friend> sortedFriends = new ArrayList<>(friends);
            try {
                // Use the actual global score for leaderboard, not just micro-app score
                int globalScore = userManager.getGlobalScore(uid);
                Friend currentUser = new Friend(uid, "You", "accepted", globalScore);
                sortedFriends.add(currentUser);
            } catch (Exception e) {
                // Fallback if something fails
            }

            Collections.sort(sortedFriends, (f1, f2) -> Integer.compare(f2.getScore(), f1.getScore()));

            ScoreboardAdapter adapter = new ScoreboardAdapter(getContext(), sortedFriends);
            scoreboardRecyclerView.setAdapter(adapter);
        });

        Guideline ovalBottomGuide = view.findViewById(R.id.guide_scoreboard_bottom);

        // Toggle scoreboard visibility helper
        View.OnClickListener toggleScoreboard = v -> {
            boolean isExpanded = bgOval.isSelected();
            // Toggle: if expanded, collapse; if collapsed, expand
            boolean shouldExpand = !isExpanded;

            ovalBottomGuide.setGuidelinePercent(shouldExpand ? 0.7f : 0.15f);
            chevron.setRotation(shouldExpand ? 180 : 0);
            leaderboardContainer.setVisibility(shouldExpand ? VISIBLE : GONE);
            friendsTxt.setVisibility(shouldExpand ? VISIBLE : GONE);
            scoreText.setVisibility(shouldExpand ? GONE : VISIBLE);
            container.setVisibility(shouldExpand ? GONE : VISIBLE);
            streakIcon.setVisibility(shouldExpand ? GONE : VISIBLE);
            streakCounter.setVisibility(shouldExpand ? GONE : VISIBLE);
            bgOval.setSelected(shouldExpand);
        };

        // bgOval handles both expand and collapse
        bgOval.setOnClickListener(toggleScoreboard);

        // Chevron and friendsScoreboardTxt allow closing when scoreboard is expanded
        // (clicking anywhere except the actual scoreboard RecyclerView)
        chevron.setClickable(true);
        chevron.setOnClickListener(v -> {
            android.util.Log.d("HomeFragment", "Chevron clicked! bgOval.isSelected() = " + bgOval.isSelected());
            if (bgOval.isSelected()) {
                android.util.Log.d("HomeFragment", "Calling toggleScoreboard to collapse");
                toggleScoreboard.onClick(bgOval);
            } else {
                android.util.Log.d("HomeFragment", "NOT collapsing because bgOval.isSelected() is false");
            }
        });

        friendsTxt.setOnClickListener(v -> {
            if (bgOval.isSelected()) {
                toggleScoreboard.onClick(bgOval);
            }
        });
    }

    /*
    Helper function for adding modular micro app cards to home fragment
     */
    private void addCardView(ViewGroup parent,
                             String title,
                             String subtitle,
                             int bgRes,
                             String deepLink) {

        View cardView = getLayoutInflater()
                .inflate(R.layout.fragment_microapp_card, parent, false);

        TextView titleTv = cardView.findViewById(R.id.title);
        TextView subtitleTv = cardView.findViewById(R.id.subtitle);
        ImageView bg = cardView.findViewById(R.id.bgImage);
        View cardRoot = cardView.findViewById(R.id.cardRoot);

        titleTv.setText(title);
        subtitleTv.setText(subtitle);
        if (bgRes != 0) bg.setImageResource(bgRes);

        UiClickEffects.setOnClickWithPulse(cardRoot, R.raw.game_start_effect, v -> {
            if (deepLink != null && !deepLink.isEmpty()) {
                try {
                    NavController nav = Navigation.findNavController(v);
                    nav.navigate(Uri.parse(deepLink));
                } catch (Exception ignored) {
                }
            }
        });

        parent.addView(cardView);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unregister the broadcast receiver to prevent leaks
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(scoreSyncReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        android.util.Log.d("HomeFragment", "onResume() called - BEFORE refreshing database connection");

        // Re-open database connection to get fresh data
        // This is necessary because ActivityJarActivity uses a different database connection
        // and SQLite WAL mode can cause stale reads
        WellnestDatabaseHelper dbHelper = new WellnestDatabaseHelper(requireActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Reinitialize managers with fresh connection
        userManager = new UserManager(db);
        snapTaskManager = new SnapTaskManager(db);
        activityJarManager = new ActivityJarManager(db);
        roamioManager = new RoamioManager(db);

        android.util.Log.d("HomeFragment", "onResume() - AFTER refreshing database connection");
        refreshGlobalScore();
    }

    /**
     * Refreshes the global score display by recalculating from micro-app scores.
     * Called on initial load and when navigating back to the home screen.
     */
    private void refreshGlobalScore() {
        if (scoreText == null || uid == null || userManager == null ||
                snapTaskManager == null || activityJarManager == null || roamioManager == null) {
            return;
        }

        int microAppScore = (int) snapTaskManager.getSnapTaskScore()
                + (int) activityJarManager.getActivityJarScore()
                + (int) roamioManager.getRoamioScore().getScore();

        // DIAGNOSTIC: Check current global score (may contain synced remote data)
        int currentGlobalScore = userManager.getGlobalScore(uid);
        android.util.Log.d("HomeFragment", "refreshGlobalScore: Calculated micro-app score = " + microAppScore);
        android.util.Log.d("HomeFragment", "refreshGlobalScore: Current global score in DB = " + currentGlobalScore);

        // Use whichever is higher - this preserves synced remote scores
        int displayScore = Math.max(microAppScore, currentGlobalScore);
        android.util.Log.d("HomeFragment", "refreshGlobalScore: Using display score = " + displayScore);

        scoreText.setText(String.valueOf(displayScore));

        // Only update global score if micro-app score is higher than what's already there
        if (microAppScore > currentGlobalScore) {
            android.util.Log.d("HomeFragment", "refreshGlobalScore: Updating global score to " + microAppScore);
            userManager.setGlobalScore(uid, microAppScore);
        } else {
            android.util.Log.d("HomeFragment", "refreshGlobalScore: Keeping global score at " + currentGlobalScore);
        }
    }
}