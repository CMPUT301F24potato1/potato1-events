// File: EventDetailsEntrantActivity.java
package com.example.potato1_events;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Settings.Secure;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity to display the details of an event for entrants.
 * Allows entrants to join or leave the waiting list.
 */
public class EventDetailsEntrantActivity extends AppCompatActivity {

    // UI Components
    private ImageView eventPosterImageView;
    private TextView eventNameTextView;
    private TextView eventDescriptionTextView;
    private TextView eventLocationTextView;
    private TextView eventDatesTextView;
    private TextView eventCapacityTextView;
    private TextView eventGeolocationTextView;
    private TextView eventStatusTextView;
    private Button joinButton;
    private Button leaveButton;

    // Firebase Firestore
    private FirebaseFirestore firestore;

    // Event Data
    private String eventId;
    private Event event;

    // Entrant Data
    private String deviceId; // Used as the unique identifier for the entrant

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details_entrant);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI Components
        eventPosterImageView = findViewById(R.id.eventPosterImageView);
        eventNameTextView = findViewById(R.id.eventNameTextView);
        eventDescriptionTextView = findViewById(R.id.eventDescriptionTextView);
        eventLocationTextView = findViewById(R.id.eventLocationTextView);
        eventDatesTextView = findViewById(R.id.eventDatesTextView);
        eventCapacityTextView = findViewById(R.id.eventCapacityTextView);
        eventGeolocationTextView = findViewById(R.id.eventGeolocationTextView);
        eventStatusTextView = findViewById(R.id.eventStatusTextView);
        joinButton = findViewById(R.id.joinButton);
        leaveButton = findViewById(R.id.leaveButton);

        // Retrieve Device ID (unique identifier for the entrant)
        deviceId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);

        // Retrieve EVENT_ID from Intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("EVENT_ID")) {
            eventId = intent.getStringExtra("EVENT_ID");
            loadEventDetails(eventId);
        } else {
            Toast.makeText(this, "No Event ID provided.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Set Click Listeners for Action Buttons
        joinButton.setOnClickListener(v -> handleJoinAction());
        leaveButton.setOnClickListener(v -> handleLeaveAction());

        // Set up the Toolbar and enable the Up button
        Toolbar toolbar = findViewById(R.id.toolbar); // Ensure your layout has a Toolbar with this ID
        setSupportActionBar(toolbar);

        // Enable the Up button
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Handles the Up button behavior.
     *
     * @param item The selected menu item.
     * @return True if handled, else false.
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
     * Loads event details from Firestore based on eventId.
     *
     * @param eventId The ID of the event to load.
     */
    private void loadEventDetails(String eventId) {
        firestore.collection("Events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        event = documentSnapshot.toObject(Event.class);
                        if (event != null) {
                            event.setId(documentSnapshot.getId());
                            populateEventDetails(event);
                        } else {
                            Toast.makeText(EventDetailsEntrantActivity.this, "Error parsing event data.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(EventDetailsEntrantActivity.this, "Event not found.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EventDetailsEntrantActivity.this, "Error loading event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * Populates the UI with event details.
     *
     * @param event The Event object containing details.
     */
    private void populateEventDetails(Event event) {
        // Load poster image
        if (!TextUtils.isEmpty(event.getPosterImageUrl())) {
            Picasso.get()
                    .load(event.getPosterImageUrl())
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_error_image)
                    .into(eventPosterImageView);
        } else {
            eventPosterImageView.setImageResource(R.drawable.ic_placeholder_image);
        }

        // Set text views with null checks
        eventNameTextView.setText(event.getName() != null ? event.getName() : "Unnamed Event");
        eventDescriptionTextView.setText(event.getDescription() != null ? event.getDescription() : "No Description Available");
        eventLocationTextView.setText("Location: " + (event.getEventLocation() != null ? event.getEventLocation() : "Unknown"));

        String capacity = "Waiting List Capacity: " + event.getCurrentEntrantsNumber() + " / " + event.getWaitingListCapacity();
        eventCapacityTextView.setText(capacity);

        String geo = "Geolocation Required: " + (event.isGeolocationRequired() ? "Yes" : "No");
        eventGeolocationTextView.setText(geo);

        String status = "Status: " + (event.getStatus() != null ? event.getStatus() : "Unknown");
        eventStatusTextView.setText(status);

        // Check if the entrant is already on the waiting list
        updateButtonStates();
    }

    /**
     * Updates the visibility of join and leave buttons based on entrant's status.
     */
    private void updateButtonStates() {
        firestore.collection("Events").document(eventId).collection("WaitingList")
                .document(deviceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Entrant is on the waiting list
                        joinButton.setVisibility(View.GONE);
                        leaveButton.setVisibility(View.VISIBLE);
                    } else {
                        // Entrant is not on the waiting list
                        joinButton.setVisibility(View.VISIBLE);
                        leaveButton.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking waiting list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Handles the join button action.
     * Adds the entrant to the event's waiting list.
     */
    private void handleJoinAction() {
        // Check if the event has reached its waiting list capacity
        if (event.getCurrentEntrantsNumber() >= event.getWaitingListCapacity()) {
            Toast.makeText(this, "Waiting list is full.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Confirm joining
        new AlertDialog.Builder(this)
                .setTitle("Join Waiting List")
                .setMessage("Are you sure you want to join the waiting list for this event?")
                .setPositiveButton("Yes", (dialog, which) -> joinWaitingList())
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Adds the entrant to the event's waiting list in Firestore.
     */
    private void joinWaitingList() {
        final DocumentReference eventRef = firestore.collection("Events").document(eventId);
        final DocumentReference entrantRef = eventRef.collection("WaitingList").document(deviceId);

        firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            // Fetch the latest event data
            DocumentSnapshot eventSnapshot = transaction.get(eventRef);
            if (!eventSnapshot.exists()) {
                throw new FirebaseFirestoreException("Event does not exist.",
                        FirebaseFirestoreException.Code.ABORTED, null);
            }

            // Check if entrant is already on the waiting list
            DocumentSnapshot entrantSnapshot = transaction.get(entrantRef);
            if (entrantSnapshot.exists()) {
                throw new FirebaseFirestoreException("Already on the waiting list.",
                        FirebaseFirestoreException.Code.ABORTED, null);
            }

            // Check if waiting list is full
            Long currentEntrants = eventSnapshot.getLong("currentEntrantsNumber");
            Long capacity = eventSnapshot.getLong("waitingListCapacity");
            if (currentEntrants == null || capacity == null) {
                throw new FirebaseFirestoreException("Invalid event data.",
                        FirebaseFirestoreException.Code.ABORTED, null);
            }

            if (currentEntrants >= capacity) {
                throw new FirebaseFirestoreException("Waiting list is full.",
                        FirebaseFirestoreException.Code.ABORTED, null);
            }

            // Add entrant to WaitingList subcollection
            Map<String, Object> entrantData = new HashMap<>();
            entrantData.put("deviceId", deviceId);
            entrantData.put("joinedAt", FieldValue.serverTimestamp());
            transaction.set(entrantRef, entrantData);

            // Increment currentEntrantsNumber
            transaction.update(eventRef, "currentEntrantsNumber", FieldValue.increment(1));

            // Add deviceId to waitingList array
            transaction.update(eventRef, "waitingList", FieldValue.arrayUnion(deviceId));

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Successfully joined the waiting list.", Toast.LENGTH_SHORT).show();
            event.setCurrentEntrantsNumber(event.getCurrentEntrantsNumber() + 1);
            updateButtonStates();
            populateEventDetails(event);
        }).addOnFailureListener(e -> {
            if (e instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
                String message = firestoreException.getMessage();
                if (firestoreException.getCode() == FirebaseFirestoreException.Code.ABORTED) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error joining waiting list: " + message, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error joining waiting list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Handles the leave button action.
     * Removes the entrant from the event's waiting list.
     */
    private void handleLeaveAction() {
        // Confirm leaving
        new AlertDialog.Builder(this)
                .setTitle("Leave Waiting List")
                .setMessage("Are you sure you want to leave the waiting list for this event?")
                .setPositiveButton("Yes", (dialog, which) -> leaveWaitingList())
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Removes the entrant from the event's waiting list in Firestore.
     */
    private void leaveWaitingList() {
        final DocumentReference eventRef = firestore.collection("Events").document(eventId);
        final DocumentReference entrantRef = eventRef.collection("WaitingList").document(deviceId);

        firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            // Fetch the latest event data
            DocumentSnapshot eventSnapshot = transaction.get(eventRef);
            if (!eventSnapshot.exists()) {
                throw new FirebaseFirestoreException("Event does not exist.",
                        FirebaseFirestoreException.Code.ABORTED, null);
            }

            // Check if entrant is on the waiting list
            DocumentSnapshot entrantSnapshot = transaction.get(entrantRef);
            if (!entrantSnapshot.exists()) {
                throw new FirebaseFirestoreException("Not on the waiting list.",
                        FirebaseFirestoreException.Code.ABORTED, null);
            }

            // Remove entrant from WaitingList subcollection
            transaction.delete(entrantRef);

            // Decrement currentEntrantsNumber
            transaction.update(eventRef, "currentEntrantsNumber", FieldValue.increment(-1));

            // Remove deviceId from waitingList array
            transaction.update(eventRef, "waitingList", FieldValue.arrayRemove(deviceId));

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Successfully left the waiting list.", Toast.LENGTH_SHORT).show();
            event.setCurrentEntrantsNumber(event.getCurrentEntrantsNumber() - 1);
            updateButtonStates();
            populateEventDetails(event);
        }).addOnFailureListener(e -> {
            if (e instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
                String message = firestoreException.getMessage();
                if (firestoreException.getCode() == FirebaseFirestoreException.Code.ABORTED) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error leaving waiting list: " + message, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error leaving waiting list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
