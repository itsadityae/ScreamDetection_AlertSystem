package com.example.scremedetectionapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.core.content.ContextCompat;

import java.util.Arrays;

public class AudioProcessor {
    private static final String TAG = "AudioProcessor";

    // Audio recording configuration
    private static final int SAMPLE_RATE = 16000; // Changed from 22050 to more commonly supported value
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = SAMPLE_RATE; // 1 second of audio

    // Feature extraction config
    private static final int FFT_SIZE = 1024;
    private static final int MEL_BANDS = 40;
    private static final int NUM_FEATURES = MEL_BANDS + 2; // MEL bands + RMS + ZCR

    private Context context;
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordingThread;

    public interface OnAudioFeaturesExtractedListener {
        void onFeaturesExtracted(float[] features);
    }

    public AudioProcessor(Context context) {
        this.context = context;
        // Initialize any resources needed for audio processing
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public void startRecording(OnAudioFeaturesExtractedListener listener) {
        if (isRecording) {
            Log.d(TAG, "Audio recording already in progress");
            return;
        }

        // Double-check permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Audio permission not granted at recording time");
            return;
        }

        try {
            // Get minimum buffer size for this configuration
            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Error getting minimum buffer size for AudioRecord. Error code: " + minBufferSize);
                return;
            }

            Log.d(TAG, "Minimum buffer size for AudioRecord: " + minBufferSize);
            int bufferSize = Math.max(BUFFER_SIZE * 2, minBufferSize);

            // Initialize AudioRecord
            Log.d(TAG, "Initializing AudioRecord with bufferSize: " + bufferSize);
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
            );

