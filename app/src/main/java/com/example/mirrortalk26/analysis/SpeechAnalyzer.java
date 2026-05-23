package com.example.mirrortalk26.analysis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SpeechAnalyzer {

    private static final Set<String> FILLER_WORDS = new HashSet<>(Arrays.asList(
            "um", "uh", "like", "basically", "literally",
            "actually", "right", "so", "you know", "kind of",
            "sort of", "i mean", "well"
    ));

    private int totalWordCount  = 0;
    private int fillerWordCount = 0;
    private long startTimeMs    = 0;
    private String lastPartial  = ""; // ← tracks last partial to avoid double-counting
    private final StringBuilder fullTranscript = new StringBuilder();

    public void start() {
        totalWordCount  = 0;
        fillerWordCount = 0;
        startTimeMs     = System.currentTimeMillis();
        lastPartial     = "";
        fullTranscript.setLength(0);
    }

    public void processText(String newText) {
        if (newText == null || newText.trim().isEmpty()) return;

        String lower    = newText.toLowerCase().trim();
        String lastLower = lastPartial.toLowerCase().trim();

        // Only count words that are NEW since the last partial result
        String newWords;
        if (!lastLower.isEmpty() && lower.startsWith(lastLower)) {
            newWords = lower.substring(lastLower.length()).trim();
        } else {
            newWords = lower; // New utterance after restart
        }
        lastPartial = lower;

        if (newWords.isEmpty()) return;

        fullTranscript.append(newWords).append(" ");
        String[] words = newWords.split("\\s+");
        totalWordCount += words.length;

        for (String word : words) {
            String clean = word.replaceAll("[^a-z]", "");
            if (FILLER_WORDS.contains(clean)) fillerWordCount++;
        }
        for (int i = 0; i < words.length - 1; i++) {
            String phrase = words[i] + " " + words[i + 1];
            if (FILLER_WORDS.contains(phrase)) fillerWordCount++;
        }
    }

    // Reset partial when recognizer restarts
    public void resetPartial() { lastPartial = ""; }

    public float getCurrentWpm() {
        long elapsedMs = System.currentTimeMillis() - startTimeMs;
        float minutes  = elapsedMs / 60000f;
        if (minutes < 0.05f) return 0f; // need at least 3 seconds
        float wpm = totalWordCount / minutes;
        return Math.min(wpm, 350f); // cap at 350 WPM — human max
    }

    public int getFillerWordCount()  { return fillerWordCount; }
    public int getTotalWordCount()   { return totalWordCount; }
    public String getTranscript()    { return fullTranscript.toString().trim(); }

    public String getWpmFeedback(float wpm) {
        if (wpm < 100) return "Too slow — speed up a little";
        if (wpm > 180) return "Too fast — slow down";
        return "Great pace!";
    }
}