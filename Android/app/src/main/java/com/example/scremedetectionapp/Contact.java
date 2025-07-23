package com.example.scremedetectionapp;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Contact model class to store emergency contact information
 */
public class Contact implements Parcelable {
    private String name;
    private String phoneNumber;
    private String relationship;

    public Contact(String name, String phoneNumber, String relationship) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.relationship = relationship;
    }

    public Contact(String name, String phoneNumber) {
        this(name, phoneNumber, "");
    }

    // Constructor that reads data from a parcel
    protected Contact(Parcel in) {
        name = in.readString();
        phoneNumber = in.readString();
        relationship = in.readString();
    }

    // Required creator field for Parcelable implementation
    public static final Creator<Contact> CREATOR = new Creator<Contact>() {
        @Override
        public Contact createFromParcel(Parcel in) {
            return new Contact(in);
        }

        @Override
        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    @Override
    public String toString() {
        return name + " (" + relationship + "): " + phoneNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Contact contact = (Contact) obj;
        return phoneNumber != null ? phoneNumber.equals(contact.phoneNumber) : contact.phoneNumber == null;
    }

    @Override
    public int hashCode() {
        return phoneNumber != null ? phoneNumber.hashCode() : 0;
    }

    // Describe the kinds of special objects contained in this Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    // Write object's data to the passed-in Parcel
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(phoneNumber);
        dest.writeString(relationship);
    }
}