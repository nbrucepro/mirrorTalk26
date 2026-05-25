package com.example.mirrortalk26.ui;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.mirrortalk26.R;
import com.example.mirrortalk26.analysis.EyeContactAnalyzer;
import com.example.mirrortalk26.viewmodel.RecordingViewModel;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class RecordingFragment extends Fragment {

    private PreviewView previewView;
    private TextView    tvTimer, tvWpm, tvFillers, tvEye, tvPartialTranscript;
    private Button      btnPauseResume, btnStop;
    private View        overlayCard;

    private RecordingViewModel viewModel;
    private SpeechRecognizer   speechRecognizer;
    private Intent             recognizerIntent;

    private VideoCapture<Recorder> videoCapture;
    private Recording              activeRecording;
    private String                 videoFilePath = "";
    private boolean                shouldSaveVideo = false;

    private boolean isPaused   = false;
    // FIX 3: guard to prevent double-fire and reset timer correctly
    private boolean isStopping = false;

    private int    secondsElapsed = 0;
    private final Handler  timerHandler  = new Handler(Looper.getMainLooper());
    private Runnable       timerRunnable;
    private String         version;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), perms -> {
                boolean camera = Boolean.TRUE.equals(perms.get(Manifest.permission.CAMERA));
                boolean audio  = Boolean.TRUE.equals(perms.get(Manifest.permission.RECORD_AUDIO));
                if (camera && audio) startCamera();
                else Toast.makeText(requireContext(),
                        "Camera and Microphone permissions required", Toast.LENGTH_LONG).show();
            });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recording, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // FIX 3: Reset on every fresh entry
        secondsElapsed = 0;
        isStopping     = false;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        version         = prefs.getString(SettingsFragment.PREF_VERSION, "A");
        shouldSaveVideo = prefs.getBoolean(SettingsFragment.PREF_SAVE_VIDEO, false);

        previewView         = view.findViewById(R.id.previewView);
        tvTimer             = view.findViewById(R.id.tvTimer);
        overlayCard         = view.findViewById(R.id.overlayCard);
        tvWpm               = view.findViewById(R.id.tvWpm);
        tvFillers           = view.findViewById(R.id.tvFillers);
        tvEye               = view.findViewById(R.id.tvEye);
        tvPartialTranscript = view.findViewById(R.id.tvPartialTranscript);
        btnPauseResume      = view.findViewById(R.id.btnPauseResume);
        btnStop             = view.findViewById(R.id.btnStop);

        // FIX 3: Force display to 0:00 before first tick
        tvTimer.setText("0:00");

        viewModel = new ViewModelProvider(this).get(RecordingViewModel.class);
        viewModel.startSession();

        overlayCard.setVisibility(version.equals("A") ? View.VISIBLE : View.GONE);

        viewModel.wpm.observe(getViewLifecycleOwner(),
                wpm -> tvWpm.setText(String.format("WPM: %d", wpm.intValue())));
        viewModel.fillerCount.observe(getViewLifecycleOwner(),
                count -> tvFillers.setText("Fillers: " + count));
        viewModel.eyeContact.observe(getViewLifecycleOwner(),
                pct -> tvEye.setText(String.format("Eye: %d%%", pct.intValue())));
        viewModel.partialText.observe(getViewLifecycleOwner(), text -> {
            if (text != null && !text.isEmpty()) {
                String tail = text.length() > 60 ? "…" + text.substring(text.length() - 60) : text;
                tvPartialTranscript.setText(tail);
            }
        });

        boolean hasCam   = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean hasAudio = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

        if (hasCam && hasAudio) startCamera();
        else permissionLauncher.launch(new String[]{
                Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO });

        btnPauseResume.setOnClickListener(v -> {
            if (isPaused) resumeSession(); else pauseSession();
        });
        btnStop.setOnLongClickListener(v -> { stopRecording(); return true; });
        btnStop.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Hold to stop session", Toast.LENGTH_SHORT).show());

        startTimer();
    }

    private void pauseSession() {
        isPaused = true;
        btnPauseResume.setText("▶ Resume");
        timerHandler.removeCallbacks(timerRunnable);
        if (speechRecognizer != null) speechRecognizer.stopListening();
        if (activeRecording  != null) activeRecording.pause();
        View recDot = requireView().findViewById(R.id.recDot);
        recDot.clearAnimation();
        recDot.setAlpha(0.25f);
    }

    private void resumeSession() {
        isPaused = false;
        btnPauseResume.setText("⏸ Pause");
        timerHandler.postDelayed(timerRunnable, 1000);
        if (speechRecognizer != null) speechRecognizer.startListening(recognizerIntent);
        if (activeRecording  != null) activeRecording.resume();
        View recDot = requireView().findViewById(R.id.recDot);
        recDot.setAlpha(1f);
        android.view.animation.AlphaAnimation blink =
                new android.view.animation.AlphaAnimation(1.0f, 0.0f);
        blink.setDuration(600);
        blink.setRepeatMode(android.view.animation.Animation.REVERSE);
        blink.setRepeatCount(android.view.animation.Animation.INFINITE);
        recDot.startAnimation(blink);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(
                        Executors.newSingleThreadExecutor(),
                        new EyeContactAnalyzer(isContact -> viewModel.updateEyeContact(isContact)));

                cameraProvider.unbindAll();

                if (shouldSaveVideo) {
                    Recorder recorder = new Recorder.Builder()
                            .setQualitySelector(QualitySelector.from(Quality.HD))
                            .build();
                    videoCapture = VideoCapture.withOutput(recorder);
                    cameraProvider.bindToLifecycle(
                            getViewLifecycleOwner(),
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview, imageAnalysis, videoCapture);
                    startSpeechRecognition();
                    startVideoRecording();
                } else {
                    cameraProvider.bindToLifecycle(
                            getViewLifecycleOwner(),
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview, imageAnalysis);
                    startSpeechRecognition();
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void startVideoRecording() {
        if (videoCapture == null) return;
        File videoFile = new File(requireContext().getExternalFilesDir(null),
                "session_" + System.currentTimeMillis() + ".mp4");
        videoFilePath = videoFile.getAbsolutePath();

        activeRecording = videoCapture.getOutput()
                .prepareRecording(requireContext(), new FileOutputOptions.Builder(videoFile).build())
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(requireContext()), event -> {
                    if (event instanceof VideoRecordEvent.Finalize) {
                        VideoRecordEvent.Finalize fin = (VideoRecordEvent.Finalize) event;
                        if (fin.hasError()) videoFilePath = "";
                        // FIX 1: navigate only after MP4 container is fully written
                        if (isStopping) navigateToResult();
                    }
                });
    }

    private void startSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(requireContext(), "Speech recognition not available",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onPartialResults(Bundle partialResults) {
                java.util.ArrayList<String> results =
                        partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (results != null && !results.isEmpty())
                    viewModel.onPartialTranscript(results.get(0));
            }
            @Override public void onResults(Bundle results) {
                viewModel.resetPartial();
                if (speechRecognizer != null && !isPaused)
                    speechRecognizer.startListening(recognizerIntent);
            }
            @Override public void onError(int error) {
                timerHandler.postDelayed(() -> {
                    if (speechRecognizer != null && !isPaused)
                        speechRecognizer.startListening(recognizerIntent);
                }, 300);
            }
            @Override public void onReadyForSpeech(Bundle p) {}
            @Override public void onBeginningOfSpeech()      {}
            @Override public void onRmsChanged(float v)      {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech()            {}
            @Override public void onEvent(int t, Bundle b)   {}
        });
        speechRecognizer.startListening(recognizerIntent);
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override public void run() {
                secondsElapsed++;
                tvTimer.setText(String.format("%d:%02d",
                        secondsElapsed / 60, secondsElapsed % 60));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);

        View recDot = requireView().findViewById(R.id.recDot);
        android.view.animation.AlphaAnimation blink =
                new android.view.animation.AlphaAnimation(1.0f, 0.0f);
        blink.setDuration(600);
        blink.setRepeatMode(android.view.animation.Animation.REVERSE);
        blink.setRepeatCount(android.view.animation.Animation.INFINITE);
        recDot.startAnimation(blink);
    }

    private void stopRecording() {
        if (isStopping) return;
        isStopping = true;

        // FIX 3: Kill and reset timer right away — display shows 0:00 instantly
        timerHandler.removeCallbacks(timerRunnable);
        tvTimer.setText("0:00");

        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        if (shouldSaveVideo && activeRecording != null) {
            // FIX 1: Stop recording; navigation fires from the Finalize event
            // so the MP4 is guaranteed to be fully written before ResultFragment opens it.
            activeRecording.stop();
            activeRecording = null;
        } else {
            activeRecording = null;
            navigateToResult();
        }
    }

    private void navigateToResult() {
        final int    duration = secondsElapsed;
        final String video    = videoFilePath;
        Executors.newSingleThreadExecutor().execute(() -> {
            long sessionId = viewModel.saveSession(duration, version, video);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                Bundle bundle = new Bundle();
                bundle.putLong("sessionId", sessionId);
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_recording_to_result, bundle);
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHandler.removeCallbacks(timerRunnable);
        if (speechRecognizer != null) { speechRecognizer.destroy(); speechRecognizer = null; }
        if (activeRecording  != null) { activeRecording.stop();     activeRecording  = null; }
    }
}