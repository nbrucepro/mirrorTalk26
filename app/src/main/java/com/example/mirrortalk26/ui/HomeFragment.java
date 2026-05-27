package com.example.mirrortalk26.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.example.mirrortalk26.R;
import com.example.mirrortalk26.data.AppDatabase;
import com.example.mirrortalk26.gamification.GamificationManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;

/**
 * HomeFragment — matches ORIGINAL fragment_home.xml IDs exactly:
 *   btnAchievements, btnSettings, tvSessionCount, tvStreak, tvLevel,
 *   btnShuffle, tvPrompt, btnStart, btnHistory
 *
 * NOTE: fragment_home.xml does NOT have a tvGreeting view, so the
 * personalised greeting is shown by replacing the subtitle TextView text
 * (the one that says "AI-powered speaking confidence trainer") via tag lookup,
 * OR we just set the session count text to include the name.
 * To keep it simple and non-breaking we set tvSessionCount to "Welcome, Bruce  •  N sessions".
 */
public class HomeFragment extends Fragment {

    private static final List<String> PROMPTS = Collections.unmodifiableList(Arrays.asList(
            "Describe a challenge you've overcome and what you learned from it.",
            "If you could change one thing about your city, what would it be and why?",
            "Explain how a smartphone works to someone who has never seen one.",
            "What is the most important skill for success in the next decade?",
            "Pitch your favourite app to someone in 60 seconds.",
            "Describe your ideal workday from morning to night.",
            "What does leadership mean to you? Give a real example.",
            "If you had unlimited resources, what problem would you solve first?",
            "Tell me about a book or film that changed how you think.",
            "Walk me through how you prepare for an important meeting.",
            "Explain the concept of machine learning to a 10-year-old.",
            "Describe a time you had to adapt quickly to unexpected change.",
            "What makes a great team, and what is your role in one?",
            "If you could have dinner with anyone in history, who and why?",
            "Explain compound interest without using any numbers.",
            "Convince me to try a food I've never eaten before.",
            "What is one misconception people have about your field of work?",
            "Describe your morning routine and how it sets up your day.",
            "What skill are you learning right now and why?",
            "Give a one-minute elevator pitch for yourself.",
            "Tell me about a decision you regret and what you'd do differently.",
            "Explain why empathy is a professional skill, not just a personal one.",
            "Describe the city you grew up in to someone who has never been there.",
            "What does work-life balance mean to you in practice?",
            "If you could redesign school from scratch, what would change?"
    ));

    private final Random random = new Random();
    private int lastIndex = -1;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvSessionCount = view.findViewById(R.id.tvSessionCount);
        TextView tvStreak       = view.findViewById(R.id.tvStreak);
        TextView tvLevel        = view.findViewById(R.id.tvLevel);

        // Load stats on background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            int count = AppDatabase.getInstance(requireContext())
                    .sessionDao().getSessionCount();
            GamificationManager gm = new GamificationManager(requireContext());
            int streak = gm.getStreakDays();
            int level  = gm.getLevel();
            int xp     = gm.getXp();

            // Personalised greeting: prepend name to session count if set
            String name = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString(OnboardingFragment.PREF_USER_NAME, "");
            String countLabel = (!name.isEmpty() ? "Hi " + name + "  •  " : "")
                    + count + (count == 1 ? " session" : " sessions");

            if (!isAdded()) return;
            String finalCountLabel = countLabel;
            requireActivity().runOnUiThread(() -> {
                tvSessionCount.setText(finalCountLabel);
                tvStreak.setText(streak > 0 ? streak + " day streak" : "Start your streak today");
                tvLevel.setText("Level " + level + "  ·  " + xp + " XP");
            });
        });

        // Navigation — route Start through CountdownFragment
        view.findViewById(R.id.btnStart).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_countdown));
        view.findViewById(R.id.btnHistory).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.historyFragment));
        view.findViewById(R.id.btnSettings).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_settings));
        view.findViewById(R.id.btnAchievements).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.achievementsFragment));

        // Practice prompt
        TextView tvPrompt = view.findViewById(R.id.tvPrompt);
        showNextPrompt(tvPrompt);
        view.findViewById(R.id.btnShuffle).setOnClickListener(v ->
                tvPrompt.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                    showNextPrompt(tvPrompt);
                    tvPrompt.animate().alpha(1f).setDuration(200).start();
                }).start());
    }

    private void showNextPrompt(TextView tv) {
        int next;
        do { next = random.nextInt(PROMPTS.size()); } while (next == lastIndex);
        lastIndex = next;
        tv.setText(PROMPTS.get(next));
    }
}
