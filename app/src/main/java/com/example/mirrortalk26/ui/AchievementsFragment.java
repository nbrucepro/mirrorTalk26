package com.example.mirrortalk26.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mirrortalk26.R;
import com.example.mirrortalk26.gamification.GamificationManager;
public class AchievementsFragment extends Fragment{
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

        // ── XP + Level bar ───────────────────────────────────────────────────
        int level    = gm.getLevel();
        int xp       = gm.getXp();
        int progress = gm.getLevelProgress();
        int streak   = gm.getStreakDays();

        ((TextView) view.findViewById(R.id.tvLevel))
                .setText("Level " + level);
        ((TextView) view.findViewById(R.id.tvXp))
                .setText(xp + " XP total");
        ((TextView) view.findViewById(R.id.tvStreak))
                .setText(streak + (streak == 1 ? " day streak 🔥" : " day streak 🔥"));

        ProgressBar xpBar = view.findViewById(R.id.xpBar);
        xpBar.setMax(100);
        xpBar.setProgress(progress);

        ((TextView) view.findViewById(R.id.tvXpProgress))
                .setText(progress + " / 100 XP to Level " + (level + 1));

        // ── Badge grid ───────────────────────────────────────────────────────
        LinearLayout badgeContainer = view.findViewById(R.id.badgeContainer);

        for (GamificationManager.Badge badge : gm.getAllBadges()) {
            boolean earned = gm.isBadgeEarned(badge);

            View card = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_badge, badgeContainer, false);

            ((TextView) card.findViewById(R.id.tvBadgeEmoji))
                    .setText(badge.emoji);
            ((TextView) card.findViewById(R.id.tvBadgeName))
                    .setText(badge.title);
            ((TextView) card.findViewById(R.id.tvBadgeDesc))
                    .setText(badge.description);

            // Dim badges not yet earned
            card.setAlpha(earned ? 1.0f : 0.35f);

            if (earned) {
                card.setBackgroundResource(R.drawable.badge_card_earned_bg);
            }

            badgeContainer.addView(card);
        }
    }
}
