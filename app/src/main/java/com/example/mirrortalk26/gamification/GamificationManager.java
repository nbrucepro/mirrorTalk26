package com.example.mirrortalk26.gamification;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.DrawableRes;

import com.example.mirrortalk26.R;
import com.example.mirrortalk26.data.SpeechSession;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Manages XP, streaks, badges, personal bests, and streak-calendar data.
 * All persistence is SharedPreferences — no extra DB table needed.
 *
 * FIXES vs original:
 *   - Badge.emoji (String) replaced with Badge.iconRes (@DrawableRes int)
 *   - getLevel() formula fixed: was Math.max(1, xp/100) → level never reached 2 until 200 XP
 *     now uses (xp / 100) + 1 so Level 2 starts at 100 XP as expected
 *   - lastSessionXp exposed so ResultFragment doesn't need to duplicate the formula
 *   - Personal bests (WPM, eye%, filler) stored and exposed
 *   - Streak-calendar: session epoch-days stored as a Set<String> for the heatmap
 */
public class GamificationManager {

    // ── Badge definitions ─────────────────────────────────────────────────────
    public enum Badge {
        FIRST_STEPS (R.drawable.ic_badge_mic,        "First Steps",        "Complete your first session"),
        CONSISTENT  (R.drawable.ic_badge_calendar,   "Getting Consistent", "Practice 3 days in a row"),
        WEEK_WARRIOR(R.drawable.ic_badge_fire,       "Week Warrior",       "7-day practice streak"),
        EYE_PRO     (R.drawable.ic_badge_eye,        "Eye Contact Pro",    "80%+ eye contact in a session"),
        SPEED_DEMON (R.drawable.ic_badge_bolt,       "Speed Demon",        "Speak faster than 180 WPM"),
        SLOW_BURN   (R.drawable.ic_badge_slow,       "Slow & Clear",       "Speak slower than 100 WPM"),
        CLEAN_SPEECH(R.drawable.ic_badge_clean,      "Clean Speech",       "Zero filler words in a session"),
        CONFIDENT   (R.drawable.ic_badge_trophy,     "Confident Speaker",  "Confidence score >= 80"),
        MARATHON    (R.drawable.ic_badge_timer,      "Marathon Speaker",   "Session longer than 5 minutes");

        @DrawableRes
        public final int iconRes;
        public final String title;
        public final String description;

        Badge(@DrawableRes int iconRes, String title, String description) {
            this.iconRes     = iconRes;
            this.title       = title;
            this.description = description;
        }
    }

    // ── SharedPreferences keys ────────────────────────────────────────────────
    private static final String PREFS         = "mirrortalk_gamification";
    private static final String KEY_XP        = "xp";
    private static final String KEY_STREAK    = "streak_days";
    private static final String KEY_LAST      = "last_session_day";
    private static final String KEY_LAST_XP   = "last_session_xp";
    private static final String KEY_PB_WPM    = "pb_wpm";
    private static final String KEY_PB_EYE    = "pb_eye";
    private static final String KEY_PB_FILLER = "pb_filler";   // lower is better, init -1 = unset
    private static final String KEY_CAL_DAYS  = "calendar_days"; // Set<String> of epoch-day strings
    private static final String BADGE_PFX     = "badge_";

    private final SharedPreferences prefs;

