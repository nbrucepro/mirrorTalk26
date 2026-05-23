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

import com.example.mirrortalk26.R;
import com.example.mirrortalk26.data.AppDatabase;
import com.example.mirrortalk26.data.SpeechSession;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ResultFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_result,container,false);
    }
    @Override
    public void onViewCreated(@NonNull View view,@Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        TextView tvTimestamp   = view.findViewById(R.id.tvTimestamp);
        TextView tvDuration    = view.findViewById(R.id.tvDuration);
        TextView tvWpmResult   = view.findViewById(R.id.tvWpmResult);
        TextView tvWpmFeedback = view.findViewById(R.id.tvWpmFeedback);
        TextView tvFillers     = view.findViewById(R.id.tvFillersResult);
        TextView tvEye         = view.findViewById(R.id.tvEyeResult);
        TextView tvTranscript  = view.findViewById(R.id.tvTranscript);

        // Get sessionId passed from RecordingFragment
        long sessionId = getArguments() != null ? getArguments().getLong("sessionId",-1): -1;
        // Load session from DB on background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            SpeechSession session = AppDatabase
                    .getInstance(requireContext())
                    .sessionDao()
                    .getSessionById(sessionId);
            if (session == null) return;
            requireActivity().runOnUiThread(() -> {
                // TimeStamp
                String date = new SimpleDateFormat("MMM dd, yyyy  HH:mm",
                        Locale.getDefault()).format(new Date(session.timestamp));
                tvTimestamp.setText(date);

                // Duration
                int mins = session.durationSeconds / 60;
                int secs = session.durationSeconds % 60;
                tvDuration.setText(String.format("%d:%02d", mins, secs));

                // WPM + feedback
                int wpm = (int) session.averageWpm;
                tvWpmResult.setText(wpm + " WPM");
                if (wpm < 100)      tvWpmFeedback.setText("⚠ Too slow — try to speak faster");
                else if (wpm > 180) tvWpmFeedback.setText("⚠ Too fast — slow down a little");
                else                tvWpmFeedback.setText("✓ Great pace! Keep it up");

                // Filler words — colour coded
                int fillers = session.fillerWordCount;
                tvFillers.setText(fillers + " filler words");
                tvFillers.setTextColor(fillers > 10
                        ? 0xFFFF6B6B   // red if too many
                        : 0xFFFFCC44); // amber otherwise

                // Eye contact — colour coded
                int eye = (int) session.eyeContactPercent;
                tvEye.setText(eye + "%");
                tvEye.setTextColor(eye >= 60
                        ? 0xFF1D9E75   // green if good
                        : 0xFFFF6B6B); // red if poor

                // Transcript
                tvTranscript.setText(session.transcript.isEmpty()
                        ? "No transcript recorded"
                        : session.transcript);

                // ── Confidence Score
                TextView tvScore   = view.findViewById(R.id.tvConfidenceScore);
                TextView tvScoreMsg = view.findViewById(R.id.tvScoreMessage);
                // WPM score: optimal is 120–160, penalise outside that range
                float wpmScore;
                if (wpm >= 120 && wpm <= 160)      wpmScore = 100f;
                else if (wpm >= 100 && wpm < 120)  wpmScore = 75f;
                else if (wpm > 160 && wpm <= 180)  wpmScore = 75f;
                else if (wpm > 0)                  wpmScore = 40f;
                else                               wpmScore = 0f;

// Filler score: 0 fillers = 100, penalise each one
                float fillerScore = Math.max(0f, 100f - (fillers * 10f));

// Eye score: directly the percentage
                float eyeScore = session.eyeContactPercent;
                // Composite score
                int score = (int)((eyeScore * 0.40f) + (wpmScore * 0.35f) + (fillerScore * 0.25f));

                tvScore.setText(String.valueOf(score));

// Colour + message based on score
                if (score >= 80) {
                    tvScore.setTextColor(0xFF1D9E75);
                    tvScoreMsg.setText("🏆 Excellent — competition ready!");
                    tvScoreMsg.setTextColor(0xFF1D9E75);
                } else if (score >= 60) {
                    tvScore.setTextColor(0xFF7F77DD);
                    tvScoreMsg.setText("💪 Good — keep practising");
                    tvScoreMsg.setTextColor(0xFF7F77DD);
                } else if (score >= 40) {
                    tvScore.setTextColor(0xFFFFCC44);
                    tvScoreMsg.setText("📈 Getting there — focus on eye contact");
                    tvScoreMsg.setTextColor(0xFFFFCC44);
                } else {
                    tvScore.setTextColor(0xFFFF6B6B);
                    tvScoreMsg.setText("🎯 Keep going — practice makes perfect");
                    tvScoreMsg.setTextColor(0xFFFF6B6B);
                }
                // ── Video Playback ──────────────────────────────────────
                androidx.cardview.widget.CardView cardVideo = view.findViewById(R.id.cardVideo);
                android.widget.VideoView videoView = view.findViewById(R.id.videoView);
                com.google.android.material.button.MaterialButton btnPlay =
                        view.findViewById(R.id.btnPlayPause);
                com.google.android.material.button.MaterialButton btnReplay =
                        view.findViewById(R.id.btnReplay);

                if (session.videoPath != null && !session.videoPath.isEmpty()) {
                    java.io.File videoFile = new java.io.File(session.videoPath);
                    if (videoFile.exists()) {
                        cardVideo.setVisibility(View.VISIBLE);
                        videoView.setVideoPath(session.videoPath);
                        android.widget.MediaController mediaController =
                                new android.widget.MediaController(requireContext());
                        mediaController.setAnchorView(videoView);
                        videoView.setMediaController(mediaController);

                        videoView.setOnPreparedListener(mp -> {
                            mp.setLooping(false);
                            btnPlay.setText("▶  Play");
                        });

                        videoView.setOnCompletionListener(mp ->
                                btnPlay.setText("▶  Play"));

                        btnPlay.setOnClickListener(v -> {
                            if (videoView.isPlaying()) {
                                videoView.pause();
                                btnPlay.setText("▶  Play");
                            } else {
                                videoView.start();
                                btnPlay.setText("⏸  Pause");
                            }
                        });

                        btnReplay.setOnClickListener(v -> {
                            videoView.seekTo(0);
                            videoView.start();
                            btnPlay.setText("⏸  Pause");
                        });
                    }
                }
            });
        });


        // Home button
        view.findViewById(R.id.btnHome).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.homeFragment));
        // History button
        view.findViewById(R.id.btnHistory).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.historyFragment));

    }
}
