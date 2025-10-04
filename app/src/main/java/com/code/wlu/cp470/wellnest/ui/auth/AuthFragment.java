package com.code.wlu.cp470.wellnest.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.code.wlu.cp470.wellnest.R;

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

        TextView titleText     = view.findViewById(R.id.titleText);
        TextView nameText      = view.findViewById(R.id.nameText);
        EditText nameForm      = view.findViewById(R.id.nameForm);
        TextView emailText     = view.findViewById(R.id.emailText);
        EditText emailForm     = view.findViewById(R.id.emailForm);
        TextView passwordText  = view.findViewById(R.id.passwordText);
        EditText passwordForm  = view.findViewById(R.id.passwordForm);
        Button primaryButton   = view.findViewById(R.id.primaryButton);
        TextView switchText    = view.findViewById(R.id.authSwitchText);
        TextView switchLink    = view.findViewById(R.id.authSwitchLink);

        // 1) Initial mode from Safe Args (default "login" in nav_graph)
        String startMode = "login";
        if (getArguments() != null) {
            // If you enabled Safe Args (you did), use the generated Args class:
            // startMode = AuthFragmentArgs.fromBundle(getArguments()).getStartMode();
            // If not, fallback:
            startMode = getArguments().getString("startMode", "login");
        }
        signUpMode = "signup".equalsIgnoreCase(startMode);
        applyMode(signUpMode, titleText, nameText, nameForm, primaryButton, switchText, switchLink);

        // 2) Toggle on click
        switchLink.setOnClickListener(v -> {
            signUpMode = !signUpMode;
            applyMode(signUpMode, titleText, nameText, nameForm, primaryButton, switchText, switchLink);
        });

        // 3) Primary button (hook your AuthViewModel here)
        primaryButton.setOnClickListener(v -> {
            String email = emailForm.getText().toString().trim();
            String pass  = passwordForm.getText().toString();

            if (signUpMode) {
                String name = nameForm.getText().toString().trim();
                // TODO: validate + vm.signUp(name, email, pass)
            } else {
                // TODO: validate + vm.signIn(email, pass)
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
            titleText.setText("Sign Up");
            nameText.setVisibility(View.VISIBLE);
            nameForm.setVisibility(View.VISIBLE);
            primaryButton.setText("Sign Up");
            switchText.setText("Already have an account?");
            switchLink.setText("Log in");
        } else {
            titleText.setText("Log in");
            nameText.setVisibility(View.GONE);   // GONE so layout collapses
            nameForm.setVisibility(View.GONE);
            primaryButton.setText("Log in");
            switchText.setText("Don't have an account?");
            switchLink.setText("Sign up");
        }
    }
}
