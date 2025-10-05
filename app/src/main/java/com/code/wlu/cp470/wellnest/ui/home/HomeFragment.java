package com.code.wlu.cp470.wellnest.ui.home;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.code.wlu.cp470.wellnest.ui.effects.UiTextEffects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.code.wlu.cp470.wellnest.R;

public class HomeFragment extends Fragment {
    @Nullable @Override
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
    }
}