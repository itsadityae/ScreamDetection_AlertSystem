package com.example.scremedetectionapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationService {
    private static final String TAG = "LocationService";

    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;

    public interface LocationResultListener {
        void onLocationResult(String address, String mapsLink);
        void onLocationError(String errorMessage);
    }

    public LocationService(Context context) {
        this.context = context;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public void getCurrentLocation(LocationResultListener listener) {
        // Check permission first
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            listener.onLocationError("Location permission not granted");
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Location location = task.getResult();
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            // Create Google Maps link
                            String mapsLink = "https://maps.google.com/?q=" + latitude + "," + longitude;

                            // Try to get the address
                            try {
                                String address = getAddressFromLocation(location);
                                listener.onLocationResult(address, mapsLink);
                            } catch (IOException e) {
                                // If we can't get the address, just use the coordinates
                                String coordinates = "Lat: " + latitude + ", Long: " + longitude;
                                listener.onLocationResult(coordinates, mapsLink);
                            }
                        } else {
                            listener.onLocationError("Could not get current location");
                        }
                    }
                });
    }

    private String getAddressFromLocation(Location location) throws IOException {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses = geocoder.getFromLocation(
                location.getLatitude(), location.getLongitude(), 1);

        if (addresses != null && !addresses.isEmpty()) {
            Address address = addresses.get(0);
            StringBuilder sb = new StringBuilder();

            // Add as much address information as we have
            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(address.getAddressLine(i));
            }

            return sb.toString();
        } else {
            return "Unknown location";
        }
    }
}