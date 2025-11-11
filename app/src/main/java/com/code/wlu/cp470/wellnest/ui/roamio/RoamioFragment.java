package com.code.wlu.cp470.wellnest.ui.roamio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.code.wlu.cp470.wellnest.R;

public class RoamioFragment extends Fragment {

    public void RoamioHomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_roamio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button start = view.findViewById(R.id.btnStartWalk);
        start.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_roamioHome_to_roamioWalk)
        );

        Button home = view.findViewById(R.id.btnHome);
        home.setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack(R.id.homeFragment, false)
        );
    }
}
