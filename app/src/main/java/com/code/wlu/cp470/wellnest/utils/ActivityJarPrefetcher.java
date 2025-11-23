package com.code.wlu.cp470.wellnest.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.code.wlu.cp470.wellnest.data.ActivityJarModels;
import com.code.wlu.cp470.wellnest.data.WellnestAiClient;
import com.code.wlu.cp470.wellnest.data.local.WellnestDatabaseHelper;
import com.code.wlu.cp470.wellnest.data.local.managers.ActivityJarCacheManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ActivityJarPrefetcher {

    private static final String TAG = "ActivityJarPrefetcher";

    // Time windows
    private static final LocalTime MORNING_START = LocalTime.of(8, 0);
    private static final LocalTime AFTERNOON_START = LocalTime.of(13, 0);
    private static final LocalTime EVENING_START = LocalTime.of(18, 0);
    private static final LocalTime NIGHT_END = LocalTime.of(21, 0);

    /**
     * Prefetches activities if needed based on time windows and cache status.
     * This method should be called from a background thread.
     *
     * @param context The application context.
     */
    public static void prefetchActivities(Context context) {
        Log.d(TAG, "prefetchActivities: Checking if prefetch is needed...");

        WellnestDatabaseHelper dbHelper = new WellnestDatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ActivityJarCacheManager cacheManager = new ActivityJarCacheManager(db);

        if (shouldPrefetch(cacheManager)) {
            Log.d(TAG, "prefetchActivities: Prefetching activities...");
            try {
                // We need to call planThingsToDo but we also need to intercept the result to save it to cache.
                // However, WellnestAiClient.planThingsToDo returns a Map, not the raw JSON.
                // We need to modify WellnestAiClient or duplicate some logic to get the raw JSON for caching.
                // Or, we can serialize the Map back to JSON. Serializing back to JSON seems safer/easier 
                // than modifying the client which might be used elsewhere.
                
                Map<ActivityJarModels.Category, List<ActivityJarModels.Activity>> result = null;
                int retryCount = 0;
                int maxRetries = 3;

                while (result == null && retryCount < maxRetries) {
                    try {
                        if (retryCount > 0) {
                            Log.d(TAG, "prefetchActivities: Retrying... attempt " + (retryCount + 1));
                            try {
                                Thread.sleep(2000L * retryCount); // Simple backoff
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break; // Stop retrying if interrupted
                            }
                        }
                        result = WellnestAiClient.planThingsToDo(context);
                    } catch (Exception e) {
                        Log.e(TAG, "prefetchActivities: Attempt " + (retryCount + 1) + " failed", e);
                    }
                    retryCount++;
                }

                if (result != null) {
                    String jsonString = serializeActivitiesToJson(result);
                    // We don't have the weather summary from planThingsToDo directly.
                    // For now, we can fetch it again or just store a placeholder/timestamp.
                    // Ideally, planThingsToDo should return a wrapper with metadata.
                    // But to minimize changes, let's fetch weather quickly or just use "Cached" as summary.
                    // Actually, let's just fetch it since we are in background.
                    // Wait, WellnestAiClient has a public method for weather.

                    // Optimization: planThingsToDo already fetches weather.
                    // If we want to avoid double fetching, we'd need to refactor WellnestAiClient.
                    // For this task, let's keep it simple. We'll just save the JSON.
                    // The cache manager expects a weather summary.

                    String weatherSummary = "Cached via Prefetcher";
                    // Ideally we would get the real weather, but let's not make another network call if we can avoid it.
                    // Or we can just pass empty string if it's not strictly used for display yet.

                    cacheManager.saveCache(jsonString, weatherSummary);
                    Log.d(TAG, "prefetchActivities: Activities prefetched and cached.");
                } else {
                    Log.e(TAG, "prefetchActivities: Failed to fetch activities after " + maxRetries + " attempts.");
                }

            } catch (Exception e) {
                Log.e(TAG, "prefetchActivities: Error during prefetch", e);
            }
        } else {
            Log.d(TAG, "prefetchActivities: Cache is valid, no prefetch needed.");
        }
    }

    private static boolean shouldPrefetch(ActivityJarCacheManager cacheManager) {
        if (!cacheManager.hasValidCache()) {
            Log.d(TAG, "shouldPrefetch: No valid cache found.");
            return true;
        }

        ActivityJarCacheManager.CacheEntry entry = cacheManager.getCachedData();
        if (entry == null) {
            return true;
        }

        long cacheTime = entry.timestamp;
        long currentTime = System.currentTimeMillis();
        
        // Simple check: if cache is older than 4 hours, refresh.
        // Or use the time windows as requested.
        
        // Convert timestamps to LocalTime for window checking would require ZoneId.
        // Let's stick to the requested logic:
        // Check if current time is within a new window (08:00-12:59, 13:00-17:59, 18:00-20:59).
        // Check if cache exists and if it's from the current window.
        
        // Implementation detail: We need to know if the cache timestamp falls into the SAME window as current time.
        
        return !isSameTimeWindow(cacheTime, currentTime);
    }

    private static boolean isSameTimeWindow(long time1, long time2) {
        // This is a simplified check. For production, use proper ZoneId.
        // Assuming device timezone for both.
        
        java.time.LocalDateTime dt1 = java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(time1), java.time.ZoneId.systemDefault());
        java.time.LocalDateTime dt2 = java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(time2), java.time.ZoneId.systemDefault());

        if (!dt1.toLocalDate().equals(dt2.toLocalDate())) {
            return false; // Different days
        }

        int window1 = getTimeWindowIndex(dt1.toLocalTime());
        int window2 = getTimeWindowIndex(dt2.toLocalTime());

        return window1 == window2 && window1 != -1;
    }

    private static int getTimeWindowIndex(LocalTime time) {
        if (time.isAfter(MORNING_START) && time.isBefore(AFTERNOON_START)) return 1;
        if (time.isAfter(AFTERNOON_START) && time.isBefore(EVENING_START)) return 2;
        if (time.isAfter(EVENING_START) && time.isBefore(NIGHT_END)) return 3;
        return -1; // Outside of defined windows
    }

    private static String serializeActivitiesToJson(Map<ActivityJarModels.Category, List<ActivityJarModels.Activity>> map) {
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
}