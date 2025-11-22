package com.code.wlu.cp470.wellnest.ui.activityjar;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.data.ActivityJarModels;

public class ActivityDetailFragment extends Fragment {

    private static final String ARG_CATEGORY_INDEX = "arg_category_index";
    private static final String ARG_ACTIVITY = "arg_activity";

    private int categoryIndex;
    private ActivityJarModels.Activity activity;

    public static ActivityDetailFragment newInstance(int categoryIndex,
                                                     ActivityJarModels.Activity activity) {
        ActivityDetailFragment fragment = new ActivityDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CATEGORY_INDEX, categoryIndex);
        args.putSerializable(ARG_ACTIVITY, activity);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryIndex = getArguments().getInt(ARG_CATEGORY_INDEX, 0);
            activity = (ActivityJarModels.Activity)
                    getArguments().getSerializable(ARG_ACTIVITY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity_detail, container, false);
        ImageView btnBackSelection = view.findViewById(R.id.btnBackSelection);
        //Back Btn
        btnBackSelection.setOnClickListener(v -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack();
        });

        ImageView imgBackground = view.findViewById(R.id.imgActivityBg);
        TextView txtPillName   = view.findViewById(R.id.txtActivityName); // the pill
        TextView txtNameValue  = view.findViewById(R.id.txtLabelName);
        TextView txtWhereValue = view.findViewById(R.id.txtLabelWhere);
        TextView txtWhenValue  = view.findViewById(R.id.txtLabelWhen);
        TextView txtDescValue  = view.findViewById(R.id.txtLabelDescription);

        // 1) Background based on category
        switch (categoryIndex) {
            case 0:
                imgBackground.setImageResource(R.drawable.activity_jar_explore_bg);
                break;
            case 1:
                imgBackground.setImageResource(R.drawable.activity_jar_nightlife_bg);
                break;
            case 2:
                imgBackground.setImageResource(R.drawable.activity_jar_play_bg);
                break;
            case 3:
                imgBackground.setImageResource(R.drawable.activity_jar_cozy_bg);
                break;
            case 4:
            default:
                imgBackground.setImageResource(R.drawable.activity_jar_culture_bg);
                break;
        }

        //Use activity data (emoji, name, where, when, description)
        if (activity != null) {
            txtPillName.setText(activity.name);
            txtNameValue.setText(activity.emoji + "  " + activity.name);
            txtWhereValue.setText(activity.where);
            txtWhenValue.setText(activity.when);
            txtDescValue.setText(activity.description);
        }

        // Back button (“Back to Categories”)
        Button btnBackToCategories = view.findViewById(R.id.btnBackToCategories);
        btnBackToCategories.setOnClickListener(v -> {
            androidx.fragment.app.FragmentManager fm =
                    requireActivity().getSupportFragmentManager();

            fm.popBackStack(null,
                    androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        });

        return view;
    }
}
