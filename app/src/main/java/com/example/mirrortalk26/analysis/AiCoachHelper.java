package com.example.mirrortalk26.analysis;

import com.example.mirrortalk26.ApiKeyConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Calls the Anthropic Messages API on a background thread and returns
 * 3 personalised coaching tips based on the session's metrics.
 *
 * IMPORTANT: Callbacks (onResult / onError) are delivered on a background
 * thread — always wrap UI updates in Activity.runOnUiThread() at the call site.
 */
public class AiCoachHelper {

    public interface CoachCallback {
        void onResult(String tips);   // newline-separated tips
        void onError(String message);
    }

    private static final Executor executor = Executors.newSingleThreadExecutor();

    /**
     * @param wpm           average words per minute for the session
     * @param fillerCount   total filler words detected
     * @param eyeContactPct eye-contact percentage (0–100)
     * @param transcript    full session transcript (may be empty)
     * @param callback      delivered on a background thread; wrap in runOnUiThread
     */
    public static void fetchTips(int wpm, int fillerCount, float eyeContactPct,
                                 String transcript, CoachCallback callback) {
        executor.execute(() -> {
            try {
                String prompt = buildPrompt(wpm, fillerCount, eyeContactPct, transcript);

                URL url = new URL("https://api.anthropic.com/v1/messages");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("x-api-key", ApiKeyConfig.ANTHROPIC_API_KEY);
                conn.setRequestProperty("anthropic-version", "2023-06-01");
                conn.setConnectTimeout(15_000);
                conn.setReadTimeout(30_000);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("model", "claude-sonnet-4-20250514");
                body.put("max_tokens", 400);

                JSONArray messages = new JSONArray();
                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                userMsg.put("content", prompt);
                messages.put(userMsg);
                body.put("messages", messages);

                body.put("system",
                        "You are a professional public speaking coach. " +
                                "The user just finished a practice session. " +
                                "Reply with EXACTLY 3 short, specific, actionable tips — one per line. " +
                                "Start each line with a relevant emoji. " +
                                "Be encouraging but honest. Do NOT add headers, numbering, or extra text.");

                byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(input);
                }

                int status = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        status >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                        StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                if (status != 200) {
                    callback.onError("API error " + status);
                    return;
                }

                JSONObject response = new JSONObject(sb.toString());
                JSONArray  content  = response.getJSONArray("content");
                String     tips     = content.getJSONObject(0).getString("text").trim();
                callback.onResult(tips);

            } catch (Exception e) {
                callback.onError("Could not reach coaching server: " + e.getMessage());
            }
        });
    }

    private static String buildPrompt(int wpm, int fillerCount,
                                      float eyeContact, String transcript) {
        StringBuilder sb = new StringBuilder();
        // FIX 2: Was "\\n" (literal backslash-n) — must be "\n" (real newline)
        // so the API receives a properly formatted prompt, not garbled text.
        sb.append("My speaking session metrics:\n");
        sb.append("- Words per minute: ").append(wpm).append("\n");
        sb.append("- Filler words used: ").append(fillerCount).append("\n");
        sb.append("- Eye contact: ").append((int) eyeContact).append("%\n");
        if (transcript != null && transcript.length() > 20) {
            String excerpt = transcript.length() > 400
                    ? transcript.substring(0, 400) + "…"
                    : transcript;
            sb.append("- Transcript excerpt: \"").append(excerpt).append("\"\n");
        }
        sb.append("\nGive me 3 personalised coaching tips for my next session.");
        return sb.toString();
    }
}