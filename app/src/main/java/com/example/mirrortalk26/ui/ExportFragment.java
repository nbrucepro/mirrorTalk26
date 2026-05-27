package com.example.mirrortalk26.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.mirrortalk26.R;
import com.example.mirrortalk26.data.AppDatabase;
import com.example.mirrortalk26.data.SpeechSession;
import com.example.mirrortalk26.util.ScoreUtils;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Implements FR-12: CSV Export.
 * Queries all sessions from Room, writes a CSV file, then shares it via system chooser.
 */
public class ExportFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_export, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        MaterialButton btnExport  = view.findViewById(R.id.btnExport);
        TextView       tvStatus   = view.findViewById(R.id.tvExportStatus);

        btnExport.setOnClickListener(v -> {
            btnExport.setEnabled(false);
            tvStatus.setText("Preparing export…");

            Executors.newSingleThreadExecutor().execute(() -> {
                // 1. Load sessions (synchronous query on background thread)
                List<SpeechSession> sessions = AppDatabase
                        .getInstance(requireContext())
                        .sessionDao()
                        .getAllSessionsSync();

                if (sessions == null || sessions.isEmpty()) {
                    requireActivity().runOnUiThread(() -> {
                        tvStatus.setText("No sessions to export yet.");
                        btnExport.setEnabled(true);
                    });
                    return;
                }

                // 2. Build CSV
                StringBuilder csv = new StringBuilder();
                csv.append("Date,Duration(s),WPM,FillerWords,EyeContact(%),ConfidenceScore,Version,Transcript\n");
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                for (SpeechSession s : sessions) {
                    int score = ScoreUtils.computeScore(s.averageWpm, s.fillerWordCount, s.eyeContactPercent);
                    csv.append(fmt.format(new Date(s.timestamp))).append(",")
                       .append(s.durationSeconds).append(",")
                       .append((int) s.averageWpm).append(",")
                       .append(s.fillerWordCount).append(",")
                       .append((int) s.eyeContactPercent).append(",")
                       .append(score).append(",")
                       .append(s.version).append(",")
                       .append("\"").append(s.transcript.replace("\"", "\"\"")).append("\"")
                       .append("\n");
                }

                // 3. Write to file
                File dir  = requireContext().getExternalFilesDir(null);
                File file = new File(dir, "mirrortalk_sessions_"
                        + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                .format(new Date()) + ".csv");

                try (FileWriter fw = new FileWriter(file)) {
                    fw.write(csv.toString());
                } catch (IOException e) {
                    requireActivity().runOnUiThread(() -> {
                        tvStatus.setText("Export failed: " + e.getMessage());
                        btnExport.setEnabled(true);
                    });
                    return;
                }

                // 4. Share
                android.net.Uri uri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        file);

                requireActivity().runOnUiThread(() -> {
                    tvStatus.setText("Exported " + sessions.size() + " sessions.");
                    btnExport.setEnabled(true);
                    try {
                        ShareCompat.IntentBuilder.from(requireActivity())
                                .setType("text/csv")
                                .setSubject("MirrorTalk session data")
                                .setStream(uri)
                                .setChooserTitle("Share CSV")
                                .startChooser();
                    } catch (Exception e) {
                        Toast.makeText(requireContext(),
                                "Could not open share dialog", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });
    }
}
