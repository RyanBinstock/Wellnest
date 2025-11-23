package com.code.wlu.cp470.wellnest.data.local.managers;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.code.wlu.cp470.wellnest.data.local.contracts.ActivityJarContract;

public class ActivityJarCacheManager {

    private final SQLiteDatabase db;

    public ActivityJarCacheManager(SQLiteDatabase db) {
        this.db = db;
    }

    /**
     * Saves the AI response and weather summary to the cache.
     * Clears any existing cache entries to ensure only the latest is stored.
     *
     * @param jsonData       The JSON string of the AI response.
     * @param weatherSummary The weather summary string.
     */
    public void saveCache(String jsonData, String weatherSummary) {
        db.beginTransaction();
        try {
            // Clear previous cache - we only keep the latest
            db.delete(ActivityJarContract.ActivityJarCache.TABLE, null, null);

            ContentValues values = new ContentValues();
            values.put(ActivityJarContract.ActivityJarCache.Col.JSON_DATA, jsonData);
            values.put(ActivityJarContract.ActivityJarCache.Col.WEATHER_SUMMARY, weatherSummary);
            values.put(ActivityJarContract.ActivityJarCache.Col.TIMESTAMP, System.currentTimeMillis());

            db.insert(ActivityJarContract.ActivityJarCache.TABLE, null, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Retrieves the cached data.
     *
     * @return A CacheEntry object containing the data, or null if no cache exists.
     */
    public CacheEntry getCachedData() {
        Cursor cursor = db.query(
                ActivityJarContract.ActivityJarCache.TABLE,
                null, null, null, null, null, null
        );

        try {
            if (cursor != null && cursor.moveToFirst()) {
                String jsonData = cursor.getString(cursor.getColumnIndexOrThrow(ActivityJarContract.ActivityJarCache.Col.JSON_DATA));
                String weatherSummary = cursor.getString(cursor.getColumnIndexOrThrow(ActivityJarContract.ActivityJarCache.Col.WEATHER_SUMMARY));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(ActivityJarContract.ActivityJarCache.Col.TIMESTAMP));
                return new CacheEntry(jsonData, weatherSummary, timestamp);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * Checks if there is a valid cache entry.
     *
     * @return true if cache exists, false otherwise.
     */
    public boolean hasValidCache() {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + ActivityJarContract.ActivityJarCache.TABLE, null);
        boolean hasData = false;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    hasData = cursor.getInt(0) > 0;
                }
            } finally {
                cursor.close();
            }
        }
        return hasData;
    }

    /**
     * Data holder for cached activity jar data.
     */
    public static class CacheEntry {
        public final String jsonData;
        public final String weatherSummary;
        public final long timestamp;

        public CacheEntry(String jsonData, String weatherSummary, long timestamp) {
            this.jsonData = jsonData;
            this.weatherSummary = weatherSummary;
            this.timestamp = timestamp;
        }
    }
}