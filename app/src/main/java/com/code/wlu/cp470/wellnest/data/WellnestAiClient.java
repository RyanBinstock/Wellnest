package com.code.wlu.cp470.wellnest.data;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * WellnestAiClient - A utility class for AI-powered functionality in the Wellnest app.
 * <p>
 * This client handles three main AI-powered features:
 * 1. SnapTask evaluation using GPT-4o vision
 * 2. Things-to-do planning using location and weather data
 * 3. Walking spot recommendations with historical context
 * <p>
 * All OpenAI and Tavily API calls are proxied through Vercel to keep API keys secure.
 * Open-Meteo weather API is accessed directly as it requires no authentication.
 * <p>
 * This is a stateless utility class with static methods. It cannot be instantiated.
 */
public final class WellnestAiClient {

    // ============================================================
    // CONSTANTS - API Configuration
    // ============================================================

    /**
     * Base URL for Vercel proxy endpoints
     */
    private static final String VERCEL_BASE_URL = "https://wellnest-proxy.vercel.app/";

    /**
     * Vercel proxy endpoint for OpenAI Chat API
     */
    private static final String OPENAI_PROXY_URL = VERCEL_BASE_URL + "/api/openai-chat";

    /**
     * Vercel proxy endpoint for Tavily Search API
     */
    private static final String TAVILY_PROXY_URL = VERCEL_BASE_URL + "/api/tavily-search";

    /**
     * Direct endpoint for Open-Meteo weather API (no authentication required)
     */
    private static final String OPEN_METEO_URL = "https://api.open-meteo.com/v1/forecast";

    /**
     * Media type for JSON requests
     */
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * Maximum number of retry attempts for GPT API calls
     */
    private static final int GPT_RETRY_MAX = 3;

    /**
     * Base delay in milliseconds between retry attempts
     */
    private static final long GPT_RETRY_DELAY_MS = 1200L;

    /**
     * Logging tag for debugging
     */
    private static final String TAG = "WellnestAiClient";

    // ============================================================
    // HTTP CLIENT CONFIGURATION
    // ============================================================

