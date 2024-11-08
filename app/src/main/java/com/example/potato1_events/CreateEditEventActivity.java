// File: CreateEditEventActivity.java
package com.example.potato1_events;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.*;

public class CreateEditEventActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // UI Components
    private EditText eventNameEditText, eventDescriptionEditText, recCentreEditText, eventLocationEditText, availableSpotsEditText, waitingListSpotsEditText;
    private Button startDateButton, endDateButton, uploadPosterButton, saveEventButton, deleteEventButton;
    private CheckBox geolocationCheckBox;
    private ImageView eventPosterImageView;
    private Button waitingListDeadlineButton;

    // Firebase Instances
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    // Navigation Components
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    // URIs and Paths
    private Uri selectedPosterUri = null;
    private String posterImagePath = null;

    // Event Identification
    private String eventId = null; // If editing an existing event
    private String existingQrCodeHash = null; // To store existing QR code hash when editing

    // Activity Result Launchers
    private ActivityResultLauncher<String> selectImageLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // Date and Time Variables
    private Calendar startDateTime = Calendar.getInstance();
    private Calendar endDateTime = Calendar.getInstance();
    private Calendar registrationEndDateTime = Calendar.getInstance();

    // Organizer's Device ID Acting as Facility ID
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_edit_event);

        // Initialize Firebase instances
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Retrieve deviceId
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Initialize UI components
        eventNameEditText = findViewById(R.id.eventNameEditText);
        eventDescriptionEditText = findViewById(R.id.eventDescriptionEditText);
        recCentreEditText = findViewById(R.id.recCentreEditText);
        eventLocationEditText = findViewById(R.id.eventLocationEditText);
        availableSpotsEditText = findViewById(R.id.availableSpotsEditText);
        waitingListSpotsEditText = findViewById(R.id.waitingListSpotsEditText);
        geolocationCheckBox = findViewById(R.id.geolocationCheckBox);
        startDateButton = findViewById(R.id.startDateButton);
        endDateButton = findViewById(R.id.endDateButton);
        eventPosterImageView = findViewById(R.id.eventPosterImageView);
        uploadPosterButton = findViewById(R.id.uploadPosterButton);
        saveEventButton = findViewById(R.id.saveEventButton);
        deleteEventButton = findViewById(R.id.deleteEventButton);
        waitingListDeadlineButton = findViewById(R.id.waitingListDeadlineButton);

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawer_organizer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Set up ActionBarDrawerToggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Initialize ActivityResultLauncher for image selection
        selectImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedPosterUri = uri;
                        // Display the selected image in ImageView
                        Picasso.get().load(uri).into(eventPosterImageView);
                        uploadPosterButton.setText("Change Poster Image"); // Update button text
                    }
                }
        );

        // Initialize ActivityResultLauncher for permission requests
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openImageSelector();
                    } else {
                        Toast.makeText(this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Set up listeners
        startDateButton.setOnClickListener(v -> showDateTimePicker(true));
        endDateButton.setOnClickListener(v -> showDateTimePicker(false));
        waitingListDeadlineButton.setOnClickListener(v -> showDateTimePickerForRegistrationEnd());
        uploadPosterButton.setOnClickListener(v -> {
            // Check and request permissions if necessary
            if (ContextCompat.checkSelfPermission(this, getReadPermission()) == PackageManager.PERMISSION_GRANTED) {
                openImageSelector();
            } else {
                // Request permission
                requestPermissionLauncher.launch(getReadPermission());
            }
        });
        saveEventButton.setOnClickListener(v -> saveEvent());
        deleteEventButton.setOnClickListener(v -> confirmDeleteEvent());

        // Check if editing an existing event
        Intent intent = getIntent();
        if (intent.hasExtra("EVENT_ID")) {
            eventId = intent.getStringExtra("EVENT_ID");
            loadEventData(eventId);
            deleteEventButton.setVisibility(View.VISIBLE); // Show delete button in edit mode
        } else {
            deleteEventButton.setVisibility(View.GONE); // Hide delete button in create mode
            recCentreEditText.setText(deviceId); // Automatically set facilityId to deviceId
            recCentreEditText.setEnabled(false); // Disable editing of facilityId
        }

        // Verify that the organizer has a facility before allowing event creation
        if (!isEditingExistingEvent() && !hasFacility()) {
            promptCreateFacility();
        }

        handleBackPressed();
    }

    /**
     * Determines the appropriate read permission based on the device's Android version.
     *
     * @return The required read permission string.
     */
    private String getReadPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        }
    }

    /**
     * Launches the image selector to allow the user to choose an event poster from their device.
     */
    private void openImageSelector() {
        // Launch the image picker
        selectImageLauncher.launch("image/*");
    }

    /**
     * Displays a DatePicker and TimePicker dialog to select the waiting list deadline.
     */
    private void showDateTimePickerForRegistrationEnd() {
        final Calendar currentDate = Calendar.getInstance();
        final Calendar date = Calendar.getInstance();
        new DatePickerDialog(CreateEditEventActivity.this, (view, year, monthOfYear, dayOfMonth) -> {
            date.set(year, monthOfYear, dayOfMonth);
            new TimePickerDialog(CreateEditEventActivity.this, (view1, hourOfDay, minute) -> {
                date.set(Calendar.HOUR_OF_DAY, hourOfDay);
                date.set(Calendar.MINUTE, minute);

                registrationEndDateTime = (Calendar) date.clone();
                waitingListDeadlineButton.setText("Deadline: " + formatDateTime(registrationEndDateTime));

            }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), false).show();
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();
    }

    /**
     * Displays a DatePicker and TimePicker dialog to select the start or end date & time.
     *
     * @param isStartDate True if selecting start date & time, false for end date & time.
     */
    private void showDateTimePicker(boolean isStartDate) {
        final Calendar currentDate = Calendar.getInstance();
        final Calendar date = Calendar.getInstance();
        new DatePickerDialog(CreateEditEventActivity.this, (view, year, monthOfYear, dayOfMonth) -> {
            date.set(year, monthOfYear, dayOfMonth);
            new TimePickerDialog(CreateEditEventActivity.this, (view1, hourOfDay, minute) -> {
                date.set(Calendar.HOUR_OF_DAY, hourOfDay);
                date.set(Calendar.MINUTE, minute);
                if (isStartDate) {
                    startDateTime = (Calendar) date.clone();
                    startDateButton.setText(formatDateTime(startDateTime));
                } else {
                    endDateTime = (Calendar) date.clone();
                    endDateButton.setText(formatDateTime(endDateTime));
                }
            }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), false).show();
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();
    }

    /**
     * Formats the Calendar date and time into a readable string.
     *
     * @param calendar The Calendar instance.
     * @return Formatted date and time string.
     */
    private String formatDateTime(Calendar calendar) {
        return String.format(Locale.getDefault(), "%02d/%02d/%04d %02d:%02d",
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE));
    }

    /**
     * Checks if the activity is in edit mode.
     *
     * @return True if editing an existing event, false otherwise.
     */
    private boolean isEditingExistingEvent() {
        return eventId != null && !eventId.isEmpty();
    }

    /**
     * Checks if the organizer has an existing facility (Placeholder for now).
     *
     * @return True if a facility exists, false otherwise.
     */
    private boolean hasFacility() {
        // TODO: Implement actual logic to check if the organizer has a facility
        // For now, returning true as a placeholder
        return true;
    }

    /**
     * Prompts the organizer to create a facility if none exists.
     */
    private void promptCreateFacility() {
        new AlertDialog.Builder(this)
                .setTitle("No Facility Found")
                .setMessage("You need to create a facility before creating events.")
                .setPositiveButton("Create Facility", (dialog, which) -> navigateToCreateFacility())
                .setNegativeButton("Cancel", (dialog, which) -> finish()) // Close activity if canceled
                .setCancelable(false)
                .show();
    }

    /**
     * Navigates the organizer to the Create/Edit Facility Activity to create a new facility.
     */
    private void navigateToCreateFacility() {
        Intent intent = new Intent(CreateEditEventActivity.this, CreateEditFacilityActivity.class);
        startActivity(intent);
    }

    /**
     * Saves the event to Firebase Firestore, handling both creation and updating.
     */
    private void saveEvent() {
        String name = eventNameEditText.getText().toString().trim();
        String description = eventDescriptionEditText.getText().toString().trim();
        String recCentre = recCentreEditText.getText().toString().trim(); // This should be deviceId
        String location = eventLocationEditText.getText().toString().trim();
        String availableSpotsStr = availableSpotsEditText.getText().toString().trim();
        String waitingListSpotsStr = waitingListSpotsEditText.getText().toString().trim();
        boolean isGeolocationEnabled = geolocationCheckBox.isChecked();

        // Input validation
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(description) || TextUtils.isEmpty(recCentre) ||
                TextUtils.isEmpty(location) || TextUtils.isEmpty(availableSpotsStr) ||
                TextUtils.isEmpty(waitingListSpotsStr)) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate registration end date
        if (registrationEndDateTime == null) {
            Toast.makeText(this, "Please select a waiting list deadline.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure registration end date is before event start date
        if (registrationEndDateTime.after(startDateTime.getTime())) {
            Toast.makeText(this, "Waiting list deadline must be before event start date.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(waitingListSpotsStr)) {
            waitingListSpotsStr = "10000";
        }

        int availableSpots;
        int waitingListSpots;

        try {
            availableSpots = Integer.parseInt(availableSpotsStr);
            waitingListSpots = Integer.parseInt(waitingListSpotsStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers for spots.", Toast.LENGTH_SHORT).show();
            return;
        }

        saveEventButton.setEnabled(false);

        // Image for the event, if empty then we have a placeholder
        if (selectedPosterUri != null) {
            uploadPosterImage(name, description, recCentre, location, availableSpots, waitingListSpots, isGeolocationEnabled);
        } else {
            // If editing and no new image is selected, keep the last image
            if (isEditingExistingEvent()) {
                // **Preserve existing posterImagePath and QR code hash**
                saveEventToFirestore(name, description, recCentre, location, availableSpots, waitingListSpots, isGeolocationEnabled, posterImagePath, existingQrCodeHash);
            } else {
                // Generate a QR code even without a poster
                generateQRCodeAndSaveEvent(name, description, recCentre, location, availableSpots, waitingListSpots, isGeolocationEnabled, null);
            }
        }
    }

    /**
     * Uploads the selected poster image to Firebase Storage and retrieves its storage path.
     *
     * @param name                 Event name.
     * @param description          Event description.
     * @param recCentre            Associated rec centre (facilityId).
     * @param location             Event location.
     * @param availableSpots       Number of available spots.
     * @param waitingListSpots     Number of waiting list spots.
     * @param isGeolocationEnabled Whether geolocation is enabled.
     */
    private void uploadPosterImage(String name, String description, String recCentre, String location,
                                   int availableSpots, int waitingListSpots, boolean isGeolocationEnabled) {
        // Create a unique filename
        String fileName = "event_posters/" + UUID.randomUUID() + ".jpg";
        StorageReference storageRef = storage.getReference().child(fileName);

        // Upload the image
        storageRef.putFile(selectedPosterUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        posterImagePath = uri.toString();
                        // Proceed to save the event with the poster URL
                        generateQRCodeAndSaveEvent(name, description, recCentre, location, availableSpots, waitingListSpots, isGeolocationEnabled, posterImagePath);
                    }).addOnFailureListener(e -> {
                        Toast.makeText(CreateEditEventActivity.this, "Failed to get poster URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        saveEventButton.setEnabled(true);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreateEditEventActivity.this, "Poster upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    saveEventButton.setEnabled(true);
                });
    }

    /**
     * Generates a QR code for the event and proceeds to save the event data to Firestore.
     *
     * @param name                 Event name.
     * @param description          Event description.
     * @param recCentre            Associated rec centre (facilityId).
     * @param location             Event location.
     * @param availableSpots       Number of available spots.
     * @param waitingListSpots     Number of waiting list spots.
     * @param isGeolocationEnabled Whether geolocation is enabled.
     * @param posterUrl            URL of the uploaded poster image.
     */
    private void generateQRCodeAndSaveEvent(String name, String description, String recCentre, String location,
                                            int availableSpots, int waitingListSpots,
                                            boolean isGeolocationEnabled, String posterUrl) {

        if (isEditingExistingEvent()) {
            // **Editing Existing Event: Use Existing QR Code Hash**
            saveEventToFirestore(name, description, recCentre, location, availableSpots, waitingListSpots, isGeolocationEnabled, posterUrl, existingQrCodeHash);
        } else {
            // **Creating New Event: Generate QR Code After Event Creation**
            // Since QR code generation requires the event ID, proceed to create the event first.
            saveEventToFirestore(name, description, recCentre, location, availableSpots, waitingListSpots, isGeolocationEnabled, posterUrl, null);
        }
    }

    /**
     * Saves the event data to Firebase Firestore.
     *
     * @param name                 Event name.
     * @param description          Event description.
     * @param recCentre            Associated rec centre (facilityId).
     * @param location             Event location.
     * @param availableSpots       Number of available spots.
     * @param waitingListSpots     Number of waiting list spots.
     * @param isGeolocationEnabled Whether geolocation is enabled.
     * @param posterUrl            URL of the uploaded poster image.
     * @param qrCodeHash           QR code hash data. If null, it will be generated after event creation.
     */
    private void saveEventToFirestore(String name, String description, String recCentre, String location,
                                      int availableSpots, int waitingListSpots,
                                      boolean isGeolocationEnabled, String posterUrl, String qrCodeHash) {

        if (isEditingExistingEvent()) {
            // **Editing an Existing Event**

            // Create a map of fields to update
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("description", description);
            updates.put("eventLocation", location);
            updates.put("capacity", availableSpots);
            updates.put("waitingListCapacity", waitingListSpots);
            updates.put("geolocationRequired", isGeolocationEnabled);
            updates.put("posterImageUrl", posterUrl);
            updates.put("startDate", startDateTime.getTime());
            updates.put("endDate", endDateTime.getTime());
            updates.put("registrationEnd", registrationEndDateTime.getTime());

            // Update only the specified fields to preserve entrants and other data
            firestore.collection("Events").document(eventId).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(CreateEditEventActivity.this, "Event updated successfully!", Toast.LENGTH_SHORT).show();
                        navigateBackToEventDetails();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(CreateEditEventActivity.this, "Failed to update event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        saveEventButton.setEnabled(true);
                    });

        } else {
            // **Creating a New Event**

            // Create an Event object with all necessary fields
            Event event = new Event();
            event.setName(name);
            event.setDescription(description);
            event.setFacilityId(deviceId); // Associate with the organizer's facility
            event.setEventLocation(location);
            event.setCapacity(availableSpots);
            event.setWaitingListCapacity(waitingListSpots);
            event.setCurrentEntrantsNumber(0); // Initialize to 0
            event.setGeolocationRequired(isGeolocationEnabled);
            event.setPosterImageUrl(posterUrl);
            event.setStatus("Open");
            event.setCreatedAt(new Date());
            event.setEntrants(new HashMap<>()); // Initialize with empty entrants map
            event.setStartDate(startDateTime.getTime());
            event.setEndDate(endDateTime.getTime());
            event.setRegistrationEnd(registrationEndDateTime.getTime());
            event.setRandomDrawPerformed(false); // Ensure this is set to false during creation

            // Create a new event document
            firestore.collection("Events").add(event)
                    .addOnSuccessListener(documentReference -> {
                        String eventIdCreated = documentReference.getId();

                        if (qrCodeHash == null) {
                            // Generate the QR code URL using the event ID
                            String qrUrl = "https://yourapp.com/joinEvent?eventId=" + eventIdCreated;

                            // Update the event with the QR code URL
                            firestore.collection("Events").document(eventIdCreated).update("qrCodeHash", qrUrl)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(CreateEditEventActivity.this, "Event created with QR code!", Toast.LENGTH_SHORT).show();
                                        // Add eventId to the facility's eventIds list
                                        addEventToFacility(eventIdCreated);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(CreateEditEventActivity.this, "Failed to update QR code data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        saveEventButton.setEnabled(true);
                                    });
                        } else {
                            // If QR code hash is provided (unlikely in create), proceed normally
                            addEventToFacility(eventIdCreated);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(CreateEditEventActivity.this, "Failed to create event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        saveEventButton.setEnabled(true);
                    });
        }
    }

    /**
     * Adds the newly created event ID to the organizer's facility's eventIds list.
     *
     * @param eventId The ID of the newly created event.
     */
    private void addEventToFacility(String eventId) {
        DocumentReference facilityRef = firestore.collection("Facilities").document(deviceId);

        // Use FieldValue.arrayUnion to add the eventId to the eventIds list
        facilityRef.update("eventIds", FieldValue.arrayUnion(eventId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CreateEditEventActivity.this, "Event added to your facility.", Toast.LENGTH_SHORT).show();
                    navigateBackToEventList();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreateEditEventActivity.this, "Failed to associate event with facility: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    saveEventButton.setEnabled(true);
                });
    }

    /**
     * Navigates back to the Event Details page after saving/updating.
     */
    private void navigateBackToEventDetails() {
        Intent intent = new Intent(CreateEditEventActivity.this, EventDetailsOrganizerActivity.class);
        intent.putExtra("EVENT_ID", eventId); // Pass the event ID if needed
        startActivity(intent);
        finish();
    }

    /**
     * Navigates back to the Event List page after creating a new event.
     */
    private void navigateBackToEventList() {
        Intent intent = new Intent(CreateEditEventActivity.this, OrganizerHomeActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Loads existing event data into the UI for editing.
     *
     * @param eventId The ID of the event to load.
     */
    private void loadEventData(String eventId) {
        firestore.collection("Events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        if (event != null) {
                            eventNameEditText.setText(event.getName());
                            eventDescriptionEditText.setText(event.getDescription());
                            recCentreEditText.setText(event.getFacilityId());
                            eventLocationEditText.setText(event.getEventLocation());
                            availableSpotsEditText.setText(String.valueOf(event.getCapacity()));
                            waitingListSpotsEditText.setText(String.valueOf(event.getWaitingListCapacity()));
                            geolocationCheckBox.setChecked(event.isGeolocationRequired());

                            // Set start and end dates
                            if (event.getStartDate() != null) {
                                startDateTime.setTime(event.getStartDate());
                                startDateButton.setText(formatDateTime(startDateTime));
                            }
                            if (event.getEndDate() != null) {
                                endDateTime.setTime(event.getEndDate());
                                endDateButton.setText(formatDateTime(endDateTime));
                            }
                            if (event.getRegistrationEnd() != null) {
                                registrationEndDateTime.setTime(event.getRegistrationEnd());
                                waitingListDeadlineButton.setText("Deadline: " + formatDateTime(registrationEndDateTime));
                            }

                            // Load poster image if available
                            if (!TextUtils.isEmpty(event.getPosterImageUrl())) {
                                posterImagePath = event.getPosterImageUrl();
                                Picasso.get().load(event.getPosterImageUrl()).into(eventPosterImageView);
                                uploadPosterButton.setText("Change Poster Image"); // Update button text
                            }

                            // Store existing QR code hash
                            existingQrCodeHash = event.getQrCodeHash();
                        }
                    } else {
                        Toast.makeText(this, "Event not found.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load event data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * Confirms with the user before deleting the event.
     */
    private void confirmDeleteEvent() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Yes", (dialog, which) -> deleteEvent())
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Deletes the event from Firebase Firestore and removes its association from the facility.
     */
    private void deleteEvent() {
        if (eventId != null) {
            firestore.collection("Events").document(eventId).delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Event deleted successfully.", Toast.LENGTH_SHORT).show();
                        // Remove eventId from the facility's eventIds list
                        removeEventFromFacility(eventId);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to delete event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    /**
     * Removes the event ID from the organizer's facility's eventIds list after deletion.
     *
     * @param eventId The ID of the event to remove.
     */
    private void removeEventFromFacility(String eventId) {
        DocumentReference facilityRef = firestore.collection("Facilities").document(deviceId);

        // Use FieldValue.arrayRemove to remove the eventId from the eventIds list
        facilityRef.update("eventIds", FieldValue.arrayRemove(eventId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CreateEditEventActivity.this, "Event removed from your facility.", Toast.LENGTH_SHORT).show();
                    navigateBackToEventList();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreateEditEventActivity.this, "Failed to dissociate event from facility: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Handles navigation item selections.
     *
     * @param item The selected menu item.
     * @return True if the event was handled, false otherwise.
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation
        int id = item.getItemId();

        if (id == R.id.nav_organizer_profile) {
            Intent intent = new Intent(CreateEditEventActivity.this, UserInfoActivity.class);
            intent.putExtra("USER_TYPE", "Organizer"); // or "Organizer"
            intent.putExtra("MODE", "EDIT");
            startActivity(intent);
        } else if (id == R.id.nav_create_event) {
            Toast.makeText(this, "Already on this page.", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_edit_facility) {
            Intent intent = new Intent(CreateEditEventActivity.this, CreateEditFacilityActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_my_events) {
            Intent intent = new Intent(CreateEditEventActivity.this, OrganizerHomeActivity.class);
            startActivity(intent);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * If back button is pressed and side bar is opened, then return to the page.
     * If done on the page itself, then default back to the normal back press action
     */
    private void handleBackPressed() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled */) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }
}
