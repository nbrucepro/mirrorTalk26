package com.example.mirrortalk26.gamification;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.mirrortalk26.data.SpeechSession;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Manages XP, streaks, and badges.
 * All data is stored in SharedPreferences — no extra DB table needed.
 *
 * Usage:
 *   GamificationManager gm = new GamificationManager(context);
 *   gm.recordSession(session, score);   // call after every session
 *   int xp     = gm.getXp();
 *   int streak = gm.getStreakDays();
 *   List<Badge> earned = gm.getEarnedBadges();
 */
public class GamificationManager {
    // ── Badge definitions ─────────────────────────────────────────────────────
    public enum Badge {
        FIRST_STEPS   ("🎤", "First Steps",        "Complete your first session"),
        CONSISTENT    ("📅", "Getting Consistent",  "Practice 3 days in a row"),
        WEEK_WARRIOR  ("🔥", "Week Warrior",        "7-day practice streak"),
        EYE_PRO       ("👁",  "Eye Contact Pro",    "80%+ eye contact in a session"),
        SPEED_DEMON   ("⚡", "Speed Demon",          "Speak faster than 180 WPM"),
        SLOW_BURN     ("🐢", "Slow & Clear",        "Speak slower than 100 WPM intentionally"),
        CLEAN_SPEECH  ("✨", "Clean Speech",         "Zero filler words in a session"),
        CONFIDENT     ("🏆", "Confident Speaker",   "Confidence score ≥ 80"),
        MARATHON      ("🎯", "Marathon Speaker",    "Session longer than 5 minutes");
        public final String emoji;
        public final String title;
        public final String description;

        Badge(String emoji, String title, String description) {
            this.emoji       = emoji;
            this.title       = title;
            this.description = description;
        }
    }
    // ── SharedPreferences keys ────────────────────────────────────────────────
    private static final String PREFS      = "mirrortalk_gamification";
    private static final String KEY_XP     = "xp";
    private static final String KEY_STREAK = "streak_days";
    private static final String KEY_LAST   = "last_session_day";   // epoch day
    private static final String BADGE_PFX  = "badge_";             // badge_FIRST_STEPS = true

    private final SharedPreferences prefs;

    public GamificationManager(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
    // ── Record a completed session & award XP/badges ──────────────────────────
    public List<Badge> recordSession(SpeechSession session, int confidenceScore) {
        List<Badge> newlyEarned = new ArrayList<>();

        // 1. Update streak
        long today = todayEpochDay();
        long last  = prefs.getLong(KEY_LAST, -1);
        int streak = prefs.getInt(KEY_STREAK, 0);
        if (last == today - 1) {
            streak++;                          // consecutive day
        } else if (last != today) {
            streak = 1;                        // reset (missed a day or first ever)
        }
        // if last == today: same day, streak unchanged

        prefs.edit()
                .putLong(KEY_LAST, today)
                .putInt(KEY_STREAK, streak)
                .apply();

        // 2. Calculate XP for this session
        int xpEarned = 10;                     // base
        if (confidenceScore >= 60) xpEarned += 5;
        if (confidenceScore >= 80) xpEarned += 10;
        if (session.fillerWordCount == 0)      xpEarned += 5;
        if (session.eyeContactPercent >= 80)   xpEarned += 5;

        int totalXp = prefs.getInt(KEY_XP, 0) + xpEarned;
        prefs.edit().putInt(KEY_XP, totalXp).apply();

        // 3. Check & award badges
        newlyEarned.addAll(checkAndAward(session, confidenceScore, streak));

        return newlyEarned;
    }
    private List<Badge> checkAndAward(SpeechSession s, int score, int streak) {
        List<Badge> earned = new ArrayList<>();

        checkBadge(Badge.FIRST_STEPS,  true,                         earned);
        checkBadge(Badge.CONSISTENT,   streak >= 3,                  earned);
        checkBadge(Badge.WEEK_WARRIOR, streak >= 7,                  earned);
        checkBadge(Badge.EYE_PRO,      s.eyeContactPercent >= 80,    earned);
        checkBadge(Badge.SPEED_DEMON,  s.averageWpm > 180,           earned);
        checkBadge(Badge.SLOW_BURN,    s.averageWpm > 0 && s.averageWpm < 100, earned);
        checkBadge(Badge.CLEAN_SPEECH, s.fillerWordCount == 0,       earned);
        checkBadge(Badge.CONFIDENT,    score >= 80,                  earned);
        checkBadge(Badge.MARATHON,     s.durationSeconds >= 300,     earned);

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
    public int getXp()          { return prefs.getInt(KEY_XP, 0); }
    public int getStreakDays()  { return prefs.getInt(KEY_STREAK, 0); }

    /** Returns all badges the user has earned so far. */
    public List<Badge> getEarnedBadges() {
        List<Badge> list = new ArrayList<>();
        for (Badge b : Badge.values()) {
            if (prefs.getBoolean(BADGE_PFX + b.name(), false)) list.add(b);
        }
        return list;
    }

    /** Returns all badges (earned and not), for the full achievements screen. */
    public List<Badge> getAllBadges() {
        List<Badge> list = new ArrayList<>();
        for (Badge b : Badge.values()) list.add(b);
        return list;
    }

    public boolean isBadgeEarned(Badge badge) {
        return prefs.getBoolean(BADGE_PFX + badge.name(), false);
    }

    /** Level = XP / 100, minimum 1. */
    public int getLevel() { return Math.max(1, getXp() / 100); }

    /** XP progress within the current level (0–99). */
    public int getLevelProgress() { return getXp() % 100; }

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
