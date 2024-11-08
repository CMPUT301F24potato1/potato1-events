// File: EntrantHomeActivity.java
package com.example.potato1_events;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;  // Added import
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.zxing.integration.android.IntentIntegrator;  // Added import
import com.google.zxing.integration.android.IntentResult;      // Added import
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity to display all events that the entrant has joined.
 * Entrants can view event details and manage their participation.
 */
public class EntrantHomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private LinearLayout eventsLinearLayout;
    private FirebaseFirestore firestore;
    private EntEventsRepository entEventRepo;

//    private Repository eventRepository;
    private List<Event> eventList = new ArrayList<>();

    private String deviceId;
//    private ArrayList<Event> eventList = new ArrayList<>(); // To store events

    // Declare the Switch Mode button
    private Button switchModeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_home);

        final boolean isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);

        // Get device ID
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Initialize Firebase Firestore
        firestore = FirebaseFirestore.getInstance();

        entEventRepo = new EntEventsRepository(FirebaseFirestore.getInstance());


        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        eventsLinearLayout = findViewById(R.id.eventsLinearLayout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Bind the Switch Mode button
        switchModeButton = findViewById(R.id.switchModeButton);

        // Set up Navigation Drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Make admin options available
        if (isAdmin) {
            navigationView.getMenu().findItem(R.id.nav_manage_media).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_users).setVisible(true);
        }

        navigationView.setNavigationItemSelectedListener(this);

        // Set Click Listener for Switch Mode Button
        switchModeButton.setOnClickListener(v -> switchMode());

        // Load events the entrant has joined
        loadJoinedEvents();
    }

    /**
     * Override onResume to refresh the event list when the activity resumes.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list of joined events
        loadJoinedEvents();
    }

    /**
     * Navigates back to LandingActivity when Switch Mode button is clicked.
     */
    private void switchMode() {
        // Create an Intent to navigate to LandingActivity
        Intent intent = new Intent(EntrantHomeActivity.this, LandingActivity.class);

        // Set flags to clear the current activity stack
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        // Start LandingActivity
        startActivity(intent);

        // Finish the current activity to remove it from the back stack
        finish();
    }

    /**
     * Sets the EntEventRepo instance (used for testing).
     *
     * @param repository The EntEventRepo instance.
     */
    public void setEntEventsRepository(EntEventsRepository repository) {
        this.entEventRepo = repository;
    }

    /**
     * Loads events that the entrant has joined from Firestore.
     */
    /**
     * Loads all events using EntEventRepo.
     * Clears existing views and populates the UI with the fetched events.
     */

    private void loadJoinedEvents() {
        // Clear existing views and list
        eventsLinearLayout.removeAllViews();
        eventList.clear();

        entEventRepo.getAllEvents(new EntEventsRepository.EventListCallback() {
            @Override
            public void onEventListLoaded(List<Event> events) {
                if (events != null && !events.isEmpty()) {
                    eventList.addAll(events);
                    eventsLinearLayout.removeAllViews(); // Clear existing views
                    // Update UI with eventList
                    for (Event event : eventList) {
                        addEventView(event);
                    }
                } else {
                    Toast.makeText(EntrantHomeActivity.this,
                            "No events available at the moment.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        // Reference to the entrant's user document
        DocumentReference userRef = firestore.collection("Entrants").document(deviceId);

        userRef.get().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                DocumentSnapshot document = task.getResult();
                if(document.exists()){
                    List<String> eventsJoined = (List<String>) document.get("eventsJoined");
                    if(eventsJoined != null && !eventsJoined.isEmpty()){
                        fetchEvents(eventsJoined);
                    } else {
                        Toast.makeText(EntrantHomeActivity.this, "You haven't joined any events yet.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(EntrantHomeActivity.this, "User profile not found.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(EntrantHomeActivity.this, "Error fetching user data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Fetches events from Firestore based on a list of event IDs.
     *
     * @param eventsJoined List of event IDs the entrant has joined.
     */
    private void fetchEvents(List<String> eventsJoined) {
        if(eventsJoined.isEmpty()){
            Toast.makeText(this, "You haven't joined any events yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Firestore allows up to 10 items in an 'in' query
        int batchSize = 10;
        int totalEvents = eventsJoined.size();
        int batches = (int) Math.ceil((double) totalEvents / batchSize);

        for(int i = 0; i < batches; i++){
            int start = i * batchSize;
            int end = Math.min(start + batchSize, totalEvents);
            List<String> batch = eventsJoined.subList(start, end);

            firestore.collection("Events")
                    .whereIn(FieldPath.documentId(), batch)
                    .get()
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            QuerySnapshot querySnapshot = task.getResult();
                            if(querySnapshot != null && !querySnapshot.isEmpty()){
                                for(QueryDocumentSnapshot eventDoc : querySnapshot){
                                    Event event = eventDoc.toObject(Event.class);
                                    event.setId(eventDoc.getId());
                                    eventList.add(event);
                                    addEventView(event);
                                }
                            }
                        } else {
                            Toast.makeText(EntrantHomeActivity.this, "Error fetching events: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    /**
     * Adds a custom event view to the LinearLayout.
     *
     * @param event The Event object to display.
     */
    private void addEventView(Event event) {
        // Inflate the event_item.xml layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View eventView = inflater.inflate(R.layout.event_item, eventsLinearLayout, false);

        // Initialize UI components within event_item.xml
        ImageView eventPosterImageView = eventView.findViewById(R.id.eventPosterImageView);
        TextView eventNameTextView = eventView.findViewById(R.id.eventNameTextView);
        TextView eventLocationTextView = eventView.findViewById(R.id.eventLocationTextView);
        CardView eventCardView = eventView.findViewById(R.id.eventCardView);

        // Populate the views with event data
        eventNameTextView.setText(event.getName());
        eventLocationTextView.setText(event.getEventLocation());

        if (!TextUtils.isEmpty(event.getPosterImageUrl())) {
            Picasso.get()
                    .load(event.getPosterImageUrl())
                    .placeholder(R.drawable.ic_placeholder_image) // Ensure you have a placeholder image
                    .error(R.drawable.ic_error_image) // Ensure you have an error image
                    .into(eventPosterImageView);
        } else {
            eventPosterImageView.setImageResource(R.drawable.ic_placeholder_image); // Default image
        }

        // Set OnClickListener to navigate to Event Details
        eventCardView.setOnClickListener(v -> {
            Intent intent = new Intent(EntrantHomeActivity.this, EventDetailsEntrantActivity.class);
            intent.putExtra("EVENT_ID", event.getId());
            startActivity(intent);
        });

        // Add the populated event view to the LinearLayout
        eventsLinearLayout.addView(eventView);
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

        if (id == R.id.nav_notifications) {
            // Navigate to NotificationsActivity
//            Intent intent = new Intent(EntrantHomeActivity.this, NotificationsActivity.class);
//            startActivity(intent);
        } else if (id == R.id.nav_edit_profile) {
            // Navigate to EditProfileActivity
            Intent intent = new Intent(EntrantHomeActivity.this, UserInfoActivity.class);
            intent.putExtra("USER_TYPE", "Entrant"); // or "Organizer"
            intent.putExtra("MODE", "EDIT");
            startActivity(intent);
        } else if (id == R.id.nav_manage_media) {
            // Navigate to ManageMediaActivity (visible only to admins)
            Intent intent = new Intent(EntrantHomeActivity.this, ManageMediaActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_manage_users) {
            // Navigate to ManageUsersActivity (visible only to admins)
            Intent intent = new Intent(EntrantHomeActivity.this, ManageUsersActivity.class);
            startActivity(intent);
        } else if (id == R.id.action_scan_qr) {
            // Handle QR code scanning
            scanQRCode();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Handles the back button press to close the drawer if open.
     */
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Initiates the QR code scanning using ZXing library.
     */
    private void scanQRCode() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan a QR Code");
        integrator.setOrientationLocked(true);  // Lock orientation to portrait
        integrator.setCaptureActivity(PortraitCaptureActivity.class);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();
    }

    /**
     * Handles the result from the QR code scanning activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                String scannedData = result.getContents();
                // Handle the scanned data
                handleScannedData(scannedData);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Handles the scanned QR code data.
     *
     * @param scannedData The data obtained from scanning the QR code.
     */
    private void handleScannedData(String scannedData) {
        // Assuming the scannedData contains the event ID
        String eventId = scannedData;

        // Start the EventDetailsEntrantActivity with the event ID
        Intent intent = new Intent(EntrantHomeActivity.this, EventDetailsEntrantActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        startActivity(intent);
    }
}
