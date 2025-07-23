package com.example.scremedetectionapp;

import android.content.Context;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

public class ScreamDetector {
    private static final String TAG = "ScreamDetector";
    private static final String MODEL_FILE = "audio_classification_model.tflite";
    private static final float SCREAM_THRESHOLD = 0.2f; // Adjust based on model performance

    // Update these values based on your model's input requirements
    private static final int INPUT_SIZE = 42; // Update this to match your model's input size
    private static final int OUTPUT_CLASSES = 2; // Assuming binary classification (scream/no scream)

    private Interpreter tflite;
    private final Context context;
    private List<String> labels;
    private boolean modelLoaded = false;

    // Indices for labels in your model's output (adjust based on your model)
    private static final int SCREAM_INDEX = 0;
    private static final int NO_SCREAM_INDEX = 1;

    public interface OnDetectionListener {
        void onScreamDetected(float confidence);
        void onNoScreamDetected(float confidence);
    }

    public ScreamDetector(Context context) {
        this.context = context;
        try {
            // Load model
            MappedByteBuffer modelFile = loadModelFile();
            if (modelFile == null) {
                Log.e(TAG, "Failed to load model file");
                return;
            }

            Log.d(TAG, "Model file loaded successfully, initializing interpreter");

            // Create interpreter options for better error reporting
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(2);
            options.setUseNNAPI(false); // Set to true if you want to use Neural Network API

            tflite = new Interpreter(modelFile, options);

            if (tflite == null) {
                Log.e(TAG, "Failed to create TensorFlow Lite interpreter");
                return;
            }

            // Try to load labels if available
            try {
                labels = FileUtil.loadLabels(context, "labels.txt");
                Log.d(TAG, "Labels loaded: " + labels.toString());
            } catch (Exception e) {
                Log.w(TAG, "Label file not found, using default indices: " + e.getMessage());
                // Create default labels if file not found
                labels = List.of("scream", "no_scream");
            }

            modelLoaded = true;
            Log.d(TAG, "TensorFlow Lite model loaded successfully");

            // Log model details
            if (tflite != null) {
                int[] inputShape = tflite.getInputTensor(0).shape();
                int[] outputShape = tflite.getOutputTensor(0).shape();
                Log.d(TAG, "Model input shape: " + arrayToString(inputShape));
                Log.d(TAG, "Model output shape: " + arrayToString(outputShape));
            }

        } catch (IOException e) {
            Log.e(TAG, "Error loading model: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error initializing model: " + e.getMessage(), e);
        }
    }

    private String arrayToString(int[] array) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        try {
            Log.d(TAG, "Attempting to load model file: " + MODEL_FILE);
            FileInputStream inputStream = context.getAssets().openFd(MODEL_FILE).createInputStream();
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = context.getAssets().openFd(MODEL_FILE).getStartOffset();
            long declaredLength = context.getAssets().openFd(MODEL_FILE).getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load model file: " + e.getMessage(), e);
            throw e;
        }
    }

    public void detectScream(float[] features, OnDetectionListener listener) {
        if (!modelLoaded || tflite == null) {
            Log.e(TAG, "TensorFlow Lite model not initialized");
            return;
        }

        try {
            Log.d(TAG, "Detecting scream with feature vector length: " + features.length);

            // Make sure we have the correct input size
            if (features.length != INPUT_SIZE) {
                Log.w(TAG, "Feature vector size mismatch: expected " + INPUT_SIZE +
                        ", got " + features.length + ". Resizing...");
                features = resizeFeatures(features, INPUT_SIZE);
            }

            // Prepare input buffer
            ByteBuffer inputBuffer = ByteBuffer.allocateDirect(INPUT_SIZE * 4); // 4 bytes per float
            inputBuffer.order(ByteOrder.nativeOrder());

            // Fill input buffer with feature values
            for (float feature : features) {
                inputBuffer.putFloat(feature);
            }
            inputBuffer.rewind();

            // Prepare output buffer
            float[][] outputBuffer = new float[1][OUTPUT_CLASSES];

            // Run inference
            Log.d(TAG, "Running TensorFlow Lite inference");
            tflite.run(inputBuffer, outputBuffer);

            // Process results
            float screamConfidence = outputBuffer[0][SCREAM_INDEX];
            float noScreamConfidence = outputBuffer[0][NO_SCREAM_INDEX];

            Log.d(TAG, String.format("Detection results - Scream: %.4f, No scream: %.4f",
                    screamConfidence, noScreamConfidence));

            // Interpret results
            if (screamConfidence > SCREAM_THRESHOLD) {
                Log.d(TAG, "SCREAM DETECTED with confidence: " + screamConfidence);
                listener.onScreamDetected(screamConfidence);
            } else {
                listener.onNoScreamDetected(screamConfidence);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error running model inference: " + e.getMessage(), e);
        }
    }

    // Utility method to resize features if needed
    private float[] resizeFeatures(float[] original, int targetSize) {
        float[] resized = new float[targetSize];

        if (original.length >= targetSize) {
            // If original is larger or equal, take first targetSize elements
            System.arraycopy(original, 0, resized, 0, targetSize);
        } else {
            // If original is smaller, copy all and pad with zeros
            System.arraycopy(original, 0, resized, 0, original.length);
            // Remaining elements are initialized to 0
        }

        return resized;
    }

    public void close() {
        Log.d(TAG, "Closing ScreamDetector resources");
        if (tflite != null) {
            tflite.close();
            tflite = null;
            modelLoaded = false;
            Log.d(TAG, "TensorFlow Lite model closed");
        }
    }
}