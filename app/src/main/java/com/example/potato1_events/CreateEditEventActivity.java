// File: CreateEditEventActivity.java
package com.example.potato1_events;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import androidx.annotation.VisibleForTesting;
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
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.squareup.picasso.Picasso;

import java.util.*;

/**
 * Activity for creating or editing events within the application.
 * Handles user input, data validation, image uploads, and interaction with Firebase services.
 * Integrates the navigation drawer with admin functionalities.
 */
public class CreateEditEventActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // UI Components
    private EditText eventNameEditText, eventDescriptionEditText, eventLocationEditText, availableSpotsEditText, waitingListSpotsEditText;
    private Button startDateButton, endDateButton, uploadPosterButton, saveEventButton, deleteEventButton, generateQRCodeButton;
    private CheckBox geolocationCheckBox;
    private ImageView eventPosterImageView, qrCodeImageView; // QR Code ImageView
    private Button waitingListDeadlineButton;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    private Uri selectedPosterUri = null;
    private String posterImageUrl = null; // Store the download URL of the poster image
    private String eventId = null; // If editing an existing event

    // ActivityResultLauncher for selecting image
    private ActivityResultLauncher<String> selectImageLauncher;

    // ActivityResultLauncher for requesting permissions
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // Variables to store selected dates and times
    private Calendar startDateTime = Calendar.getInstance();
    private Calendar endDateTime = Calendar.getInstance();
    private Calendar registrationEndDateTime = Calendar.getInstance();

    // Organizer's deviceId acting as facilityId
    private String deviceId;

    // User Privileges
    private boolean isAdmin = false; // Retrieved from Intent
    private String qrCodeHash = null; // To track QR code hash

    /**
     * Sets the Firestore instance for testing purposes.
     *
     * @param firestore The mocked FirebaseFirestore instance.
     */
    @VisibleForTesting
    public void setFirestore(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Initializes the activity, sets up UI components, Firebase instances, and event listeners.
     *
     * @param savedInstanceState The previously saved state of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_edit_event);

        // Retrieve the isAdmin flag from Intent extras
        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);

        // Initialize Firebase instances
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Retrieve deviceId
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Initialize UI components
        eventNameEditText = findViewById(R.id.eventNameEditText);
        eventDescriptionEditText = findViewById(R.id.eventDescriptionEditText);
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
        qrCodeImageView = findViewById(R.id.qrCodeImageView); // Initialize QR Code ImageView
        generateQRCodeButton = findViewById(R.id.generateQRCodeButton); // Initialize Generate QR Code Button

        // Initialize Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Create/Edit Event");

        // Initialize DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Set up ActionBarDrawerToggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Adjust Navigation Drawer Menu Items Based on isAdmin
        if (isAdmin) {
            navigationView.getMenu().findItem(R.id.nav_manage_media).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_users).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_create_event).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_edit_facility).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_my_events).setVisible(true);
        }

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

        // Set up listeners for various UI components
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

        // Set up Generate QR Code button listener
        generateQRCodeButton.setOnClickListener(v -> {
            if (generateQRCodeButton.getText().toString().equalsIgnoreCase("Generate QR Code")) {
                // Generate QR Code
                generateQRCode();
            } else if (generateQRCodeButton.getText().toString().equalsIgnoreCase("Remove QR Code")) {
                // Remove QR Code
                removeQRCode();
            }
        });

        // Check if editing an existing event
        Intent intent = getIntent();
        if (intent.hasExtra("EVENT_ID")) {
            eventId = intent.getStringExtra("EVENT_ID");
            loadEventData(eventId);
            deleteEventButton.setVisibility(View.VISIBLE); // Show delete button in edit mode
            // The visibility and text of generateQRCodeButton will be handled in loadEventData()
        } else {
            deleteEventButton.setVisibility(View.GONE); // Hide delete button in create mode
            generateQRCodeButton.setVisibility(View.VISIBLE); // Show generate QR code button in create mode
            generateQRCodeButton.setText("Generate QR Code"); // Ensure correct initial text
        }

        // Verify that the organizer has a facility before allowing event creation
        if (!isEditingExistingEvent()) {
            checkFacilityExists();
        }

        // Handle back button presses to manage navigation drawer state
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
     * Checks if the organizer has an existing facility.
     * This method verifies the existence of a facility document with the deviceId as its ID.
     * If no such document exists, it prompts the user to create one.
     */
    private void checkFacilityExists() {
        firestore.collection("Facilities").document(deviceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Facility exists; no action needed
                    } else {
                        // Facility does not exist; prompt the user to create one
                        promptCreateFacility();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to verify facility: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Optionally, handle the failure (e.g., retry or exit)
                });
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
        intent.putExtra("IS_ADMIN", isAdmin); // Pass isAdmin flag
        startActivity(intent);
    }

    /**
     * Saves the event to Firebase Firestore, handling both creation and updating.
     * If creating a new event, it generates a QR code hash based on the event ID.
     */
    private void saveEvent() {
        String name = eventNameEditText.getText().toString().trim();
        String description = eventDescriptionEditText.getText().toString().trim();
        String location = eventLocationEditText.getText().toString().trim();
        String availableSpotsStr = availableSpotsEditText.getText().toString().trim();
        boolean isGeolocationEnabled = geolocationCheckBox.isChecked();

        // Input validation
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(description) ||
                TextUtils.isEmpty(location) || TextUtils.isEmpty(availableSpotsStr)) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate registration end date
        if (registrationEndDateTime == null) {
            Toast.makeText(this, "Please select a waiting list deadline.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!startDateTime.before(endDateTime)) {
            Toast.makeText(this, "Start date must be before end date.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!registrationEndDateTime.before(startDateTime)) {
            Toast.makeText(this, "Waiting List deadline must be before event starting date.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse available spots
        int availableSpots;
        try {
            availableSpots = Integer.parseInt(availableSpotsStr);
            if (availableSpots <= 0) {
                Toast.makeText(this, "Available spots must be greater than zero.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number for available spots.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse waiting list spots (optional)
        Integer waitingListSpots = null; // Null indicates unlimited
        String waitingListSpotsStr = waitingListSpotsEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(waitingListSpotsStr)) {
            try {
                waitingListSpots = Integer.parseInt(waitingListSpotsStr);
                if (waitingListSpots <= 0) {
                    Toast.makeText(this, "Waiting list spots must be greater than zero.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (waitingListSpots < availableSpots) {
                    Toast.makeText(this, "Waiting list spots must be greater than or equal to available spots.", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number for waiting list spots.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        saveEventButton.setEnabled(false);

        // Image for the event, if empty then we have a placeholder
        if (selectedPosterUri != null) {
            uploadPosterImage(name, description, location, availableSpots, waitingListSpots, isGeolocationEnabled);
        } else {
            // If editing and no new image is selected, keep the last image
            if (isEditingExistingEvent()) {
                // Preserve existing posterImageUrl and handle QR code hash based on user action
                saveEventToFirestore(name, description, location, availableSpots, waitingListSpots, isGeolocationEnabled, posterImageUrl);
            } else {
                // Proceed to save event without poster image
                saveEventToFirestore(name, description, location, availableSpots, waitingListSpots, isGeolocationEnabled, null);
            }
        }
    }

    /**
     * Uploads the selected poster image to Firebase Storage and retrieves its download URL.
     *
     * @param name                 Event name.
     * @param description          Event description.
     * @param location             Event location.
     * @param availableSpots       Number of available spots.
     * @param waitingListSpots     Number of waiting list spots (can be null for unlimited).
     * @param isGeolocationEnabled Whether geolocation is enabled.
     */
    private void uploadPosterImage(String name, String description, String location,
                                   int availableSpots, Integer waitingListSpots, boolean isGeolocationEnabled) {
        // Create a unique filename
        String fileName = "event_posters/" + UUID.randomUUID() + ".jpg";
        StorageReference storageRef = storage.getReference().child(fileName);

        // Upload the image
        storageRef.putFile(selectedPosterUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        posterImageUrl = uri.toString();
                        // Proceed to save the event with the poster URL
                        saveEventToFirestore(name, description, location, availableSpots, waitingListSpots, isGeolocationEnabled, posterImageUrl);
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
     * Generates a QR code based on the provided hash and displays it in the UI.
     *
     * @param qrHash The data to encode in the QR code (typically the event ID).
     */
    private void generateQRCodeAndDisplay(String qrHash) {
        // Define the data to be encoded in the QR code (using qrHash)
        String qrData = qrHash;

        // Generate QR code bitmap
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        int size = 512; // Size of the QR code image

        Bitmap qrBitmap;
        try {
            com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, size, size);
            qrBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);

            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    qrBitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
        } catch (WriterException e) {
            Toast.makeText(this, "Failed to generate QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            saveEventButton.setEnabled(true);
            return;
        }

        // Display the QR code in the UI
        qrCodeImageView.setImageBitmap(qrBitmap);
        qrCodeImageView.setVisibility(View.VISIBLE); // Make the QR code visible

        Toast.makeText(this, "QR code generated!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Generates a QR code and handles its state based on whether it's being generated or removed.
     */
    private void generateQRCode() {
        if (isEditingExistingEvent()) {
            if (qrCodeHash == null) {
                // Generate QR code hash based on eventId
                if (eventId == null) {
                    // eventId should already be set when editing
                    Toast.makeText(this, "Invalid event ID.", Toast.LENGTH_SHORT).show();
                    return;
                }
                qrCodeHash = eventId;
                generateQRCodeAndDisplay(qrCodeHash);
                generateQRCodeButton.setText("Remove QR Code");
            } else {
                Toast.makeText(this, "QR code already exists.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Creating a new event; QR code will be generated after saving
            Toast.makeText(this, "Please save the event first to generate a QR code.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Removes the QR code from the UI and clears the qrCodeHash.
     */
    private void removeQRCode() {
        if (isEditingExistingEvent() && qrCodeHash != null) {
            // Remove the QR code image
            qrCodeImageView.setImageBitmap(null);
            qrCodeImageView.setVisibility(View.GONE);

            // Clear the qrCodeHash
            qrCodeHash = null;

            // Update button text back to "Generate QR Code"
            generateQRCodeButton.setText("Generate QR Code");

            Toast.makeText(this, "QR code removed.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No QR code to remove.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Saves the event data to Firebase Firestore.
     *
     * @param name                 Event name.
     * @param description          Event description.
     * @param location             Event location.
     * @param availableSpots       Number of available spots.
     * @param waitingListSpots     Number of waiting list spots (can be null for unlimited).
     * @param isGeolocationEnabled Whether geolocation is enabled.
     * @param posterUrl            URL of the uploaded poster image.
     */
    private void saveEventToFirestore(String name, String description, String location,
                                      int availableSpots, Integer waitingListSpots,
                                      boolean isGeolocationEnabled, String posterUrl) {

        if (isEditingExistingEvent()) {
            // **Editing an Existing Event**

            // Create a map of fields to update
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("description", description);
            updates.put("eventLocation", location);
            updates.put("capacity", availableSpots);
            if (waitingListSpots != null) {
                updates.put("waitingListCapacity", waitingListSpots);
            } else {
                updates.put("waitingListCapacity", FieldValue.delete()); // Remove the field for unlimited
            }
            updates.put("geolocationRequired", isGeolocationEnabled);
            updates.put("posterImageUrl", posterUrl);
            updates.put("startDate", startDateTime.getTime());
            updates.put("endDate", endDateTime.getTime());
            updates.put("registrationEnd", registrationEndDateTime.getTime());

            // Handle QR code hash
            if (qrCodeHash != null) {
                updates.put("qrCodeHash", qrCodeHash);
            } else {
                updates.put("qrCodeHash", FieldValue.delete()); // Remove the QR code hash from Firestore
            }

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
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("name", name);
            eventData.put("description", description);
            eventData.put("facilityId", deviceId); // Associate with deviceId directly
            eventData.put("eventLocation", location);
            eventData.put("capacity", availableSpots);
            if (waitingListSpots != null) {
                eventData.put("waitingListCapacity", waitingListSpots);
            } else {
                // Omit the field to represent an unlimited waiting list
                // Firestore doesn't support null values in maps directly, so omitting the field is preferred
            }
            eventData.put("currentEntrantsNumber", 0); // Initialize to 0
            eventData.put("geolocationRequired", isGeolocationEnabled);
            eventData.put("posterImageUrl", posterUrl);
            eventData.put("status", "Open");
            eventData.put("createdAt", new Date());
            eventData.put("entrants", new HashMap<>()); // Initialize with empty entrants map
            eventData.put("startDate", startDateTime.getTime());
            eventData.put("endDate", endDateTime.getTime());
            eventData.put("registrationEnd", registrationEndDateTime.getTime());
            eventData.put("randomDrawPerformed", false); // Ensure this is set to false during creation
            eventData.put("entrantsLocation", new HashMap<>()); // Initialize with empty entrantsLocation map")
            if (qrCodeHash != null) {
                eventData.put("qrCodeHash", qrCodeHash);
            }

            eventData.put("waitingListFilled", false);
            // Create a new event document
            firestore.collection("Events").add(eventData)
                    .addOnSuccessListener(documentReference -> {
                        String eventIdCreated = documentReference.getId();

                        if (qrCodeHash != null) {
                            // QR code has been generated; already set in eventData
                            // Add eventId to the facility's eventIds list
                            addEventToFacility(eventIdCreated);
                            navigateBackToEventList();
                        } else {
                            // QR code not generated yet
                            // Add eventId to the facility's eventIds list
                            addEventToFacility(eventIdCreated);
                            navigateBackToEventList();
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
        intent.putExtra("EVENT_ID", eventId);
        intent.putExtra("IS_ADMIN", isAdmin); // Pass isAdmin flag
        startActivity(intent);
        finish();
    }

    /**
     * Navigates back to the Event List page after creating a new event.
     */
    private void navigateBackToEventList() {
        Intent intent = new Intent(CreateEditEventActivity.this, OrganizerHomeActivity.class);
        intent.putExtra("IS_ADMIN", isAdmin); // Pass isAdmin flag
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
                        // Extract the event data
                        String name = documentSnapshot.getString("name");
                        String description = documentSnapshot.getString("description");
                        String facilityId = documentSnapshot.getString("facilityId");
                        String location = documentSnapshot.getString("eventLocation");
                        Long capacityLong = documentSnapshot.getLong("capacity");
                        Long waitingListCapacityLong = documentSnapshot.getLong("waitingListCapacity");
                        Boolean geolocationRequired = documentSnapshot.getBoolean("geolocationRequired");
                        String posterUrl = documentSnapshot.getString("posterImageUrl");
                        Date startDate = documentSnapshot.getDate("startDate");
                        Date endDate = documentSnapshot.getDate("endDate");
                        Date registrationEnd = documentSnapshot.getDate("registrationEnd");
                        String qrCodeHashFetched = documentSnapshot.getString("qrCodeHash");

                        // Populate the UI fields
                        eventNameEditText.setText(name);
                        eventDescriptionEditText.setText(description);
                        eventLocationEditText.setText(location);
                        if (capacityLong != null) {
                            availableSpotsEditText.setText(String.valueOf(capacityLong.intValue()));
                        }
                        if (waitingListCapacityLong != null) {
                            waitingListSpotsEditText.setText(String.valueOf(waitingListCapacityLong.intValue()));
                        }
                        if (geolocationRequired != null) {
                            geolocationCheckBox.setChecked(geolocationRequired);
                        }

                        // Set start and end dates
                        if (startDate != null) {
                            startDateTime.setTime(startDate);
                            startDateButton.setText(formatDateTime(startDateTime));
                        }
                        if (endDate != null) {
                            endDateTime.setTime(endDate);
                            endDateButton.setText(formatDateTime(endDateTime));
                        }
                        if (registrationEnd != null) {
                            registrationEndDateTime.setTime(registrationEnd);
                            waitingListDeadlineButton.setText("Deadline: " + formatDateTime(registrationEndDateTime));
                        }

                        // Load poster image if available
                        if (!TextUtils.isEmpty(posterUrl)) {
                            posterImageUrl = posterUrl;
                            Picasso.get().load(posterUrl).into(eventPosterImageView);
                            uploadPosterButton.setText("Change Poster Image"); // Update button text
                        }

                        // Store existing QR code hash and update button accordingly
                        if (!TextUtils.isEmpty(qrCodeHashFetched)) {
                            qrCodeHash = qrCodeHashFetched;
                            generateQRCodeAndDisplay(qrCodeHashFetched);
                            generateQRCodeButton.setText("Remove QR Code");
                        } else {
                            qrCodeHash = null;
                            generateQRCodeButton.setText("Generate QR Code");
                        }

                        // Handle unlimited waiting list based on waitingListCapacity being null
                        if (waitingListCapacityLong == null) {
                            // Waiting list is unlimited
                            waitingListSpotsEditText.setEnabled(false);
                            waitingListSpotsEditText.setText(""); // Clear any existing text
                        } else {
                            // Waiting list has a capacity
                            waitingListSpotsEditText.setEnabled(true);
                        }

                        // Ensure the generateQRCodeButton is visible in edit mode
                        generateQRCodeButton.setVisibility(View.VISIBLE);

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
     * Handles navigation item selections from the navigation drawer.
     *
     * @param item The selected menu item.
     * @return True if the event was handled, false otherwise.
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation
        int id = item.getItemId();
        Intent intent = null;

        if (id == R.id.nav_notifications) {
            // Navigate to NotificationsActivity
            // Uncomment and implement if NotificationsActivity exists
            intent = new Intent(CreateEditEventActivity.this, NotificationsActivity.class);
        } else if (id == R.id.nav_edit_profile) {
            // Navigate to UserInfoActivity
            intent = new Intent(CreateEditEventActivity.this, UserInfoActivity.class);
            intent.putExtra("MODE", "EDIT");
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_manage_media) {
            // Navigate to ManageMediaActivity (visible only to admins)
            if (isAdmin) {
                intent = new Intent(CreateEditEventActivity.this, ManageMediaActivity.class);
                intent.putExtra("IS_ADMIN", isAdmin); // Pass isAdmin flag
            } else {
                // Access denied message (optional as menu item is hidden)
                Toast.makeText(this, "Access Denied: Admins Only", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.nav_manage_users) {
            // Navigate to ManageUsersActivity (visible only to admins)
            if (isAdmin) {
                intent = new Intent(CreateEditEventActivity.this, ManageUsersActivity.class);
                intent.putExtra("IS_ADMIN", isAdmin); // Pass isAdmin flag
            } else {
                // Access denied message (optional as menu item is hidden)
                Toast.makeText(this, "Access Denied: Admins Only", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.action_scan_qr) {
            // Handle QR code scanning
            intent = new Intent(CreateEditEventActivity.this, QRScanActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin); // Pass isAdmin flag
        } else if (id == R.id.nav_create_event) {
            // Navigate to CreateEditEventActivity and pass isAdmin flag
            Toast.makeText(this, "Already on this page.", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_edit_facility) {
            // Navigate to CreateEditFacilityActivity and pass isAdmin flag
            intent = new Intent(CreateEditEventActivity.this, CreateEditFacilityActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin); // Pass isAdmin flag
        } else if (id == R.id.nav_my_events) {
            // Navigate to OrganizerHomeActivity
            intent = new Intent(CreateEditEventActivity.this, OrganizerHomeActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin); // Pass isAdmin flag
        } else if (id == R.id.nav_view_joined_events) {
            // Navigate to EntrantHomeActivity
            intent = new Intent(CreateEditEventActivity.this, EntrantHomeActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin); // Pass isAdmin flag
        }

        if (intent != null) {
            startActivity(intent);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Handles the back button press to close the navigation drawer if it's open.
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
