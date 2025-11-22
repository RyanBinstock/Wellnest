package com.code.wlu.cp470.wellnest.ui.activityjar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.data.ActivityJarModels;

import java.util.List;
import java.util.Random;

public class activityJarSelection extends Fragment {

    private static final String ARG_CATEGORY_INDEX = "arg_category_index";

    private int categoryIndex = 0;

    public static activityJarSelection newInstance(int categoryIndex) {
        activityJarSelection fragment = new activityJarSelection();
        Bundle args = new Bundle();
        args.putInt(ARG_CATEGORY_INDEX, categoryIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryIndex = getArguments().getInt(ARG_CATEGORY_INDEX, 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_activity_jar_selection, container, false);

        ViewPager2 viewPager = view.findViewById(R.id.vpActivities);

        List<ActivityJarModels.Activity> activities =
                ActivityJarModels.getActivitiesForCategory(categoryIndex);

        ActivitiesPagerAdapter adapter = new ActivitiesPagerAdapter(
                requireContext(),
                activities,
                this::openDetailForActivity
        );

        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3);

        // Dice button
        ImageView btnRandomDice = view.findViewById(R.id.btnRandomDice);
        btnRandomDice.setOnClickListener(v -> {
            if (!activities.isEmpty()) {
                int index = new Random().nextInt(activities.size());
                viewPager.setCurrentItem(index, true);
                openDetailForActivity(activities.get(index));
            }
        });

        // Filters button
        ImageView btnFilters = view.findViewById(R.id.btnFilters);
        btnFilters.setOnClickListener(v -> {
            FiltersBottomSheetDialog dialog = new FiltersBottomSheetDialog();
            dialog.show(getChildFragmentManager(), "filters");
        });

        // Back arrow
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        return view;
    }

    private void openDetailForActivity(ActivityJarModels.Activity activity) {
        ActivityDetailFragment fragment =
                ActivityDetailFragment.newInstance(categoryIndex, activity);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.activity_jar_root, fragment)
                .addToBackStack(null)
                .commit();
    }
}
