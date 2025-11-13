package com.code.wlu.cp470.wellnest.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * WellnestAiClient (Vercel-proxied)
 * - OpenAI and Tavily go through your Vercel functions (no keys in app).
 * - Open-Meteo stays direct.
 * - Includes: URL field for plan ideas, address in walk, nano fixes, timeouts, retries.
 */
public abstract class WellnestAiClient {

    // ===== Your Vercel base URL =====
    private static final String VERCEL_BASE_URL = "https://wellnest-proxy.vercel.app/";

    // ===== Proxy endpoints =====
    private static final String OPENAI_PROXY_URL = VERCEL_BASE_URL + "/api/openai-chat";
    private static final String TAVILY_PROXY_URL = VERCEL_BASE_URL + "/api/tavily-search";

    // ===== Direct (no key) =====
    private static final String OPEN_METEO_URL = "https://api.open-meteo.com/v1/forecast";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int GPT_RETRY_MAX = 3;
    private static final long GPT_RETRY_DELAY_MS = 1200L;

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(40, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .pingInterval(15, TimeUnit.SECONDS)
            .build();

    // ===== Utilities =====

    private static JSONObject imageUrlPart(byte[] jpegBytes) throws JSONException {
        String dataUrl = "data:image/jpeg;base64," +
                android.util.Base64.encodeToString(jpegBytes, android.util.Base64.NO_WRAP);
        return new JSONObject()
                .put("type", "image_url")
                .put("image_url", new JSONObject()
                        .put("url", dataUrl)
                        .put("detail", "low"));
    }

    private static String normalizePassFail(String raw) {
        String s = raw.trim().toLowerCase(Locale.US);
        if (s.contains("pass") && !s.contains("fail")) return "pass";
        if (s.matches("^pass\\b.*")) return "pass";
        if (s.contains("fail")) return "fail";
        return s.equals("pass") ? "pass" : "fail";
    }

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

    private static String safeBody(Response resp) throws IOException {
        return resp.body() == null ? "" : resp.body().string();
    }

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

    // ===== Public API =====

    /**
     * Flow #1 — SnapTask vision verifier (GPT-4o). Retries; after 3 failures returns "pass".
     */
    public String evaluateSnapTask(String criteria, byte[] beforeJpeg, byte[] afterJpeg) {
        for (int attempt = 1; attempt <= GPT_RETRY_MAX; attempt++) {
            try {
                JSONObject req = new JSONObject()
                        .put("model", "gpt-4o")
                        .put("temperature", 0.0);

                JSONArray messages = new JSONArray();
                messages.put(new JSONObject()
                        .put("role", "system")
                        .put("content",
                                "You are an image judge for household chores. " +
                                        "You will receive: (1) acceptance criteria text, (2) BEFORE photo, (3) AFTER photo. " +
                                        "Decide if the AFTER photo satisfies the criteria compared to BEFORE. " +
                                        "Reply with exactly one word: pass or fail. No punctuation, no explanation."));

                JSONArray content = new JSONArray()
                        .put(new JSONObject().put("type", "text").put("text", "Criteria:\n" + criteria))
                        .put(new JSONObject().put("type", "text").put("text", "BEFORE photo:"))
                        .put(imageUrlPart(beforeJpeg))
                        .put(new JSONObject().put("type", "text").put("text", "AFTER photo:"))
                        .put(imageUrlPart(afterJpeg));

                messages.put(new JSONObject().put("role", "user").put("content", content));
                req.put("messages", messages);

                String text = callOpenAIText(req);
                return normalizePassFail(text);
            } catch (Exception e) {
                if (attempt == GPT_RETRY_MAX) return "pass";
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
     * Flow #2 — Things to do (adds `url` per idea).
     */
    public String planThingsToDo(String locationName,
                                 double latitude,
                                 double longitude,
                                 String isoDate,
                                 String timeOfDay) throws IOException {
        String weatherSummary = fetchWeatherSummary(latitude, longitude);

        String searchPrompt = String.format(Locale.US,
                "Create a single concise web search query for interesting things to do in %s on %s in the %s, " +
                        "considering the weather: %s. Focus on current, local options. Return ONLY the query text.",
                locationName, isoDate, timeOfDay, weatherSummary);
        String query = askNanoForQuery(searchPrompt);

        JSONObject tavily = tavilySearch(query);

        String synthPrompt =
                "Using the Tavily results (JSON below) and the context, return ONLY a JSON object with exactly these top-level keys: " +
                        "[\"Explore\",\"Nightlife\",\"Play\",\"Cozy\",\"Culture\"]. " +
                        "Each key maps to an array of 3–10 objects with this exact shape:\n" +
                        "{\"title\": string, \"why\": string, \"tag\": \"solo\"|\"friends\"|\"family\", \"url\": string}\n\n" +
                        "Rules:\n" +
                        "- For each idea, choose ONE source URL from the Tavily results (prefer the official site). Output a full http(s) URL.\n" +
                        "- Make ideas feasible given the date/time/weather.\n\n" +
                        "Context:\n" +
                        "location: " + locationName + "\n" +
                        "date: " + isoDate + "\n" +
                        "time_of_day: " + timeOfDay + "\n" +
                        "weather: " + weatherSummary + "\n\n" +
                        "Tavily JSON:\n" + tavily.toString();

        return askNanoForStrictJson(synthPrompt);
    }

    /**
     * Flow #3 — Walking spot pick (+address) and short story.
     */
    public String pickWalkAndStory(String locationName, String isoTime) throws IOException {
        String qPrompt = String.format(Locale.US,
                "Create one concise web search query to find scenic, safe walking spots in %s around %s. " +
                        "Prefer parks, river paths, waterfronts, or historic districts. " +
                        "Return ONLY the query text.", locationName, isoTime);
        String query = askNanoForQuery(qPrompt);

        JSONObject tavily = tavilySearch(query);

        String choosePrompt =
                "From these Tavily results, pick ONE great walking location. Return ONLY JSON with:\n" +
                        "{\n" +
                        "  \"query\": string,\n" +
                        "  \"pick\": {\"name\": string, \"address\": string, \"why_now\": string, \"tips\": string},\n" +
                        "  \"story\": string  // an interesting backstory & history in 4–7 sentences, friendly tone\n" +
                        "}\n\n" +
                        "If multiple addresses exist, choose the most official/precise formatted address. Keep fields concise.\n\n" +
                        "Tavily JSON:\n" + tavily.toString();

        return askNanoForStrictJson(choosePrompt.replace("//", "#"));
    }

    /**
     * Public getter so tests/screens can fetch a weather string.
     */
    public String getWeatherSummary(double lat, double lon) throws IOException {
        return fetchWeatherSummary(lat, lon);
    }

    // ===== OpenAI (nano) helpers =====

    private String askNanoForQuery(String instruction) throws IOException {
        try {
            JSONObject req = new JSONObject().put("model", "gpt-5-nano"); // no temperature for nano
            JSONArray messages = new JSONArray()
                    .put(new JSONObject().put("role", "system")
                            .put("content", "You write concise web search queries. Output only the query text, nothing else."))
                    .put(new JSONObject().put("role", "user").put("content", instruction));
            req.put("messages", messages);
            return callOpenAITextWithRetry(req, 3).trim().replace("\n", " ");
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    private String askNanoForStrictJson(String instruction) throws IOException {
        try {
            JSONObject req = new JSONObject().put("model", "gpt-5-nano");
            JSONArray messages = new JSONArray()
                    .put(new JSONObject().put("role", "system")
                            .put("content", "You output ONLY strict JSON. No code fences. No commentary."))
                    .put(new JSONObject().put("role", "user").put("content", instruction));
            req.put("messages", messages);
            String out = callOpenAITextWithRetry(req, 3).trim();
            if (out.startsWith("```")) {
                int first = out.indexOf('{'), last = out.lastIndexOf('}');
                if (first >= 0 && last >= first) out = out.substring(first, last + 1);
            }
            return out;
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    private String callOpenAITextWithRetry(JSONObject chatRequest, int maxAttempts) throws IOException {
        IOException last = null;
        for (int i = 1; i <= maxAttempts; i++) {
            try {
                return callOpenAIText(chatRequest);
            } catch (IOException ioe) {
                last = ioe;
                try {
                    Thread.sleep(800L * i);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw (last != null ? last : new IOException("interrupted"));
                }
            }
        }
        throw last != null ? last : new IOException("unknown failure");
    }

    /**
     * Uses Vercel proxy (no Authorization header in app). Protected so tests can override/stub.
     */
    protected String callOpenAIText(JSONObject chatRequest) throws IOException {
        Headers headers = new Headers.Builder()
                .add("Content-Type", "application/json")
                .add("User-Agent", "Wellnest/1.0 (Android)")
                .build();

        RequestBody body = RequestBody.create(chatRequest.toString(), JSON);
        Request req = new Request.Builder().url(OPENAI_PROXY_URL).headers(headers).post(body).build();

        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful())
                throw new IOException("OpenAI proxy error: " + resp.code() + " " + safeBody(resp));
            String text = safeBody(resp);
            try {
                JSONObject root = new JSONObject(text);
                JSONArray choices = root.optJSONArray("choices");
                if (choices == null || choices.length() == 0)
                    throw new IOException("OpenAI: no choices");
                JSONObject message = choices.getJSONObject(0).getJSONObject("message");
                return message.optString("content", "").trim();
            } catch (JSONException je) {
                throw new IOException("OpenAI parse error: " + je.getMessage() + "\n" + text);
            }
        }
    }

    // ===== External APIs =====

    private JSONObject tavilySearch(String query) throws IOException {
        try {
            // No api_key here; proxy injects it.
            JSONObject payload = new JSONObject()
                    .put("query", query)
                    .put("search_depth", "basic")
                    .put("include_answer", true)
                    .put("include_images", false)
                    .put("max_results", 6);

            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request req = new Request.Builder().url(TAVILY_PROXY_URL).post(body).build();

            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful())
                    throw new IOException("Tavily proxy error: " + resp.code() + " " + safeBody(resp));
                String text = safeBody(resp);
                return new JSONObject(text);
            }
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    private String fetchWeatherSummary(double lat, double lon) throws IOException {
        String url = OPEN_METEO_URL
                + "?latitude=" + lat
                + "&longitude=" + lon
                + "&current_weather=true"
                + "&hourly=temperature_2m,precipitation_probability,weathercode,wind_speed_10m";

        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) return "weather unavailable";
            String text = safeBody(resp);
            try {
                JSONObject root = new JSONObject(text);
                JSONObject cw = root.optJSONObject("current_weather");
                if (cw == null) return "weather unavailable";
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
}
