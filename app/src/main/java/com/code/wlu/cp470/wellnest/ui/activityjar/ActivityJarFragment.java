package com.code.wlu.cp470.wellnest.ui.activityjar;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.code.wlu.cp470.wellnest.R;

public class ActivityJarFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate original layout to avoid ID dependency issues; real UI is moved to Activity
        return inflater.inflate(R.layout.activity_jar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Launch Activity-based implementation and remove this fragment from back stack
        Intent intent = new Intent(requireContext(), ActivityJarActivity.class);
        startActivity(intent);
        requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        NavHostFragment.findNavController(this).navigateUp();
    }
}
