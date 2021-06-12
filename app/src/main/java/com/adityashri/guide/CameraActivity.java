package com.adityashri.guide;

import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.adityashri.guide.MainActivity.feature;


public class CameraActivity {

    private final Executor executor;                //Start the executor for camera activity
    private final PreviewView mainView;                   //PreviewView for displaying camera preview
    private final MainActivity mainObject;                //MainActivity object
    private ImageAnalysis imageAnalysis;
    private CameraSelector cameraSelector;
    private Preview.SurfaceProvider ps;
    private Preview preview;
    private ProcessCameraProvider cameraProvider;

    public CameraActivity(PreviewView mainView, MainActivity mainObject) {
        this.mainView = mainView;
        this.mainObject = mainObject;
        this.executor = Executors.newSingleThreadExecutor();
    }

    //Starts Camera activity
    public void startCamera() {

        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(mainObject);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(mainObject));
    }

    //Binds the Camera output to PreviewView
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        preview = new Preview.Builder().build();

        ps = mainView.createSurfaceProvider();
        preview.setSurfaceProvider(ps);

        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        newFeature(feature);
    }

    private ImageAnalysis setImageAnalysis(String feature, ImageAnalysis imageAnalysis) {
        switch (feature.toLowerCase()) {
            case "dumb":
                imageAnalysis.setAnalyzer(executor, SignDetection::analyze);
                break;
            default:
                imageAnalysis.setAnalyzer(executor, ObjectDetection::analyze);
                break;
        }
        return imageAnalysis;
    }

    private void newFeature(String feature) {
        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(mainObject, cameraSelector, setImageAnalysis(feature, imageAnalysis), preview);
    }

    public void stopCamera() {
        cameraProvider.unbindAll();
    }

    public ProcessCameraProvider getCameraProvider() {
        return cameraProvider;
    }
}
