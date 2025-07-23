package com.example.scremedetectionapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements ContactAdapter.OnContactDeleteListener {
    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final int SETTINGS_REQUEST_CODE = 101;

    private final String[] requiredPermissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS
    };

    private static final String POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";

    private EditText nameEditText;
    private EditText phoneEditText;
    private EditText relationshipEditText;
    private Button addContactButton;
    private Button safetyModeOnButton;
    private Button safetyModeOffButton;
    private RecyclerView contactsRecyclerView;
    private ContactAdapter contactAdapter;

    private ArrayList<Contact> emergencyContacts = new ArrayList<>();
    private boolean safetyModeActive = false;

    private AudioProcessor audioProcessor;

    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nameEditText = findViewById(R.id.nameEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        relationshipEditText = findViewById(R.id.relationshipEditText);
        addContactButton = findViewById(R.id.addContactButton);
        safetyModeOnButton = findViewById(R.id.safetyModeOnButton);
        safetyModeOffButton = findViewById(R.id.safetyModeOffButton);
        testSmsButton = findViewById(R.id.testSmsButton);  // ADD THIS LINE HERE

        contactsRecyclerView = findViewById(R.id.contactsRecyclerView);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        contactAdapter = new ContactAdapter(this);
        contactAdapter.setContacts(emergencyContacts);
        contactsRecyclerView.setAdapter(contactAdapter);

        addContactButton.setOnClickListener(v -> addContact());
        safetyModeOnButton.setOnClickListener(v -> toggleSafetyMode(true));
        safetyModeOffButton.setOnClickListener(v -> toggleSafetyMode(false));
        testSmsButton.setOnClickListener(v -> testSmsAlerts());  // ADD THIS LINE HERE

        updateSafetyButtonStates();
        checkAndRequestPermissions();

        audioProcessor = new AudioProcessor(this);
    }
    private void updateSafetyButtonStates() {
        safetyModeOnButton.setEnabled(!safetyModeActive);
        safetyModeOffButton.setEnabled(safetyModeActive);
    }

    private void checkAndRequestPermissions() {
        ArrayList<String> permissionsToRequest = new ArrayList<>();

        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(POST_NOTIFICATIONS);
        }

        if (!permissionsToRequest.isEmpty()) {
            showPermissionsDialog(permissionsToRequest.toArray(new String[0]));
        }
    }

    // Add to MainActivity.java
    // Add this as a class member
    private Button testSmsButton;



    // Add this method to your MainActivity class
    // Modify your testSmsAlerts() method to add more logging and robust error handling

    private void testSmsAlerts() {
        if (emergencyContacts == null || emergencyContacts.isEmpty()) {
            Toast.makeText(this, "No emergency contacts to test with", Toast.LENGTH_SHORT).show();
            return;
        }

        // Log to verify contacts are available
        Log.d("TestSMS", "Testing SMS with " + emergencyContacts.size() + " contacts");
        for (Contact contact : emergencyContacts) {
            Log.d("TestSMS", "Contact: " + contact.getName() + " - " + contact.getPhoneNumber());
        }

        // First check SMS permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission not granted!", Toast.LENGTH_LONG).show();
            // Request SMS permission specifically
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    PERMISSIONS_REQUEST_CODE);
            return;
        }

        // Make a test location
        String testLocation = "Test location - https://maps.app.goo.gl/JoupQy9z89avg1GS6";

        // Create SmsService and send test message
        SmsService smsService = new SmsService(this);

        // Show a progress dialog or toast
        Toast.makeText(this, "Attempting to send test SMS...", Toast.LENGTH_SHORT).show();

        smsService.sendAlerts(emergencyContacts, testLocation,
                new SmsService.SmsResultListener() {
                    @Override
                    public void onSmsSent(String phoneNumber) {
                        Log.d("TestSMS", "Test SMS sent to " + phoneNumber);
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this,
                                    "Test SMS sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onSmsError(String error) {
                        Log.e("TestSMS", "Test SMS error: " + error);
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this,
                                    "Test SMS error: " + error, Toast.LENGTH_LONG).show();

                            // Show more details in an alert dialog
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("SMS Error")
                                    .setMessage("Error sending SMS: " + error +
                                            "\n\nPlease check your permissions and contact information.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        });
                    }
                });
    }

    // Add a test button in your layout and connect it to this method
    private void showPermissionsDialog(String[] permissionsToRequest) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissions Required");
        builder.setMessage("This app requires all permissions to function correctly. Please grant all permissions.");
        builder.setPositiveButton("Request Again", (dialog, which) -> {
            ActivityCompat.requestPermissions(MainActivity.this, permissionsToRequest, PERMISSIONS_REQUEST_CODE);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
            Toast.makeText(this, "App cannot function without permissions", Toast.LENGTH_LONG).show();
        });
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                showSettingsDialog();
            }
        }
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissions Required");
        builder.setMessage("Some permissions were denied. The app needs these permissions to function properly. Please grant them in the app settings.");
        builder.setPositiveButton("Open Settings", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivityForResult(intent, SETTINGS_REQUEST_CODE);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void addContact() {
        String name = nameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String relationship = relationshipEditText.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Name and phone number are required", Toast.LENGTH_SHORT).show();
            return;
        }

        phone = phone.replaceAll("[\\s-()]", "");

        Contact contact = new Contact(name, phone, relationship);
        emergencyContacts.add(contact);
        contactAdapter.addContact(contact);

        if (safetyModeActive) {
            stopScreamDetectionService();
            startScreamDetectionService();
        }

        Toast.makeText(this, "Contact added", Toast.LENGTH_SHORT).show();
        nameEditText.setText("");
        phoneEditText.setText("");
        relationshipEditText.setText("");
    }

    private void toggleSafetyMode(boolean enabled) {
        if (enabled) {
            if (emergencyContacts.isEmpty()) {
                Toast.makeText(this, "Please add at least one emergency contact", Toast.LENGTH_LONG).show();
                return;
            }

            for (String permission : requiredPermissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Please grant all required permissions", Toast.LENGTH_LONG).show();
                    checkAndRequestPermissions();
                    return;
                }
            }

            startScreamDetectionService();

            audioProcessor.startRecording(features -> {
                Log.d(TAG, "Features extracted: " + Arrays.toString(features));
                // TODO: Add scream detection logic here
            });

            safetyModeActive = true;
            updateSafetyButtonStates();
            Toast.makeText(this, "Safety Mode activated", Toast.LENGTH_SHORT).show();
        } else {
            stopScreamDetectionService();
            audioProcessor.stopRecording();
            safetyModeActive = false;
            updateSafetyButtonStates();
            Toast.makeText(this, "Safety Mode deactivated", Toast.LENGTH_SHORT).show();
        }
    }

    private void startScreamDetectionService() {
        Intent serviceIntent = new Intent(this, ScreamDetectionService.class);
        ArrayList<Contact> contacts = new ArrayList<>(contactAdapter.getContacts());
        serviceIntent.putParcelableArrayListExtra("emergencyContacts", contacts);
        serviceIntent.putExtra("sampleRate", SAMPLE_RATE);
        serviceIntent.putExtra("channelConfig", CHANNEL_CONFIG);
        serviceIntent.putExtra("audioFormat", AUDIO_FORMAT);
        serviceIntent.putExtra("bufferSize", BUFFER_SIZE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Log.d(TAG, "Scream detection service started with " + contacts.size() + " contacts");
    }

    private void stopScreamDetectionService() {
        Intent serviceIntent = new Intent(this, ScreamDetectionService.class);
        stopService(serviceIntent);
        Log.d(TAG, "Scream detection service stopped");
    }

    @Override
    public void onContactDelete(int position) {
        contactAdapter.removeContact(position);
        if (position >= 0 && position < emergencyContacts.size()) {
            emergencyContacts.remove(position);
        }

        if (safetyModeActive) {
            stopScreamDetectionService();
            startScreamDetectionService();
        }

        Toast.makeText(this, "Contact removed", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndRequestPermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (safetyModeActive) {
            stopScreamDetectionService();
        }
        if (audioProcessor != null) {
            audioProcessor.stopRecording();
        }
    }
}
