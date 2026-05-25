package com.example.mirrortalk26.analysis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpeechAnalyzer {

    // BUG 2 FIX: Removed "actually" — it is a normal English word, not a filler.
    // Also cleaned up the set to keep only genuine filler/hedge words.
    private static final Set<String> FILLER_WORDS = new HashSet<>(Arrays.asList(
            "um", "uh", "like", "basically", "literally",
            "right", "so", "you know", "kind of",
            "sort of", "i mean", "well"
    ));

    private int totalWordCount  = 0;
    private int fillerWordCount = 0;
    private long startTimeMs    = 0;
    private String lastPartial  = "";
    private final StringBuilder fullTranscript = new StringBuilder();

    // NEW FEATURE: Track per-word filler counts for breakdown display
    private final Map<String, Integer> fillerBreakdown = new HashMap<>();

    public void start() {
        totalWordCount  = 0;
        fillerWordCount = 0;
        startTimeMs     = System.currentTimeMillis();
        lastPartial     = "";
        fullTranscript.setLength(0);
        fillerBreakdown.clear();
    }

    public void processText(String newText) {
        if (newText == null || newText.trim().isEmpty()) return;

        String lower     = newText.toLowerCase().trim();
        String lastLower = lastPartial.toLowerCase().trim();

        String newWords;
        if (!lastLower.isEmpty() && lower.startsWith(lastLower)) {
            newWords = lower.substring(lastLower.length()).trim();
        } else {
            newWords = lower;
        }
        lastPartial = lower;

        if (newWords.isEmpty()) return;

        fullTranscript.append(newWords).append(" ");
        String[] words = newWords.split("\\s+");
        totalWordCount += words.length;

        // BUG 2 FIX: Single-word filler check (with punctuation stripped)
        for (String word : words) {
            String clean = word.replaceAll("[^a-z]", "");
            if (FILLER_WORDS.contains(clean)) {
                fillerWordCount++;
                fillerBreakdown.merge(clean, 1, Integer::sum);
            }
        }

        // BUG 2 FIX: Phrase check — clean words before building bigrams to avoid
        // punctuation causing misses (e.g. "you," + "know" was never matching)
        for (int i = 0; i < words.length - 1; i++) {
            String w1     = words[i].replaceAll("[^a-z]", "");
            String w2     = words[i + 1].replaceAll("[^a-z]", "");
            String phrase = w1 + " " + w2;
            if (FILLER_WORDS.contains(phrase)) {
                fillerWordCount++;
                fillerBreakdown.merge(phrase, 1, Integer::sum);
            }
        }
    }

    public void resetPartial() { lastPartial = ""; }

    public float getCurrentWpm() {
        long elapsedMs = System.currentTimeMillis() - startTimeMs;
        float minutes  = elapsedMs / 60000f;
        if (minutes < 0.05f) return 0f;
        float wpm = totalWordCount / minutes;
        return Math.min(wpm, 350f);
    }

    public int getFillerWordCount()              { return fillerWordCount; }
    public int getTotalWordCount()               { return totalWordCount; }
    public String getTranscript()                { return fullTranscript.toString().trim(); }
    public Map<String, Integer> getFillerBreakdown() { return new HashMap<>(fillerBreakdown); }

    public String getWpmFeedback(float wpm) {
        if (wpm < 100) return "Too slow — speed up a little";
        if (wpm > 180) return "Too fast — slow down";
        return "Great pace!";
    }

    /**
     * NEW FEATURE: Static helper — recomputes per-word filler counts from a stored
     * transcript string. Used in ResultFragment to show breakdown for saved sessions.
     */
    public static Map<String, Integer> analyzeFillers(String transcript) {
        Map<String, Integer> breakdown = new HashMap<>();
        if (transcript == null || transcript.trim().isEmpty()) return breakdown;

        String[] words = transcript.toLowerCase().trim().split("\\s+");

        for (String word : words) {
            String clean = word.replaceAll("[^a-z]", "");
            if (FILLER_WORDS.contains(clean)) {
                breakdown.merge(clean, 1, Integer::sum);
            }
        }
        for (int i = 0; i < words.length - 1; i++) {
            String w1     = words[i].replaceAll("[^a-z]", "");
            String w2     = words[i + 1].replaceAll("[^a-z]", "");
            String phrase = w1 + " " + w2;
            if (FILLER_WORDS.contains(phrase)) {
                breakdown.merge(phrase, 1, Integer::sum);
            }
        }
        return breakdown;
    }
}
