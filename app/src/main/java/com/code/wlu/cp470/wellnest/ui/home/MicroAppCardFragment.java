package com.code.wlu.cp470.wellnest.ui.home;


import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.ui.effects.UiClickEffects;

public class MicroAppCardFragment extends Fragment {
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_SUBTITLE = "arg_subtitle";
    private static final String ARG_BG_RES = "arg_bg_res";
    private static final String ARG_DEEPLINK = "arg_deeplink";

    public static MicroAppCardFragment newInstance(String title, String subtitle,
                                                   @DrawableRes int backgroundResId,
                                                   String deepLinkUri) {
        MicroAppCardFragment f = new MicroAppCardFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TITLE, title);
        b.putString(ARG_SUBTITLE, subtitle);
        b.putInt(ARG_BG_RES, backgroundResId);
        b.putString(ARG_DEEPLINK, deepLinkUri);
        f.setArguments(b);
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_microapp_card, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, Bundle savedInstanceState) {
        Bundle args = getArguments() != null ? getArguments() : Bundle.EMPTY;

        String title = args.getString(ARG_TITLE, "");
        String subtitle = args.getString(ARG_SUBTITLE, "");
        int bgRes = args.getInt(ARG_BG_RES, 0);
        String deepLink = args.getString(ARG_DEEPLINK, null);

        TextView titleTv = v.findViewById(R.id.title);
        TextView subtitleTv = v.findViewById(R.id.subtitle);
        ImageView bg = v.findViewById(R.id.bgImage);
        View card = v.findViewById(R.id.cardRoot);

        titleTv.setText(title);
        subtitleTv.setText(subtitle);
        if (bgRes != 0) bg.setImageResource(bgRes);

        UiClickEffects.setOnClickWithPulse(card, R.raw.game_start_effect, view -> {
            if (deepLink != null && !deepLink.isEmpty()) {
                try {
                    NavController nav = Navigation.findNavController(view);
                    nav.navigate(Uri.parse(deepLink));
                } catch (Exception e) {
                    // optional: log or toast
                }
            }
        });
    }
}

