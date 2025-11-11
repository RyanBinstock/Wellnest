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

public class RoamioCompleteFragment extends Fragment {

    public RoamioCompleteFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_roamio_complete, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button back = view.findViewById(R.id.btnComeBack);
        back.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_roamioComplete_to_home)
        );
    }
}
