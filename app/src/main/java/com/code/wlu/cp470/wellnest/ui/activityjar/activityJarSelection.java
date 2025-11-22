package com.code.wlu.cp470.wellnest.ui.activityjar;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.code.wlu.cp470.wellnest.R;

import java.util.Arrays;
import java.util.List;

public class activityJarSelection extends Fragment {

    private ViewPager2 viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_activity_jar_selection, container, false);

        viewPager = view.findViewById(R.id.vpActivities);

        // List of the 5 card drawables
        List<Integer> cards = Arrays.asList(
                R.drawable.card_explore,
                R.drawable.card_nightlife,
                R.drawable.card_play,
                R.drawable.card_cozy,
                R.drawable.card_culture
        );

        ActivitiesPagerAdapter adapter =
                new ActivitiesPagerAdapter(requireContext(), cards);
        viewPager.setAdapter(adapter);

        viewPager.setOffscreenPageLimit(3);


        return view;
    }
}
