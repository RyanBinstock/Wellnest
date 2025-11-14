package com.code.wlu.cp470.wellnest.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.ui.effects.UiClickEffects;
import com.code.wlu.cp470.wellnest.viewmodel.AuthViewModel;
import com.code.wlu.cp470.wellnest.viewmodel.ProfileViewModel;

public class ProfileFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton signout = view.findViewById(R.id.sign_out_account_button);
        ImageButton delete = view.findViewById(R.id.delete_account_button);
        TextView name = view.findViewById(R.id.profile_name);
        TextView email = view.findViewById(R.id.profile_email);

        ViewModelProvider.Factory profileFactory =
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication());
        ProfileViewModel profileViewModel = new ViewModelProvider(this, profileFactory).get(ProfileViewModel.class);

        ViewModelProvider.Factory authFactory =
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication());
        AuthViewModel authViewModel = new ViewModelProvider(this, authFactory).get(AuthViewModel.class);

        name.setText(profileViewModel.getName());
        email.setText(profileViewModel.getEmail());


        UiClickEffects.setOnClickWithPulse(signout, 0, v -> {
            authViewModel.signOut();
            NavController navController =
                    NavHostFragment.findNavController(ProfileFragment.this);
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)   // inclusive = clear back stack
                    .build();
            navController.navigate(R.id.authFragment, null, navOptions);
        });
        UiClickEffects.setOnClickWithPulse(delete, 0, v -> {
            authViewModel.deleteAccount();
            NavController navController =
                    NavHostFragment.findNavController(ProfileFragment.this);
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)   // inclusive = clear back stack
                    .build();
            navController.navigate(R.id.authFragment, null, navOptions);
        });
        FragmentManager fm = getChildFragmentManager();
        fm.beginTransaction()
                .replace(R.id.profile_navbar_container, new com.code.wlu.cp470.wellnest.ui.nav.NavFragment("profile"))
                .commit();
    }


}
