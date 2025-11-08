package com.code.wlu.cp470.wellnest.ui.splash;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.code.wlu.cp470.wellnest.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashFragment extends Fragment {

    private final FirebaseAuth.AuthStateListener listener = firebaseAuth -> {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        NavController nav = NavHostFragment.findNavController(this);
        if (user != null) {
            // Session persisted → go straight to Home
            nav.navigate(R.id.action_splash_to_home);
        } else {
            // No session → go to Welcome (then Auth)
            nav.navigate(R.id.action_splash_to_welcome);
        }
    };
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // You can inflate a minimal layout with just a logo/progress, or return an empty View
        return new View(inflater.getContext());
    }

    @Override
    public void onStart() {
        super.onStart();
        auth = FirebaseAuth.getInstance();
        auth.addAuthStateListener(listener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (auth != null) auth.removeAuthStateListener(listener);
    }
}
