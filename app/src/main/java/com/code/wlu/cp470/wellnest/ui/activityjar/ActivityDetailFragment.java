package com.code.wlu.cp470.wellnest.ui.activityjar;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.code.wlu.cp470.wellnest.R;

public class ActivityDetailFragment extends Fragment {

    private static final String ARG_INDEX = "arg_activity_index";

    // Use this to create the fragment for a given card index
    public static ActivityDetailFragment newInstance(int index) {
        ActivityDetailFragment f = new ActivityDetailFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_INDEX, index);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_activity_detail, container, false);

        ImageView imgBg   = view.findViewById(R.id.imgActivityBg);
        TextView  txtName = view.findViewById(R.id.txtActivityName);

        int index = 0;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_INDEX, 0);
        }
        int safeIndex = Math.min(Math.max(index, 0), 4);

        // Pick the correct background + title based on index/order of cards
        switch (index) {
            case 0: // Explore
                imgBg.setImageResource(R.drawable.activity_jar_explore_bg);
                txtName.setText("Explore");
                break;
            case 1: // Nightlife
                imgBg.setImageResource(R.drawable.activity_jar_nightlife_bg);
                txtName.setText("Nightlife");
                break;
            case 2: // Play
                imgBg.setImageResource(R.drawable.activity_jar_play_bg);
                txtName.setText("Play");
                break;
            case 3: // Cozy
                imgBg.setImageResource(R.drawable.activity_jar_cozy_bg);
                txtName.setText("Cozy");
                break;
            case 4: // Culture
            default:
                imgBg.setImageResource(R.drawable.activity_jar_culture_bg);
                txtName.setText("Culture");
                break;
        }

        TextView txtTitle = view.findViewById(R.id.txtActivityName);

        int[] pillColorRes = new int[] {
                R.color.pill_explore,
                R.color.pill_nightlife,
                R.color.pill_play,
                R.color.pill_cozy,
                R.color.pill_culture
        };

        int pillColor = ContextCompat.getColor(
                requireContext(),
                pillColorRes[safeIndex]
        );

        Drawable bg = txtTitle.getBackground().mutate();
        if (bg instanceof GradientDrawable) {
            ((GradientDrawable) bg).setColor(pillColor);
        }

        // Back button on bottom (“Back to Categories”)
        view.findViewById(R.id.btnBackToCategories).setOnClickListener(v ->
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack()
        );

        return view;
    }
}
