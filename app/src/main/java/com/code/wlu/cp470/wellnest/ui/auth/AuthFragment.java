package com.code.wlu.cp470.wellnest.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.code.wlu.cp470.wellnest.R;
import com.code.wlu.cp470.wellnest.ui.effects.UiClickEffects;
import com.code.wlu.cp470.wellnest.ui.effects.UiTouchEffects;
import com.code.wlu.cp470.wellnest.viewmodel.AuthViewModel;

public class AuthFragment extends Fragment {
    private boolean signUpMode = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView titleText = view.findViewById(R.id.titleText);
        TextView nameText = view.findViewById(R.id.nameText);
        EditText nameForm = view.findViewById(R.id.nameForm);
        TextView emailText = view.findViewById(R.id.emailText);
        EditText emailForm = view.findViewById(R.id.emailForm);
        TextView passwordText = view.findViewById(R.id.passwordText);
        EditText passwordForm = view.findViewById(R.id.passwordForm);
        Button primaryButton = view.findViewById(R.id.primaryButton);
        TextView switchText = view.findViewById(R.id.authSwitchText);
        TextView switchLink = view.findViewById(R.id.authSwitchLink);
        ImageView birdHouse = view.findViewById(R.id.birdHouse);
        ImageView birdFeeder = view.findViewById(R.id.birdFeeder);

        // Init ViewModel
        ViewModelProvider.Factory factory =
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication());
        AuthViewModel vm = new ViewModelProvider(this, factory).get(AuthViewModel.class);

        // Observe results
        vm.user().observe(getViewLifecycleOwner(), u -> {
            if (u != null) {
                Navigation.findNavController(view).navigate(R.id.action_auth_to_home);
            }
        });

        vm.error().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });

        vm.loading().observe(getViewLifecycleOwner(), isLoading -> {
            primaryButton.setEnabled(!Boolean.TRUE.equals(isLoading));
            switchLink.setEnabled(!Boolean.TRUE.equals(isLoading));
        });

        // 1) Initial mode from Safe Args (default "login" in nav_graph)
        String startMode = getString(R.string.auth_mode_login);
        if (getArguments() != null) {
            startMode = getArguments().getString("startMode", getString(R.string.auth_mode_login));
        }
        signUpMode = getString(R.string.auth_mode_signup).equalsIgnoreCase(startMode);
        applyMode(signUpMode, titleText, nameText, nameForm, primaryButton, switchText, switchLink);

        // 2) Toggle
        switchLink.setOnClickListener(v -> {
            signUpMode = !signUpMode;
            applyMode(signUpMode, titleText, nameText, nameForm, primaryButton, switchText, switchLink);
        });

        // 3) Primary
        UiTouchEffects.attachPressScale(primaryButton, 0.96f);
        UiClickEffects.setOnClickWithPulse(primaryButton, v -> {
            String email = emailForm.getText().toString().trim();
            String pass = passwordForm.getText().toString();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
                Toast.makeText(requireContext(), getString(R.string.auth_error_email_password_required), Toast.LENGTH_SHORT).show();
                return;
            }

            if (signUpMode) {
                String name = nameForm.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(requireContext(), getString(R.string.auth_error_name_required), Toast.LENGTH_SHORT).show();
                    return;
                }
                vm.signUp(name, email, pass);
            } else {
                vm.signIn(email, pass);
            }
        });
    }

    private void applyMode(boolean isSignUp,
                           TextView titleText,
                           TextView nameText,
                           EditText nameForm,
                           Button primaryButton,
                           TextView switchText,
                           TextView switchLink) {
        if (isSignUp) {
            titleText.setText(R.string.auth_title_sign_up);
            nameText.setVisibility(View.VISIBLE);
            nameForm.setVisibility(View.VISIBLE);
            primaryButton.setText(R.string.auth_cta_sign_up);
            switchText.setText(R.string.auth_switch_prompt_have_account);
            switchLink.setText(R.string.auth_switch_action_log_in);
        } else {
            titleText.setText(R.string.auth_title_log_in);
            nameText.setVisibility(View.GONE);
            nameForm.setVisibility(View.GONE);
            primaryButton.setText(R.string.auth_cta_log_in);
            switchText.setText(R.string.auth_switch_prompt_no_account);
            switchLink.setText(R.string.auth_switch_action_sign_up);
        }
    }
}
