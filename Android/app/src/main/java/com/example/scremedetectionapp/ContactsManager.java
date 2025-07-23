package com.example.scremedetectionapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * ContactsManager handles storing and retrieving emergency contacts
 * using SharedPreferences for persistence.
 */
public class ContactsManager {
    private static final String TAG = "ContactsManager";
    private static final String PREFS_NAME = "EmergencyContactsPrefs";
    private static final String CONTACTS_KEY = "contacts";

    private final Context context;
    private final SharedPreferences prefs;

    public ContactsManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Save a list of emergency contacts to SharedPreferences
     */
    public boolean saveEmergencyContacts(List<com.example.scremedetectionapp.Contact> contacts) {
        try {
            JSONArray contactsArray = new JSONArray();

            for (com.example.scremedetectionapp.Contact contact : contacts) {
                JSONObject contactObj = new JSONObject();
                contactObj.put("name", contact.getName());
                contactObj.put("phoneNumber", contact.getPhoneNumber());
                contactObj.put("relationship", contact.getRelationship());
                contactsArray.put(contactObj);
            }

            String contactsJson = contactsArray.toString();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(CONTACTS_KEY, contactsJson);
            boolean success = editor.commit();

            if (success) {
                Log.d(TAG, "Saved " + contacts.size() + " emergency contacts");
            } else {
                Log.e(TAG, "Failed to save emergency contacts");
            }

            return success;

        } catch (JSONException e) {
            Log.e(TAG, "Error saving contacts: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the list of saved emergency contacts
     */
    public List<com.example.scremedetectionapp.Contact> getEmergencyContacts() {
        List<com.example.scremedetectionapp.Contact> contacts = new ArrayList<>();

        String contactsJson = prefs.getString(CONTACTS_KEY, null);
        if (contactsJson == null) {
            Log.d(TAG, "No saved contacts found");
            return contacts;
        }

        try {
            JSONArray contactsArray = new JSONArray(contactsJson);

            for (int i = 0; i < contactsArray.length(); i++) {
                JSONObject contactObj = contactsArray.getJSONObject(i);

                String name = contactObj.getString("name");
                String phoneNumber = contactObj.getString("phoneNumber");
                String relationship = contactObj.optString("relationship", "");

                contacts.add(new com.example.scremedetectionapp.Contact(name, phoneNumber, relationship));
            }

            Log.d(TAG, "Loaded " + contacts.size() + " emergency contacts");

        } catch (JSONException e) {
            Log.e(TAG, "Error loading contacts: " + e.getMessage());
        }

        return contacts;
    }

    /**
     * Add a single emergency contact
     */
    public boolean addEmergencyContact(com.example.scremedetectionapp.Contact newContact) {
        List<com.example.scremedetectionapp.Contact> currentContacts = getEmergencyContacts();

        // Check if contact with same number already exists
        for (com.example.scremedetectionapp.Contact contact : currentContacts) {
            if (contact.getPhoneNumber().equals(newContact.getPhoneNumber())) {
                Log.d(TAG, "Contact with phone number " + newContact.getPhoneNumber() + " already exists");
                return false;
            }
        }

        currentContacts.add(newContact);
        return saveEmergencyContacts(currentContacts);
    }

    /**
     * Remove an emergency contact by phone number
     */
    public boolean removeEmergencyContact(String phoneNumber) {
        List<com.example.scremedetectionapp.Contact> currentContacts = getEmergencyContacts();
        boolean found = false;

        for (int i = 0; i < currentContacts.size(); i++) {
            if (currentContacts.get(i).getPhoneNumber().equals(phoneNumber)) {
                currentContacts.remove(i);
                found = true;
                break;
            }
        }

        if (found) {
            return saveEmergencyContacts(currentContacts);
        } else {
            Log.d(TAG, "Contact with phone number " + phoneNumber + " not found");
            return false;
        }
    }

    /**
     * Clear all emergency contacts
     */
    public boolean clearAllContacts() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(CONTACTS_KEY);
        boolean success = editor.commit();

        if (success) {
            Log.d(TAG, "All emergency contacts cleared");
        }

        return success;
    }
}