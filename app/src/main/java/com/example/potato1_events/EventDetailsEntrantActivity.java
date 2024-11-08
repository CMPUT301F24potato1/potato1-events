// File: EventDetailsEntrantActivity.java
package com.example.potato1_events;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Activity to display the details of an event for entrants.
 * Allows entrants to join or leave the waiting list.
 */
public class EventDetailsEntrantActivity extends AppCompatActivity {

    // UI Components

    /**
     * ImageView to display the event poster.
     */
    private ImageView eventPosterImageView;

    /**
     * TextView to display the event name.
     */
    private TextView eventNameTextView;

    /**
     * TextView to display the event description.
     */
    private TextView eventDescriptionTextView;

    /**
     * TextView to display the event location.
     */
    private TextView eventLocationTextView;

    /**
     * TextView to display the event dates.
     */
    private TextView eventDatesTextView;

    /**
     * TextView to display the event capacity details.
     */
    private TextView eventCapacityTextView;

    /**
     * TextView to display whether geolocation is required for the event.
     */
    private TextView eventGeolocationTextView;

    /**
     * TextView to display the current status of the event.
     */
    private TextView eventStatusTextView;

    /**
     * Button to allow the entrant to join the waiting list.
     */
    private Button joinButton;

    /**
     * Button to allow the entrant to leave the waiting list.
     */
    private Button leaveButton;

    // Firebase Firestore

    /**
     * FirebaseFirestore instance for database interactions.
     */
    private FirebaseFirestore firestore;
    // Repository
    private EntEventsRepository entEventsRepository;

    // Event Data

    /**
     * The unique identifier of the event.
     */
    private String eventId;

    /**
     * Event object containing all details of the event.
     */
    private Event event;

    // Entrant Data

    /**
     * The unique device ID of the entrant, used as the identifier.
     */
    private String deviceId;

    /**
     * The type of user, set to "Entrant".
     */
    private String userType = "Entrant";

    /**
     * Called when the activity is first created.
     * Initializes UI components, Firebase instances, and retrieves event details.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details_entrant);

        // Initialize Repository
        entEventsRepository = new EntEventsRepository(FirebaseFirestore.getInstance());

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

        // Enable the Up button and set the title
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Event Details"); // Set your desired title here
        }
    }

    /**
     * Sets the EntEventsRepository instance (used for testing).
     *
     * @param repository The EntEventsRepository instance.
     */
    public void setEntEventsRepository(EntEventsRepository repository) {
        this.entEventsRepository = repository;
    }

    /**
     * Sets the device ID (used for testing).
     *
     * @param id The device ID.
     */
    public void setDeviceId(String id) {
        this.deviceId = id;
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
     * Loads event details using EntEventsRepository based on eventId.
     *
     * @param eventId The ID of the event to load.
     */
    public void loadEventDetails(String eventId) {
        entEventsRepository.getEventById(eventId, new EntEventsRepository.EventCallback() {
            @Override
            public void onEventLoaded(Event loadedEvent) {
                if (loadedEvent != null) {
                    event = loadedEvent;
                    populateEventDetails(event);
                } else {
                    Toast.makeText(EventDetailsEntrantActivity.this, "Event not found.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
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

        if (event.getStartDate() != null && event.getEndDate() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            String dates = "Event Dates: " + dateFormat.format(event.getStartDate()) + " - " + dateFormat.format(event.getEndDate());
            eventDatesTextView.setText(dates);
        } else {
            eventDatesTextView.setText("Event Dates: Not Available");
        }

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
        if (event.getEntrants() != null && event.getEntrants().containsKey(deviceId)) {
            // Entrant is in the entrants map
            joinButton.setVisibility(View.GONE);
            leaveButton.setVisibility(View.VISIBLE);
        } else {
            // Entrant is not in the entrants map
            joinButton.setVisibility(View.VISIBLE);
            leaveButton.setVisibility(View.GONE);
        }
    }

    /**
     * Handles the join button action.
     * Adds the entrant to the event's waiting list.
     */
    private void handleJoinAction() {
        if (event.isGeolocationRequired()) {
            // **If Geolocation is Enabled, Show Geolocation Alert First**
            showGeolocationAlert();
        } else {
            // **If Geolocation is Not Enabled, Proceed to Join Confirmation**
            showJoinConfirmationDialog();
        }
    }

    /**
     * Displays a Geolocation Alert Dialog prompting the user about geolocation requirements.
     * Upon confirmation, proceeds to show the join waiting list confirmation dialog.
     */
    private void showGeolocationAlert() {
        new AlertDialog.Builder(this)
                .setTitle("Geolocation Required")
                .setMessage("This event requires geolocation access. Please ensure your location services are enabled to join the waiting list.")
                .setPositiveButton("OK", (dialog, which) -> {
                    // After acknowledging, proceed to join confirmation
                    showJoinConfirmationDialog();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Displays the Join Waiting List confirmation dialog.
     */
    private void showJoinConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Join Waiting List")
                .setMessage("Are you sure you want to join the waiting list for this event?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    dialog.dismiss();
                    joinWaitingList();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Adds the entrant to the event's waiting list using EntEventsRepository.
     */
    private void joinWaitingList() {
        entEventsRepository.joinWaitingList(eventId, deviceId, new EntEventsRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(EventDetailsEntrantActivity.this, "Successfully joined the waiting list.", Toast.LENGTH_SHORT).show();
                // Update local event data
                event.setCurrentEntrantsNumber(event.getCurrentEntrantsNumber() + 1);
                event.getEntrants().put(deviceId, "waitlist");
                updateButtonStates();
                populateEventDetails(event);
            }

            @Override
            public void onFailure(Exception e) {
                if (e instanceof FirebaseFirestoreException) {
                    FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
                    String message = firestoreException.getMessage();
                    if (firestoreException.getCode() == FirebaseFirestoreException.Code.ABORTED) {
                        Toast.makeText(EventDetailsEntrantActivity.this, message, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(EventDetailsEntrantActivity.this, "Error joining waiting list: " + message, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(EventDetailsEntrantActivity.this, "Error joining waiting list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
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
     * Removes the entrant from the event's waiting list using EntEventsRepository.
     */
    private void leaveWaitingList() {
        entEventsRepository.leaveWaitingList(eventId, deviceId, new EntEventsRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(EventDetailsEntrantActivity.this, "Successfully left the waiting list.", Toast.LENGTH_SHORT).show();
                // Update local event data
                event.setCurrentEntrantsNumber(event.getCurrentEntrantsNumber() - 1);
                event.getEntrants().remove(deviceId);
                updateButtonStates();
                populateEventDetails(event);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(EventDetailsEntrantActivity.this, "Error leaving waiting list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
