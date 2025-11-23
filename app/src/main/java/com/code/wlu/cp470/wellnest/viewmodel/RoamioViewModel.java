package com.code.wlu.cp470.wellnest.viewmodel;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.code.wlu.cp470.wellnest.data.RoamioModels;
import com.code.wlu.cp470.wellnest.data.RoamioRepository;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.RoamioManager;
import com.code.wlu.cp470.wellnest.data.remote.managers.FirebaseRoamioManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoamioViewModel extends AndroidViewModel {

    /**
     * Callback interface for asynchronous walk generation operations.
     * @param <T> The type of result expected on success
     */
    public interface RoamioCallback<T> {
        /**
         * Called when the operation completes successfully.
         * @param result The result of the operation
         */
        void onSuccess(T result);

        /**
         * Called when the operation fails.
         * @param error The error message describing the failure
         */
        void onError(String error);

        /**
         * Called to report progress.
         * @param percent Progress percentage (0-100)
         * @param message Status message describing the current step
         */
        default void onProgress(int percent, String message) {}
    }

    private final RoamioRepository repo;
    private final WellnestDatabaseHelper dbHelper;
    private final SQLiteDatabase db;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public RoamioViewModel(@NonNull Application application) {
        super(application);
        dbHelper = new WellnestDatabaseHelper(application);
        db = dbHelper.getWritableDatabase();

        RoamioManager local = new RoamioManager(db);
        FirebaseRoamioManager remote = new FirebaseRoamioManager();
        repo = new RoamioRepository(application, local, remote);
    }

    public void syncScore() {
        io.execute(repo::syncScore);
    }

    public RoamioModels.RoamioScore getScore() {
        return repo.getRoamioScore();
    }

    public void addToScore(int points) {
        repo.addToRoamioScore(points);
    }

    /**
     * Generates a walk asynchronously using the device's current location.
     * <p>
     * This method now automatically obtains the user's current location via GPS,
     * performs reverse geocoding to get the location name, and generates a
     * personalized walking recommendation.
     * <p>
     * The callback will be invoked on the main thread with either success or error results.
     * <p>
     * <b>Required Permissions:</b>
     * The calling activity/fragment must have already obtained location permissions:
     * - {@link android.Manifest.permission#ACCESS_FINE_LOCATION} or
     * - {@link android.Manifest.permission#ACCESS_COARSE_LOCATION}
     * <p>
     * <b>Error Handling:</b>
     * The callback's onError will be invoked if:
     * - Location permissions are not granted
     * - Location cannot be obtained
     * - Geocoding fails
     * - Network requests fail
     *
     * @param callback Callback to handle success or error results
     */
    public void generateWalk(RoamioCallback<RoamioModels.Walk> callback) {
        io.execute(() -> {
            try {
                RoamioModels.Walk walk = repo.generateWalk((percent, message) -> {
                    mainHandler.post(() -> callback.onProgress(percent, message));
                });
                
                if (walk != null) {
                    mainHandler.post(() -> callback.onSuccess(walk));
                } else {
                    mainHandler.post(() -> callback.onError("Failed to generate walk. Please ensure location permissions are granted and location services are enabled."));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Error generating walk: " + e.getMessage()));
            }
        });
    }
}
