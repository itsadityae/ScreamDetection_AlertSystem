package com.example.scremedetectionapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class ScreamDetectionService extends Service {
    private static final String TAG = "ScreamDetectionService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "ScreamDetectionChannel";

    private AudioProcessor audioProcessor;
    private ScreamDetector screamDetector;
    private LocationService locationService;
    private SmsService smsService;
    private ArrayList<Contact> emergencyContacts;

    // Use this to avoid multiple alerts for the same scream
    private long lastAlertTimestamp = 0;
    private static final long MIN_ALERT_INTERVAL_MS = 30000; // 30 seconds

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize components
        Log.d(TAG, "Initializing ScreamDetectionService components");
        audioProcessor = new AudioProcessor(this); // Pass context to AudioProcessor
        screamDetector = new ScreamDetector(this);
        locationService = new LocationService(this);
        smsService = new SmsService(this);

        Log.d(TAG, "Scream detection service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called for ScreamDetectionService");

        // Double check permissions at runtime
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO permission not granted, cannot start service");
            stopSelf();
            return START_NOT_STICKY;
        }

        // Get emergency contacts from intent
        if (intent != null && intent.hasExtra("emergencyContacts")) {
            emergencyContacts = intent.getParcelableArrayListExtra("emergencyContacts");
            Log.d(TAG, "Received " + (emergencyContacts != null ? emergencyContacts.size() : 0) + " contacts");
        } else {
            Log.w(TAG, "No emergency contacts received in intent");
        }

        // Create notification channel for foreground service
        createNotificationChannel();

        // Start service in foreground with notification
        startForeground(NOTIFICATION_ID, createNotification("Scream detection active"));

        // Start audio processing
        Log.d(TAG, "About to start audio processor recording");
        startScreamDetection();

        // Restart if service is killed
        return START_STICKY;
    }

    private void startScreamDetection() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Cannot start audio recording - permission not granted");
            updateNotification("❌ Error: Microphone permission not granted");
            return;
        }

        try {
            Log.d(TAG, "Starting audio processor recording now");
            audioProcessor.startRecording(features -> {
                if (features == null) {
                    Log.e(TAG, "Received null features from audio processor");
                    return;
                }

                Log.v(TAG, "Audio features extracted, length: " + features.length);

                // Use ML model to detect scream
                screamDetector.detectScream(features, new ScreamDetector.OnDetectionListener() {
                    @Override
                    public void onScreamDetected(float confidence) {
                        Log.d(TAG, "SCREAM DETECTED with confidence: " + confidence);

                        // Throttle alerts to avoid spam
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastAlertTimestamp < MIN_ALERT_INTERVAL_MS) {
                            Log.d(TAG, "Alert throttled (last alert was " +
                                    (currentTime - lastAlertTimestamp) / 1000 + " seconds ago)");
                            return;
                        }

                        lastAlertTimestamp = currentTime;

                        // Update notification
                        updateNotification("⚠ SCREAM DETECTED! Sending alerts...");

                        // Get location and send alerts
                        locationService.getCurrentLocation(new LocationService.LocationResultListener() {
                            @Override
                            public void onLocationResult(String address, String mapsLink) {
                                String locationInfo = address + "\n🌍 Google Maps: " + mapsLink;

                                // Send SMS alerts to emergency contacts
                                if (emergencyContacts != null && !emergencyContacts.isEmpty()) {
                                    smsService.sendAlerts(emergencyContacts, locationInfo,
                                            new SmsService.SmsResultListener() {
                                                @Override
                                                public void onSmsSent(String phoneNumber) {
                                                    Log.d(TAG, "Alert sent to: " + phoneNumber);
                                                }

                                                @Override
                                                public void onSmsError(String error) {
                                                    Log.e(TAG, "SMS error: " + error);
                                                    updateNotification("Failed to send some alerts. Tap to check.");
                                                }
                                            });
                                } else {
                                    Log.w(TAG, "No emergency contacts to alert");
                                    updateNotification("Scream detected but no contacts to alert");
                                }
                            }

                            @Override
                            public void onLocationError(String errorMessage) {
                                Log.e(TAG, "Location error: " + errorMessage);

                                // Try sending alerts without location
                                if (emergencyContacts != null && !emergencyContacts.isEmpty()) {
                                    smsService.sendAlerts(emergencyContacts, "Location not available",
                                            new SmsService.SmsResultListener() {
                                                @Override
                                                public void onSmsSent(String phoneNumber) {
                                                    Log.d(TAG, "Alert sent to: " + phoneNumber);
                                                }

                                                @Override
                                                public void onSmsError(String error) {
                                                    Log.e(TAG, "SMS error: " + error);
                                                }
                                            });
                                }
                            }
                        });
                    }

                    @Override
                    public void onNoScreamDetected(float confidence) {
                        // Just log, no action needed
                        Log.v(TAG, "No scream detected (confidence: " + confidence + ")");
                    }
                });
            });
        } catch (Exception e) {
            Log.e(TAG, "Error starting scream detection: " + e.getMessage(), e);
            updateNotification("❌ Error starting audio monitoring");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Scream Detection Service",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for Scream Detection Service");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Scream Detector")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void updateNotification(String contentText) {
        Notification notification = createNotification(contentText);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy called for ScreamDetectionService");

        // Clean up resources
        if (audioProcessor != null) {
            audioProcessor.stopRecording();
        }

        if (screamDetector != null) {
            screamDetector.close();
        }

        Log.d(TAG, "Scream detection service destroyed");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}