    /**
     * Configured OkHttpClient with generous timeouts for AI API calls.
     * Features:
     * - Connection timeout: 20 seconds
     * - Write timeout: 40 seconds
     * - Read timeout: 60 seconds
     * - Total call timeout: 120 seconds
     * - Automatic retry on connection failure
     * - Periodic ping to keep connection alive
     */
    private static final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .callTimeout(180, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .pingInterval(15, TimeUnit.SECONDS)
            .build();

    // ============================================================
    // TEST HOOKS
    // ============================================================

    /**
     * Test-only hook for overriding SnapTask evaluation behavior.
     * When set from androidTest, {@link #evaluateSnapTask} will delegate to this
     * override instead of calling the real network-backed implementation.
     */
    private static volatile SnapTaskEvaluationOverride snapTaskEvaluationOverride = null;

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private WellnestAiClient() {
        throw new AssertionError("WellnestAiClient is a utility class and should not be instantiated");
    }

    /**
     * Sets a test hook to override SnapTask evaluation behavior.
     * This allows tests to avoid network calls and provide deterministic results.
     *
     * @param override The override implementation, or null to use real network calls
     */
    public static void setSnapTaskEvaluationOverride(SnapTaskEvaluationOverride override) {
        snapTaskEvaluationOverride = override;
    }

    // ============================================================
    // PUBLIC API - Main Features
    // ============================================================

    /**
     * Evaluates a SnapTask by comparing before and after photos using GPT-4o vision.
     * <p>
     * This method uses GPT-4o's vision capabilities to determine if a household chore
     * was completed according to the specified criteria. The evaluation considers both
     * the before and after photos to assess whether meaningful progress was made.
     * <p>
     * Features:
     * - Automatic retry logic (up to 3 attempts)
     * - Test hook for instrumented testing
     * - Graceful fallback to "pass" on repeated failures
     * - Zero-temperature for consistent results
     *
     * @param criteria   The acceptance criteria describing what constitutes task completion
     * @param beforeJpeg JPEG image data showing the state before the task
     * @param afterJpeg  JPEG image data showing the state after the task
     * @return "pass" if the task was completed successfully, "fail" otherwise
     */
    public static String evaluateSnapTask(String criteria, byte[] beforeJpeg, byte[] afterJpeg) {
        // Check for test-only override to avoid hitting the real network in androidTest
        SnapTaskEvaluationOverride override = snapTaskEvaluationOverride;
        if (override != null) {
            try {
                String raw = override.evaluate(criteria, beforeJpeg, afterJpeg);
                return normalizePassFail(raw == null ? "" : raw);
            } catch (Exception e) {
                Log.e(TAG, "SnapTaskEvaluationOverride threw; falling back to network evaluation", e);
                // Fall through to the normal network path
            }
        }

        Log.d(TAG, "evaluateSnapTask() called; criteriaLen=" +
                (criteria == null ? "null" : criteria.length()) +
                ", beforeBytes=" + (beforeJpeg == null ? "null" : beforeJpeg.length) +
                ", afterBytes=" + (afterJpeg == null ? "null" : afterJpeg.length));

        // Retry logic with exponential backoff
        for (int attempt = 1; attempt <= GPT_RETRY_MAX; attempt++) {
            try {
                Log.d(TAG, "evaluateSnapTask() attempt " + attempt);

                // Build GPT-4o request with vision content
                JSONObject req = new JSONObject()
                        .put("model", "gpt-4o")
                        .put("temperature", 0.0);

                // Construct system message with evaluation instructions
                JSONArray messages = new JSONArray();
                messages.put(new JSONObject()
                        .put("role", "system")
                        .put("content",
                                "You are an image judge for household chores. " +
                                        "You will receive: (1) acceptance criteria text, (2) BEFORE photo, (3) AFTER photo. " +
                                        "Decide if the AFTER photo satisfies the criteria compared to BEFORE. " +
                                        "Reply with exactly one word: pass or fail. No punctuation, no explanation."));

                // Construct user message with criteria and images
                JSONArray content = new JSONArray()
                        .put(new JSONObject().put("type", "text").put("text", "Criteria:\n" + criteria))
                        .put(new JSONObject().put("type", "text").put("text", "BEFORE photo:"))
                        .put(createImageUrlPart(beforeJpeg))
                        .put(new JSONObject().put("type", "text").put("text", "AFTER photo:"))
                        .put(createImageUrlPart(afterJpeg));

                messages.put(new JSONObject().put("role", "user").put("content", content));
                req.put("messages", messages);

                String text = callOpenAIText(req);
                Log.d(TAG, "evaluateSnapTask() got raw response: " + text);
                return normalizePassFail(text);
            } catch (Exception e) {
                Log.e(TAG, "evaluateSnapTask() error on attempt " + attempt, e);
                if (attempt == GPT_RETRY_MAX) {
                    // After all retries fail, return "pass" to be lenient
                    return "pass";
                }
                try {
                    Thread.sleep(GPT_RETRY_DELAY_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return "pass";
                }
            }
        }
        return "pass";
    }

    /**
     * Plans things to do at a location based on date, time, and weather conditions.
     * <p>
     * This method combines multiple data sources to generate personalized activity
     * recommendations:
     * 1. Fetches current weather for the location
     * 2. Uses AI to create a targeted search query
     * 3. Searches the web for current local activities
     * 4. Synthesizes results into categorized recommendations
     * <p>
     * Each activity includes:
     * - Emoji: Emoji representing the activity
     * - Title: Name of the activity
     * - Description: Brief description of the activity
     * - Address: Address of the activity (if available)
     * - Tag: Whether it's best for "solo", "friends", or "family"
     * - URL: Link to more information
     *
     * @return Map of {@link ActivityJarModels.Category} to a list of {@link ActivityJarModels.Activity}
     * @throws IOException If network requests fail or data cannot be retrieved
     */
    public static Map<ActivityJarModels.Category, List<ActivityJarModels.Activity>> planThingsToDo(
            Context context) throws IOException, JSONException {

        Log.d(TAG, "planThingsToDo: Starting...");

        if (!hasLocationPermission(context)) {
            Log.e(TAG, "planThingsToDo: Location permission not granted");
            throw new IOException("Location permission not granted");
        }

        // Get current location
        Log.d(TAG, "planThingsToDo: Getting location...");
        Location location = getCurrentLocation(context);
        if (location == null) {
            Log.e(TAG, "planThingsToDo: Unable to obtain current location");
            return null;
        }
        Log.d(TAG, "planThingsToDo: Location obtained: " + location.getLatitude() + ", " + location.getLongitude());

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        String isoDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // Fetch current weather conditions for the location
        Log.d(TAG, "planThingsToDo: Fetching weather...");
        String weatherSummary = fetchWeatherSummary(latitude, longitude);
        String locationName = getLocationName(context, latitude, longitude);
        Log.d(TAG, "planThingsToDo: Weather: " + weatherSummary + ", Location: " + locationName);


        // Generate a targeted search query using AI
        Log.d(TAG, "planThingsToDo: Asking Nano for query...");
        String query = String.format(Locale.US,
                "Best local things to do in %s on %s considering %s. " +
                        "Include outdoor hikes and nature spots, nightlife bars and live music, " +
                        "active recreation like sports or arcades, cozy coffee shops or chill " +
                        "indoor spaces, and cultural places like museums or notable restaurants. " +
                        "Use current, official sources.",
                locationName, isoDate, weatherSummary);


        // Search the web for relevant activities
        Log.d(TAG, "planThingsToDo: Searching Tavily...");
        JSONObject tavilyResults = tavilySearch(query);
        Log.d(TAG, "planThingsToDo: Tavily search complete");

        // Synthesize search results into categorized recommendations
        Log.d(TAG, "planThingsToDo: Synthesizing results...");
        String synthPrompt =
                "Using the Tavily results (JSON below) and the context, return ONLY a valid JSON object with exactly these top-level keys: " +
                        "[\"Explore\",\"Nightlife\",\"Play\",\"Cozy\",\"Culture\"]. No commentary.\n\n" +

                        "Each key maps to an array of 3–5 objects with this exact shape:\n" +
                        "{\n" +
                        "  \"emoji\": string,\n" +
                        "  \"title\": string,\n" +
                        "  \"description\": string,\n" +
                        "  \"address\": string,\n" +
                        "  \"tags\": [\"solo\",\"friends\",\"family\"],\n" +
                        "  \"url\": string\n" +
                        "}\n\n" +

                        "CATEGORY DEFINITIONS (STRICT):\n" +
                        "- Explore: Outdoor, adventure-style activities (hikes, trails, viewpoints, nature walks, scenic exploration)\n" +
                        "- Nightlife: After-dark social activities (bars, lounges, live music, nightlife venues, late-night events)\n" +
                        "- Play: Active & recreational fun (sports, arcades, bowling, mini golf, physical games)\n" +
                        "- Cozy: Relaxed, low-energy comfort activities (coffee shops, reading spots, at-home vibes, calm spaces)\n" +
                        "- Culture: Intellectual & culinary experiences (museums, galleries, cultural sites, notable restaurants)\n\n" +

                        "Rules:\n" +
                        "- Choose ONE source URL per activity from Tavily results (prefer official sites). Use full http(s) URLs.\n" +
                        "- Activities must be realistic given the provided dateTime and weather.\n" +
                        "- Tags may include one or more of: solo, friends, family.\n" +
                        "- The emoji field must contain EXACTLY ONE emoji that best represents the activity.\n" +
                        "- Do not invent places not justified by Tavily data.\n\n" +

                        "Context:\n" +
                        "location: " + locationName + "\n" +
                        "dateTime: " + isoDate + "\n" +
                        "weather: " + weatherSummary + "\n\n" +
                        "Tavily JSON:\n" + tavilyResults.toString();


        String jsonResponse = askNanoForStrictJson(synthPrompt);

        JSONObject root;
        try {
            root = new JSONObject(jsonResponse);
        } catch (Exception e) {
            Log.e(TAG, "planThingsToDo: Failed to parse GPT response JSON", e);
            return null;
        }

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
    }

    /**
     * Picks a scenic walking spot and generates an interesting backstory.
     * <p>
     * This method automatically obtains the user's current location and combines
     * web search and AI to find and describe interesting walking locations:
     * 1. Gets current location using FusedLocationProviderClient
     * 2. Converts coordinates to human-readable location name via Geocoder
     * 3. Fetches current weather for the location
     * 4. Searches for scenic walking spots nearby
     * 5. Selects the best option with detailed addresses
     * 6. Geocodes start and end addresses to coordinates
     * 7. Calculates distance between start and end points
     * 8. Generates historical context and interesting facts
     * <p>
     * The returned Walk object includes:
     * - name: The walking location name
     * - start_address: Starting point address
     * - end_address: Ending point address
     * - story: 4-7 sentences of historical context and interesting facts
     * - distanceMeters: Calculated distance between start and end points
     * - status: "generated" to indicate it's a new suggestion
     * <p>
     * <b>Required Permissions:</b>
     * - {@link android.Manifest.permission#ACCESS_FINE_LOCATION} or
     * - {@link android.Manifest.permission#ACCESS_COARSE_LOCATION}
     * <p>
     * <b>Error Handling:</b>
     * - Returns null if location permissions are not granted
     * - Returns null if location cannot be obtained
     * - Returns null if geocoding fails
     * - Returns null if GPT response parsing fails
     * - Returns null if distance calculation fails
     *
     * @param context  Android Context for location services and geocoding
     * @param callback Optional callback for progress updates (can be null)
     * @return Walk object with all attributes set, or null on error
     */
    public static RoamioModels.Walk pickWalkAndStory(Context context, ProgressCallback callback) throws IOException {
        if (context == null) {
            Log.e(TAG, "pickWalkAndStory: context is null");
            return null;
        }

        if (callback != null) callback.onProgress(5, "Checking permissions...");

        // Check location permissions
        if (!hasLocationPermission(context)) {
            Log.e(TAG, "pickWalkAndStory: Location permission not granted");
            return null;
        }

        if (callback != null) callback.onProgress(10, "Finding your location...");

        // Get current location
        Location location = getCurrentLocation(context);
        if (location == null) {
            Log.e(TAG, "pickWalkAndStory: Unable to obtain current location");
            return null;
        }

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        Log.d(TAG, String.format(Locale.CANADA, "pickWalkAndStory: Got location: %.6f, %.6f", latitude, longitude));

        if (callback != null) callback.onProgress(20, "Identifying neighborhood...");

        // Get location name via reverse geocoding
        String locationName = getLocationName(context, latitude, longitude);
        if (locationName == null) {
            Log.e(TAG, "pickWalkAndStory: Unable to get location name from coordinates");
            return null;
        }
        Log.d(TAG, "pickWalkAndStory: Location name: " + locationName);

        // Generate current ISO time
        String isoTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        if (callback != null) callback.onProgress(30, "Checking weather conditions...");

        // Fetch weather for the location
        String weatherSummary;
        try {
            weatherSummary = fetchWeatherSummary(latitude, longitude);
        } catch (IOException e) {
            Log.w(TAG, "pickWalkAndStory: Weather fetch failed, using fallback", e);
            weatherSummary = "current conditions";
        }

        if (callback != null) callback.onProgress(40, "Searching for scenic spots...");

        // Generate search query for walking spots
        String qPrompt = String.format(Locale.US,
                "Create one concise web search query to find scenic, safe walking spots in %s around %s. " +
                        "Prefer parks, river paths, waterfronts, or historic districts. " +
                        "Return ONLY the query text.", locationName, isoTime);
        String query = askNanoForQuery(qPrompt);

        // Search for walking spots
        JSONObject tavilyResults = tavilySearch(query);

        if (callback != null) callback.onProgress(60, "Crafting your adventure...");

        // Ask AI to select the best spot and generate a story
        String choosePrompt = "From these Tavily results, pick ONE great walking location. Return ONLY JSON with:\n" +
                "{\n" +
                "  \"query\": string,\n" +
                "  \"pick\": {\"name\": string, \"start_address\": string, \"end_address\": string},\n" +
                "  \"story\": string  # an interesting backstory & history in 4–7 sentences, friendly tone\n" +
                "}\n\n" +
                "If multiple addresses exist, choose the most official/precise formatted address. Keep fields concise.\n\n" +
                "Context:\n" +
                "location: " + locationName + "\n" +
                "weather: " + weatherSummary + "\n" +
                "Tavily JSON:\n" + tavilyResults.toString();

        String jsonResponse;
        try {
            jsonResponse = askNanoForStrictJson(choosePrompt.replace("//", "#"));
        } catch (IOException e) {
            Log.e(TAG, "pickWalkAndStory: Failed to get GPT response", e);
            return null;
        }

        if (callback != null) callback.onProgress(80, "Finalizing details...");

        // Parse the JSON response
        try {
            JSONObject root = new JSONObject(jsonResponse);
            JSONObject pick = root.getJSONObject("pick");
            String story = root.optString("story", "Enjoy your walk!");

            String name = pick.optString("name", "Mystery Walk");
            String startAddress = pick.optString("start_address", locationName);
            String endAddress = pick.optString("end_address", locationName);

            // Geocode start address to coordinates
            double[] startCoords = geocodeAddress(context, startAddress);
            if (startCoords == null) {
                Log.e(TAG, "pickWalkAndStory: Failed to geocode start address: " + startAddress);
                return null;
            }

            // Geocode end address to coordinates
            double[] endCoords = geocodeAddress(context, endAddress);
            if (endCoords == null) {
                Log.e(TAG, "pickWalkAndStory: Failed to geocode end address: " + endAddress);
                return null;
            }

            // Calculate distance between start and end coordinates
            float distance = calculateDistance(startCoords[0], startCoords[1], endCoords[0], endCoords[1]);
            Log.d(TAG, String.format(Locale.US, "pickWalkAndStory: Calculated distance: %.2f meters", distance));

            if (callback != null) callback.onProgress(100, "Ready to explore!");

            // Create and return the Walk object
            return new RoamioModels.Walk(
                    null,           // uid - null for generated walks
                    name,
                    story,
                    startAddress,
                    endAddress,
                    distance,
                    false  // completed - false for new walks
            );
        } catch (JSONException e) {
            Log.e(TAG, "pickWalkAndStory: Failed to parse GPT response JSON", e);
            return null;
        }
    }

    /**
     * Overload for backward compatibility or when no callback is needed.
     */
    public static RoamioModels.Walk pickWalkAndStory(Context context) throws IOException {
        return pickWalkAndStory(context, null);
    }

    /**
     * Fetches a human-readable weather summary for a location.
     * <p>
     * This is a public convenience method that wraps the internal weather fetching
     * functionality. It's useful for tests and UI components that need weather data.
     *
     * @param lat Latitude of the location
     * @param lon Longitude of the location
     * @return Weather summary string (e.g., "Clear, 22°C, wind 15 km/h")
     * @throws IOException If the weather API request fails
     */
    public static String getWeatherSummary(double lat, double lon) throws IOException {
        return fetchWeatherSummary(lat, lon);
    }

    /**
     * Pretty-prints JSON for debugging purposes.
     * <p>
     * Attempts to parse the input as JSON (object or array) and format it with
     * 2-space indentation. If parsing fails, returns the original string.
     *
     * @param json JSON string to format
     * @return Formatted JSON string with indentation, or original string if not valid JSON
     */
    public static String pretty(String json) {
        try {
            return new JSONObject(json).toString(2);
        } catch (Exception e) {
            try {
                return new JSONArray(json).toString(2);
            } catch (Exception ex) {
                return json;
            }
        }
    }

    /**
     * Calls the OpenAI API via Vercel proxy to get a text response.
     * <p>
     * This method handles the network communication with the Vercel proxy,
     * which injects the API key server-side to keep it secure.
     *
     * @param chatRequest JSON object containing the chat completion request
     * @return The text content from the AI's response
     * @throws IOException If the request fails or response cannot be parsed
     */
    private static String callOpenAIText(JSONObject chatRequest) throws IOException {
        Headers headers = new Headers.Builder()
                .add("Content-Type", "application/json")
                .add("User-Agent", "Wellnest/1.0 (Android)")
                .build();

        RequestBody body = RequestBody.create(chatRequest.toString(), JSON);
        Request req = new Request.Builder()
                .url(OPENAI_PROXY_URL)
                .headers(headers)
                .post(body)
                .build();

        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("OpenAI proxy error: " + resp.code() + " " + safeBody(resp));
            }
            String text = safeBody(resp);
            try {
                JSONObject root = new JSONObject(text);
                JSONArray choices = root.optJSONArray("choices");
                if (choices == null || choices.length() == 0) {
                    throw new IOException("OpenAI: no choices");
                }
                JSONObject message = choices.getJSONObject(0).getJSONObject("message");
                return message.optString("content", "").trim();
            } catch (JSONException je) {
                throw new IOException("OpenAI parse error: " + je.getMessage() + "\n" + text);
            }
        }
    }

