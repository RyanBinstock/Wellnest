package com.code.wlu.cp470.wellnest.data.remote.managers;

import android.util.Log;

import com.code.wlu.cp470.wellnest.data.RoamioModels;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Map;
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

    public boolean upsertScore(RoamioModels.RoamioScore roamioScore) {
        if (roamioScore == null || roamioScore.getUid() == null) return false;

        DocumentReference ref = db
                .collection("users")
                .document(roamioScore.getUid())
                .collection("microapp_scores")
                .document("roamio");

        Map<String, Object> data = new HashMap<>();
        data.put("score", roamioScore.getScore());

        return awaitOk(ref.set(data));
    }

    public RoamioModels.RoamioScore getScore(String uid) {
        if (uid == null) {
            return null;
        }

        DocumentReference ref = db
                .collection("users")
                .document(uid)
                .collection("microapp_scores")
                .document("roamio");

        try {
            Task<DocumentSnapshot> task = ref.get();
            if (!awaitOk(task)) return null;

            DocumentSnapshot snap = task.getResult();

            if (snap != null && snap.exists()) {
                Long val = snap.getLong("score");
                int score = val != null ? val.intValue() : 0;
                return new RoamioModels.RoamioScore(uid, score);
            }

        } catch (Exception e) {
            Log.e(TAG, "getScore failed", e);
        }

        return null;
    }

}
