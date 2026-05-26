package com.example.mirrortalk26.util;

/**
 * Single source of truth for the confidence-score formula.
 *
 * Previously the formula was copy-pasted (slightly differently) in three places:
 *   ResultFragment, CompareFragment, and SessionAdapter.
 * CompareFragment and SessionAdapter used a simplified check
 *   "else if (wpm >= 100) wpmScore = 75f"
 * which never penalised WPM > 180, producing scores different from ResultFragment.
 *
 * All three screens now call ScoreUtils.computeScore() and are guaranteed to agree.
 *
 * Scoring weights:
 *   Eye contact  : 40 %   (most impactful for perceived confidence)
 *   WPM          : 35 %
 *   Filler words : 25 %
 *
 * WPM scoring bands:
 *   120–160 wpm  → 100  (ideal conversational pace)
 *   100–119 wpm  → 75   (slightly slow but clear)
 *   161–180 wpm  → 75   (slightly fast but still understandable)
 *   > 0 wpm      → 40   (too slow or too fast)
 *   0 wpm        → 0    (no speech detected)
 *
 * Filler score: 100 − (fillerCount × 10), floored at 0.
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
                + (wpmScore          * 0.35f)
                + (fillerScore       * 0.25f));
    }

    /** Human-readable label for a given score. */
    public static String scoreLabel(int score) {
        if (score >= 80) return "Excellent 🏆";
        if (score >= 60) return "Good 💪";
        if (score >= 40) return "Getting there 📈";
        return "Keep going 🎯";
    }
}