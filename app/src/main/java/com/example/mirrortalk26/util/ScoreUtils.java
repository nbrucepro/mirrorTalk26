package com.example.mirrortalk26.util;

/**
 * Single source of truth for the confidence-score formula.
 * Used by ResultFragment, CompareFragment, SessionAdapter, and HistoryFragment chart.
 *
 * Scoring weights:
 *   Eye contact  : 40%
 *   WPM          : 35%
 *   Filler words : 25%
 */
public final class ScoreUtils {

    private ScoreUtils() {}

    public static int computeScore(float averageWpm, int fillerCount, float eyeContactPercent) {
        int wpm = (int) averageWpm;

        float wpmScore;
        if      (wpm >= 120 && wpm <= 160) wpmScore = 100f;
        else if (wpm >= 100 && wpm <  120) wpmScore = 75f;
        else if (wpm >  160 && wpm <= 180) wpmScore = 75f;
        else if (wpm > 0)                  wpmScore = 40f;
        else                               wpmScore = 0f;

        float fillerScore = Math.max(0f, 100f - (fillerCount * 10f));

        return (int) ((eyeContactPercent * 0.40f)
                + (wpmScore            * 0.35f)
                + (fillerScore         * 0.25f));
    }

    /** Human-readable label (no emoji — use vector icons in the UI). */
    public static String scoreLabel(int score) {
        if (score >= 80) return "Excellent";
        if (score >= 60) return "Good";
        if (score >= 40) return "Getting there";
        return "Keep going";
    }

    /** Returns the colour int for a given score (for programmatic tinting). */
    public static int scoreColor(int score) {
        if (score >= 80) return 0xFF1D9E75;
        if (score >= 60) return 0xFF7F77DD;
        if (score >= 40) return 0xFFFFCC44;
        return 0xFFFF6B6B;
    }
}
