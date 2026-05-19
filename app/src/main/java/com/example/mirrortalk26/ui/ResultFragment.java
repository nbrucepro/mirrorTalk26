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
