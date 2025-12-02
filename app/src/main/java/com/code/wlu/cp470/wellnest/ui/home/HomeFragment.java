package com.code.wlu.cp470.wellnest.ui.home;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

        friendViewModel = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication())).get(FriendViewModel.class);

        ImageView friendsTxt = view.findViewById(R.id.friendsScoreboardTxt);
        friendsTxt.setVisibility(GONE);
        TextView scoreText = view.findViewById(R.id.scoreText);
        ImageView bgOval = view.findViewById(R.id.bgOval);
        ImageView chevron = view.findViewById(R.id.chevron);

        WellnestDatabaseHelper dbHelper = new WellnestDatabaseHelper(getActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        UserManager userManager = new UserManager(db);
        SnapTaskManager snapTaskManager = new SnapTaskManager(db);
        ActivityJarManager activityJarManager = new ActivityJarManager(db);
        RoamioManager roamioManager = new RoamioManager(db);

        int score = (int) snapTaskManager.getSnapTaskScore() + (int) activityJarManager.getActivityJarScore() + (int) roamioManager.getRoamioScore().getScore();
        String uid = userManager.currentUid();
        scoreText.setText(String.valueOf(score));
        userManager.setGlobalScore(uid, score);

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
                Friend currentUser = new Friend(uid, "You", "accepted", score);
                sortedFriends.add(currentUser);
            } catch (Exception e) {
                // Fallback if something fails
            }

            Collections.sort(sortedFriends, (f1, f2) -> Integer.compare(f2.getScore(), f1.getScore()));

            ScoreboardAdapter adapter = new ScoreboardAdapter(getContext(), sortedFriends);
            scoreboardRecyclerView.setAdapter(adapter);
        });

        Guideline ovalBottomGuide = view.findViewById(R.id.guide_scoreboard_bottom);
        bgOval.setOnClickListener(v -> {
            boolean selected = v.isSelected();
            ovalBottomGuide.setGuidelinePercent(selected ? 0.15f : 0.7f);
            chevron.setRotation(selected ? 0 : 180);
            leaderboardContainer.setVisibility(selected ? GONE : VISIBLE);
            friendsTxt.setVisibility(selected ? GONE : VISIBLE);
            scoreText.setVisibility(selected ? VISIBLE : GONE);
            container.setVisibility(selected ? VISIBLE : GONE);
            streakIcon.setVisibility(selected ? VISIBLE : GONE);
            streakCounter.setVisibility(selected ? VISIBLE : GONE);
            v.setSelected(!selected);
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
}