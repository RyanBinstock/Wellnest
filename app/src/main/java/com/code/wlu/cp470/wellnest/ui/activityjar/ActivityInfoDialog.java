package com.code.wlu.cp470.wellnest.ui.activityjar;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.data.ActivityJarModels;

public class ActivityInfoDialog extends DialogFragment {

    private static final String ARG_EMOJI = "emoji";
    private static final String ARG_TITLE = "title";
    private static final String ARG_DESCRIPTION = "description";
    private static final String ARG_ADDRESS = "address";
    private static final String ARG_TAGS = "tags";

    private OnActivityAcceptListener listener;

    public interface OnActivityAcceptListener {
        void onActivityAccepted();
    }

    public static ActivityInfoDialog newInstance(ActivityJarModels.Activity activity) {
        ActivityInfoDialog fragment = new ActivityInfoDialog();
        Bundle args = new Bundle();
        args.putString(ARG_EMOJI, activity.getEmoji());
        args.putString(ARG_TITLE, activity.getTitle());
        args.putString(ARG_DESCRIPTION, activity.getDescription());
        args.putString(ARG_ADDRESS, activity.getAddress());
        
        if (activity.getTags() != null && activity.getTags().length > 0) {
            StringBuilder tagsBuilder = new StringBuilder();
            for (String tag : activity.getTags()) {
                tagsBuilder.append("#").append(tag).append(" ");
            }
            args.putString(ARG_TAGS, tagsBuilder.toString().trim());
        }
        
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnActivityAcceptListener(OnActivityAcceptListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_activity_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView emojiTv = view.findViewById(R.id.activityEmoji);
        TextView titleTv = view.findViewById(R.id.activityTitle);
        TextView descriptionTv = view.findViewById(R.id.activityDescription);
        TextView addressTv = view.findViewById(R.id.activityAddress);
        View addressContainer = view.findViewById(R.id.addressContainer);
        TextView tagsTv = view.findViewById(R.id.activityTags);
        Button btnAccept = view.findViewById(R.id.btnAccept);

        if (getArguments() != null) {
            emojiTv.setText(getArguments().getString(ARG_EMOJI, "✨"));
            titleTv.setText(getArguments().getString(ARG_TITLE, "Activity"));
            descriptionTv.setText(getArguments().getString(ARG_DESCRIPTION, ""));
            
            String address = getArguments().getString(ARG_ADDRESS);
            if (address != null && !address.isEmpty()) {
                addressTv.setText(address);
                addressContainer.setVisibility(View.VISIBLE);
            } else {
                addressContainer.setVisibility(View.GONE);
            }

            String tags = getArguments().getString(ARG_TAGS);
            if (tags != null && !tags.isEmpty()) {
                tagsTv.setText(tags);
                tagsTv.setVisibility(View.VISIBLE);
            } else {
                tagsTv.setVisibility(View.GONE);
            }
        }

        btnAccept.setOnClickListener(v -> {
            if (listener != null) {
                listener.onActivityAccepted();
            }
            dismiss();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}