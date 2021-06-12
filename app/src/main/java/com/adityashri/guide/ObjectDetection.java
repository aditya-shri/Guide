package com.adityashri.guide;

import android.annotation.SuppressLint;
import android.media.Image;
import android.speech.tts.TextToSpeech;
import android.widget.TextView;

import androidx.camera.core.ImageProxy;

import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;

import static com.adityashri.guide.MainActivity.speak;
import static com.adityashri.guide.MainActivity.speakerState;

public class ObjectDetection {

    private static final ObjectDetector objectDetector;      //AI-ObjectDetector's object
    private static final StringBuilder text;                 //Text used to display AI result
    private static boolean toSpeak;                    //Used to check whether to speak while in the TTS thread
    private static TextView textView;                  //TextView for hold text

    static {
        toSpeak = true;
        text = new StringBuilder();

        LocalModel tfModel = new LocalModel.Builder()
                .setAssetFilePath("...")
                .build();
        CustomObjectDetectorOptions options = new CustomObjectDetectorOptions.Builder(tfModel)
                .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                .enableClassification()  // Optional
                .setClassificationConfidenceThreshold(0.65f)
                .setMaxPerObjectLabelCount(1)
                .build();
        objectDetector = com.google.mlkit.vision.objects.ObjectDetection.getClient(options);
    }

    ObjectDetection(TextView textView) {
        ObjectDetection.textView = textView;
        ObjectDetection.textView.setText("...");
    }

    //Analyzes frame at an interval received from PreviewView
    //Contains the AI-ObjectDetector which outputs the result
    @SuppressLint("UnsafeExperimentalUsageError")
    public static void analyze(ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            objectDetector.process(image)
                    .addOnSuccessListener(
                            detectedObjects -> {
                                for (DetectedObject item : detectedObjects) {
                                    DetectedObject.Label label;
                                    text.delete(0, text.length());
                                    textView.setText("Processing...");
                                    int limit = 3 > item.getLabels().size() ? item.getLabels().size() : 3;
                                    for (int i = 0; i < limit; i++) {
                                        label = item.getLabels().get(i);
                                        String temp = label.getText().replace("good", "");
                                        text.append(temp).append("\n");
                                    }
                                    textView.setText(text.toString().toUpperCase());
                                    if (speakerState.equalsIgnoreCase("true")) {
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (toSpeak) {
                                                    toSpeak = false;
                                                    speak.speak(text + " ahead!", TextToSpeech.QUEUE_FLUSH, null, "AI");
                                                    try {
                                                        Thread.sleep(5000);
                                                    } catch (InterruptedException e) {
                                                        e.printStackTrace();
                                                    }
                                                    toSpeak = true;
                                                }
                                            }
                                        }).start();
                                    }
                                }
                            })
                    .addOnFailureListener(
                            e -> {
                                textView.setText("Unable to detect : " + e.getMessage());
                            })
                    .addOnCompleteListener(
                            task -> {
                                imageProxy.close();
                                mediaImage.close();
                            });
        }
    }

}
