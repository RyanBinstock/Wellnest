package com.code.wlu.cp470.wellnest.data.auth;

import android.content.Context;

import com.code.wlu.cp470.wellnest.data.UserRepository;
import com.code.wlu.cp470.wellnest.data.local.managers.UserManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseUserManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * AuthRepository:
 * - Handles FirebaseAuth sign-in/up/out.
 * - Ensures Firestore users/{uid} exists.
 * - Persists normalized user data via UserRepository (LOCAL) and prepares local score row.
 * <p>
 * Note: UserRepository delegates all UI-facing reads/writes to LOCAL; REMOTE is used only by its sync helpers.
 * Call sync helpers (e.g., pushLocalGlobalScoreToCloud / refreshFriendsScoresFromCloud) from app startup flows as needed.
 */
public class AuthRepository {
    private final FirebaseAuth auth;
    private final Context context;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final UserRepository userRepo;

    /**
     * Preferred: inject a ready UserRepository (constructed with your LOCAL + REMOTE managers).
     */
    public AuthRepository(Context context, UserRepository userRepository) {
        this.auth = FirebaseAuth.getInstance();
        this.context = context.getApplicationContext();
        this.userRepo = userRepository;
    }

    /**
     * Convenience overload if you only have managers here.
     * Pass your concrete implementations that implement UserInterface (e.g., UserManager, FirebaseUserManager).
     */
    public AuthRepository(Context context, UserManager localManager, FirebaseUserManager remoteManager) {
        this(context, new UserRepository(context.getApplicationContext(), localManager, remoteManager));
    }

    private String mapAuthError(Exception e) {
        if (e instanceof com.google.firebase.auth.FirebaseAuthException) {
            String code = ((com.google.firebase.auth.FirebaseAuthException) e).getErrorCode();
            return switch (code) {
                case "ERROR_INVALID_EMAIL" -> "Please enter a valid email address.";
                case "ERROR_EMAIL_ALREADY_IN_USE" -> "That email is already registered.";
                case "ERROR_WRONG_PASSWORD" -> "Incorrect password.";
                case "ERROR_USER_NOT_FOUND" -> "No account found with that email.";
                case "ERROR_WEAK_PASSWORD" -> "Password must be at least 6 characters.";
                case "ERROR_OPERATION_NOT_ALLOWED" -> "Email/password accounts are not enabled.";
                default -> "Sign-in failed. Please try again.";
            };
        }
        return "Something went wrong. Please try again.";
    }

    public void signIn(String email, String password, Callback<FirebaseUser> cb) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(t -> {
                    if (!t.isSuccessful()) {
                        cb.onResult(null, new Exception(mapAuthError(t.getException())));
                        return;
                    }
                    FirebaseUser u = auth.getCurrentUser();
                    if (u == null) {
                        cb.onResult(null, new Exception("No user"));
                        return;
                    }
                    // Ensure Firestore doc exists (no-op if present)
                    String uid = u.getUid();

                    // Make sure the doc exists without blocking the UI thread
                    bootstrapUserDocument(uid, u.getDisplayName(), email, (v, e2) -> {
                        // even if this fails, keep the user signed in; you can retry later
                        persistLocalUser(uid, u.getDisplayName(), email);
                        userRepo.ensureGlobalScore(uid);
                        cb.onResult(u, null);
                    });
                });
    }

    public void signUp(String name, String email, String password, Callback<FirebaseUser> cb) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(t -> {
                    if (!t.isSuccessful()) {
                        cb.onResult(null, new Exception(mapAuthError(t.getException())));
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        cb.onResult(null, new Exception("No user"));
                        return;
                    }

                    // Keep Auth profile in sync for convenience
                    user.updateProfile(new UserProfileChangeRequest.Builder()
                            .setDisplayName(name).build());

                    // Create users/{uid} with Name + Email + timestamps
                    bootstrapUserDocument(user.getUid(), name, user.getEmail(), (v, e) -> {
                        if (e != null) {
                            cb.onResult(null, new Exception(mapAuthError(e)));
                            return;
                        }
                        // Persist locally via repository and create a score row for new users
                        userRepo.upsertUserProfile(user.getUid(), name, user.getEmail());
                        // if not present, create a local score row for this uid (0 initial)
                        userRepo.ensureGlobalScore(user.getUid()); // contract: create if missing
                        cb.onResult(user, null);
                    });
                });
    }

    private void persistLocalUser(String uid, String displayName, String email) {
        // Upsert profile locally (normalized via UserInterface contract)
        userRepo.upsertUserProfile(uid, displayName, email);
        // Make sure a local global_score row exists for this user to avoid nulls in UI
        userRepo.ensureGlobalScore(uid);
        // Optional (call from app start if you prefer): userRepo.pushLocalGlobalScoreToCloud();
        // Optional: userRepo.refreshFriendsScoresFromCloud();
    }

    private void bootstrapUserDocument(String uid, String displayName, String email, Callback<Void> cb) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("Name", displayName == null ? "" : displayName); // exact key
        doc.put("Email", email == null ? "" : email);            // exact key
        doc.put("createdAt", FieldValue.serverTimestamp());
        doc.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("users").document(uid)
                .set(doc, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onResult(null, null))
                .addOnFailureListener(e -> cb.onResult(null, e));
    }

    public FirebaseUser currentUser() {
        return auth.getCurrentUser();
    }

    public void signOut() {
        auth.signOut();
    }

    public interface Callback<T> {
        void onResult(T value, Exception e);
    }
}
