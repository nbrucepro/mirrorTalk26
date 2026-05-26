package com.example.mirrortalk26.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mirrortalk26.R;
import com.example.mirrortalk26.data.AppDatabase;
import com.example.mirrortalk26.data.SpeechSession;
import com.example.mirrortalk26.util.ScoreUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Shows a side-by-side comparison of two sessions.
 * Navigation: pass sessionIdA and sessionIdB as long arguments.
 * HistoryFragment sets these when the user long-presses two sessions.
 */
public class CompareFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_compare, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        long idA = getArguments() != null ? getArguments().getLong("sessionIdA", -1) : -1;
        long idB = getArguments() != null ? getArguments().getLong("sessionIdB", -1) : -1;

        if (idA == -1 || idB == -1) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            SpeechSession a = AppDatabase.getInstance(requireContext()).sessionDao().getSessionById(idA);
            SpeechSession b = AppDatabase.getInstance(requireContext()).sessionDao().getSessionById(idB);
            if (a == null || b == null) return;

            // Ensure A is the older session for natural "older → newer" reading
            if (a.timestamp > b.timestamp) { SpeechSession tmp = a; a = b; b = tmp; }

            final SpeechSession sessionA = a;
            final SpeechSession sessionB = b;

            requireActivity().runOnUiThread(() -> bindUI(view, sessionA, sessionB));
        });
    }

    private void bindUI(View view, SpeechSession a, SpeechSession b) {
        SimpleDateFormat fmt = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

        ((TextView) view.findViewById(R.id.tvHeaderA)).setText(fmt.format(new Date(a.timestamp)));
        ((TextView) view.findViewById(R.id.tvHeaderB)).setText(fmt.format(new Date(b.timestamp)));

        // ── Confidence score — uses ScoreUtils so it matches ResultFragment ───
        // FIX 3: Old formula was "else if (wpm >= 100) wpmScore = 75f" which
        // never penalised WPM > 180, giving different scores than ResultFragment.
        int scoreA = ScoreUtils.computeScore(a.averageWpm, a.fillerWordCount, a.eyeContactPercent);
        int scoreB = ScoreUtils.computeScore(b.averageWpm, b.fillerWordCount, b.eyeContactPercent);
        setRow(view, R.id.tvScoreA, R.id.tvScoreB, R.id.tvScoreDelta,
                scoreA, scoreB, "", "");

        // ── WPM ───────────────────────────────────────────────────────────────
        setRow(view, R.id.tvWpmA, R.id.tvWpmB, R.id.tvWpmDelta,
                (int) a.averageWpm, (int) b.averageWpm, " wpm", " wpm");

        // ── Filler words (lower is better) ────────────────────────────────────
        setRowInverted(view, R.id.tvFillersA, R.id.tvFillersB, R.id.tvFillersDelta,
                a.fillerWordCount, b.fillerWordCount);

        // ── Eye contact ───────────────────────────────────────────────────────
        setRow(view, R.id.tvEyeA, R.id.tvEyeB, R.id.tvEyeDelta,
                (int) a.eyeContactPercent, (int) b.eyeContactPercent, "%", "%");

        // ── Duration ──────────────────────────────────────────────────────────
        ((TextView) view.findViewById(R.id.tvDurA)).setText(formatDur(a.durationSeconds));
        ((TextView) view.findViewById(R.id.tvDurB)).setText(formatDur(b.durationSeconds));
        int durDelta = b.durationSeconds - a.durationSeconds;
        setDelta(view.findViewById(R.id.tvDurDelta), durDelta, "s", true);
    }

    private void setRow(View root, int idA, int idB, int idDelta,
                        int valA, int valB, String unitA, String unitB) {
        ((TextView) root.findViewById(idA)).setText(valA + unitA);
        ((TextView) root.findViewById(idB)).setText(valB + unitB);
        setDelta(root.findViewById(idDelta), valB - valA, unitB, true);
    }

    private void setRowInverted(View root, int idA, int idB, int idDelta,
                                int valA, int valB) {
        ((TextView) root.findViewById(idA)).setText(String.valueOf(valA));
        ((TextView) root.findViewById(idB)).setText(String.valueOf(valB));
        setDelta(root.findViewById(idDelta), valB - valA, "", false);
    }

    private void setDelta(TextView tv, int delta, String unit, boolean higherBetter) {
        if (delta == 0) {
            tv.setText("—");
            tv.setTextColor(0xFF888899);
        } else if ((delta > 0) == higherBetter) {
            tv.setText("+" + delta + unit);
            tv.setTextColor(0xFF1D9E75);  // green = improvement
        } else {
            tv.setText(delta + unit);
            tv.setTextColor(0xFFFF6B6B);  // red = regression
        }
    }

    private String formatDur(int seconds) {
        return seconds / 60 + ":" + String.format(Locale.getDefault(), "%02d", seconds % 60);
    }
}