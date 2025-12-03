package com.code.wlu.cp470.wellnest.ui.nav;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.ui.effects.UiClickEffects;
import com.code.wlu.cp470.wellnest.ui.effects.UiTouchEffects;

public class NavFragment extends Fragment {
    private View friendsButton;
    private View homeButton;
    private View profileButton;
    private String page;
    private NavController navController;

    // Required empty public constructor
    public NavFragment() {
    }

    // Convenience constructor for backward compatibility
    // NOTE: This constructor should be avoided as it won't survive fragment recreation
    @Deprecated
    public NavFragment(String page) {
        Bundle args = new Bundle();
        args.putString("page", page);
        setArguments(args);
        Log.w("NavFragment", "Using deprecated constructor with page=" + page +
                ". Consider using no-arg constructor with setArguments() instead.");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            page = args.getString("page", "home");
        } else {
            page = "home";
        }
        Log.d("NavFragment", "onCreate: page=" + page + ", args=" + args);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_navbar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        friendsButton = view.findViewById(R.id.navbar_friends_button);
        homeButton = view.findViewById(R.id.navbar_home_button);
        profileButton = view.findViewById(R.id.navbar_profile_button);

        // set icons
        ImageView friendsIcon = friendsButton.findViewById(R.id.nav_item_icon);
        ImageView homeIcon = homeButton.findViewById(R.id.nav_item_icon);
        ImageView profileIcon = profileButton.findViewById(R.id.nav_item_icon);
        friendsIcon.setImageResource(R.drawable.ic_bottom_friends);
        homeIcon.setImageResource(R.drawable.ic_bottom_home);
        profileIcon.setImageResource(R.drawable.ic_bottom_profile);

        // init state
        switch (page) {
            case "friends":
                setActive(friendsButton);
                setInactive(homeButton);
                setInactive(profileButton);
                break;
            case "home":
                setActive(homeButton);
                setInactive(friendsButton);
                setInactive(profileButton);
                break;
            case "profile":
                setActive(profileButton);
                setInactive(friendsButton);
                setInactive(homeButton);
                break;
        }

        try {
            navController = androidx.navigation.fragment.NavHostFragment.findNavController(this);
        } catch (IllegalStateException e) {
            Log.e("NavFragment", "Failed to find NavController. This is expected in unit tests if not mocked.", e);
        }
        
        UiTouchEffects.attachPressScale(friendsButton, 0.9F);
        UiTouchEffects.attachPressScale(homeButton, 0.9F);
        UiTouchEffects.attachPressScale(profileButton, 0.9F);


        UiClickEffects.setOnClickWithPulse(friendsButton, R.raw.ui_click_effect, v -> {
            setActive(friendsButton);
            setInactive(homeButton);
            setInactive(profileButton);
            navController.navigate(R.id.friendsFragment);
        });
        UiClickEffects.setOnClickWithPulse(homeButton, R.raw.ui_click_effect, v -> {
            setActive(homeButton);
            setInactive(friendsButton);
            setInactive(profileButton);
            navController.navigate(R.id.homeFragment);
        });
        UiClickEffects.setOnClickWithPulse(profileButton, R.raw.ui_click_effect, v -> {
            setActive(profileButton);
            setInactive(friendsButton);
            setInactive(homeButton);
            navController.navigate(R.id.profileFragment);
        });
    }

    private void setActive(View view) {
        float d = getResources().getDisplayMetrics().density;

        // Debug logging
        String buttonName = "unknown";
        if (view.getId() == R.id.navbar_friends_button) buttonName = "friends";
        else if (view.getId() == R.id.navbar_home_button) buttonName = "home";
        else if (view.getId() == R.id.navbar_profile_button) buttonName = "profile";

        Log.d("NavFragment", "=== ACTIVATING " + buttonName.toUpperCase() + " BUTTON ===");

        LinearLayout.LayoutParams lp =
                (LinearLayout.LayoutParams) view.getLayoutParams();

        lp.width = 0;
        lp.weight = 0.4f;
        lp.height = WRAP_CONTENT;
        view.setLayoutParams(lp);

        ImageView bg = view.findViewById(R.id.nav_item_bg);
        ImageView icon = view.findViewById(R.id.nav_item_icon);
        ImageView text = view.findViewById(R.id.nav_item_home_text);

        bg.setImageResource(R.drawable.nav_card_active);
        icon.setTranslationY(-120);


        if (view.getId() == R.id.navbar_home_button) {
            text.setVisibility(View.VISIBLE);
        } else {
            text.setVisibility(View.GONE);
        }
    }

    private void setInactive(View view) {
        float d = getResources().getDisplayMetrics().density;

        // Debug logging
        String buttonName = "unknown";
        if (view.getId() == R.id.navbar_friends_button) buttonName = "friends";
        else if (view.getId() == R.id.navbar_home_button) buttonName = "home";
        else if (view.getId() == R.id.navbar_profile_button) buttonName = "profile";

        LinearLayout.LayoutParams lp =
                (LinearLayout.LayoutParams) view.getLayoutParams();
        lp.width = 0;
        lp.weight = 0.3f;
        lp.height = WRAP_CONTENT;
        view.setLayoutParams(lp);

        ImageView bg = view.findViewById(R.id.nav_item_bg);
        ImageView icon = view.findViewById(R.id.nav_item_icon);
        ImageView text = view.findViewById(R.id.nav_item_home_text);

        text.setVisibility(View.GONE);
        bg.setImageResource(R.drawable.nav_card_inactive);
        icon.setTranslationY(-10);
    }
}
