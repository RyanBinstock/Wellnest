package com.code.wlu.cp470.wellnest.data.auth;

import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import java.util.HashMap;
import java.util.Map;

public class AuthRepository {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback<T> { void onResult(T value, Exception e); }

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
            // Ensure doc exists (no-ops if present)
            FirebaseFirestore.getInstance().collection("users").document(u.getUid())
            .get()
            .addOnSuccessListener(snap -> {
                if (!snap.exists()) {
                    bootstrapUserDocument(u.getUid(), u.getDisplayName(), u.getEmail(), (v, e) -> {
                        if (e != null) cb.onResult(null, new Exception(mapAuthError(e)));
                        else cb.onResult(u, null);
                    });
                } else {
                    cb.onResult(u, null);
                }
            })
            .addOnFailureListener(e -> cb.onResult(null, new Exception(mapAuthError(e))));
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
            if (user == null) { cb.onResult(null, new Exception("No user")); return; }

            // Keep Auth profile in sync for convenience
            user.updateProfile(new UserProfileChangeRequest.Builder()
                    .setDisplayName(name).build());

            // Create users/{uid} with Name + Email + timestamps
            bootstrapUserDocument(user.getUid(), name, user.getEmail(), (v, e) -> {
                if (e != null) cb.onResult(null, new Exception(mapAuthError(e)));
                else cb.onResult(user, null);
            });
        });
    }



    private void bootstrapUserDocument(String uid, String displayName, String email, Callback<Void> cb) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("Name",  displayName == null ? "" : displayName); // exact key
        doc.put("Email", email == null ? "" : email);             // exact key
        // Do NOT set globalPoints here from client.
        doc.put("createdAt", FieldValue.serverTimestamp());
        doc.put("updatedAt", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
        .collection("users").document(uid)
        .set(doc, SetOptions.merge())
        .addOnSuccessListener(v -> cb.onResult(null, null))
        .addOnFailureListener(e -> cb.onResult(null, e));
    }

    public FirebaseUser currentUser() { return auth.getCurrentUser(); }
    public void signOut() { auth.signOut(); }
}
