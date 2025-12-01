package com.code.wlu.cp470.wellnest.ui.profile;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.view.ViewCompat;
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
        ImageButton help = view.findViewById(R.id.profile_help_button);
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
            // Show password confirmation dialog
            showDeleteAccountDialog(authViewModel);
        });
        if (help != null) {
            UiClickEffects.applyPulse(help);
            help.setOnClickListener(v -> showHelpDialog());
        }
        FragmentManager fm = getChildFragmentManager();
        com.code.wlu.cp470.wellnest.ui.nav.NavFragment navFragment = new com.code.wlu.cp470.wellnest.ui.nav.NavFragment();
        Bundle navArgs = new Bundle();
        navArgs.putString("page", "profile");
        navFragment.setArguments(navArgs);
        fm.beginTransaction()
                .replace(R.id.profile_navbar_container, navFragment)
                .commit();
    }

    private void showHelpDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_profile_help, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        setupAccordionSection(
                dialogView.findViewById(R.id.profile_help_section_micro_header),
                (TextView) dialogView.findViewById(R.id.profile_help_section_micro_body),
                (ImageView) dialogView.findViewById(R.id.profile_help_section_micro_chevron),
                R.string.profile_help_section_micro_title
        );

        setupAccordionSection(
                dialogView.findViewById(R.id.profile_help_section_friends_header),
                (TextView) dialogView.findViewById(R.id.profile_help_section_friends_body),
                (ImageView) dialogView.findViewById(R.id.profile_help_section_friends_chevron),
                R.string.profile_help_section_friends_title
        );

        setupAccordionSection(
                dialogView.findViewById(R.id.profile_help_section_profile_header),
                (TextView) dialogView.findViewById(R.id.profile_help_section_profile_body),
                (ImageView) dialogView.findViewById(R.id.profile_help_section_profile_chevron),
                R.string.profile_help_section_profile_title
        );

        dialogView.findViewById(R.id.profile_help_dismiss_button).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showDeleteAccountDialog(AuthViewModel authViewModel) {
        // Inflate the custom dialog layout
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete_account, null);
        EditText passwordInput = dialogView.findViewById(R.id.passwordInput);

        // Create styled dialog using custom view
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Set transparent background to show our custom card styling
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Handle cancel button
        dialogView.findViewById(R.id.cancelButton).setOnClickListener(v -> {
            dialog.dismiss();
        });

        // Handle delete button
        dialogView.findViewById(R.id.deleteButton).setOnClickListener(v -> {
            String password = passwordInput.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(requireContext(), "Password is required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Disable buttons while processing
            dialogView.findViewById(R.id.deleteButton).setEnabled(false);
            dialogView.findViewById(R.id.cancelButton).setEnabled(false);

            // Call delete with password
            authViewModel.deleteAccountWithPassword(password, (success, error) -> {
                if (success) {
                    // Dismiss dialog and navigate to auth screen on successful deletion
                    requireActivity().runOnUiThread(() -> {
                        dialog.dismiss();
                        NavController navController =
                                NavHostFragment.findNavController(ProfileFragment.this);
                        NavOptions navOptions = new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_graph, true)
                                .build();
                        navController.navigate(R.id.authFragment, null, navOptions);
                    });
                } else {
                    requireActivity().runOnUiThread(() -> {
                        // Re-enable buttons on error
                        dialogView.findViewById(R.id.deleteButton).setEnabled(true);
                        dialogView.findViewById(R.id.cancelButton).setEnabled(true);
                        String message = error != null ? error.getMessage() : "Failed to delete account";
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });

        dialog.show();
    }

    private void setupAccordionSection(View header, TextView body, ImageView chevron, @StringRes int titleRes) {
        if (header == null || body == null || chevron == null) {
            return;
        }
        body.setVisibility(View.GONE);
        chevron.setRotation(0f);
        updateAccordionAccessibility(header, titleRes, false);
        header.setOnClickListener(v -> {
            boolean expand = body.getVisibility() != View.VISIBLE;
            body.setVisibility(expand ? View.VISIBLE : View.GONE);
            chevron.animate()
                    .rotation(expand ? 180f : 0f)
                    .setDuration(180)
                    .start();
            updateAccordionAccessibility(header, titleRes, expand);
        });
    }

    private void updateAccordionAccessibility(View header, @StringRes int titleRes, boolean expanded) {
        if (header == null) {
            return;
        }
        String sectionTitle = getString(titleRes);
        String state = getString(expanded ? R.string.profile_help_state_expanded : R.string.profile_help_state_collapsed);
        header.setContentDescription(getString(R.string.profile_help_section_a11y, sectionTitle, state));
        ViewCompat.setStateDescription(header, state);
    }


}
