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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.mirrortalk26.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class RecordingFragment extends Fragment {

    private PreviewView previewView;
    private TextView tvTimer, tvWpm, tvFillers, tvEye;
    private View overlayCard;

    private int secondsElapsed = 0;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timeRunnable;

    // Camera permission launcher
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
    registerForActivityResult(new ActivityResultContracts.RequestPermission(),granted -> {
        if(granted){
            startCamera();
        } else {
            Toast.makeText(requireContext(),
                    "Camera permission required", Toast.LENGTH_SHORT).show();
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

        // Check and request camera permission
        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED){
            startCamera();
        }else{
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
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

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(), cameraSelector, preview
                );
            } catch (ExecutionException | InterruptedException e){
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
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
        Toast.makeText(requireContext(),"Session saved!",Toast.LENGTH_SHORT).show();
        // We'll add navigation to ResultFragment in Phase 8
        requireActivity().onBackPressed();
    }
    @Override
    public void onDestroyView(){
        super.onDestroyView();
        timerHandler.removeCallbacks(timeRunnable);
    }
}
