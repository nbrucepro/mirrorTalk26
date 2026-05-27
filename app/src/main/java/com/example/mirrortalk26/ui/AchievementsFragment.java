package com.example.mirrortalk26.ui;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;

import com.example.mirrortalk26.R;
import com.example.mirrortalk26.gamification.GamificationManager;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Calendar;
import java.util.Set;

public class AchievementsFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_achievements, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        GamificationManager gm = new GamificationManager(requireContext());

        // ── XP + Level ────────────────────────────────────────────────────────
        int level    = gm.getLevel();
        int xp       = gm.getXp();
        int progress = gm.getLevelProgress();
        int streak   = gm.getStreakDays();

        ((TextView) view.findViewById(R.id.tvLevel)).setText("Level " + level);
        ((TextView) view.findViewById(R.id.tvXp)).setText(xp + " XP total");
        ((TextView) view.findViewById(R.id.tvStreak))
                .setText(streak > 0 ? streak + (streak == 1 ? " day streak" : " day streak") : "No streak yet");

        LinearProgressIndicator xpBar = view.findViewById(R.id.xpBar);
        xpBar.setMax(100);
        xpBar.setProgress(progress);

        ((TextView) view.findViewById(R.id.tvXpProgress))
                .setText(progress + " / 100 XP to Level " + (level + 1));

        // ── Personal bests ────────────────────────────────────────────────────
        int   pbWpm    = gm.getPbWpm();
        float pbEye    = gm.getPbEye();
        int   pbFiller = gm.getPbFiller();

        TextView tvPbWpm    = view.findViewById(R.id.tvPbWpm);
        TextView tvPbEye    = view.findViewById(R.id.tvPbEye);
        TextView tvPbFiller = view.findViewById(R.id.tvPbFiller);

        tvPbWpm.setText(pbWpm > 0 ? pbWpm + " WPM" : "--");
        tvPbEye.setText(pbEye > 0 ? (int) pbEye + "%" : "--");
        tvPbFiller.setText(pbFiller >= 0 ? String.valueOf(pbFiller) : "--");

        // ── Streak heatmap ────────────────────────────────────────────────────
        buildHeatmap(view, gm.getCalendarDays());

        // ── Badge grid ────────────────────────────────────────────────────────
        LinearLayout badgeContainer = view.findViewById(R.id.badgeContainer);

        for (GamificationManager.Badge badge : gm.getAllBadges()) {
            boolean earned = gm.isBadgeEarned(badge);

            View card = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_badge, badgeContainer, false);

            // Use vector drawable icon instead of emoji
            ImageView ivIcon = card.findViewById(R.id.ivBadgeIcon);
            ivIcon.setImageResource(badge.iconRes);

            // Tint: purple for earned, dim grey for locked
            int tint = earned ? 0xFF7F77DD : 0xFF333355;
            ImageViewCompat.setImageTintList(ivIcon, ColorStateList.valueOf(tint));

            ((TextView) card.findViewById(R.id.tvBadgeName)).setText(badge.title);
            ((TextView) card.findViewById(R.id.tvBadgeDesc)).setText(badge.description);

            card.setAlpha(earned ? 1.0f : 0.38f);
            if (earned) card.setBackgroundResource(R.drawable.badge_card_earned_bg);

            badgeContainer.addView(card);
        }
    }

    /**
     * Builds a 7-column heatmap of the last 10 weeks (70 cells).
     * Green = practiced that day, dark grey = missed.
     */
    private void buildHeatmap(View root, Set<Long> practicedDays) {
        GridLayout grid = root.findViewById(R.id.heatmapGrid);
        grid.removeAllViews();
        grid.setColumnCount(7);

        long todayEpoch = todayEpochDay();
        // Start from 69 days ago (10 weeks × 7 - 1)
        long startEpoch = todayEpoch - 69;

        int cellDp  = (int) (getResources().getDisplayMetrics().density * 16);
        int marginDp = (int) (getResources().getDisplayMetrics().density * 3);

        for (int i = 0; i < 70; i++) {
            long day = startEpoch + i;
            boolean practiced = practicedDays.contains(day);
            boolean isToday   = day == todayEpoch;

            View cell = new View(requireContext());
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width  = cellDp;
            lp.height = cellDp;
            lp.setMargins(marginDp, marginDp, marginDp, marginDp);
            cell.setLayoutParams(lp);

            // Draw rounded square
            android.graphics.drawable.GradientDrawable bg =
                    new android.graphics.drawable.GradientDrawable();
            bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            bg.setCornerRadius(getResources().getDisplayMetrics().density * 3);

            if (practiced) {
                bg.setColor(0xFF1D9E75);   // green — practiced
            } else if (isToday) {
                bg.setColor(0xFF3C3489);   // purple — today but not yet practiced
                bg.setStroke((int)(getResources().getDisplayMetrics().density * 1.5f), 0xFF7F77DD);
            } else {
                bg.setColor(0xFF1A1A2E);   // dark — missed
            }
            cell.setBackground(bg);
            grid.addView(cell);
        }
    }

    private long todayEpochDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis() / 86_400_000L;
    }
}
