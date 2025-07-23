package com.example.scremedetectionapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class SmsService {
    private static final String TAG = "SmsService";

    private final Context context;
    private static final String SMS_SENT = "SMS_SENT";
    private static final String SMS_DELIVERED = "SMS_DELIVERED";

    public interface SmsResultListener {
        void onSmsSent(String phoneNumber);

        void onSmsError(String error);
    }

    public SmsService(Context context) {
        this.context = context;
    }

    public void sendAlerts(List<Contact> emergencyContacts, String locationInfo, SmsResultListener listener) {
        // Check SMS permission first
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SMS permission not granted!");
            listener.onSmsError("SMS permission not granted");
            return;
        }

        String messageBody = "🚨 EMERGENCY ALERT: Scream detected! The user may be in danger!\n" +
                "📍 Location info: " + locationInfo;

        sendDirectSmsAlerts(emergencyContacts, messageBody, listener);
    }

    public void sendAlerts(List<Contact> contacts, String address, String mapsLink, SmsResultListener listener) {
        // Check SMS permission first
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SMS permission not granted!");
            listener.onSmsError("SMS permission not granted");
            return;
        }

        String messageBody = "🚨 EMERGENCY ALERT: Scream detected! The user may be in danger!\n" +
                "📍 Location: " + address + "\n" +
                "🔗 Maps: " + mapsLink;

        sendDirectSmsAlerts(contacts, messageBody, listener);
    }

    private void sendDirectSmsAlerts(List<Contact> contacts, String messageBody, SmsResultListener listener) {
        try {
            SmsManager smsManager = SmsManager.getDefault();

            if (contacts == null || contacts.isEmpty()) {
                Log.e(TAG, "No emergency contacts to send alerts to");
                listener.onSmsError("No emergency contacts available");
                return;
            }

            Log.d(TAG, "Attempting to send alerts to " + contacts.size() + " contacts");

            for (Contact contact : contacts) {
                try {
                    String phoneNumber = formatPhoneNumber(contact.getPhoneNumber());

                    if (phoneNumber == null || phoneNumber.isEmpty()) {
                        Log.e(TAG, "Invalid phone number for contact: " + contact.getName());
                        listener.onSmsError("Invalid phone number for " + contact.getName());
                        continue;
                    }

                    Log.d(TAG, "Sending SMS to: " + phoneNumber + " (" + contact.getName() + ")");

                    // Create PendingIntents to track message status
                    PendingIntent sentIntent = PendingIntent.getBroadcast(
                            context, 0, new Intent(SMS_SENT), PendingIntent.FLAG_IMMUTABLE);
                    PendingIntent deliveredIntent = PendingIntent.getBroadcast(
                            context, 0, new Intent(SMS_DELIVERED), PendingIntent.FLAG_IMMUTABLE);

                    // Split message if it's too long
                    ArrayList<String> parts = smsManager.divideMessage(messageBody);
                    ArrayList<PendingIntent> sentIntents = new ArrayList<>();
                    ArrayList<PendingIntent> deliveredIntents = new ArrayList<>();

                    for (int i = 0; i < parts.size(); i++) {
                        sentIntents.add(sentIntent);
                        deliveredIntents.add(deliveredIntent);
                    }

                    // Send the message with status tracking
                    smsManager.sendMultipartTextMessage(
                            phoneNumber,
                            null,
                            parts,
                            sentIntents,
                            deliveredIntents
                    );

                    Log.d(TAG, "SMS alert queued for " + contact.getName() + " (" + phoneNumber + ")");
                    listener.onSmsSent(phoneNumber);

                } catch (Exception e) {
                    Log.e(TAG, "Error sending SMS to " + contact.getPhoneNumber() + ": " + e.getMessage(), e);
                    listener.onSmsError("Failed to send SMS to " + contact.getName() + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "SMS manager error: " + e.getMessage(), e);
            listener.onSmsError("SMS error: " + e.getMessage());
        }
    }

    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return null;

        // Log original number for debugging
        Log.d(TAG, "Formatting phone number: " + phoneNumber);

        // Remove any non-digit characters except the + sign
        String cleaned = phoneNumber.replaceAll("[^\\d+]", "");

        // Make sure we have at least some digits
        if (cleaned.length() < 6) {
            Log.e(TAG, "Phone number too short: " + cleaned);
            return null;
        }

        // If number doesn't start with +, add country code if it's missing
        if (!cleaned.startsWith("+")) {
            // For India numbers (your example has +91)
            if (cleaned.length() == 10) {
                // This assumes a 10-digit number is a local number without country code
                // Modify this based on your country's phone number format
                cleaned = "+91" + cleaned;  // Change this to your country code if not India
                Log.d(TAG, "Added country code to 10-digit number: " + cleaned);
            } else if (cleaned.startsWith("91") && cleaned.length() == 12) {
                // If number starts with country code but missing +
                cleaned = "+" + cleaned;
                Log.d(TAG, "Added + to number with country code: " + cleaned);
            }
            // Add additional country-specific formatting rules if needed
        }

        Log.d(TAG, "Final formatted phone number: " + phoneNumber + " -> " + cleaned);
        return cleaned;
    }
}