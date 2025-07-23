package com.example.scremedetectionapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scremedetectionapp.Contact;

import java.util.ArrayList;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    private List<Contact> contacts = new ArrayList<>();
    private final OnContactDeleteListener deleteListener;

    public interface OnContactDeleteListener {
        void onContactDelete(int position);
    }

    public ContactAdapter(OnContactDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contacts.get(position);
        holder.bind(contact, position);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public void setContacts(List<Contact> newContacts) {
        this.contacts = new ArrayList<>(newContacts);
        notifyDataSetChanged();
    }

    public void addContact(Contact contact) {
        contacts.add(contact);
        notifyItemInserted(contacts.size() - 1);
    }

    public void removeContact(int position) {
        if (position >= 0 && position < contacts.size()) {
            contacts.remove(position);
            notifyItemRemoved(position);
        }
    }

    public List<Contact> getContacts() {
        return new ArrayList<>(contacts);
    }

    class ContactViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView phoneTextView;
        private final TextView relationshipTextView;
        private final ImageButton deleteButton;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.contactNameText);
            phoneTextView = itemView.findViewById(R.id.contactPhoneText);
            relationshipTextView = itemView.findViewById(R.id.contactRelationshipText);
            deleteButton = itemView.findViewById(R.id.deleteContactButton);
        }

        public void bind(Contact contact, int position) {
            nameTextView.setText(contact.getName());
            phoneTextView.setText(contact.getPhoneNumber());

            if (contact.getRelationship() != null && !contact.getRelationship().isEmpty()) {
                relationshipTextView.setVisibility(View.VISIBLE);
                relationshipTextView.setText(contact.getRelationship());
            } else {
                relationshipTextView.setVisibility(View.GONE);
            }

            deleteButton.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (deleteListener != null && pos != RecyclerView.NO_POSITION) {
                    deleteListener.onContactDelete(pos);
                }
            });
        }
    }
}
