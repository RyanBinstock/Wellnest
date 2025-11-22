package com.code.wlu.cp470.wellnest.ui.activityjar;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.code.wlu.cp470.wellnest.R;

import java.util.Arrays;
import java.util.List;

public class activityJarSelection extends Fragment {

    private ViewPager2 viewPager;
    private ActivitiesPagerAdapter adapter;
    private static final String ARG_START_INDEX = "start_index";
    public static activityJarSelection newInstance(int startIndex) {
        activityJarSelection fragment = new activityJarSelection();
        Bundle args = new Bundle();
        args.putInt(ARG_START_INDEX, startIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_activity_jar_selection, container, false);

        viewPager = view.findViewById(R.id.vpActivities);

        ImageView btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            FragmentManager fm = requireActivity().getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack();
            } else {
                requireActivity().finish();
            }
        });

        List<Integer> cards = Arrays.asList(
                R.drawable.card_explore,
                R.drawable.card_nightlife,
                R.drawable.card_play,
                R.drawable.card_cozy,
                R.drawable.card_culture
        );

        adapter = new ActivitiesPagerAdapter(
                        requireContext(),
                        cards,
                        position -> {
                            openActivityDetail(position);
                        });
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3);

        int startIndex = 0;
        if (getArguments() != null) {
            startIndex = getArguments().getInt(ARG_START_INDEX, 0);
        }

        if (startIndex < 0) startIndex = 0;
        if (startIndex >= adapter.getItemCount()) {
            startIndex = adapter.getItemCount() - 1;
        }

        viewPager.setCurrentItem(startIndex, false);

        ImageView btnDice = view.findViewById(R.id.btnRandomDice);
        btnDice.setOnClickListener(v -> {
            int count = adapter.getItemCount();
            if (count == 0) return;
            int randomIndex = new java.util.Random().nextInt(count);
            viewPager.setCurrentItem(randomIndex, true);
            openActivityDetail(randomIndex);
        });

        ImageView btnFilters = view.findViewById(R.id.btnFilters);
        btnFilters.setOnClickListener(v -> {
            FiltersBottomSheetDialog dialog = new FiltersBottomSheetDialog();
            dialog.show(getChildFragmentManager(), "filters");
        });

        return view;
    }

    private void openActivityDetail(int index){
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                )
                .replace(R.id.activity_jar_root,
                        ActivityDetailFragment.newInstance(index))
                .addToBackStack(null)
                .commit();
    }
}
