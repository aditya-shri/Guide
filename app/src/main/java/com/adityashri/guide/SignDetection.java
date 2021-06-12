package com.adityashri.guide;

import android.annotation.SuppressLint;
import android.media.Image;
import android.speech.tts.TextToSpeech;
import android.widget.TextView;

import androidx.camera.core.ImageProxy;

import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.adityashri.guide.MainActivity.speak;
import static com.adityashri.guide.MainActivity.speakerState;

public class SignDetection {

    private static final ImageLabeler labeler;      //AI-ObjectDetector's object
    private static final StringBuilder text;                 //Text used to display AI result
    private static boolean toSpeak;                    //Used to check whether to speak while in the TTS thread
    private static boolean toWrite;                    //Used to check if complete word is commplete or not
    private static TextView textView;                  //TextView for hold text
    private static Date now;

    static {
        toSpeak = true;
        toWrite = false;
        text = new StringBuilder();

        LocalModel tfModel = new LocalModel.Builder()
                .setAssetFilePath("...")
                .build();
        CustomImageLabelerOptions options = new CustomImageLabelerOptions.Builder(tfModel)
                .setConfidenceThreshold(0.65f)
                .setMaxResultCount(1)
                .build();
        labeler = ImageLabeling.getClient(options);
    }

    SignDetection(TextView textView) {
        SignDetection.textView = textView;
        SignDetection.textView.setText("...");
        now = new Date();
    }

    //Analyzes frame at an interval received from PreviewView
    //Contains the AI-ObjectDetector which outputs the result
    @SuppressLint("UnsafeExperimentalUsageError")
    public static void analyze(ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            labeler.process(image)
                    .addOnSuccessListener(
                            detectedObjects -> {
                                if (detectedObjects.size() > 0) {
                                    ImageLabel item = detectedObjects.get(0);
                                    if (item.getText().equalsIgnoreCase("nothing")) {
                                        Date temp = new Date();
                                        if (TimeUnit.MILLISECONDS.toSeconds(now.getTime() - temp.getTime()) % 60 > 3) {
                                            toWrite = true;
                                        }
                                    } else if (item.getText().equalsIgnoreCase("space")) {
                                        toWrite = true;
                                    } else if (item.getText().equalsIgnoreCase("del")) {
                                        text.deleteCharAt(text.length() - 1);
                                    } else {
                                        text.append(item.getText());
                                        now = new Date();
                                    }
                                    textView.setText("");
                                    textView.setText(text.toString().toUpperCase());
                                    if (toWrite) {
                                        toWrite = false;
                                        if (speakerState.equalsIgnoreCase("true")) {
                                            new Thread(() -> {
                                                if (toSpeak) {
                                                    toSpeak = false;
                                                    speak.speak(text.toString(), TextToSpeech.QUEUE_FLUSH, null, "Image");
                                                    try {
                                                        Thread.sleep(5000);
                                                    } catch (InterruptedException e) {
                                                        e.printStackTrace();
                                                    }
                                                    toSpeak = true;
                                                }
                                            }).start();
                                        }
                                        text.delete(0, text.length());
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
