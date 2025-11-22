package com.code.wlu.cp470.wellnest.ui.activityjar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.code.wlu.cp470.wellnest.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class FiltersBottomSheetDialog extends BottomSheetDialogFragment {

    private enum Filter { FRIENDS, SOLO, FAMILY }

    private Filter current = Filter.SOLO;  // default

    private ImageView iconFriends, iconSolo, iconFamily;
    private TextView txtDescription;

    public FiltersBottomSheetDialog() {
        // required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.bottom_sheet_filters, container, false);

        LinearLayout optionFriends = view.findViewById(R.id.filterFriends);
        LinearLayout optionSolo = view.findViewById(R.id.filterSolo);
        LinearLayout optionFamily = view.findViewById(R.id.filterFamily);

        iconFriends = view.findViewById(R.id.iconFriends);
        iconSolo = view.findViewById(R.id.iconSolo);
        iconFamily = view.findViewById(R.id.iconFamily);

        txtDescription = view.findViewById(R.id.txtFilterDescription);

        optionFriends.setOnClickListener(v -> {
            current = Filter.FRIENDS;
            updateUi();
        });

        optionSolo.setOnClickListener(v -> {
            current = Filter.SOLO;
            updateUi();
        });

        optionFamily.setOnClickListener(v -> {
            current = Filter.FAMILY;
            updateUi();
        });

        view.findViewById(R.id.btnApplyFilters).setOnClickListener(v -> {
            // TODO: send selection back to parent via interface / ViewModel if needed
            dismiss();
        });

        updateUi();
        return view;
    }

    private void updateUi() {
        // NOTE: following your request: GREY when selected, BLUE when not.
        // Swap the drawables if you decide the opposite looks better.

        switch (current) {
            case FRIENDS:
                iconFriends.setImageResource(R.drawable.ic_filter_friends_grey);
                iconSolo.setImageResource(R.drawable.ic_filter_solo_blue);
                iconFamily.setImageResource(R.drawable.ic_filter_family_blue);
                txtDescription.setText("Activities that work well with friends.");
                break;

            case SOLO:
                iconFriends.setImageResource(R.drawable.ic_filter_friends_blue);
                iconSolo.setImageResource(R.drawable.ic_filter_solo_grey);
                iconFamily.setImageResource(R.drawable.ic_filter_family_blue);
                txtDescription.setText("Activities that are great to do on your own.");
                break;

            case FAMILY:
                iconFriends.setImageResource(R.drawable.ic_filter_friends_blue);
                iconSolo.setImageResource(R.drawable.ic_filter_solo_blue);
                iconFamily.setImageResource(R.drawable.ic_filter_family_grey);
                txtDescription.setText("Activities that are fun for the whole family.");
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog != null) {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }
}