    // ============================================================
    // PRIVATE HELPERS - OpenAI Integration
    // ============================================================

    /**
     * Calls OpenAI with retry logic for improved reliability.
     * <p>
     * Retries failed requests with exponential backoff to handle transient
     * network issues or rate limits.
     *
     * @param chatRequest JSON object containing the chat completion request
     * @param maxAttempts Maximum number of retry attempts
     * @return The text content from the AI's response
     * @throws IOException If all retry attempts fail
     */
    private static String callOpenAITextWithRetry(JSONObject chatRequest, int maxAttempts) throws IOException {
        IOException lastException = null;
        for (int i = 1; i <= maxAttempts; i++) {
            try {
                return callOpenAIText(chatRequest);
            } catch (IOException ioe) {
                lastException = ioe;
                try {
                    Thread.sleep(800L * i);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw (lastException != null ? lastException : new IOException("interrupted"));
                }
            }
        }
        throw lastException != null ? lastException : new IOException("unknown failure");
    }

    /**
     * Uses GPT-5-nano to generate a concise web search query.
     * <p>
     * This method is optimized for quick query generation using the smaller
     * and faster nano model.
     *
     * @param instruction Instruction describing what to search for
     * @return A concise search query string
     * @throws IOException If the request fails
     */
    private static String askNanoForQuery(String instruction) throws IOException {
        try {
            JSONObject req = new JSONObject().put("model", "gpt-5-nano");
            JSONArray messages = new JSONArray()
                    .put(new JSONObject()
                            .put("role", "system")
                            .put("content", "You write concise web search queries. Output only the query text, nothing else."))
                    .put(new JSONObject()
                            .put("role", "user")
                            .put("content", instruction));
            req.put("messages", messages);
            return callOpenAITextWithRetry(req, 3).trim().replace("\n", " ");
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    /**
     * Uses GPT-5-nano to generate strict JSON output.
     * <p>
     * This method instructs the model to output only valid JSON without any
     * surrounding text or code fences. It also strips code fences if present.
     *
     * @param instruction Instruction for what JSON to generate
     * @return Strict JSON string (without code fences or commentary)
     * @throws IOException If the request fails or JSON cannot be extracted
     */
    private static String askNanoForStrictJson(String instruction) throws IOException {
        try {
            JSONObject req = new JSONObject().put("model", "gpt-5-nano");
            JSONArray messages = new JSONArray()
                    .put(new JSONObject()
                            .put("role", "system")
                            .put("content", "You output ONLY strict JSON. No code fences. No commentary."))
                    .put(new JSONObject()
                            .put("role", "user")
                            .put("content", instruction));
            req.put("messages", messages);
            String out = callOpenAITextWithRetry(req, 3).trim();

            // Strip code fences if present
            if (out.startsWith("```")) {
                int first = out.indexOf('{');
                int last = out.lastIndexOf('}');
                if (first >= 0 && last >= first) {
                    out = out.substring(first, last + 1);
                }
            }
            return out;
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    /**
     * Performs a web search using Tavily API via Vercel proxy.
     * <p>
     * Tavily is optimized for AI applications and returns structured results
     * suitable for synthesis by language models.
     *
     * @param query The search query string
     * @return JSON object containing search results
     * @throws IOException If the search request fails
     */
    private static JSONObject tavilySearch(String query) throws IOException {
        try {
            // Build request (no api_key here; proxy injects it)
            JSONObject payload = new JSONObject()
                    .put("query", query)
                    .put("search_depth", "basic")
                    .put("include_answer", true)
                    .put("include_images", false)
                    .put("max_results", 12);

            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request req = new Request.Builder()
                    .url(TAVILY_PROXY_URL)
                    .post(body)
                    .build();

            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    throw new IOException("Tavily proxy error: " + resp.code() + " " + safeBody(resp));
                }
                String text = safeBody(resp);
                return new JSONObject(text);
            }
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    // ============================================================
    // PRIVATE HELPERS - Tavily Search Integration
    // ============================================================

    /**
     * Fetches weather data from Open-Meteo and formats it as a summary string.
     * <p>
     * Open-Meteo provides free weather data without requiring authentication.
     * This method requests current weather plus hourly forecasts.
     *
     * @param lat Latitude of the location
     * @param lon Longitude of the location
     * @return Human-readable weather summary (e.g., "Clear, 22°C, wind 15 km/h")
     * @throws IOException If the weather API request fails
     */
    private static String fetchWeatherSummary(double lat, double lon) throws IOException {
        String url = OPEN_METEO_URL +
                "?latitude=" + lat +
                "&longitude=" + lon +
                "&current_weather=true" +
                "&hourly=temperature_2m,precipitation_probability,weathercode,wind_speed_10m";

        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                return "weather unavailable";
            }
            String text = safeBody(resp);
            try {
                JSONObject root = new JSONObject(text);
                JSONObject cw = root.optJSONObject("current_weather");
                if (cw == null) {
                    return "weather unavailable";
                }
                double temp = cw.optDouble("temperature", Double.NaN);
                double wind = cw.optDouble("windspeed", Double.NaN);
                int wcode = cw.optInt("weathercode", -1);
                String desc = weatherCodeToText(wcode);
                if (!Double.isNaN(temp) && !Double.isNaN(wind)) {
                    return String.format(Locale.US, "%s, %.0f°C, wind %.0f km/h", desc, temp, wind);
                }
                return desc;
            } catch (JSONException e) {
                return "weather unavailable";
            }
        }
    }

    // ============================================================
    // PRIVATE HELPERS - Weather API Integration
    // ============================================================

    /**
     * Normalizes AI evaluation responses to "pass" or "fail".
     * <p>
     * Handles various response formats from the AI model and converts them
     * to a consistent pass/fail result.
     *
     * @param raw Raw response text from the AI
     * @return "pass" or "fail"
     */
    private static String normalizePassFail(String raw) {
        String s = raw.trim().toLowerCase(Locale.US);
        Log.d(TAG, s);
        if (s.contains("pass") && !s.contains("fail")) return "pass";
        if (s.matches("^pass\\b.*")) return "pass";
        if (s.contains("fail")) return "fail";
        return s.equals("pass") ? "pass" : "fail";
    }

    // ============================================================
    // PRIVATE HELPERS - Utility Methods
    // ============================================================

    /**
     * Converts Open-Meteo weather codes to human-readable descriptions.
     * <p>
     * Weather codes follow the WMO Weather interpretation codes standard.
     *
     * @param code WMO weather code
     * @return Human-readable weather description
     */
    private static String weatherCodeToText(int code) {
        if (code == 0) return "Clear";
        if (code == 1 || code == 2) return "Mainly clear/Partly cloudy";
        if (code == 3) return "Overcast";
        if (code >= 45 && code <= 48) return "Foggy";
        if (code >= 51 && code <= 67) return "Drizzle/Rain";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 80 && code <= 82) return "Rain showers";
        if (code >= 85 && code <= 86) return "Snow showers";
        if (code >= 95) return "Thunderstorm";
        return "Mixed conditions";
    }

    /**
     * Safely extracts response body as string.
     *
     * @param resp HTTP response object
     * @return Response body as string, or empty string if body is null
     * @throws IOException If reading the response body fails
     */
    private static String safeBody(Response resp) throws IOException {
        return resp.body() == null ? "" : resp.body().string();
    }

    /**
     * Creates a JSON object representing an image URL part for GPT vision.
     * <p>
     * Encodes JPEG bytes as a base64 data URL suitable for GPT-4o's vision API.
     *
     * @param jpegBytes JPEG image data as byte array
     * @return JSON object with image_url structure
     * @throws JSONException If JSON creation fails
     */
    private static JSONObject createImageUrlPart(byte[] jpegBytes) throws JSONException {
        String dataUrl = "data:image/jpeg;base64," +
                android.util.Base64.encodeToString(jpegBytes, android.util.Base64.NO_WRAP);
        return new JSONObject()
                .put("type", "image_url")
                .put("image_url", new JSONObject()
                        .put("url", dataUrl)
                        .put("detail", "low"));
    }
    // ============================================================
    // PRIVATE HELPERS - Location Services
    // ============================================================

    /**
     * Checks if the app has location permissions.
     *
     * @param context Android Context
     * @return true if either FINE or COARSE location permission is granted
     */
    private static boolean hasLocationPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Gets the current location using FusedLocationProviderClient.
     * <p>
     * This method uses a synchronous approach with a timeout to get the location.
     * It prioritizes accuracy using PRIORITY_HIGH_ACCURACY.
     *
     * @param context Android Context for location services
     * @return Current Location object, or null if location cannot be obtained
     */
    private static Location getCurrentLocation(Context context) {
        try {
            FusedLocationProviderClient fusedLocationClient =
                    LocationServices.getFusedLocationProviderClient(context);

            // Check permissions again (defensive programming)
            if (!hasLocationPermission(context)) {
                return null;
            }

            // Get current location with high accuracy priority
            @SuppressWarnings("MissingPermission")
            Task<Location> locationTask = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
            );

            // Wait for the result with a timeout (10 seconds)
            Location location = Tasks.await(locationTask, 10, TimeUnit.SECONDS);

            if (location == null) {
                Log.w(TAG, "getCurrentLocation: FusedLocationProviderClient returned null, trying last known location");
                // Fallback to last known location
                @SuppressWarnings("MissingPermission")
                Task<Location> lastLocationTask = fusedLocationClient.getLastLocation();
                location = Tasks.await(lastLocationTask, 5, TimeUnit.SECONDS);
            }

            return location;
        } catch (Exception e) {
            Log.e(TAG, "getCurrentLocation: Failed to get location", e);
            return null;
        }
    }

