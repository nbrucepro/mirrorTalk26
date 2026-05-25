package com.example.mirrortalk26.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.mirrortalk26.R;
import com.example.mirrortalk26.analysis.AiCoachHelper;
import com.example.mirrortalk26.analysis.SpeechAnalyzer;
import com.example.mirrortalk26.data.AppDatabase;
import com.example.mirrortalk26.data.SpeechSession;
import com.example.mirrortalk26.gamification.GamificationManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class ResultFragment extends Fragment {

    private SpeechSession loadedSession;
    private int           computedScore;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        long sessionId = getArguments() != null
                ? getArguments().getLong("sessionId", -1) : -1;

        Executors.newSingleThreadExecutor().execute(() -> {
            SpeechSession session = AppDatabase
                    .getInstance(requireContext())
                    .sessionDao()
                    .getSessionById(sessionId);
            if (session == null) return;
            requireActivity().runOnUiThread(() -> bindUI(view, session));
        });

        view.findViewById(R.id.btnHome).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.homeFragment));
        view.findViewById(R.id.btnHistory).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.historyFragment));
        view.findViewById(R.id.btnShare).setOnClickListener(v -> {
            if (loadedSession != null) shareSession(loadedSession, computedScore);
        });
        view.findViewById(R.id.btnChallenge).setOnClickListener(v -> {
            if (loadedSession != null) shareChallenge(loadedSession, computedScore);
        });
    }

    private void bindUI(View view, SpeechSession session) {
        loadedSession = session;

        // ── Timestamp ──────────────────────────────────────────────────────────
        String date = new SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault())
                .format(new Date(session.timestamp));
        ((TextView) view.findViewById(R.id.tvTimestamp)).setText(date);

        // ── Duration ───────────────────────────────────────────────────────────
        int mins = session.durationSeconds / 60;
        int secs = session.durationSeconds % 60;
        ((TextView) view.findViewById(R.id.tvDuration))
                .setText(String.format("%d:%02d", mins, secs));

        // ── WPM ────────────────────────────────────────────────────────────────
        int wpm = (int) session.averageWpm;
        TextView tvWpmResult   = view.findViewById(R.id.tvWpmResult);
        TextView tvWpmFeedback = view.findViewById(R.id.tvWpmFeedback);
        tvWpmResult.setText(wpm + " WPM");
        if      (wpm < 100) { tvWpmFeedback.setText("⚠ Too slow — try to speak faster");  tvWpmFeedback.setTextColor(0xFFFF6B6B); }
        else if (wpm > 180) { tvWpmFeedback.setText("⚠ Too fast — slow down a little");   tvWpmFeedback.setTextColor(0xFFFFCC44); }
        else                { tvWpmFeedback.setText("✓ Great pace! Keep it up");           tvWpmFeedback.setTextColor(0xFF1D9E75); }

        // ── Filler words ───────────────────────────────────────────────────────
        int fillers = session.fillerWordCount;
        TextView tvFillers = view.findViewById(R.id.tvFillersResult);
        tvFillers.setText(fillers + " filler words");
        tvFillers.setTextColor(fillers > 10 ? 0xFFFF6B6B : 0xFFFFCC44);

        TextView tvBreakdown = view.findViewById(R.id.tvFillerBreakdown);
        if (!session.transcript.isEmpty()) {
            Map<String, Integer> breakdown = SpeechAnalyzer.analyzeFillers(session.transcript);
            if (!breakdown.isEmpty()) {
                List<Map.Entry<String, Integer>> entries = new ArrayList<>(breakdown.entrySet());
                Collections.sort(entries, (a, b) -> b.getValue() - a.getValue());
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Integer> e : entries) {
                    if (sb.length() > 0) sb.append("   •   ");
                    sb.append('"').append(e.getKey()).append("\" ×").append(e.getValue());
                }
                tvBreakdown.setText(sb.toString());
            } else {
                tvBreakdown.setText("No filler words detected 🎉");
                tvBreakdown.setTextColor(0xFF1D9E75);
            }
        }

        // ── Eye contact ────────────────────────────────────────────────────────
        int eye = (int) session.eyeContactPercent;
        TextView tvEye = view.findViewById(R.id.tvEyeResult);
        tvEye.setText(eye + "%");
        tvEye.setTextColor(eye >= 60 ? 0xFF1D9E75 : 0xFFFF6B6B);

        // ── Transcript ─────────────────────────────────────────────────────────
        ((TextView) view.findViewById(R.id.tvTranscript))
                .setText(session.transcript.isEmpty() ? "No transcript recorded" : session.transcript);

        // ── Confidence Score ───────────────────────────────────────────────────
        float wpmScore;
        if      (wpm >= 120 && wpm <= 160) wpmScore = 100f;
        else if (wpm >= 100 && wpm < 120)  wpmScore = 75f;
        else if (wpm > 160 && wpm <= 180)  wpmScore = 75f;
        else if (wpm > 0)                  wpmScore = 40f;
        else                               wpmScore = 0f;

        float fillerScore = Math.max(0f, 100f - (fillers * 10f));
        int score = (int) ((session.eyeContactPercent * 0.40f)
                + (wpmScore                 * 0.35f)
                + (fillerScore              * 0.25f));
        computedScore = score;

        TextView tvScore    = view.findViewById(R.id.tvConfidenceScore);
        TextView tvScoreMsg = view.findViewById(R.id.tvScoreMessage);
        tvScore.setText(String.valueOf(score));
        if      (score >= 80) { tvScore.setTextColor(0xFF1D9E75); tvScoreMsg.setText("🏆 Excellent — competition ready!"); tvScoreMsg.setTextColor(0xFF1D9E75); }
        else if (score >= 60) { tvScore.setTextColor(0xFF7F77DD); tvScoreMsg.setText("💪 Good — keep practising");         tvScoreMsg.setTextColor(0xFF7F77DD); }
        else if (score >= 40) { tvScore.setTextColor(0xFFFFCC44); tvScoreMsg.setText("📈 Getting there — focus on eye contact"); tvScoreMsg.setTextColor(0xFFFFCC44); }
        else                  { tvScore.setTextColor(0xFFFF6B6B); tvScoreMsg.setText("🎯 Keep going — practice makes perfect");  tvScoreMsg.setTextColor(0xFFFF6B6B); }

        // ── Gamification: record session & show new badges ─────────────────────
        GamificationManager gm = new GamificationManager(requireContext());
        List<GamificationManager.Badge> newBadges = gm.recordSession(session, score);

        TextView tvXpEarned = view.findViewById(R.id.tvXpEarned);
        tvXpEarned.setText("+" + computeXp(session, score) + " XP  •  Level " + gm.getLevel());

        if (!newBadges.isEmpty()) {
            View badgeToast = view.findViewById(R.id.newBadgeCard);
            badgeToast.setVisibility(View.VISIBLE);
            StringBuilder badgeText = new StringBuilder("🎉 New badge");
            if (newBadges.size() > 1) badgeText.append("s");
            badgeText.append(" unlocked: ");
            for (int i = 0; i < newBadges.size(); i++) {
                if (i > 0) badgeText.append(", ");
                badgeText.append(newBadges.get(i).emoji).append(" ").append(newBadges.get(i).title);
            }
            ((TextView) view.findViewById(R.id.tvNewBadge)).setText(badgeText.toString());
        }

        // ── AI Coaching tips (async) ───────────────────────────────────────────
        TextView tvCoachTitle = view.findViewById(R.id.tvCoachTitle);
        TextView tvCoachTips  = view.findViewById(R.id.tvCoachTips);
        tvCoachTitle.setText("Fetching your personalised tips…");

        AiCoachHelper.fetchTips(wpm, fillers, session.eyeContactPercent,
                session.transcript, new AiCoachHelper.CoachCallback() {
                    @Override public void onResult(String tips) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            tvCoachTitle.setText("AI Coach Feedback");
                            tvCoachTips.setText(tips);
                        });
                    }
                    @Override public void onError(String message) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            tvCoachTitle.setText("AI Coach");
                            tvCoachTips.setText("Could not load tips right now — check your internet connection.");
                        });
                    }
                });

        // ── Video playback ─────────────────────────────────────────────────────
        androidx.cardview.widget.CardView cardVideo = view.findViewById(R.id.cardVideo);
        android.widget.VideoView videoView           = view.findViewById(R.id.videoView);
        com.google.android.material.button.MaterialButton btnPlay   = view.findViewById(R.id.btnPlayPause);
        com.google.android.material.button.MaterialButton btnReplay = view.findViewById(R.id.btnReplay);

        if (session.videoPath != null && !session.videoPath.isEmpty()) {
            File f = new File(session.videoPath);
            if (f.exists()) {
                cardVideo.setVisibility(View.VISIBLE);
                Uri videoUri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider", f);
                requireActivity().grantUriPermission(
                        requireActivity().getPackageName(), videoUri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                videoView.setVideoURI(videoUri);
                android.widget.MediaController mc =
                        new android.widget.MediaController(requireContext());
                mc.setAnchorView(videoView);
                videoView.setMediaController(mc);
                videoView.setOnPreparedListener(mp -> { mp.setLooping(false); btnPlay.setText("Play"); });
                videoView.setOnCompletionListener(mp -> btnPlay.setText("Play"));
                btnPlay.setOnClickListener(v -> {
                    if (videoView.isPlaying()) { videoView.pause(); btnPlay.setText("Play"); }
                    else                       { videoView.start(); btnPlay.setText("Pause"); }
                });
                btnReplay.setOnClickListener(v -> {
                    videoView.seekTo(0); videoView.start(); btnPlay.setText("Pause");
                });
            }
        }
    }

    private int computeXp(SpeechSession s, int score) {
        int xp = 10;
        if (score >= 60) xp += 5;
        if (score >= 80) xp += 10;
        if (s.fillerWordCount == 0) xp += 5;
        if (s.eyeContactPercent >= 80) xp += 5;
        return xp;
    }

    // ── Standard share ─────────────────────────────────────────────────────────
    private void shareSession(SpeechSession s, int score) {
        String date = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                .format(new Date(s.timestamp));
        int mins = s.durationSeconds / 60, secs = s.durationSeconds % 60;
        String scoreLabel = score >= 80 ? "Excellent 🏆" : score >= 60 ? "Good 💪"
                : score >= 40 ? "Getting there 📈" : "Keep going 🎯";

        String text = "🎙 MirrorTalk Session — " + date + "\n\n"
                + "  Confidence score : " + score + " / 100 (" + scoreLabel + ")\n"
                + "  Duration         : " + String.format("%d:%02d", mins, secs) + "\n"
                + "  Words per minute : " + (int) s.averageWpm + " WPM\n"
                + "  Filler words     : " + s.fillerWordCount + "\n"
                + "  Eye contact      : " + (int) s.eyeContactPercent + "%\n\n"
                + "Tracked with MirrorTalk — AI speaking coach";

        ShareCompat.IntentBuilder.from(requireActivity())
                .setType("text/plain").setSubject("My MirrorTalk session – " + date)
                .setText(text).setChooserTitle("Share session results").startChooser();
    }

    // ── Challenge share: dares someone to beat your score ─────────────────────
    private void shareChallenge(SpeechSession s, int score) {
        String date = new SimpleDateFormat("MMM dd", Locale.getDefault())
                .format(new Date(s.timestamp));

        String text = "🎤 Speaking Challenge from MirrorTalk!\n\n"
                + "I just scored " + score + "/100 on my public speaking session (" + date + ").\n"
                + "My stats:\n"
                + "  • " + (int) s.averageWpm + " WPM\n"
                + "  • " + s.fillerWordCount + " filler words\n"
                + "  • " + (int) s.eyeContactPercent + "% eye contact\n\n"
                + "Think you can beat " + score + "/100? 🏆\n"
                + "Download MirrorTalk and take the challenge!";

        ShareCompat.IntentBuilder.from(requireActivity())
                .setType("text/plain")
                .setSubject("I challenge you to beat my MirrorTalk score!")
                .setText(text).setChooserTitle("Challenge a friend").startChooser();
    }
}