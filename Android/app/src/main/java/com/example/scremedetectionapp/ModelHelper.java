package com.example.scremedetectionapp;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.tensorflow.lite.support.model.Model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Helper class that handles model preparation and provides training utilities
 * for advanced use cases where on-device training is needed.
 */
public class ModelHelper {
    private static final String TAG = "ModelHelper";

    private static final String TFLITE_MODEL_FILENAME = "audio_classification_model.tflite";
    private static final String LABELS_FILENAME = "labels.txt";

    /**
     * Interface for model preparation callbacks
     */
    public interface ModelPreparationCallback {
        void onModelPrepared(boolean success);
    }

    /**
     * Copy the TFLite model from assets to the cache directory for faster loading
     * @param context Application context
     * @param callback Callback to notify when preparation is complete
     */
    public static void prepareModel(android.content.Context context, ModelPreparationCallback callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    // Copy model from assets to cache dir
                    copyAssetToCache(context, TFLITE_MODEL_FILENAME);

                    // Copy labels if they exist
                    try {
                        copyAssetToCache(context, LABELS_FILENAME);
                    } catch (IOException e) {
                        Log.w(TAG, "Labels file not found: " + e.getMessage());
                        // Continue without labels file
                    }

                    return true;
                } catch (IOException e) {
                    Log.e(TAG, "Error preparing model: " + e.getMessage(), e);
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                // Execute callback on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onModelPrepared(success);
                });
            }
        }.execute();
    }

    /**
     * Copy an asset file to the cache directory
     */
    private static void copyAssetToCache(android.content.Context context, String filename) throws IOException {
        InputStream in = context.getAssets().open(filename);
        String outPath = context.getCacheDir() + "/" + filename;
        java.io.File outFile = new java.io.File(outPath);

        if (!outFile.exists()) {
            OutputStream out = new java.io.FileOutputStream(outFile);
            copyFile(in, out);
            in.close();
            out.flush();
            out.close();
            Log.d(TAG, "Copied asset to: " + outPath);
        } else {
            Log.d(TAG, "Asset already exists in cache: " + outPath);
        }
    }

    /**
     * Helper method to copy a file from input stream to output stream
     */
    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    /**
     * Get the cached model file path
     */
    public static String getCachedModelPath(android.content.Context context) {
        return context.getCacheDir() + "/" + TFLITE_MODEL_FILENAME;
    }

    /**
     * Preprocess features before feeding them to the model
     * @param features Raw audio features
     * @return ByteBuffer containing preprocessed features
     */
    public static ByteBuffer preprocessFeatures(float[] features, int inputSize) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(inputSize * 4); // 4 bytes per float
        buffer.order(ByteOrder.nativeOrder());

        // Fill buffer with feature values (resize if needed)
        if (features.length != inputSize) {
            // Resize features to match model input size
            float[] resizedFeatures = new float[inputSize];

            if (features.length > inputSize) {
                // Take the first 'inputSize' features
                System.arraycopy(features, 0, resizedFeatures, 0, inputSize);
            } else {
                // Copy all features and pad with zeros
                System.arraycopy(features, 0, resizedFeatures, 0, features.length);
                // Remaining elements are already initialized to 0
            }

            for (float feature : resizedFeatures) {
                buffer.putFloat(feature);
            }
        } else {
            // Features already have the correct size
            for (float feature : features) {
                buffer.putFloat(feature);
            }
        }

        buffer.rewind();
        return buffer;
    }

    /**
     * Load labels from assets
     */
    public static String[] loadLabels(android.content.Context context) {
        try {
            InputStream is = context.getAssets().open(LABELS_FILENAME);
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            String labelsContent = result.toString("UTF-8");
            return labelsContent.split("\n");
        } catch (IOException e) {
            Log.e(TAG, "Error loading labels: " + e.getMessage(), e);
            // Return default labels for binary classification
            return new String[]{"scream", "no_scream"};
        }
    }
}