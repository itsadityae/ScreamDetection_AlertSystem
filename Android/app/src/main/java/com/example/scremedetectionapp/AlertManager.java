package com.example.scremedetectionapp;

import android.content.Context;
import android.util.Log;

import java.util.List;

/**
 * AlertManager coordinates location fetching and SMS sending when a scream is detected.
 */
public class AlertManager {
    private static final String TAG = "AlertManager";

    private final Context context;
    private final LocationService locationService;
    private final SmsService smsService;
    private final ContactsManager contactsManager;

    public interface AlertResultListener {
        void onAlertSent();
        void onAlertError(String error);
    }

    public AlertManager(Context context) {
        this.context = context;
        this.locationService = new LocationService(context);
        this.smsService = new SmsService(context);
        this.contactsManager = new ContactsManager(context); // Assuming you have a class to manage contacts
    }

    /**
     * Called when a scream is detected to send alerts to emergency contacts
     */
    public void sendEmergencyAlerts(AlertResultListener listener) {
        Log.d(TAG, "Sending emergency alerts");

        // First get the user's emergency contacts
        List<Contact> emergencyContacts = contactsManager.getEmergencyContacts();

        if (emergencyContacts.isEmpty()) {
            listener.onAlertError("No emergency contacts available");
            return;
        }

        // Get current location
        locationService.getCurrentLocation(new LocationService.LocationResultListener() {
            @Override
            public void onLocationResult(String address, String mapsLink) {
                Log.d(TAG, "Location retrieved: " + address);

                // Send SMS alerts with the location info
                smsService.sendAlerts(emergencyContacts, address, mapsLink, new SmsService.SmsResultListener() {
                    @Override
                    public void onSmsSent(String phoneNumber) {
                        Log.d(TAG, "Alert sent to: " + phoneNumber);
                        listener.onAlertSent();
                    }

                    @Override
                    public void onSmsError(String error) {
                        Log.e(TAG, "SMS Error: " + error);
                        listener.onAlertError("Failed to send SMS: " + error);
                    }
                });
            }

            @Override
            public void onLocationError(String errorMessage) {
                Log.e(TAG, "Location Error: " + errorMessage);

                // Send alerts anyway, but without precise location
                smsService.sendAlerts(emergencyContacts, "Location unavailable", "No maps link available",
                        new SmsService.SmsResultListener() {
                            @Override
                            public void onSmsSent(String phoneNumber) {
                                Log.d(TAG, "Alert sent to: " + phoneNumber + " (without location)");
                                listener.onAlertSent();
                            }

                            @Override
                            public void onSmsError(String error) {
                                Log.e(TAG, "SMS Error: " + error);
                                listener.onAlertError("Failed to send SMS: " + error);
                            }
                        });
            }
        });
    }

    /**
     * Example usage in your scream detection activity
     */
    public static void exampleUsage(Context context) {
        // When a scream is detected
        AlertManager alertManager = new AlertManager(context);
        alertManager.sendEmergencyAlerts(new AlertResultListener() {
            @Override
            public void onAlertSent() {
                // Update UI to show alerts were sent
                Log.d(TAG, "Emergency alerts sent successfully");
            }

            @Override
            public void onAlertError(String error) {
                // Show error in UI
                Log.e(TAG, "Failed to send emergency alerts: " + error);
            }
        });
    }
}