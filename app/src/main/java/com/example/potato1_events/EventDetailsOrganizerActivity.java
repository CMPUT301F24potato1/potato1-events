// File: EventDetailsOrganizerActivity.java
package com.example.potato1_events;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Activity to display the details of an event for organizers.
 */
public class EventDetailsOrganizerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // UI Components
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    private ImageView eventPosterImageView;
    private TextView eventNameTextView;
    private TextView eventDescriptionTextView;
    private TextView eventLocationTextView;
    private TextView eventDatesTextView;
    private TextView eventCapacityTextView;
    private TextView eventGeolocationTextView;
    private TextView eventStatusTextView;
    private TextView eventWaitlistCountTextView; // New TextView for Waitlist Count
    private ImageView qrCodeImageView; // ImageView for QR Code
    private Button shareQRCodeButton; // Button to share QR Code
    private Button editButton;
    private Button deleteButton;
    private Button entrantsListButton;

    // Firebase Firestore
    private FirebaseFirestore firestore;

    // Event Data
    private String eventId;
    private Event event;

    @VisibleForTesting
    public void setFirestore(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details_organizer);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI
        drawerLayout = findViewById(R.id.drawer_event_details_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup Navigation Drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize Event Detail Views
        eventPosterImageView = findViewById(R.id.eventPosterImageView);
        eventNameTextView = findViewById(R.id.eventNameTextView);
        eventDescriptionTextView = findViewById(R.id.eventDescriptionTextView);
        eventLocationTextView = findViewById(R.id.eventLocationTextView);
        eventDatesTextView = findViewById(R.id.eventDatesTextView);
        eventCapacityTextView = findViewById(R.id.eventCapacityTextView);
        eventGeolocationTextView = findViewById(R.id.eventGeolocationTextView);
        eventStatusTextView = findViewById(R.id.eventStatusTextView);
        eventWaitlistCountTextView = findViewById(R.id.eventWaitlistCountTextView);
        qrCodeImageView = findViewById(R.id.qrCodeImageView); // Initialize QR Code ImageView
        shareQRCodeButton = findViewById(R.id.shareQRCodeButton); // Initialize Share QR Code Button
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

        handleBackPressed();

        // Set Click Listeners for Action Buttons
        editButton.setOnClickListener(v -> handleEditAction());
        deleteButton.setOnClickListener(v -> handleDeleteAction());
        entrantsListButton.setOnClickListener(v -> navigateToWaitingList());
        shareQRCodeButton.setOnClickListener(v -> shareQRCodeImage());
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
                            Toast.makeText(EventDetailsOrganizerActivity.this, "Error parsing event data.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(EventDetailsOrganizerActivity.this, "Event not found.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EventDetailsOrganizerActivity.this, "Error loading event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

        String capacity = "Available spots for event: " + event.getCapacity();
        eventCapacityTextView.setText(capacity);

        String geo = "Geolocation Required: " + (event.isGeolocationRequired() ? "Yes" : "No");
        eventGeolocationTextView.setText(geo);

        String status = "Status: " + event.getStatus();
        eventStatusTextView.setText(status);

        // Fetch and display waitlist count
        fetchWaitlistCount(eventId);

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
        firestore.collection("Events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        if (event != null) {
                            Map<String, String> entrantsMap = event.getEntrants();
                            if (entrantsMap != null) {
                                // Count the number of entrants with the "waitlist" status
                                long waitlistCount = entrantsMap.values().stream()
                                        .filter(status -> "waitlist".equalsIgnoreCase(status))
                                        .count();

                                String waitlistText = "Waiting List: " + waitlistCount + "/" + event.getWaitingListCapacity();
                                eventWaitlistCountTextView.setText(waitlistText);
                            } else {
                                eventWaitlistCountTextView.setText("Waiting List: 0/" + event.getWaitingListCapacity());
                            }
                        }
                    } else {
                        Toast.makeText(this, "Event not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching waitlist count: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    eventWaitlistCountTextView.setText("Waiting List: N/A");
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
     * Navigates to the EventWaitingListActivity to display the waiting list entrants.
     */
    private void navigateToWaitingList() {
        Intent intent = new Intent(EventDetailsOrganizerActivity.this, EventWaitingListActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        startActivity(intent);
    }

    /**
     * Handles the Edit button action.
     * Navigates to the CreateEditEventActivity for editing the event.
     */
    private void handleEditAction() {
        Intent intent = new Intent(EventDetailsOrganizerActivity.this, CreateEditEventActivity.class);
        intent.putExtra("EVENT_ID", eventId);
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
     * Deletes the event from Firestore and removes it from the facility's event list.
     */
    private void deleteEvent() {
        firestore.collection("Events").document(eventId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event deleted successfully.", Toast.LENGTH_SHORT).show();
                    // Remove eventId from the facility's eventIds list
                    removeEventFromFacility(event.getFacilityId(), eventId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Removes the event ID from the facility's eventIds array in Firestore.
     *
     * @param facilityId The ID of the facility.
     * @param eventId    The ID of the event to remove.
     */
    private void removeEventFromFacility(String facilityId, String eventId) {
        firestore.collection("Facilities").document(facilityId)
                .update("eventIds", FieldValue.arrayRemove(eventId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EventDetailsOrganizerActivity.this, "Event removed from facility.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(EventDetailsOrganizerActivity.this, OrganizerHomeActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EventDetailsOrganizerActivity.this, "Error updating facility: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Handles navigation menu item selections.
     *
     * @param item The selected menu item.
     * @return True if handled, else false.
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation
        int id = item.getItemId();

        if (id == R.id.nav_organizer_profile) {
            Intent intent = new Intent(EventDetailsOrganizerActivity.this, UserInfoActivity.class);
            intent.putExtra("USER_TYPE", "Organizer");
            intent.putExtra("MODE", "EDIT");
            startActivity(intent);
        } else if (id == R.id.nav_create_event) {
            Intent intent = new Intent(EventDetailsOrganizerActivity.this, CreateEditEventActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_edit_facility) {
            Intent intent = new Intent(EventDetailsOrganizerActivity.this, CreateEditFacilityActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_manage_media) {
            // Navigate to ManageMediaActivity
            Intent intent = new Intent(EventDetailsOrganizerActivity.this, ManageMediaActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_my_events) {
            Intent intent = new Intent(EventDetailsOrganizerActivity.this, OrganizerHomeActivity.class);
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
