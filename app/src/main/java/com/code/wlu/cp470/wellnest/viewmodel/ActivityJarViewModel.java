package com.code.wlu.cp470.wellnest.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import android.database.sqlite.SQLiteDatabase;

import com.code.wlu.cp470.wellnest.data.ActivityJarModels;
import com.code.wlu.cp470.wellnest.data.ActivityJarRepository;
import com.code.wlu.cp470.wellnest.data.WellnestAiClient;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.ActivityJarCacheManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActivityJarViewModel extends AndroidViewModel {

    private static final String TAG = "ActivityJarViewModel";
    private final MutableLiveData<Map<ActivityJarModels.Category, List<ActivityJarModels.Activity>>> activities = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Integer> score = new MutableLiveData<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ActivityJarCacheManager cacheManager;
    private final ActivityJarRepository repository;

    public ActivityJarViewModel(@NonNull Application application) {
        super(application);
        WellnestDatabaseHelper dbHelper = new WellnestDatabaseHelper(application);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        cacheManager = new ActivityJarCacheManager(db);
        repository = new ActivityJarRepository(db);
        loadScore();
    }

    public LiveData<Map<ActivityJarModels.Category, List<ActivityJarModels.Activity>>> getActivities() {
        return activities;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Integer> getScore() {
        return score;
    }

    private void loadScore() {
        repository.getScore(new ActivityJarRepository.ScoreCallback() {
            @Override
            public void onScoreUpdated(int newScore) {
                score.postValue(newScore);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading score", e);
            }
        });
    }

    public void acceptActivity(ActivityJarModels.Activity activity) {
        // 1. Add points
        repository.addScore(50, new ActivityJarRepository.ScoreCallback() {
            @Override
            public void onScoreUpdated(int newScore) {
                score.postValue(newScore);
            }

            @Override
            public void onError(Exception e) {
                error.postValue("Failed to update score: " + e.getMessage());
            }
        });

        // 2. Remove activity from list and update cache
        removeActivity(activity);
    }

    private void removeActivity(ActivityJarModels.Activity activityToRemove) {
        Map<ActivityJarModels.Category, List<ActivityJarModels.Activity>> currentMap = activities.getValue();
        if (currentMap == null) return;

        boolean removed = false;
        for (List<ActivityJarModels.Activity> list : currentMap.values()) {
            if (list.remove(activityToRemove)) {
                removed = true;
                break;
            }
        }

        if (removed) {
            activities.postValue(currentMap);
            // Update cache in background
            executor.execute(() -> {
                String jsonString = serializeActivitiesToJson(currentMap);
                cacheManager.saveCache(jsonString, "Cached via ViewModel (after removal)");
            });
        }
    }

    public void loadActivities() {
        Log.d(TAG, "loadActivities() called");
        if (activities.getValue() != null) {
            Log.d(TAG, "Activities already loaded, returning");
            return; // Data already loaded
        }

        Log.d(TAG, "Setting isLoading to true and starting background task");
        isLoading.setValue(true);
        executor.execute(() -> {
            try {
                // 1. Try to load from cache first
                if (cacheManager.hasValidCache()) {
                    Log.d(TAG, "Valid cache found, loading from cache...");
                    ActivityJarCacheManager.CacheEntry entry = cacheManager.getCachedData();
                    if (entry != null) {
                        Map<ActivityJarModels.Category, List<ActivityJarModels.Activity>> cachedResult =
                                parseActivitiesFromJson(entry.jsonData);
                        if (cachedResult != null && !cachedResult.isEmpty()) {
                            Log.d(TAG, "Cache loaded successfully, posting value.");
                            activities.postValue(cachedResult);
                            isLoading.postValue(false);
                            return; // Done!
                        }
                    }
                }

                // 2. If no cache or parsing failed, fetch from network
                Log.d(TAG, "No valid cache or parsing failed. Fetching from network...");
                
                // planThingsToDo requires Context. getApplication() returns the Application context.
                Map<ActivityJarModels.Category, List<ActivityJarModels.Activity>> result =
                        WellnestAiClient.planThingsToDo(getApplication());

                Log.d(TAG, "planThingsToDo returned result: " + (result != null ? "success" : "null"));

                if (result != null) {
                    activities.postValue(result);
                    
                    // Save to cache for next time
                    String jsonString = serializeActivitiesToJson(result);
                    cacheManager.saveCache(jsonString, "Cached via ViewModel");
                } else {
                    error.postValue("Failed to generate activities. Please try again.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading activities", e);
                error.postValue("Error: " + e.getMessage());
            } finally {
                Log.d(TAG, "Background task finished, posting isLoading = false");
                isLoading.postValue(false);
            }
        });
    }

    private Map<ActivityJarModels.Category, List<ActivityJarModels.Activity>> parseActivitiesFromJson(String jsonString) {
        try {
            JSONObject root = new JSONObject(jsonString);
            Map<ActivityJarModels.Category, List<ActivityJarModels.Activity>> byCategory =
                    new EnumMap<>(ActivityJarModels.Category.class);

            for (ActivityJarModels.Category category : ActivityJarModels.Category.values()) {
                if (!root.has(category.name())) continue;

                JSONArray activitiesArray = root.getJSONArray(category.name());
                List<ActivityJarModels.Activity> activities = new ArrayList<>();

                for (int i = 0; i < activitiesArray.length(); i++) {
                    JSONObject a = activitiesArray.getJSONObject(i);

                    String emoji = a.optString("emoji");
                    String title = a.optString("title");
                    String description = a.optString("description");
                    String address = a.optString("address");
                    String url = a.optString("url");

                    JSONArray tagsJson = a.optJSONArray("tags");
                    String[] tags = new String[tagsJson != null ? tagsJson.length() : 0];

                    if (tagsJson != null) {
                        for (int t = 0; t < tagsJson.length(); t++) {
                            tags[t] = tagsJson.getString(t);
                        }
                    }

                    ActivityJarModels.Activity activity =
                            new ActivityJarModels.Activity(
                                    category.name(),
                                    emoji,
                                    title,
                                    description,
                                    address,
                                    tags,
                                    url
                            );

                    activities.add(activity);
                }
                byCategory.put(category, activities);
            }
            return byCategory;
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing cached JSON", e);
            return null;
        }
    }

    private String serializeActivitiesToJson(Map<ActivityJarModels.Category, List<ActivityJarModels.Activity>> map) {
        JSONObject root = new JSONObject();
        try {
            for (Map.Entry<ActivityJarModels.Category, List<ActivityJarModels.Activity>> entry : map.entrySet()) {
                JSONArray array = new JSONArray();
                for (ActivityJarModels.Activity activity : entry.getValue()) {
                    JSONObject obj = new JSONObject();
                    obj.put("emoji", activity.getEmoji());
                    obj.put("title", activity.getTitle());
                    obj.put("description", activity.getDescription());
                    obj.put("address", activity.getAddress());
                    obj.put("url", activity.getUrl());

                    JSONArray tags = new JSONArray();
                    if (activity.getTags() != null) {
                        for (String tag : activity.getTags()) {
                            tags.put(tag);
                        }
                    }
                    obj.put("tags", tags);

                    array.put(obj);
                }
                root.put(entry.getKey().name(), array);
            }
            return root.toString();
        } catch (JSONException e) {
            Log.e(TAG, "Error serializing activities", e);
            return "{}";
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
        repository.shutdown();
    }
}