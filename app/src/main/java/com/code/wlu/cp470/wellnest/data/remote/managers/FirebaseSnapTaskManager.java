package com.code.wlu.cp470.wellnest.data.remote.managers;

import android.util.Log;

import com.code.wlu.cp470.wellnest.data.SnapTaskModels;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FirebaseSnapTaskManager {

    private static final String TAG = "FirebaseSnapTaskManager";
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

    public List<SnapTaskModels.Task> getTasks() {
        try {
            QuerySnapshot snap = Tasks.await(
                    db.collection("micro_app_data")
                            .document("snap_task")
                            .collection("tasks")
                            .get()
            );
            List<SnapTaskModels.Task> tasks = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                tasks.add(new SnapTaskModels.Task(
                        d.getId(),
                        d.getString("Name"),
                        d.getLong("Points").intValue(),
                        d.getString("Description"),
                        false
                ));
            }
            return tasks;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

}
