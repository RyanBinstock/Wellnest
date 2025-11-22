package com.code.wlu.cp470.wellnest.ui.home;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.ui.effects.UiClickEffects;

public class HomeFragment extends Fragment {
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

        TextView scoreText = view.findViewById(R.id.scoreText);

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