            // Check if initialization was successful
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Audio Record failed to initialize. State: " + audioRecord.getState());
                releaseAudioRecord();
                return;
            }

            // Start recording
            Log.d(TAG, "Starting AudioRecord...");
            audioRecord.startRecording();

            // Check if recording started successfully
            if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                Log.e(TAG, "AudioRecord failed to start recording. State: " + audioRecord.getRecordingState());
                releaseAudioRecord();
                return;
            }

            Log.d(TAG, "AudioRecord successfully started recording");
            isRecording = true;

            // Start processing thread
            recordingThread = new Thread(() -> {
                short[] audioBuffer = new short[BUFFER_SIZE];
                Log.d(TAG, "Audio recording thread started");

                while (isRecording) {
                    try {
                        int readSize = audioRecord.read(audioBuffer, 0, BUFFER_SIZE);

                        if (readSize <= 0) {
                            Log.w(TAG, "Audio read error: " + readSize);
                            continue;
                        }

                        Log.v(TAG, "Audio data read: " + readSize + " samples");

                        // Check if we have silence or very low audio
                        boolean hasAudio = false;
                        for (int i = 0; i < readSize; i++) {
                            if (Math.abs(audioBuffer[i]) > 100) { // Threshold for non-silence
                                hasAudio = true;
                                break;
                            }
                        }

                        if (!hasAudio) {
                            Log.v(TAG, "Mostly silence detected, skipping processing");
                            continue;
                        }

                        // Convert short[] to float[] for processing (-1.0 to 1.0 range)
                        float[] floatBuffer = new float[readSize];
                        for (int i = 0; i < readSize; i++) {
                            floatBuffer[i] = (float) audioBuffer[i] / Short.MAX_VALUE;
                        }

                        // Extract features
                        float[] features = extractFeatures(floatBuffer);

                        if (features != null && listener != null) {
                            Log.v(TAG, "Features extracted successfully, length: " + features.length);
                            listener.onFeaturesExtracted(features);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in audio processing thread: " + e.getMessage(), e);
                    }
                }

                Log.d(TAG, "Audio recording thread stopped");
            });

            recordingThread.start();
            Log.d(TAG, "Audio recording started successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error starting recording: " + e.getMessage(), e);
            releaseAudioRecord();
            isRecording = false;
        }
    }

    public void stopRecording() {
        Log.d(TAG, "Stopping audio recording");
        isRecording = false;
        releaseAudioRecord();

        // Wait for recording thread to finish
        if (recordingThread != null && recordingThread.isAlive()) {
            try {
                recordingThread.join(1000); // Wait up to 1 second
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while waiting for recording thread to stop", e);
            }
        }

        Log.d(TAG, "Audio recording stopped");
    }

    private void releaseAudioRecord() {
        if (audioRecord != null) {
            try {
                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                        audioRecord.stop();
                    }
                }
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping AudioRecord: " + e.getMessage(), e);
            } finally {
                audioRecord = null;
            }
        }
    }

    private float[] extractFeatures(float[] audioData) {
        try {
            Log.v(TAG, "Starting feature extraction for " + audioData.length + " samples");

            // Calculate energy-based features
            float rmsEnergy = calculateRMSEnergy(audioData);
            float zeroCrossingRate = calculateZeroCrossingRate(audioData);

            // Preprocess audio data (apply window function)
            float[] windowedData = applyHammingWindow(audioData);

            // Calculate spectral features using FFT
            float[] melFeatures = calculateMelFeatures(windowedData, MEL_BANDS);

            // Log feature values for debugging
            Log.v(TAG, String.format("Audio features - RMS: %.4f, ZCR: %.4f",
                    rmsEnergy, zeroCrossingRate));

            // Combine all features
            float[] allFeatures = new float[NUM_FEATURES];
            allFeatures[0] = rmsEnergy;
            allFeatures[1] = zeroCrossingRate;
            System.arraycopy(melFeatures, 0, allFeatures, 2, melFeatures.length);

            Log.v(TAG, "Feature extraction complete");
            return allFeatures;

        } catch (Exception e) {
            Log.e(TAG, "Error extracting audio features: " + e.getMessage(), e);
            return null;
        }
    }

    private float calculateRMSEnergy(float[] audioData) {
        float sum = 0;
        for (float sample : audioData) {
            sum += sample * sample;
        }
        return (float) Math.sqrt(sum / audioData.length);
    }

    private float calculateZeroCrossingRate(float[] audioData) {
        int crossings = 0;
        for (int i = 1; i < audioData.length; i++) {
            if ((audioData[i] >= 0 && audioData[i - 1] < 0) ||
                    (audioData[i] < 0 && audioData[i - 1] >= 0)) {
                crossings++;
            }
        }
        return (float) crossings / audioData.length;
    }

    private float[] applyHammingWindow(float[] audioData) {
        float[] windowedData = new float[audioData.length];
        for (int i = 0; i < audioData.length; i++) {
            // Hamming window formula: 0.54 - 0.46 * cos(2π * i / (N-1))
            float windowValue = 0.54f - 0.46f * (float) Math.cos((2 * Math.PI * i) / (audioData.length - 1));
            windowedData[i] = audioData[i] * windowValue;
        }
        return windowedData;
    }

    private float[] calculateMelFeatures(float[] windowedData, int numBands) {
        // Calculate FFT
        float[] fftMagnitudes = calculateFFT(windowedData);

        // Convert to Mel scale (approximation)
        return mapToMelScale(fftMagnitudes, numBands);
    }

    private float[] calculateFFT(float[] windowedData) {
        int fftSize = FFT_SIZE;

        // Prepare input data for FFT (pad if necessary)
        float[] fftInput = new float[fftSize];
        System.arraycopy(windowedData, 0, fftInput, 0, Math.min(windowedData.length, fftSize));

        // Implement Fast Fourier Transform (simplified version)
        // For real implementation, consider using a library like JTransforms

        // This is a placeholder for the FFT operation
        // In a real implementation, you would use a proper FFT algorithm
        float[] fftOutput = new float[fftSize / 2]; // Only half - we only need magnitude spectrum

        // Simple power spectrum calculation (for demonstration)
        for (int i = 0; i < fftSize / 2; i++) {
            // Calculate approximate frequency energy bands
            int binSize = windowedData.length / (fftSize / 2);
            int startIdx = i * binSize;
            int endIdx = Math.min(startIdx + binSize, windowedData.length);

            float sum = 0;
            for (int j = startIdx; j < endIdx; j++) {
                sum += windowedData[j] * windowedData[j];
            }

            fftOutput[i] = (float) Math.sqrt(sum / (endIdx - startIdx));
        }

        return fftOutput;
    }

    private float[] mapToMelScale(float[] fftMagnitudes, int numBands) {
        float[] melFeatures = new float[numBands];

        // Convert to Mel scale (simplified approach)
        // In reality, this would use proper Mel filterbanks

        // Low frequency (300Hz) and high frequency (8000Hz) in mel
        float lowFreqMel = 2595 * (float) Math.log10(1 + 300 / 700.0);
        float highFreqMel = 2595 * (float) Math.log10(1 + 8000 / 700.0);

        // Calculate filter center frequencies
        float melStep = (highFreqMel - lowFreqMel) / (numBands + 1);

        for (int i = 0; i < numBands; i++) {
            // Calculate which FFT bins contribute to this Mel band
            float centerMel = lowFreqMel + melStep * (i + 1);

            // Convert back to Hz
            float centerHz = 700 * ((float) Math.pow(10, centerMel / 2595) - 1);

            // Map to FFT bin index
            int fftBin = Math.round(centerHz * fftMagnitudes.length / (SAMPLE_RATE / 2));
            fftBin = Math.max(0, Math.min(fftBin, fftMagnitudes.length - 1));

            // Take the log of the energy in this band
            float energy = fftMagnitudes[fftBin];
            melFeatures[i] = (float) Math.log10(energy + 1e-6f);
        }

        return melFeatures;
    }
}