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

    private int totalWordCount = 0;


    private int fillerWordCount = 0;
    private long startTimeMs = 0;
    private final StringBuilder fullTranscript = new StringBuilder();

    public void start(){
        totalWordCount = 0;
        fillerWordCount = 0;
        startTimeMs = System.currentTimeMillis();
        fullTranscript.setLength(0);
    }
    public void processText(String newText){
        if (newText == null || newText.trim().isEmpty()) return;

        fullTranscript.append(newText).append(" ");
        String lower = newText.toLowerCase().trim();
        String[] words = lower.split("\\s+");

        totalWordCount += words.length;

        // Check single words
        for (String word:words){
            String clean = word.replaceAll("[^a-z]","");
            if (FILLER_WORDS.contains(clean)) fillerWordCount++;
        }
        // Check two-word phrases (e.g. "you know", "kind of")
        for(int i =0; i<words.length - 1; i++){
            String phrase = words[i] + " " + words[i+1];
            if (FILLER_WORDS.contains(phrase)) fillerWordCount++;
        }
    }
    public float getCurrentWpm(){
        long elapsedMs = System.currentTimeMillis() - startTimeMs;
        float minutes = elapsedMs / 60000f;
        return (minutes > 0) ? totalWordCount /minutes : 0f;
    }
    public int getTotalWordCount() {
        return totalWordCount;
    }

    public int getFillerWordCount() {
        return fillerWordCount;
    }

    public String getTranscript() {
        return fullTranscript.toString().trim();
    }

    public String getWpmFeedback(float wpm){
        if (wpm < 100) return "Too slow -- speed up a little";
        if (wpm > 180) return "Too fast -- slow down";
        return "Great pace";
    }
}

