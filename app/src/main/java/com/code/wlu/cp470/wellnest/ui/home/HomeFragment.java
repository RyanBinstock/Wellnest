package com.code.wlu.cp470.wellnest.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.code.wlu.cp470.wellnest.R;

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
//        UiTextEffects.applyVerticalGradient(
//                scoreText,
//                R.color.wl_scoreText_gradientEnd,
//                R.color.wl_scoreText_gradientStart);

        // Add modular micro app cards to home fragment
        FragmentManager fm = getChildFragmentManager();
        ViewGroup container = view.findViewById(R.id.cardsContainer);

        addCard(container,
                getChildFragmentManager(),
                MicroAppCardFragment.newInstance(
                        "SnapTask", "Snappy",
                        R.drawable.microappcard_snaptask,
                        "wellnest://snaptask"));

        addCard(container,
                getChildFragmentManager(),
                MicroAppCardFragment.newInstance(
                        "ActivityJar", "Zippy",
                        R.drawable.microappcard_activityjar,
                        "wellnest://activityjar"));

        addCard(container,
                getChildFragmentManager(),
                MicroAppCardFragment.newInstance(
                        "Roamio", "Rico",
                        R.drawable.microappcard_roamio,
                        "wellnest://roamio"));

    }

    /*
    Helper function for adding modular micro app cards to home fragment
     */
    private void addCard(ViewGroup parent,
                         FragmentManager fm,
                         Fragment fragment) {
        // Create a holder for the child fragment
        FrameLayout holder = new FrameLayout(requireContext());
        int viewId = View.generateViewId();
        holder.setId(viewId);

        // Layout params for vertical stacking
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (getResources().getDisplayMetrics().density * 12); // 12dp
        holder.setLayoutParams(lp);

        parent.addView(holder);

        fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(viewId, fragment)
                .commit();
    }
}