package com.example.mirrortalk26.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mirrortalk26.R;
import com.example.mirrortalk26.analysis.EyeContactAnalyzer;
import com.example.mirrortalk26.viewmodel.RecordingViewModel;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class RecordingFragment extends Fragment {

    private PreviewView previewView;
    private TextView tvTimer, tvWpm, tvFillers, tvEye;
    private View overlayCard;

    private RecordingViewModel viewModel;
    private SpeechRecognizer   speechRecognizer;
    private Intent  recognizerIntent;

    private int secondsElapsed = 0;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timeRunnable;

    // Default Version A (live overlay). Change to "B" via Settings later.
    private final String version = "A";

    // Camera permission launcher
    private final ActivityResultLauncher<String[]> permissionLauncher =
    registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),perms -> {
        boolean camera = Boolean.TRUE.equals(perms.get(Manifest.permission.CAMERA));
        boolean audio = Boolean.TRUE.equals(perms.get(Manifest.permission.RECORD_AUDIO));
        if(camera && audio){
            startCamera();
            startSpeechRecognition();
        } else {
            Toast.makeText(requireContext(),
                    "Camera and Microphone  permissions are required", Toast.LENGTH_LONG).show();
        }
    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_recording, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view,savedInstanceState);

        previewView = view.findViewById(R.id.previewView);
        tvTimer = view.findViewById(R.id.tvTimer);
        overlayCard = view.findViewById(R.id.overlayCard);
        tvWpm       = view.findViewById(R.id.tvWpm);
        tvFillers   = view.findViewById(R.id.tvFillers);
        tvEye       = view.findViewById(R.id.tvEye);

        viewModel = new ViewModelProvider(this).get(RecordingViewModel.class);
        viewModel.startSession();

        // Show overlay only for Version A
        overlayCard.setVisibility(version.equals("A") ? View.VISIBLE : View.GONE);

        // Observe live data
        viewModel.wpm.observe(getViewLifecycleOwner(),wpm ->
                tvWpm.setText(String.format("WPM: %d", wpm.intValue())));
        viewModel.fillerCount.observe(getViewLifecycleOwner(),count ->
                tvFillers.setText("Fillers: "+count));
        viewModel.eyeContact.observe(getViewLifecycleOwner(), pct ->
                tvEye.setText(String.format("Eye: %d%%", pct.intValue())));

        // Request permissions
        boolean hasCam = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean hasAudio = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        if(hasCam && hasAudio){
            startCamera();
            startSpeechRecognition();
        }else{
            permissionLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            });
        }

        // Long-press stop button to prevent accidental stops
        view.findViewById(R.id.btnStop).setOnLongClickListener(v -> {
            stopRecording();
            return true;
        });
        view.findViewById(R.id.btnStop).setOnClickListener(v ->
                Toast.makeText(requireContext(),"Hold button to stop",Toast.LENGTH_SHORT).show());
        startTimer();
    }

    private void startCamera(){
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // ImageAnalysis for ML Kit eye contact
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(
                        Executors.newSingleThreadExecutor(),
                        new EyeContactAnalyzer(isContact ->
                                viewModel.updateEyeContact(isContact))
                );
                CameraSelector frontCamera = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(),frontCamera,preview, imageAnalysis
                );
            } catch (ExecutionException | InterruptedException e){
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }
    private void startSpeechRecognition(){
        if (!SpeechRecognizer.isRecognitionAvailable((requireContext()))) {
            Toast.makeText(requireContext(),
                    "Speech recognition not available on this device",Toast.LENGTH_SHORT).show();
            return;
        }
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onPartialResults(Bundle partialResults){
               String text = partialResults
                       .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                       .get(0);
               viewModel.onPartialTranscript(text);
            }
            @Override
            public void onResults(Bundle results) {
                // Auto-restart to keep listening continuously
                speechRecognizer.startListening(recognizerIntent);
            }
            @Override
            public void onError(int error) {
                // Auto-restart on timeout or network error
                speechRecognizer.startListening(recognizerIntent);
            }
            @Override
            public void onReadyForSpeech(Bundle params) {}
            @Override
            public void onBeginningOfSpeech() {}
            @Override
            public void onRmsChanged(float rmsdB) {}
            @Override
            public void onBufferReceived(byte[] buffer) {}
            @Override
            public void onEndOfSpeech() {}
            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
        speechRecognizer.startListening(recognizerIntent);
    }
    private void startTimer(){
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                secondsElapsed++;
                int minutes = secondsElapsed / 60;
                int seconds = secondsElapsed % 60;
                tvTimer.setText(String.format("%d:%02d",minutes,seconds));
                timerHandler.postDelayed(this,1000);
            }
        };
        timerHandler.postDelayed(timeRunnable, 1000);
    }
    private void stopRecording(){
        timerHandler.removeCallbacks(timeRunnable);
        if(speechRecognizer != null){
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
        }
        viewModel.saveSession(secondsElapsed,version);
        Toast.makeText(requireContext(),"Session saved!",Toast.LENGTH_SHORT).show();
        // We'll add navigation to ResultFragment in Phase 8
        requireActivity().getOnBackPressedDispatcher().onBackPressed();
    }
    @Override
    public void onDestroyView(){
        super.onDestroyView();
        timerHandler.removeCallbacks(timeRunnable);
        if(speechRecognizer != null) speechRecognizer.destroy();
    }
}