    public GamificationManager(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ── Record a completed session & award XP / badges ────────────────────────
    public List<Badge> recordSession(SpeechSession session, int confidenceScore) {
        List<Badge> newlyEarned = new ArrayList<>();

        // 1. Update streak
        long today = todayEpochDay();
        long last  = prefs.getLong(KEY_LAST, -1);
        int  streak = prefs.getInt(KEY_STREAK, 0);
        if      (last == today - 1) streak++;
        else if (last != today)     streak = 1;
        // if last == today: same day, unchanged

        // 2. Update calendar set (for heatmap)
        java.util.Set<String> calDays = new java.util.HashSet<>(
                prefs.getStringSet(KEY_CAL_DAYS, new java.util.HashSet<>()));
        calDays.add(String.valueOf(today));

        // 3. Calculate XP for this session
        int xpEarned = 10;
        if (confidenceScore >= 60) xpEarned += 5;
        if (confidenceScore >= 80) xpEarned += 10;
        if (session.fillerWordCount == 0)    xpEarned += 5;
        if (session.eyeContactPercent >= 80) xpEarned += 5;
        int totalXp = prefs.getInt(KEY_XP, 0) + xpEarned;

        // 4. Personal bests
        int   pbWpm    = prefs.getInt(KEY_PB_WPM, 0);
        float pbEye    = prefs.getFloat(KEY_PB_EYE, 0f);
        int   pbFiller = prefs.getInt(KEY_PB_FILLER, -1); // -1 = unset

        int   newWpm    = (int) session.averageWpm;
        float newEye    = session.eyeContactPercent;
        int   newFiller = session.fillerWordCount;

        SharedPreferences.Editor editor = prefs.edit()
                .putLong(KEY_LAST,    today)
                .putInt(KEY_STREAK,   streak)
                .putInt(KEY_XP,       totalXp)
                .putInt(KEY_LAST_XP,  xpEarned)
                .putStringSet(KEY_CAL_DAYS, calDays);

        if (newWpm > pbWpm)                         editor.putInt(KEY_PB_WPM,    newWpm);
        if (newEye > pbEye)                         editor.putFloat(KEY_PB_EYE,  newEye);
        if (pbFiller == -1 || newFiller < pbFiller) editor.putInt(KEY_PB_FILLER, newFiller);

        editor.apply();

        // 5. Check & award badges
        newlyEarned.addAll(checkAndAward(session, confidenceScore, streak));
        return newlyEarned;
    }

    private List<Badge> checkAndAward(SpeechSession s, int score, int streak) {
        List<Badge> earned = new ArrayList<>();
        checkBadge(Badge.FIRST_STEPS,  true,                              earned);
        checkBadge(Badge.CONSISTENT,   streak >= 3,                       earned);
        checkBadge(Badge.WEEK_WARRIOR, streak >= 7,                       earned);
        checkBadge(Badge.EYE_PRO,      s.eyeContactPercent >= 80,         earned);
        checkBadge(Badge.SPEED_DEMON,  s.averageWpm > 180,                earned);
        checkBadge(Badge.SLOW_BURN,    s.averageWpm > 0 && s.averageWpm < 100, earned);
        checkBadge(Badge.CLEAN_SPEECH, s.fillerWordCount == 0,            earned);
        checkBadge(Badge.CONFIDENT,    score >= 80,                       earned);
        checkBadge(Badge.MARATHON,     s.durationSeconds >= 300,          earned);
        return earned;
    }

    private void checkBadge(Badge badge, boolean condition, List<Badge> earned) {
        String key = BADGE_PFX + badge.name();
        if (condition && !prefs.getBoolean(key, false)) {
            prefs.edit().putBoolean(key, true).apply();
            earned.add(badge);
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public int   getXp()         { return prefs.getInt(KEY_XP, 0); }
    public int   getStreakDays() { return prefs.getInt(KEY_STREAK, 0); }
    public int   getLastSessionXp() { return prefs.getInt(KEY_LAST_XP, 0); }

    /** FIXED: was Math.max(1, xp/100) — Level 2 never appeared until 200 XP.
     *  Now (xp/100)+1 so Level 2 starts at 100 XP. */
    public int   getLevel()         { return (getXp() / 100) + 1; }
    public int   getLevelProgress() { return getXp() % 100; }

    // Personal bests
    public int   getPbWpm()    { return prefs.getInt(KEY_PB_WPM, 0); }
    public float getPbEye()    { return prefs.getFloat(KEY_PB_EYE, 0f); }
    /** Returns -1 if no session recorded yet. */
    public int   getPbFiller() { return prefs.getInt(KEY_PB_FILLER, -1); }

    /** Epoch-day longs for every day the user practiced (for heatmap). */
    public java.util.Set<Long> getCalendarDays() {
        java.util.Set<String> raw = prefs.getStringSet(KEY_CAL_DAYS, new java.util.HashSet<>());
        java.util.Set<Long> result = new java.util.HashSet<>();
        for (String s : raw) {
            try { result.add(Long.parseLong(s)); } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    public List<Badge> getAllBadges() {
        List<Badge> list = new ArrayList<>();
        for (Badge b : Badge.values()) list.add(b);
        return list;
    }

    public boolean isBadgeEarned(Badge badge) {
        return prefs.getBoolean(BADGE_PFX + badge.name(), false);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private long todayEpochDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis() / 86_400_000L;
    }
}
