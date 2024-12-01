// File: ManageEventsActivity.java
package com.example.potato1_events;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for admins to manage events.
 * Allows viewing, deleting entire events, or deleting QR code hash data from event documents.
 */
public class ManageEventsActivity extends AppCompatActivity {

    // UI Components
    private RecyclerView eventsRecyclerView;
    private EventsAdapter eventsAdapter;
    private List<Event> eventList;

    // Firebase Firestore
    private FirebaseFirestore firestore;

    // Tag for logging
    private static final String TAG = "ManageEventsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_events);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI Components
        Toolbar toolbar = findViewById(R.id.toolbar_manage_events);
        setSupportActionBar(toolbar);

        // Enable the Up button for navigation
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Initialize RecyclerView
        eventsRecyclerView = findViewById(R.id.eventsRecyclerView);
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventsRecyclerView.setHasFixedSize(true);

        // Initialize event list and adapter
        eventList = new ArrayList<>();
        eventsAdapter = new EventsAdapter(eventList);
        eventsRecyclerView.setAdapter(eventsAdapter);

        // Load events from Firestore
        loadEvents();
    }

    /**
     * Loads all events from the "Events" collection in Firestore.
     * Populates the RecyclerView with the fetched event data.
     */
    private void loadEvents() {
        CollectionReference eventsRef = firestore.collection("Events");

        // Fetch all events from the "Events" collection
        eventsRef.get()
                .addOnSuccessListener(querySnapshot -> {
                    eventList.clear(); // Clear existing events to avoid duplication

                    for (DocumentSnapshot doc : querySnapshot) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            // Ensure the event ID is set
                            event.setId(doc.getId());
                            eventList.add(event);
                        }
                    }

                    eventsAdapter.notifyDataSetChanged(); // Refresh the RecyclerView

                    if (eventList.isEmpty()) {
                        Toast.makeText(ManageEventsActivity.this, "No events found.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ManageEventsActivity.this, "Loaded Events: " + eventList.size(), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ManageEventsActivity.this, "Error loading events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading events", e);
                });
    }

    /**
     * Handles the selection of menu items in the action bar.
     * Specifically handles the Up button to navigate back to the parent activity.
     *
     * @param item The selected menu item.
     * @return True if the event was handled, false otherwise.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle Up button presses
        if (item.getItemId() == android.R.id.home) {
            finish(); // Closes the current activity and returns to the parent
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Adapter class for managing and displaying a list of events in the RecyclerView.
     * Binds event data to the UI components and handles admin interactions such as deletion.
     */
    private class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

        private List<Event> events;

        EventsAdapter(List<Event> events) {
            this.events = events;
        }

        @NonNull
        @Override
        public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Inflate the item_event layout
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.manage_event_item, parent, false);
            return new EventViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
            Event event = events.get(position);

            // Bind event data to TextViews
            holder.eventNameTextView.setText(event.getName());
            holder.eventLocationTextView.setText("Location: " + event.getEventLocation());

            // Load event poster image
            if (!TextUtils.isEmpty(event.getPosterImageUrl())) {
                // Use Picasso or any other image loading library
                Picasso.get()
                        .load(event.getPosterImageUrl())
                        .placeholder(R.drawable.ic_placeholder_image)
                        .error(R.drawable.ic_error_image)
                        .into(holder.eventPosterImageView);
            } else {
                holder.eventPosterImageView.setImageResource(R.drawable.ic_placeholder_image);
            }

            // Set Delete QR Code Button Listener
            holder.deleteQrButton.setOnClickListener(v -> {
                confirmDeleteQrCode(event);
            });

            // Set Delete Event Button Listener
            holder.deleteEventButton.setOnClickListener(v -> {
                confirmDeleteEvent(event);
            });
        }

        @Override
        public int getItemCount() {
            return events.size();
        }

        /**
         * ViewHolder class for holding and recycling event item views.
         * Caches references to the UI components for each event item to improve performance.
         */
        class EventViewHolder extends RecyclerView.ViewHolder {

            ImageView eventPosterImageView;
            TextView eventNameTextView;
            TextView eventLocationTextView;
            Button deleteQrButton;
            Button deleteEventButton;

            EventViewHolder(@NonNull View itemView) {
                super(itemView);
                eventPosterImageView = itemView.findViewById(R.id.eventPosterImageView);
                eventNameTextView = itemView.findViewById(R.id.eventNameTextView);
                eventLocationTextView = itemView.findViewById(R.id.eventLocationTextView);
                deleteQrButton = itemView.findViewById(R.id.deleteQrButton);
                deleteEventButton = itemView.findViewById(R.id.deleteEventButton);
            }
        }
    }

    /**
     * Prompts the admin to confirm deletion of the QR code hash data for an event.
     *
     * @param event The Event object whose QR code hash is to be deleted.
     */
    private void confirmDeleteQrCode(Event event) {
        new AlertDialog.Builder(this)
                .setTitle("Delete QR Code Hash")
                .setMessage("Are you sure you want to delete the QR code hash for this event?")
                .setPositiveButton("Yes", (dialog, which) -> deleteQrCodeHash(event))
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Deletes the QR code hash from the specified event document in Firestore.
     *
     * @param event The Event object whose QR code hash is to be deleted.
     */
    private void deleteQrCodeHash(Event event) {
        if (event == null || TextUtils.isEmpty(event.getId())) {
            Toast.makeText(this, "Invalid event data.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference eventRef = firestore.collection("Events").document(event.getId());

        // Update the qrCodeHash field to null or remove it
        eventRef.update("qrCodeHash", null)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "QR code hash deleted successfully.", Toast.LENGTH_SHORT).show();
                    loadEvents(); // Refresh the event list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting QR code hash: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error deleting QR code hash", e);
                });
    }

    /**
     * Prompts the admin to confirm deletion of an entire event.
     *
     * @param event The Event object to delete.
     */
    private void confirmDeleteEvent(Event event) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this entire event? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> deleteEvent(event))
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Deletes the specified event from Firestore.
     * Also handles deletion of associated QR code images from Firebase Storage if applicable.
     *
     * @param event The Event object to delete.
     */
    private void deleteEvent(Event event) {
        if (event == null || TextUtils.isEmpty(event.getId())) {
            Toast.makeText(this, "Invalid event data.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference eventRef = firestore.collection("Events").document(event.getId());

        // First, delete the Firestore document
        eventRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event deleted successfully.", Toast.LENGTH_SHORT).show();

                    // Optionally, delete the poster image from Firebase Storage
                    if (!TextUtils.isEmpty(event.getPosterImageUrl())) {
                        // Assuming the posterImageUrl contains the storage path
                        // Adjust this logic based on how you store images
                        String storagePath = event.getPosterImageUrl(); // Modify as needed
                        FirebaseStorage.getInstance().getReference(storagePath).delete()
                                .addOnSuccessListener(aVoid1 -> {
                                    Log.d(TAG, "Poster image deleted successfully.");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error deleting poster image: " + e.getMessage(), e);
                                });
                    }

                    loadEvents(); // Refresh the event list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error deleting event", e);
                });
    }
}
