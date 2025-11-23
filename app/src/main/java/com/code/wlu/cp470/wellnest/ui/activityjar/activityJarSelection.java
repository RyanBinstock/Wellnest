package com.code.wlu.cp470.wellnest.ui.activityjar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.data.ActivityJarModels;
import com.code.wlu.cp470.wellnest.ui.components.WellnestCarouselView;
import com.code.wlu.cp470.wellnest.viewmodel.ActivityJarViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class activityJarSelection extends Fragment {

    private static final String ARG_START_INDEX = "start_index";
    private WellnestCarouselView carouselView;
    private ActivityCarouselAdapter adapter;
    private ActivityJarViewModel viewModel;
    private ProgressBar loadingProgressBar;

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

        carouselView = view.findViewById(R.id.carousel);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);

        viewModel = new ViewModelProvider(requireActivity()).get(ActivityJarViewModel.class);

        ImageView btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            FragmentManager fm = requireActivity().getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack();
            } else {
                requireActivity().finish();
            }
        });

        adapter = new ActivityCarouselAdapter(activity -> {
            ActivityInfoDialog dialog = ActivityInfoDialog.newInstance(activity);
            dialog.setOnActivityAcceptListener(() -> {
                viewModel.acceptActivity(activity);
                Toast.makeText(requireContext(), "Accepted! +50 points", Toast.LENGTH_SHORT).show();
            });
            dialog.show(getChildFragmentManager(), "ActivityInfoDialog");
        });
        carouselView.setAdapter(adapter);

        int startIndex = 0;
        if (getArguments() != null) {
            startIndex = getArguments().getInt(ARG_START_INDEX, 0);
        }

        // Observe ViewModel
        viewModel.getActivities().observe(getViewLifecycleOwner(), map -> {
            android.util.Log.d("ActivityJarSelection", "Observer: activities updated. Map is " + (map != null ? "present" : "null"));
            if (map != null) {
                List<ActivityJarModels.Activity> allActivities = new ArrayList<>();
                // Flatten the map for the carousel for now, or filter based on selection
                // The original code passed an index which seemed to map to categories
                // 0: Explore, 1: Nightlife, 2: Play, 3: Cozy, 4: Culture
                
                ActivityJarModels.Category selectedCategory = getCategoryByIndex(getArguments() != null ? getArguments().getInt(ARG_START_INDEX, 0) : 0);
                
                if (map.containsKey(selectedCategory)) {
                    allActivities.addAll(map.get(selectedCategory));
                } else {
                    // Fallback: add all if specific category empty or not found
                    for (List<ActivityJarModels.Activity> list : map.values()) {
                        allActivities.addAll(list);
                    }
                }
                
                adapter.setActivities(allActivities);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            android.util.Log.d("ActivityJarSelection", "Observer: isLoading = " + isLoading);
            loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            carouselView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                android.util.Log.e("ActivityJarSelection", "Observer: error = " + error);
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        // Load data
        viewModel.loadActivities();

        ImageView btnDice = view.findViewById(R.id.btnRandomDice);
        btnDice.setOnClickListener(v -> {
            int count = adapter.getItemCount();
            if (count == 0) return;
            int randomIndex = new java.util.Random().nextInt(count);
            
            // Scroll to the item
            carouselView.getViewPager().setCurrentItem(randomIndex, true);
            
            // Open the dialog for the random activity
            ActivityJarModels.Activity randomActivity = adapter.getItem(randomIndex);
            if (randomActivity != null) {
                ActivityInfoDialog dialog = ActivityInfoDialog.newInstance(randomActivity);
                dialog.setOnActivityAcceptListener(() -> {
                    viewModel.acceptActivity(randomActivity);
                    Toast.makeText(requireContext(), "Accepted! +50 points", Toast.LENGTH_SHORT).show();
                });
                dialog.show(getChildFragmentManager(), "ActivityInfoDialog");
            }
        });

        ImageView btnFilters = view.findViewById(R.id.btnFilters);
        btnFilters.setOnClickListener(v -> {
            FiltersBottomSheetDialog dialog = new FiltersBottomSheetDialog();
            dialog.show(getChildFragmentManager(), "filters");
        });

        return view;
    }

    private ActivityJarModels.Category getCategoryByIndex(int index) {
        switch (index) {
            case 1: return ActivityJarModels.Category.Nightlife;
            case 2: return ActivityJarModels.Category.Play;
            case 3: return ActivityJarModels.Category.Cozy;
            case 4: return ActivityJarModels.Category.Culture;
            default: return ActivityJarModels.Category.Explore;
        }
    }
}
