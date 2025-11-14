package com.code.wlu.cp470.wellnest.ui.nav;

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

public class NavFragment extends Fragment {
    private View friendsButton;
    private View homeButton;
    private View profileButton;
    private String page;
    private NavController navController;

    public NavFragment(String page) {
        this.page = page;
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

        navController = androidx.navigation.fragment.NavHostFragment.findNavController(this);
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
//        friendsButton.setOnClickListener(v -> {
//            setActive(friendsButton);
//            setInactive(homeButton);
//            setInactive(profileButton);
//            navController.navigate(R.id.friendsFragment);
//        });
//
//        homeButton.setOnClickListener(v -> {
//            setActive(homeButton);
//            setInactive(friendsButton);
//            setInactive(profileButton);
//            navController.navigate(R.id.homeFragment);
//        });
//
//        profileButton.setOnClickListener(v -> {
//            setActive(profileButton);
//            setInactive(friendsButton);
//            setInactive(homeButton);
//            navController.navigate(R.id.profileFragment);
//        });
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
        
        // Log current dimensions before change
        Log.d("NavFragment", "Current dimensions: width=" + lp.width/d + "dp, height=" + lp.height/d + "dp");
        
        lp.width = 0;                    // use weight
        lp.height = (int) (140 * d);     // 140dp for all active buttons
        lp.weight = 1f;
        view.setLayoutParams(lp);
        
        Log.d("NavFragment", "New dimensions: width=0 (weight=1), height=140dp");

        ImageView bg = view.findViewById(R.id.nav_item_bg);
        ImageView icon = view.findViewById(R.id.nav_item_icon);
        ImageView text = view.findViewById(R.id.nav_item_home_text);

        bg.setImageResource(R.drawable.nav_card_active);
        icon.setTranslationY(-30 * d);
        
        Log.d("NavFragment", "Icon translationY set to: -30dp");
        Log.d("NavFragment", "Total space needed: 140dp + 30dp translation = 170dp");
        
        // Check parent clipping
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            Log.d("NavFragment", "Parent clipChildren: " + !parent.getClipChildren());
            Log.d("NavFragment", "Parent height: " + parent.getHeight()/d + "dp");
        }

        if (view.getId() == R.id.navbar_home_button) {
            text.setVisibility(View.VISIBLE);
            Log.d("NavFragment", "Home text visibility: VISIBLE");
        } else {
            text.setVisibility(View.GONE);
            Log.d("NavFragment", buttonName + " text visibility: GONE");
        }
        
        // Check if icon might be clipped
        view.post(() -> {
            int viewHeight = view.getHeight();
            float iconTop = icon.getY();
            Log.d("NavFragment", "After layout - View height: " + viewHeight/d + "dp");
            Log.d("NavFragment", "After layout - Icon top position: " + iconTop/d + "dp");
            if (iconTop < 0) {
                Log.w("NavFragment", "⚠️ WARNING: Icon is extending above container by " + Math.abs(iconTop/d) + "dp - WILL BE CLIPPED!");
            }
        });
    }

    private void setInactive(View view) {
        float d = getResources().getDisplayMetrics().density;
        
        // Debug logging
        String buttonName = "unknown";
        if (view.getId() == R.id.navbar_friends_button) buttonName = "friends";
        else if (view.getId() == R.id.navbar_home_button) buttonName = "home";
        else if (view.getId() == R.id.navbar_profile_button) buttonName = "profile";
        
        Log.d("NavFragment", "--- Deactivating " + buttonName + " button ---");

        LinearLayout.LayoutParams lp =
                (LinearLayout.LayoutParams) view.getLayoutParams();
        lp.width = (int) (130 * d);      // 130dp
        lp.height = (int) (120 * d);     // 120dp for all inactive buttons
        lp.weight = 0f;
        view.setLayoutParams(lp);
        
        Log.d("NavFragment", "Set to inactive: width=130dp, height=120dp");

        ImageView bg = view.findViewById(R.id.nav_item_bg);
        ImageView icon = view.findViewById(R.id.nav_item_icon);
        ImageView text = view.findViewById(R.id.nav_item_home_text);

        text.setVisibility(View.GONE);
        bg.setImageResource(R.drawable.nav_card_inactive);
        icon.setTranslationY(0);
        
        Log.d("NavFragment", "Icon translationY reset to: 0dp");
    }
}
