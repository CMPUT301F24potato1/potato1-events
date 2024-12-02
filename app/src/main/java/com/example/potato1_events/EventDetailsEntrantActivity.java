// File: EventDetailsEntrantActivity.java
package com.example.potato1_events;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
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

    // Location Components
    private FusedLocationProviderClient fusedLocationClient;

    private TextView eventRegistrationDeadlineTextView;
    private TextView eventWaitlistCountTextView;
    private TextView eventAvailableSpotsTextView;

    // Permission Components
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

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
        eventRegistrationDeadlineTextView = findViewById(R.id.eventRegistrationDeadlineTextView);
        eventWaitlistCountTextView = findViewById(R.id.eventWaitlistCountTextView);
        eventAvailableSpotsTextView = findViewById(R.id.eventAvailableSpotsTextView);

        // Initialize Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize ActivityResultLauncher for location permissions
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    if (fineLocationGranted != null && fineLocationGranted) {
                        // Precise location access granted
                        Toast.makeText(this, "Location permission granted.", Toast.LENGTH_SHORT).show();
                        showJoinConfirmationDialog();
                    } else if (coarseLocationGranted != null && coarseLocationGranted) {
                        // Only approximate location access granted
                        Toast.makeText(this, "Approximate location permission granted.", Toast.LENGTH_SHORT).show();
                        showJoinConfirmationDialog();
                    } else {
                        // No location access granted
                        Toast.makeText(this, "Location permission denied. Cannot join the event.", Toast.LENGTH_SHORT).show();
                        // Optionally, disable the join button or provide further instructions
                        joinButton.setEnabled(false);
                    }
                }
        );

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
        setupFirestoreListener();
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

        if (event.getCapacity() != 0 ) {
            String capacity = "Total Capacity: " + event.getCapacity();
            eventCapacityTextView.setText(capacity);
        } else if (event.getWaitingListCapacity() != null) {
            String capacity = "Currently " + event.getCurrentEntrantsNumber() + " on the waiting list. No limit to spots on the waiting list.";
            eventCapacityTextView.setText(capacity);
        } else {
            eventCapacityTextView.setText("Capacity: Not Available");
        }

        String geo = "Geolocation Required: " + (event.isGeolocationRequired() ? "Yes" : "No");
        eventGeolocationTextView.setText(geo);

        // Display Registration Deadline
        if (event.getRegistrationEnd() != null) {
            SimpleDateFormat deadlineFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            String deadline = "Registration Deadline: " + deadlineFormat.format(event.getRegistrationEnd());
            eventRegistrationDeadlineTextView.setText(deadline);
        } else {
            eventRegistrationDeadlineTextView.setText("Registration Deadline: Not Available");
        }

        // Calculate Waitlist Count
        int waitlistCount = calculateWaitlistCount(event.getEntrants());
        String waitlistText = "Waitlist Count: " + waitlistCount;
        eventWaitlistCountTextView.setText(waitlistText);

        // Calculate Available Spots
        int availableSpots = (event.getCapacity()) - (event.getAcceptedCount());
        String availableSpotsText = "Available Spots Left: " + availableSpots;
        eventAvailableSpotsTextView.setText(availableSpotsText);

        // Determine and display dynamic event status
        String dynamicStatus = determineEventStatus(event);
        eventStatusTextView.setText("Status: " + dynamicStatus);

        // Update join and leave button states
        updateButtonStates();
    }

    /**
     * Calculates the number of entrants on the waitlist based on their statuses.
     *
     * @param entrantsMap Map of entrant IDs to their statuses.
     * @return The total number of entrants on the waitlist.
     */
    private int calculateWaitlistCount(Map<String, String> entrantsMap) {
        if (entrantsMap == null || entrantsMap.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String status : entrantsMap.values()) {
            if (status.equalsIgnoreCase("selected") ||
                    status.equalsIgnoreCase("not selected") ||
                    status.equalsIgnoreCase("accepted") ||
                    status.equalsIgnoreCase("declined") ||
                    status.equalsIgnoreCase("left") ||
                    status.equalsIgnoreCase("waitlist") ||
                    status.equalsIgnoreCase("cancelled")) {
                count++;
            }
        }
        return count;
    }

    /**
     * Determines the current status of the event based on various fields.
     *
     * @param event The Event object.
     * @return A string representing the current status of the event.
     */
    private String determineEventStatus(Event event) {
        if (event.getRandomDrawPerformed()) {
            return "Finalized";
        } else if (event.isWaitingListFilled()) {
            // Check if registration deadline has passed
            if (event.getRegistrationEnd() != null) {
                long currentTime = System.currentTimeMillis();
                long deadlineTime = event.getRegistrationEnd().getTime();
                if (currentTime < deadlineTime) {
                    return "Waiting on Participants";
                } else {
                    return "Possibility of Resample";
                }
            } else {
                return "Waiting on Participants"; // Default if deadline not set
            }
        } else {
            // Check if registration deadline has passed
            if (event.getRegistrationEnd() != null) {
                long currentTime = System.currentTimeMillis();
                long deadlineTime = event.getRegistrationEnd().getTime();
                if (currentTime < deadlineTime) {
                    return "Open";
                } else {
                    return "Possibility of Resample";
                }
            } else {
                return "Open"; // Default if deadline not set
            }
        }
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
            // Check if location permissions are granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Permissions are granted, proceed to join
                showGeolocationAlert();
            } else {
                // Permissions are not granted, request them with rationale
                new AlertDialog.Builder(this)
                        .setTitle("Geolocation Permission Needed")
                        .setMessage("This event requires access to your location to join. Please grant location permissions.")
                        .setPositiveButton("Grant", (dialog, which) -> {
                            // Request location permissions
                            locationPermissionLauncher.launch(new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                            });
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            dialog.dismiss();
                            Toast.makeText(this, "Cannot join the event without location permissions.", Toast.LENGTH_SHORT).show();
                        })
                        .create()
                        .show();
            }
        } else {
            // Geolocation not required, proceed to join
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
     * Also captures and stores the entrant's current geopoint in Firestore.
     */
    private void joinWaitingList() {
        // Fetch the entrant's current location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request fine location permission if needed
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
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

                            // Create GeoPoint
                            GeoPoint geoPoint = new GeoPoint(latitude, longitude);

                            // Proceed to add entrant with geopoint
                            entEventsRepository.joinWaitingList(eventId, deviceId, geoPoint, new EntEventsRepository.ActionCallback() {
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
                        } else {
                            Toast.makeText(EventDetailsEntrantActivity.this, "Unable to retrieve location. Please ensure location services are enabled.", Toast.LENGTH_SHORT).show();
                            //Log.e(TAG, "Failed to get location.", task.getException());
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
    /**
     * Sets up a real-time listener to Firestore to listen for changes in the event document.
     */
    private void setupFirestoreListener() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("Events").document(eventId).addSnapshotListener(this, (documentSnapshot, e) -> {
            if (e != null) {
                //Log.e(TAG, "Listen failed.", e);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                Event updatedEvent = documentSnapshot.toObject(Event.class);
                if (updatedEvent != null) {
                    event = updatedEvent; // Update the current event
                    populateEventDetails(event); // Refresh UI with updated event details
                }
            } else {
                //Log.d(TAG, "Current data: null");
            }
        });
    }
}
