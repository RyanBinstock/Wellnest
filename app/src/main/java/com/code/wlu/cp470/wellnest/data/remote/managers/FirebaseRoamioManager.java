package com.code.wlu.cp470.wellnest.data.remote.managers;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.concurrent.ExecutionException;

public class FirebaseRoamioManager {

    private static final String TAG = "FirebaseRoamioManager";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();


    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static boolean awaitOk(Task<?> t) {
        try {
            Tasks.await(t);
            return true;
        } catch (ExecutionException e) {
            Throwable c = e.getCause();
            if (c instanceof FirebaseFirestoreException f) {
                Log.e(TAG, "awaitOk failed: " + f.getCode() + " / " + f.getMessage(), f);
            } else {
                Log.e(TAG, "awaitOk failed (exec)", e);
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "awaitOk interrupted", e);
            return false;
        }
    }

    public boolean upsertScore(String uid, int score) {
        Task<?> t = db.collection("users")
                .document(uid)
                .set(new java.util.HashMap<String, Object>() {{
                    put("roamio_score", score);
                }}, com.google.firebase.firestore.SetOptions.merge());

        return awaitOk(t);
    }

    public int getScore(String uid) {
        Task<com.google.firebase.firestore.DocumentSnapshot> t =
                db.collection("users").document(uid).get();

        if (!awaitOk(t)) return 0;

        try {
            com.google.firebase.firestore.DocumentSnapshot doc = Tasks.await(t);
            Long val = doc.getLong("roamio_score");
            return val != null ? val.intValue() : 0;
        } catch (Exception e) {
            Log.e(TAG, "getScore failed", e);
            return 0;
        }
    }

}
