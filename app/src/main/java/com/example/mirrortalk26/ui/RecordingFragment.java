package com.example.mirrortalk26.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.example.mirrortalk26.R;
import com.example.mirrortalk26.analysis.EyeContactAnalyzer;
import com.example.mirrortalk26.analysis.SpeechAnalyzer;
import com.example.mirrortalk26.data.AppDatabase;
import com.example.mirrortalk26.data.SpeechSession;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RecordingFragment extends Fragment {

    private static final int PERMISSIONS_REQUEST = 1001;
    private static final int MIN_SESSION_MS      = 5_000;

    // ── Views ─────────────────────────────────────────────────────────────────
    // NOTE: IDs match the ORIGINAL fragment_recording.xml exactly:
    //   tvWpm, tvFillers, tvEye, tvPartialTranscript, btnPauseResume, btnStop, tvTimer
    private PreviewView    previewView;
    private TextView       tvWpm, tvFillers, tvEye, tvPartialTranscript, tvTimer;
    private View           overlayCard;
    private MaterialButton btnStop, btnPauseResume;

    // ── State ──────────────────────────────────────────────────────────────────
    private SpeechAnalyzer     speechAnalyzer;
    private EyeContactAnalyzer eyeContactAnalyzer;

    // Eye contact: EyeContactAnalyzer uses a callback, so we track % ourselves
    private final AtomicInteger eyeFrames   = new AtomicInteger(0);
    private final AtomicInteger contactFrames = new AtomicInteger(0);

    private long    sessionStartMs = 0;
    private boolean isPaused       = false;
    private boolean isRecording    = false;

    private final Handler mainHandler  = new Handler(Looper.getMainLooper());
    private final Handler timerHandler = new Handler(Looper.getMainLooper());

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recording, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        previewView        = view.findViewById(R.id.previewView);
        tvWpm              = view.findViewById(R.id.tvWpm);
        tvFillers          = view.findViewById(R.id.tvFillers);
        tvEye              = view.findViewById(R.id.tvEye);
        tvPartialTranscript = view.findViewById(R.id.tvPartialTranscript);
        overlayCard        = view.findViewById(R.id.overlayCard);
        btnStop            = view.findViewById(R.id.btnStop);
        btnPauseResume     = view.findViewById(R.id.btnPauseResume);
        tvTimer            = view.findViewById(R.id.tvTimer);

        // Version A = live overlay visible, Version B = hidden
        String version = PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getString("selected_version", "A");
        overlayCard.setVisibility("A".equals(version) ? View.VISIBLE : View.GONE);

        // FIX: Stop button disabled for first 5 seconds
        btnStop.setEnabled(false);
        btnStop.setAlpha(0.4f);

        if (checkPermissions()) {
            startSession(version);
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            }, PERMISSIONS_REQUEST);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void startSession(String version) {
        sessionStartMs = System.currentTimeMillis();
        isRecording    = true;

        // Enable Stop after MIN_SESSION_MS
        mainHandler.postDelayed(() -> {
            if (!isAdded() || !isRecording) return;
            btnStop.setEnabled(true);
            btnStop.setAlpha(1f);
        }, MIN_SESSION_MS);

        // ── Timer ─────────────────────────────────────────────────────────────
        Runnable timerTick = new Runnable() {
            @Override public void run() {
                if (!isAdded() || !isRecording) return;
                long elapsed = (System.currentTimeMillis() - sessionStartMs) / 1000;
                tvTimer.setText(String.format("%d:%02d", elapsed / 60, elapsed % 60));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerTick);

        // ── Eye contact: EyeContactAnalyzer takes a callback, NOT a Context ──
        eyeContactAnalyzer = new EyeContactAnalyzer(isContact -> {
            eyeFrames.incrementAndGet();
            if (isContact) contactFrames.incrementAndGet();
        });

        // Camera + ImageAnalysis
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                analysis.setAnalyzer(Executors.newSingleThreadExecutor(), eyeContactAnalyzer);

                provider.unbindAll();
                provider.bindToLifecycle(getViewLifecycleOwner(),
                        CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(requireContext(),
                        "Camera error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));

        // Eye % update tick
        mainHandler.post(new Runnable() {
            @Override public void run() {
                if (!isAdded() || !isRecording) return;
                int total = eyeFrames.get();
                int pct   = total > 0
                        ? (int)(contactFrames.get() * 100f / total) : 0;
                tvEye.setText("Eye: " + pct + "%");
                mainHandler.postDelayed(this, 500);
            }
        });

        // ── Speech: SpeechAnalyzer takes no constructor args ──────────────────
        speechAnalyzer = new SpeechAnalyzer();
        speechAnalyzer.start();

        // We drive SpeechRecognizer manually here so we can wire it to SpeechAnalyzer
        startSpeechRecognizer();

        // ── Pause / Resume ────────────────────────────────────────────────────
        btnPauseResume.setOnClickListener(v -> {
            if (!isPaused) {
                stopSpeechRecognizer();
                btnPauseResume.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_play));
                btnPauseResume.setText("Resume");
            } else {
                startSpeechRecognizer();
                btnPauseResume.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_pause));
                btnPauseResume.setText("Pause");
            }
            isPaused = !isPaused;
        });

        btnStop.setOnClickListener(v -> finishSession());
    }

    // ── SpeechRecognizer wiring ───────────────────────────────────────────────
    private android.speech.SpeechRecognizer recognizer;
    private android.speech.RecognizerIntent recognizerIntent;

    private void startSpeechRecognizer() {
        if (!isAdded()) return;
        recognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(requireContext());

        android.content.Intent intent = new android.content.Intent(
                android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        recognizer.setRecognitionListener(new android.speech.RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle b) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float v) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onEvent(int t, Bundle b) {}

            @Override public void onPartialResults(Bundle b) {
                java.util.ArrayList<String> r =
                        b.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                if (r != null && !r.isEmpty()) {
                    String partial = r.get(0);
                    speechAnalyzer.processText(partial);
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            tvPartialTranscript.setText(partial);
                            tvWpm.setText("WPM: " + (int) speechAnalyzer.getCurrentWpm());
                            tvFillers.setText("Fillers: " + speechAnalyzer.getFillerWordCount());
                        });
                    }
                }
            }

            @Override public void onResults(Bundle b) {
                java.util.ArrayList<String> r =
                        b.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                if (r != null && !r.isEmpty()) {
                    speechAnalyzer.processText(r.get(0));
                    speechAnalyzer.resetPartial();
                }
                // Auto-restart for continuous listening
                if (isRecording && !isPaused && isAdded()) {
                    mainHandler.postDelayed(() -> {
                        if (isRecording && !isPaused && isAdded()) recognizer.startListening(intent);
                    }, 100);
                }
            }

            @Override public void onError(int error) {
                // Restart on most errors
                if (isRecording && !isPaused && isAdded()) {
                    mainHandler.postDelayed(() -> {
                        if (isRecording && !isPaused && isAdded()) recognizer.startListening(intent);
                    }, 300);
                }
            }
        });
        recognizer.startListening(intent);
    }

    private void stopSpeechRecognizer() {
        if (recognizer != null) {
            recognizer.stopListening();
            recognizer.destroy();
            recognizer = null;
        }
    }

    // ── Finish session ────────────────────────────────────────────────────────
    private void finishSession() {
        if (!isRecording) return;
        isRecording = false;
        stopEverything();

        int    durationSec = (int) ((System.currentTimeMillis() - sessionStartMs) / 1000);
        int    fillers     = speechAnalyzer.getFillerWordCount();
        float  avgWpm      = speechAnalyzer.getCurrentWpm();
        int    total       = eyeFrames.get();
        float  eyePct      = total > 0 ? (contactFrames.get() * 100f / total) : 0f;
        String transcript  = speechAnalyzer.getTranscript();
        String version     = PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getString("selected_version", "A");

        SpeechSession session = new SpeechSession(
                System.currentTimeMillis(), durationSec, fillers,
                avgWpm, eyePct, transcript, version, "");

        Executors.newSingleThreadExecutor().execute(() -> {
            long id = AppDatabase.getInstance(requireContext())
                    .sessionDao().insert(session);
            if (!isAdded()) return;
            Bundle args = new Bundle();
            args.putLong("sessionId", id);
            requireActivity().runOnUiThread(() ->
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_recording_to_result, args));
        });
    }

    private void stopEverything() {
        timerHandler.removeCallbacksAndMessages(null);
        mainHandler.removeCallbacksAndMessages(null);
        stopSpeechRecognizer();
        eyeFrames.set(0);
        contactFrames.set(0);
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST
                && grantResults.length >= 2
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            String version = PreferenceManager
                    .getDefaultSharedPreferences(requireContext())
                    .getString("selected_version", "A");
            startSession(version);
        } else {
            Toast.makeText(requireContext(),
                    "Camera and microphone permissions required.",
                    Toast.LENGTH_LONG).show();
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isRecording = false;
        stopEverything();
    }
}
