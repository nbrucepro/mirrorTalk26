package com.example.mirrortalk26.analysis;

import android.util.Log;

import com.example.mirrortalk26.ApiKeyConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AiCoachHelper {

    private static final String TAG = "GEMINI_API";

    public interface CoachCallback {
        void onResult(String tips);
        void onError(String message);
    }

    private static final ExecutorService pool =
            Executors.newCachedThreadPool();

    private Future<?> currentTask;

    public void fetchTips(
            int wpm,
            int fillerCount,
            float eyeContactPct,
            String transcript,
            CoachCallback callback
    ) {

        // Cancel previous request if it's still running
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }

        currentTask = pool.submit(() -> {
            try {
                String prompt = buildPrompt(
                        wpm,
                        fillerCount,
                        eyeContactPct,
                        transcript
                );

                // REFACTORED: Updated model version to gemini-2.5-flash to resolve HTTP 404
                URL url = new URL(
                        "https://generativelanguage.googleapis.com/v1beta/models/" +
                                "gemini-2.5-flash:generateContent?key=" +
                                ApiKeyConfig.GEMINI_API_KEY
                );

                Log.d(TAG, "Request URL: " + url);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setDoOutput(true);

                // Build Request Body
                JSONObject body = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                content.put("role", "user");

                JSONArray parts = new JSONArray();
                JSONObject textPart = new JSONObject();
                textPart.put(
                        "text",
                        "You are a professional public speaking coach.\n\n" +
                                "Reply with EXACTLY 3 short actionable coaching tips.\n" +
                                "Each tip must start with an emoji.\n" +
                                "No numbering.\n" +
                                "No headers.\n\n" +
                                prompt
                );

                parts.put(textPart);
                content.put("parts", parts);
                contents.put(content);
                body.put("contents", contents);

                Log.d(TAG, "Request Body: " + body.toString());

                byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(input);
                }

                int status = conn.getResponseCode();
                Log.d(TAG, "HTTP Status: " + status);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                status >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                                StandardCharsets.UTF_8
                        )
                );

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                Log.d(TAG, "Raw Response: " + sb.toString());

                if (status != 200) {
                    Log.e(TAG, "Gemini API Error");
                    callback.onError(
                            "Gemini API error\n\n" +
                                    "Status: " + status +
                                    "\n\nResponse:\n" +
                                    sb
                    );
                    return;
                }

                // Parse response
                JSONObject response = new JSONObject(sb.toString());
                String tips = response
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                        .trim();

                Log.d(TAG, "Parsed Tips: " + tips);
                callback.onResult(tips);

            } catch (Exception e) {
                Log.e(TAG, "Exception happened", e);
                if (!Thread.currentThread().isInterrupted()) {
                    callback.onError("Exception:\n\n" + e.toString());
                }
            }
        });
    }

    public void cancel() {
        if (currentTask != null) {
            currentTask.cancel(true);
        }
    }

    private String buildPrompt(
            int wpm,
            int fillerCount,
            float eyeContact,
            String transcript
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("My speaking session metrics:\n");
        sb.append("- Words per minute: ").append(wpm).append("\n");
        sb.append("- Filler words used: ").append(fillerCount).append("\n");
        sb.append("- Eye contact: ").append((int) eyeContact).append("%\n");

        if (transcript != null && transcript.length() > 20) {
            String excerpt = transcript.length() > 400
                    ? transcript.substring(0, 400) + "..."
                    : transcript;
            sb.append("- Transcript excerpt: \"").append(excerpt).append("\"\n");
        }

        sb.append("\nGive me 3 personalised coaching tips for my next session.");
        return sb.toString();
    }
}