package com.example.mirrortalk26.analysis;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

public class EyeContactAnalyzer implements ImageAnalysis.Analyzer{
    public interface EyeContactCallback {
        void onResult(boolean isContact);
    }
    private final FaceDetector detector;
    private final EyeContactCallback callback;
    private int frameCount = 0;

    public EyeContactAnalyzer(EyeContactCallback callback){
        this.callback = callback;
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build();
        detector = FaceDetection.getClient(options);
    }
    @Override
    public void analyze(@NonNull ImageProxy imageProxy){
        frameCount++;
        // Only process every 3rd frame — saves battery
        if (frameCount % 3 != 0){
            imageProxy.close();
            return;
        }
        android.media.Image mediaImage = imageProxy.getImage();
        if(mediaImage == null){
            imageProxy.close();
            return;
        }
        InputImage image = InputImage.fromMediaImage(
                mediaImage, imageProxy.getImageInfo().getRotationDegrees()
        );
        detector.process(image)
                .addOnSuccessListener(faces -> {
                    boolean contact = false;
                    if(!faces.isEmpty()){
                        Face face = faces.get(0);
                        float yaw = Math.abs(face.getHeadEulerAngleY());
                        float pitch = Math.abs(face.getHeadEulerAngleX());
                        contact = yaw < 20f && pitch < 20f;
                    }
                    callback.onResult(contact);
                })
                .addOnFailureListener(e -> callback.onResult(false))
                .addOnCompleteListener(task -> imageProxy.close());
    }
}
