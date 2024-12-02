// File: EntrantHomeActivity.java
package com.example.potato1_events;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Activity to display all events that the entrant has joined.
 * Entrants can view event details and manage their participation.
 * Integrates the navigation drawer with admin functionalities.
 */
public class EntrantHomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // UI Components
    private DrawerLayout drawerLayout; // Navigation drawer layout
    private LinearLayout eventsLinearLayout; // Container for event views
    private EntEventsRepository entEventRepo; // Repository for fetching events
    private List<Event> eventList; // List to hold fetched events

    private String deviceId; // Unique device identifier
    private Button switchModeButton; // Button to switch user mode (if applicable)

    // To keep track of added event IDs to prevent duplicates
    private Set<String> addedEventIds = new HashSet<>();

    // User Privileges
    private boolean isAdmin = false; // Indicates if the user has admin privileges

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001; // Request code for notification permissions

    /**
     * Initializes the activity, sets up UI components, Firebase instances, and event listeners.
     *
     * @param savedInstanceState The previously saved state of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_home);

        // Retrieve the isAdmin flag from Intent extras
        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);

        // Get device ID
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Initialize Firebase Firestore
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Initialize EntEventsRepository with Firestore instance
        entEventRepo = new EntEventsRepository(firestore);

        // Initialize UI components
        drawerLayout = findViewById(R.id.drawer_layout); // Navigation drawer
        NavigationView navigationView = findViewById(R.id.nav_view); // Navigation view inside the drawer
        eventsLinearLayout = findViewById(R.id.eventsLinearLayout); // Container for event items
        Toolbar toolbar = findViewById(R.id.toolbar); // Toolbar at the top
        setSupportActionBar(toolbar); // Set the toolbar as the app bar

        // Set up Navigation Drawer with toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle); // Add toggle as a listener to the drawer
        toggle.syncState(); // Synchronize the state of the drawer toggle

        // Make admin options available in the navigation drawer if the user is an admin
        if (isAdmin) {
            navigationView.getMenu().findItem(R.id.nav_manage_media).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_users).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_events).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_create_event).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_edit_facility).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_my_events).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_facilities).setVisible(true);
        }

        // Request notification permissions if necessary
        requestNotificationPermission();

        // Set the navigation item selected listener to handle menu item clicks
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize event list
        eventList = new ArrayList<>();

        // Load events the entrant has joined
        loadJoinedEvents();

        // Set up Firestore listener to monitor admin status changes
        setupFirestoreListener(navigationView, toggle);
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
     * (Assuming there is a switch mode functionality; implement if applicable)
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
     * Sets the EntEventsRepository instance (used for testing).
     *
     * @param repository The EntEventsRepository instance.
     */
    public void setEntEventsRepository(EntEventsRepository repository) {
        this.entEventRepo = repository;
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
     * Loads events that the entrant has joined from the repository.
     * Clears existing views and populates the UI with the fetched events.
     */
    public void loadJoinedEvents() {
        // Clear existing views and list
        eventsLinearLayout.removeAllViews();
        eventList.clear();
        addedEventIds.clear();

        // Fetch joined events using the repository
        entEventRepo.getJoinedEvents(deviceId, new EntEventsRepository.EventListCallback() {
            @Override
            public void onEventListLoaded(List<Event> events) {
                if (events != null && !events.isEmpty()) {
                    eventList.addAll(events);
                    // Update UI with eventList
                    for (Event event : eventList) {
                        if (!addedEventIds.contains(event.getId())) {
                            addEventView(event); // Add event view to the layout
                            addedEventIds.add(event.getId()); // Track added event ID
                        }
                    }
                } else if (events != null && events.isEmpty()) {
                    // Entrant hasn't joined any events
                    Toast.makeText(EntrantHomeActivity.this, "You haven't joined any events yet.", Toast.LENGTH_SHORT).show();
                } else {
                    // An error occurred while fetching events
                    Toast.makeText(EntrantHomeActivity.this, "Error fetching events.", Toast.LENGTH_SHORT).show();
                }
            }
        });
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

        // Load and display the event poster image using Picasso library
        if (!TextUtils.isEmpty(event.getPosterImageUrl())) {
            Picasso.get()
                    .load(event.getPosterImageUrl())
                    .placeholder(R.drawable.ic_placeholder_image) // Placeholder image while loading
                    .error(R.drawable.ic_error_image) // Error image if loading fails
                    .into(eventPosterImageView);
        } else {
            eventPosterImageView.setImageResource(R.drawable.ic_placeholder_image); // Default placeholder image
        }

        // Set OnClickListener to navigate to Event Details when the event card is clicked
        eventCardView.setOnClickListener(v -> {
            Intent intent = new Intent(EntrantHomeActivity.this, EventDetailsEntrantActivity.class);
            intent.putExtra("EVENT_ID", event.getId()); // Pass the event ID to the details activity
            startActivity(intent);
        });

        // Add the populated event view to the LinearLayout
        eventsLinearLayout.addView(eventView);
    }

    /**
     * Handles navigation menu item selections.
     *
     * @param item The selected menu item.
     * @return True if the event was handled, false otherwise.
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation menu item selections
        int id = item.getItemId();
        Intent intent = null;

        if (id == R.id.nav_notifications) {
            // Navigate to NotificationsActivity
            // Uncomment and implement if NotificationsActivity exists
            intent = new Intent(EntrantHomeActivity.this, NotificationsActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_edit_profile) {
            // Navigate to UserInfoActivity in EDIT mode
            intent = new Intent(EntrantHomeActivity.this, UserInfoActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
            intent.putExtra("MODE", "EDIT");
        } else if (id == R.id.nav_manage_media) {
            // Navigate to ManageMediaActivity (visible only to admins)
            intent = new Intent(EntrantHomeActivity.this, ManageMediaActivity.class);
        } else if (id == R.id.nav_manage_users) {
            // Navigate to ManageUsersActivity (visible only to admins)
            intent = new Intent(EntrantHomeActivity.this, ManageUsersActivity.class);
        } else if (id == R.id.nav_manage_events) {
            // Navigate to ManageEventsActivity
            intent = new Intent(EntrantHomeActivity.this, ManageEventsActivity.class);
        } else if (id == R.id.nav_manage_facilities) {
            // Navigate to ManageFacilitiesActivity
            intent = new Intent(EntrantHomeActivity.this, ManageFacilitiesActivity.class);
        } else if (id == R.id.action_scan_qr) {
            // Navigate to QRScanActivity for scanning QR codes
            intent = new Intent(EntrantHomeActivity.this, QRScanActivity.class);
        } else if (id == R.id.nav_create_event) {
            // Navigate to CreateEditEventActivity to create a new event
            intent = new Intent(EntrantHomeActivity.this, CreateEditEventActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_edit_facility) {
            // Navigate to CreateEditFacilityActivity to edit the facility
            intent = new Intent(EntrantHomeActivity.this, CreateEditFacilityActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_my_events) {
            // Navigate to OrganizerHomeActivity to view/manage own events
            intent = new Intent(EntrantHomeActivity.this, OrganizerHomeActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_view_joined_events) {
            // Already on this page; inform the user
            Toast.makeText(this, "Already on this page.", Toast.LENGTH_SHORT).show();
        }

        // Start the intended activity if an intent was created
        if (intent != null) {
            startActivity(intent);
        }

        // Close the navigation drawer after selection
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Handles the back button press to close the drawer if open.
     */
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            // If the navigation drawer is open, close it
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // Otherwise, proceed with the default back behavior
            super.onBackPressed();
        }
    }

    /**
     * Requests notification permission if the device is running Android Tiramisu (API 33) or higher.
     * This is necessary for sending notifications to the user.
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if notification permission is already granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Request notification permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    /**
     * Handles the result of permission requests.
     *
     * @param requestCode  The request code passed in requestPermissions().
     * @param permissions  The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("EntrantHomeActivity", "Notification permission granted.");
            } else {
                Log.d("EntrantHomeActivity", "Notification permission denied.");
                // Optionally, inform the user that notifications will not be shown
            }
        }
    }

    /**
     * Sets up a Firestore listener to monitor changes in the user's admin status.
     * Updates the navigation menu accordingly in real-time.
     *
     * @param navigationView The NavigationView to update menu items.
     * @param toggle         The ActionBarDrawerToggle to sync state.
     */
    private void setupFirestoreListener(NavigationView navigationView, ActionBarDrawerToggle toggle) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Listen to changes in the user's document to detect admin status changes
        firestore.collection("Users")
                .document(deviceId)
                .addSnapshotListener(this, (documentSnapshot, e) -> {
                    if (e != null) {
                        // Log the error or handle it as needed
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // Retrieve the 'admin' field from the user document
                        Boolean admin = documentSnapshot.getBoolean("admin");

                        if (admin != null && admin != isAdmin) {
                            // Update the isAdmin variable if there's a change
                            isAdmin = admin;

                            // Update the navigation menu based on the new isAdmin value
                            runOnUiThread(() -> {
                                if (isAdmin) {
                                    // Show admin-specific menu items
                                    navigationView.getMenu().findItem(R.id.nav_manage_media).setVisible(true);
                                    navigationView.getMenu().findItem(R.id.nav_manage_users).setVisible(true);
                                    navigationView.getMenu().findItem(R.id.nav_manage_facilities).setVisible(true);
                                    navigationView.getMenu().findItem(R.id.nav_manage_events).setVisible(true);
                                } else {
                                    // Hide admin-specific menu items
                                    navigationView.getMenu().findItem(R.id.nav_manage_media).setVisible(false);
                                    navigationView.getMenu().findItem(R.id.nav_manage_users).setVisible(false);
                                    navigationView.getMenu().findItem(R.id.nav_manage_facilities).setVisible(false);
                                    navigationView.getMenu().findItem(R.id.nav_manage_events).setVisible(false);
                                }
                                // Sync the toggle state to reflect menu changes
                                toggle.syncState();
                            });
                        }
                    }
                });
    }
}
