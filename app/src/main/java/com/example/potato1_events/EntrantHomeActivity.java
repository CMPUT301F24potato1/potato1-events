// File: EntrantHomeActivity.java
package com.example.potato1_events;

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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Activity to display all events that the entrant has joined.
 * Entrants can view event details and manage their participation.
 */
public class EntrantHomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // UI Components
    private DrawerLayout drawerLayout;
    private LinearLayout eventsLinearLayout;
    private EntEventsRepository entEventRepo;
    private List<Event> eventList;

    private String deviceId;
    private Button switchModeButton;

    // To keep track of added event IDs to prevent duplicates
    private Set<String> addedEventIds = new HashSet<>();

    // User Privileges
    private boolean isAdmin = false; // Class-level variable

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;


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

        // Initialize EntEventsRepository
        entEventRepo = new EntEventsRepository(firestore);

        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        eventsLinearLayout = findViewById(R.id.eventsLinearLayout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up Navigation Drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Make admin options available
        if (isAdmin) {
            navigationView.getMenu().findItem(R.id.nav_manage_media).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_users).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_create_event).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_edit_facility).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_my_events).setVisible(true);
        }
        requestNotificationPermission();
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize event list
        eventList = new ArrayList<>();

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
                            addEventView(event);
                            addedEventIds.add(event.getId());
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
            intent = new Intent(EntrantHomeActivity.this, NotificationsActivity.class);
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
        } else if (id == R.id.action_scan_qr) {
            intent = new Intent(EntrantHomeActivity.this, QRScanActivity.class);
        } else if (id == R.id.nav_create_event) {

            intent = new Intent(EntrantHomeActivity.this, CreateEditEventActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_edit_facility) {

            intent = new Intent(EntrantHomeActivity.this, CreateEditFacilityActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_my_events) {

            intent = new Intent(EntrantHomeActivity.this, OrganizerHomeActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_view_joined_events) {
            Toast.makeText(this, "Already on this page.", Toast.LENGTH_SHORT).show();
        }

        if (intent != null) {
            startActivity(intent);
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
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

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
}
