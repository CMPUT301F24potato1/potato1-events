// File: EventDetailsOrganizerActivity.java
package com.example.potato1_events;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.ActionBar;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.activity.OnBackPressedCallback;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.squareup.picasso.Picasso;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Activity to display the details of an event for organizers.
 * Allows organizers to view event details, share QR codes, edit or delete events, and manage entrants.
 */
public class EventDetailsOrganizerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // UI Components

    /**
     * DrawerLayout for the navigation drawer.
     */
    private DrawerLayout drawerLayout;

    private boolean isAdmin = false;

    /**
     * NavigationView for handling navigation menu items.
     */
    private NavigationView navigationView;

    /**
     * Toolbar for the activity.
     */
    private Toolbar toolbar;

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
     * TextView to display the event Available Spots details.
     */
    private TextView eventAvailableSpotsTextView;

    private TextView eventRegistrationDeadlineTextView;


    /**
     * TextView to display whether geolocation is required for the event.
     */
    private TextView eventGeolocationTextView;

    /**
     * TextView to display the current status of the event.
     */
    private TextView eventStatusTextView;

    /**
     * TextView to display the current waitlist count.
     */
    private TextView eventWaitlistCountTextView; // New TextView for Waitlist Count

    /**
     * ImageView to display the generated QR code for the event.
     */
    private ImageView qrCodeImageView; // ImageView for QR Code

    /**
     * Button to share the QR code image via other applications.
     */
    private Button shareQRCodeButton; // Button to share QR Code

    /**
     * Button to navigate to the edit event activity.
     */
    private Button editButton;

    /**
     * Button to delete the event.
     */
    private Button deleteButton;

    /**
     * Button to view the list of entrants on the waiting list.
     */
    private Button entrantsListButton;

    // Repository
    private OrgEventsRepository orgEventsRepository;
    // Firebase Firestore

    /**
     * FirebaseFirestore instance for database interactions.
     */
    private FirebaseFirestore firestore;

    // Event Data

    /**
     * The unique identifier of the event.
     */
    private String eventId;

    /**
     * Event object containing all details of the event.
     */
    private Event event;

    private ListenerRegistration eventListener;

    /**
     * Sets the OrgEventsRepository instance for testing purposes.
     *
     * @param repository The OrgEventsRepository instance.
     */
    @VisibleForTesting
    public void setOrgEventsRepository(OrgEventsRepository repository) {
        this.orgEventsRepository = repository;
    }

    /**
     * Called when the activity is first created.
     * Initializes UI components, Firebase instances, and retrieves event details.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details_organizer);

        // Initialize Repository
        orgEventsRepository = OrgEventsRepository.getInstance();

        // Initialize UI Components
        drawerLayout = findViewById(R.id.drawer_event_details_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // **Enable the Up Button**
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true); // Shows the Up button
            actionBar.setTitle("Event Details"); // Optional: Set a custom title
        }

        // Initialize Event Detail Views
        eventPosterImageView = findViewById(R.id.eventPosterImageView);
        eventNameTextView = findViewById(R.id.eventNameTextView);
        eventDescriptionTextView = findViewById(R.id.eventDescriptionTextView);
        eventLocationTextView = findViewById(R.id.eventLocationTextView);
        eventDatesTextView = findViewById(R.id.eventDatesTextView);
        eventRegistrationDeadlineTextView = findViewById(R.id.eventRegistrationDeadlineTextView);
        eventWaitlistCountTextView = findViewById(R.id.eventWaitlistCountTextView);
        eventAvailableSpotsTextView = findViewById(R.id.eventAvailableSpotsTextView);
        eventCapacityTextView = findViewById(R.id.eventCapacityTextView);
        eventGeolocationTextView = findViewById(R.id.eventGeolocationTextView);
        eventStatusTextView = findViewById(R.id.eventStatusTextView);
        qrCodeImageView = findViewById(R.id.qrCodeImageView);
        shareQRCodeButton = findViewById(R.id.shareQRCodeButton);
        editButton = findViewById(R.id.editButton);
        deleteButton = findViewById(R.id.deleteButton);
        entrantsListButton = findViewById(R.id.entrantsListButton);

        // Retrieve EVENT_ID from Intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("EVENT_ID")) {
            eventId = intent.getStringExtra("EVENT_ID");
            loadEventDetails(eventId);
        } else {
            Toast.makeText(this, "Event ID not provided.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Adjust Navigation Drawer Menu Items Based on isAdmin
        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);
        if (isAdmin) {
            navigationView.getMenu().findItem(R.id.nav_manage_media).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_users).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_create_event).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_edit_facility).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_my_events).setVisible(true);
        }

        handleBackPressed();

        // Set Click Listeners for Action Buttons
        editButton.setOnClickListener(v -> handleEditAction());
        deleteButton.setOnClickListener(v -> handleDeleteAction());
        entrantsListButton.setOnClickListener(v -> navigateToWaitingList());
        shareQRCodeButton.setOnClickListener(v -> shareQRCodeImage());
        setupFirestoreListener();
    }


    /**
     * Loads event details using OrgEventsRepository based on eventId.
     *
     * @param eventId The ID of the event to load.
     */
    public void loadEventDetails(String eventId) {
        if (orgEventsRepository == null) {
            Toast.makeText(this, "Repository not initialized.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        orgEventsRepository.getEventById(eventId, new OrgEventsRepository.EventCallback() {
            @Override
            public void onEventLoaded(Event loadedEvent) {
                if (loadedEvent != null) {
                    event = loadedEvent;
                    populateEventDetails(event);
                } else {
                    Toast.makeText(EventDetailsOrganizerActivity.this, "Event not found.", Toast.LENGTH_SHORT).show();
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

        // Set text views
        eventNameTextView.setText(event.getName());
        eventDescriptionTextView.setText(event.getDescription());
        eventLocationTextView.setText("Location: " + event.getEventLocation());

        // Format dates if startDate and endDate are available
        if (event.getStartDate() != null && event.getEndDate() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            String dates = "Event Dates: " + dateFormat.format(event.getStartDate()) + " - " + dateFormat.format(event.getEndDate());
            eventDatesTextView.setText(dates);
        } else {
            eventDatesTextView.setText("Event Dates: Not Available");
        }

        String capacity = "Total Capacity: " + event.getCapacity();
        eventCapacityTextView.setText(capacity);

        // Calculate and set Available Spots
        int availableSpots = (event.getCapacity()) - (event.getAcceptedCount());
        String availableSpotsText = "Available Spots Left: " + availableSpots;
        eventAvailableSpotsTextView.setText(availableSpotsText);

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

        // Determine and display dynamic event status
        String dynamicStatus = determineEventStatus(event);
        eventStatusTextView.setText("Status: " + dynamicStatus);

        // Fetch and display waitlist count (if not already set)
        // Optional: If you prefer fetching separately
        // fetchWaitlistCount(eventId);

        // Generate and display QR code
        if (!TextUtils.isEmpty(event.getQrCodeHash())) {
            generateQRCodeAndDisplay(event.getQrCodeHash());
        }
    }

    /**
     * Formats a Date object to a readable string.
     *
     * @param date The Date object to format.
     * @return Formatted date string.
     */
    private String formatDate(java.util.Date date) {
        if (date == null) return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    /**
     * Fetches the number of entrants with the "waitlist" status for the event.
     *
     * @param eventId The ID of the event.
     */
    private void fetchWaitlistCount(String eventId) {
        if (orgEventsRepository == null) {
            Toast.makeText(this, "Repository not initialized.", Toast.LENGTH_SHORT).show();
            eventWaitlistCountTextView.setText("Waiting List: N/A");
            return;
        }

        orgEventsRepository.getEventById(eventId, new OrgEventsRepository.EventCallback() {
            @Override
            public void onEventLoaded(Event loadedEvent) {
                if (loadedEvent != null && loadedEvent.getEntrants() != null) {
                    Map<String, String> entrantsMap = loadedEvent.getEntrants();
                    // Count the number of entrants with the "waitlist" status
                    long waitlistCount = entrantsMap.values().stream()
                            .filter(status -> "waitlist".equalsIgnoreCase(status) || "selected".equalsIgnoreCase(status))
                            .count();

                    // Check if waitingListCapacity is null
                    if (loadedEvent.getWaitingListCapacity() != null) {
                        String waitlistText = "Waiting List: " + waitlistCount + "/" + loadedEvent.getWaitingListCapacity();
                        eventWaitlistCountTextView.setText(waitlistText);
                    } else {
                        String waitlistText = "Waiting List: " + waitlistCount;
                        eventWaitlistCountTextView.setText(waitlistText);
                    }
                } else {
                    eventWaitlistCountTextView.setText("Waiting List: 0/" + (event != null ? event.getWaitingListCapacity() : "N/A"));
                }
            }
        });
    }

    /**
     * Generates a QR code based on the provided hash and displays it in the UI.
     *
     * @param qrHash The data to encode in the QR code (typically the event ID).
     */
    private void generateQRCodeAndDisplay(String qrHash) {
        // Define the data to be encoded in the QR code
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
            return;
        }

        // Display the QR code in the ImageView
        qrCodeImageView.setImageBitmap(qrBitmap);
        qrCodeImageView.setVisibility(View.VISIBLE); // Make the QR code visible
        shareQRCodeButton.setVisibility(View.VISIBLE);
    }

    /**
     * Shares the QR code image via a share popup.
     */
    private void shareQRCodeImage() {
        // Get the bitmap from the ImageView
        qrCodeImageView.buildDrawingCache();
        Bitmap bitmap = qrCodeImageView.getDrawingCache();

        if (bitmap == null) {
            Toast.makeText(this, "QR Code not available to share.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Save the bitmap to cache directory
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs(); // Create directory if not exists
            File file = new File(cachePath, "qr_code.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            // Get the URI using FileProvider
            Uri contentUri = FileProvider.getUriForFile(this, "com.example.potato1_events.fileprovider", file);

            if (contentUri != null) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Temporary permission for receiving app to read this file
                shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                startActivity(Intent.createChooser(shareIntent, "Share QR Code via"));
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error sharing QR Code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
                    status.equalsIgnoreCase("canceled")) {
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
     * Navigates to the EventWaitingListActivity to display the waiting list entrants.
     */
    private void navigateToWaitingList() {
        Intent intent = new Intent(EventDetailsOrganizerActivity.this, EventWaitingListActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        intent.putExtra("IS_ADMIN", isAdmin);
        startActivity(intent);
    }

    /**
     * Handles the Edit button action.
     * Navigates to the CreateEditEventActivity for editing the event.
     */
    private void handleEditAction() {
        Intent intent = new Intent(EventDetailsOrganizerActivity.this, CreateEditEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        intent.putExtra("IS_ADMIN", isAdmin);
        startActivity(intent);
    }

    /**
     * Handles the Delete button action.
     * Prompts for confirmation before deleting the event.
     */
    private void handleDeleteAction() {
        // Confirm deletion
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Yes", (dialog, which) -> deleteEvent())
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Deletes the event using OrgEventsRepository.
     */
    private void deleteEvent() {
        if (orgEventsRepository == null) {
            Toast.makeText(this, "Repository not initialized.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Assume the Facility ID is part of the Event object
        String facilityId = event.getFacilityId();
        if (TextUtils.isEmpty(facilityId)) {
            Toast.makeText(this, "Facility ID not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        orgEventsRepository.deleteEvent(eventId, facilityId, new OrgEventsRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(EventDetailsOrganizerActivity.this, "Event deleted successfully.", Toast.LENGTH_SHORT).show();
                // Navigate back to OrganizerHomeActivity
                Intent intent = new Intent(EventDetailsOrganizerActivity.this, OrganizerHomeActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                if (e instanceof FirebaseFirestoreException) {
                    FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
                    String message = firestoreException.getMessage();
                    if (firestoreException.getCode() == FirebaseFirestoreException.Code.ABORTED) {
                        Toast.makeText(EventDetailsOrganizerActivity.this, message, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(EventDetailsOrganizerActivity.this, "Error deleting event: " + message, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(EventDetailsOrganizerActivity.this, "Error deleting event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
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
            intent = new Intent(EventDetailsOrganizerActivity.this, NotificationsActivity.class);
        } else if (id == R.id.nav_edit_profile) {
            // Navigate to UserInfoActivity
            intent = new Intent(EventDetailsOrganizerActivity.this, UserInfoActivity.class);
            intent.putExtra("MODE", "EDIT");
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_manage_media) {

            intent = new Intent(EventDetailsOrganizerActivity.this, ManageMediaActivity.class);

        } else if (id == R.id.nav_manage_users) {

            intent = new Intent(EventDetailsOrganizerActivity.this, ManageUsersActivity.class);

        } else if (id == R.id.action_scan_qr) {
            // Handle QR code scanning
            intent = new Intent(EventDetailsOrganizerActivity.this, QRScanActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_create_event) {
            // Navigate to CreateEditEventActivity
            intent = new Intent(EventDetailsOrganizerActivity.this, CreateEditEventActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_edit_facility) {
            intent = new Intent(EventDetailsOrganizerActivity.this, CreateEditFacilityActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_my_events) {
            // Navigate to OrganizerHomeActivity and pass isAdmin flag
            intent = new Intent(EventDetailsOrganizerActivity.this, OrganizerHomeActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_view_joined_events) {
            // Navigate to EntrantHomeActivity and pass isAdmin flag
            intent = new Intent(EventDetailsOrganizerActivity.this, EntrantHomeActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        }

        if (intent != null) {
            startActivity(intent);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Handles back button presses, ensuring that the navigation drawer is closed if open.
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
     * Sets up a real-time listener to Firestore to listen for changes in the event document.
     */
    private void setupFirestoreListener() {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            eventListener = firestore.collection("Events").document(eventId).addSnapshotListener(this, (documentSnapshot, e) -> {
                if (e != null) {
                    //Log.e(TAG, "Listen failed.", e);
                    return;
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    Event updatedEvent = documentSnapshot.toObject(Event.class);
                    if (updatedEvent != null) {
                        Log.d("FirestoreListener", "Event updated: " + updatedEvent.getName());
                        event = updatedEvent; // Update the current event
                        populateEventDetails(event); // Refresh UI with updated event details
                    }
                } else {
                    Log.d("FirestoreListener", "Current data: null");
                }
            });
        }


        @Override
    protected void onStart() {
        super.onStart();
        setupFirestoreListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (eventListener != null) {
            eventListener.remove();
            eventListener = null;
        }
    }



}