    /**
     * Converts latitude and longitude to a human-readable location name.
     * <p>
     * Uses Android's Geocoder to perform reverse geocoding. Returns the most
     * specific available location information (locality > admin area > country).
     *
     * @param context   Android Context for Geocoder
     * @param latitude  Latitude coordinate
     * @param longitude Longitude coordinate
     * @return Human-readable location name (e.g., "Toronto, Ontario"), or null if geocoding fails
     */
    private static String getLocationName(Context context, double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());

            // Check if Geocoder is present (not available on all devices/emulators)
            if (!Geocoder.isPresent()) {
                Log.e(TAG, "getLocationName: Geocoder not available on this device");
                return null;
            }

            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses == null || addresses.isEmpty()) {
                Log.w(TAG, "getLocationName: No addresses found for coordinates");
                return null;
            }

            Address address = addresses.get(0);

            // Build location name from most specific to least specific
            // Priority: Locality (city) > Admin Area (state/province) > Country
            String locality = address.getLocality();
            String adminArea = address.getAdminArea();
            String countryName = address.getCountryName();

            if (locality != null && adminArea != null) {
                return locality + ", " + adminArea;
            } else if (locality != null) {
                return locality;
            } else if (adminArea != null) {
                return adminArea + (countryName != null ? ", " + countryName : "");
            } else if (countryName != null) {
                return countryName;
            } else {
                // Fallback to formatted address
                String fullAddress = address.getAddressLine(0);
                return fullAddress != null ? fullAddress : "Unknown Location";
            }
        } catch (IOException e) {
            Log.e(TAG, "getLocationName: Geocoder IOException", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "getLocationName: Unexpected error", e);
            return null;
        }
    }

    /**
     * Geocodes an address string to latitude and longitude coordinates.
     * <p>
     * Uses Android's Geocoder to convert a human-readable address to coordinates.
     *
     * @param context Android Context for Geocoder
     * @param address The address string to geocode
     * @return Array with [latitude, longitude], or null if geocoding fails
     */
    private static double[] geocodeAddress(Context context, String address) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());

            if (!Geocoder.isPresent()) {
                Log.e(TAG, "geocodeAddress: Geocoder not available on this device");
                return null;
            }

            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses == null || addresses.isEmpty()) {
                Log.w(TAG, "geocodeAddress: No coordinates found for address: " + address);
                return null;
            }

            Address addr = addresses.get(0);
            double latitude = addr.getLatitude();
            double longitude = addr.getLongitude();

            Log.d(TAG, String.format(Locale.US, "geocodeAddress: %s -> [%.6f, %.6f]", address, latitude, longitude));
            return new double[]{latitude, longitude};
        } catch (IOException e) {
            Log.e(TAG, "geocodeAddress: IOException for address: " + address, e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "geocodeAddress: Unexpected error for address: " + address, e);
            return null;
        }
    }

    /**
     * Calculates the distance between two geographic coordinates.
     * <p>
     * Uses Android's Location.distanceBetween() method to calculate the distance
     * in meters between two points specified by their latitude and longitude.
     *
     * @param startLat Starting point latitude
     * @param startLon Starting point longitude
     * @param endLat   Ending point latitude
     * @param endLon   Ending point longitude
     * @return Distance in meters between the two points
     */
    private static float calculateDistance(double startLat, double startLon, double endLat, double endLon) {
        float[] results = new float[1];
        Location.distanceBetween(startLat, startLon, endLat, endLon, results);
        return results[0];
    }


    /**
     * Test hook interface for overriding SnapTask evaluation.
     * Implement this interface in tests to provide custom evaluation logic.
     */
    public interface SnapTaskEvaluationOverride {
        /**
         * Evaluates whether a SnapTask was completed successfully.
         *
         * @param criteria   The acceptance criteria for the task
         * @param beforeJpeg JPEG image data before task completion
         * @param afterJpeg  JPEG image data after task completion
         * @return "pass" or "fail" evaluation result
         */
        String evaluate(String criteria, byte[] beforeJpeg, byte[] afterJpeg);
    }

    /**
     * Callback interface for reporting progress during long-running AI operations.
     */
    public interface ProgressCallback {
        /**
         * Called to report progress.
         *
         * @param percent Progress percentage (0-100)
         * @param message Status message describing the current step
         */
        void onProgress(int percent, String message);
    }
}
