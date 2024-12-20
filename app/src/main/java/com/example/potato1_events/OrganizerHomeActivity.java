// File: OrganizerHomeActivity.java
package com.example.potato1_events;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity representing the Organizer's Home screen.
 * Allows organizers to view and manage events associated with their facility.
 * Provides navigation to profile editing, event creation, facility editing, media management, and user management.
 */
public class OrganizerHomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // UI Components

    /**
     * DrawerLayout for the navigation drawer.
     */
    private DrawerLayout drawerLayout;

    /**
     * LinearLayout to display the list of events.
     */
    private LinearLayout eventsLinearLayout;

    /**
     * FirebaseFirestore instance for database interactions.
     */
    private FirebaseFirestore firestore;

    /**
     * Unique device ID used to identify the organizer's facility.
     */
    private String deviceId;

    /**
     * List holding all Event objects associated with the organizer's facility.
     */
    private OrgEventsRepository eventRepository;
    private ArrayList<Event> eventList = new ArrayList<>(); // To store events

    /**
     * Button to switch between Organizer and Entrant modes.
     */
    private Button switchModeButton;


    /**
     * Sets the Firestore instance for testing purposes.
     *
     * @param firestore The mocked Firestore instance.
     */

    private boolean isAdmin = false; // Retrieved from Intent
    @VisibleForTesting
    public void setFirestore(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Called when the activity is first created.
     * Initializes UI components, Firebase instances, and loads events associated with the organizer's facility.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_home);

        // Get device ID
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        eventRepository = OrgEventsRepository.getInstance();



        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);

        // Initialize UI Components
        drawerLayout = findViewById(R.id.drawer_organizer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        eventsLinearLayout = findViewById(R.id.eventsLinearLayout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        // Set up Navigation Drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);


        if (isAdmin) {
            navigationView.getMenu().findItem(R.id.nav_manage_media).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_users).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_create_event).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_edit_facility).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_my_events).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_events).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_facilities).setVisible(true);
        }

        // Load events associated with the organizer's facility
        loadEventsForOrganizerFacility();
        handleBackPressed();
        setupFirestoreListener(navigationView, toggle);
    }

    /**
     * Sets the EventRepository instance (used for testing).
     *
     * @param repository The EventRepository instance.
     */
    public void setOrgEventsRepository(OrgEventsRepository repository) {
        this.eventRepository = repository;
    }

    /**
     * Navigates back to LandingActivity when Switch Mode button is clicked.
     * Clears the current activity stack to prevent returning to this activity.
     */
    private void switchMode() {
        // Create an Intent to navigate to LandingActivity
        Intent intent = new Intent(OrganizerHomeActivity.this, LandingActivity.class);

        // Set flags to clear the current activity stack
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        // Start LandingActivity
        startActivity(intent);

        // Finish the current activity to remove it from the back stack
        finish();
    }

    public void loadEventsForOrganizerFacility() {
        // Clear existing views and list
        eventsLinearLayout.removeAllViews();
        eventList.clear();

        // Reference to the organizer's facility document using deviceId as facilityId
        CollectionReference facilitiesRef = firestore.collection("Facilities");
        facilitiesRef.document(deviceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Facility facility = documentSnapshot.toObject(Facility.class);
                        if (facility != null) {
                            List<String> eventIds = facility.getEventIds();

                            if (eventIds != null && !eventIds.isEmpty()) {
                                // Due to Firestore limitations, 'whereIn' can handle up to 10 elements
                                // If you have more, you'll need to batch your queries
                                List<String> limitedEventIds = eventIds.size() > 10 ? eventIds.subList(0, 10) : eventIds;

                                firestore.collection("Events")
                                        .whereIn(FieldPath.documentId(), limitedEventIds)
                                        .get()
                                        .addOnSuccessListener(eventsSnapshot -> {
                                            for (QueryDocumentSnapshot eventDoc : eventsSnapshot) { // Adding event to the list in firebase and the view
                                                Event event = eventDoc.toObject(Event.class);
                                                event.setId(eventDoc.getId());
                                                eventList.add(event);
                                                addEventView(event);
                                            }
                                            // Case if your facility has no events
                                            if (eventList.isEmpty()) {
                                                Toast.makeText(OrganizerHomeActivity.this, "No events found for your facility.", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        // Case if there was a failure when loading events
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(OrganizerHomeActivity.this, "Error loading events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                Toast.makeText(OrganizerHomeActivity.this, "No events associated with your facility.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        // Facility does not exist; prompt the organizer to create one
                        Toast.makeText(OrganizerHomeActivity.this, "No facility found. Please create a facility first.", Toast.LENGTH_SHORT).show();
                        navigateToCreateFacility();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(OrganizerHomeActivity.this, "Error loading facility data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Adds an event view to the eventsLinearLayout for the specified event.
     *
     * @param event The Event object to display.
     */
    private void addEventView(Event event) {
        // Inflate event_item.xml
        LayoutInflater inflater = LayoutInflater.from(this);
        View eventView = inflater.inflate(R.layout.event_item, eventsLinearLayout, false);

        // Initialize UI components within event_item.xml
        ImageView eventPosterImageView = eventView.findViewById(R.id.eventPosterImageView);
        TextView eventNameTextView = eventView.findViewById(R.id.eventNameTextView);
        TextView eventLocationTextView = eventView.findViewById(R.id.eventLocationTextView);
        CardView eventCardView = eventView.findViewById(R.id.eventCardView);

        // Set event information
        eventNameTextView.setText(event.getName());
        eventLocationTextView.setText(event.getEventLocation());

        // Load event poster image using Picasso
        if (!TextUtils.isEmpty(event.getPosterImageUrl())) {
            Picasso.get()
                    .load(event.getPosterImageUrl())
                    .placeholder(R.drawable.ic_placeholder_image) // Placeholder image while loading
                    .error(R.drawable.ic_error_image)             // Image to display on error
                    .into(eventPosterImageView);
        } else {
            // Set a default placeholder image if poster URL is empty
            eventPosterImageView.setImageResource(R.drawable.ic_placeholder_image);
        }

        // Listener to navigate to EventDetailsOrganizerActivity if clicked
        eventCardView.setOnClickListener(v -> {
            Intent intent = new Intent(OrganizerHomeActivity.this, EventDetailsOrganizerActivity.class);
            intent.putExtra("EVENT_ID", event.getId());
            intent.putExtra("IS_ADMIN", isAdmin);
            startActivity(intent);
        });

        // Finally add the new view to the linear layout
        eventsLinearLayout.addView(eventView);
    }

    /**
     * Navigates the organizer to the Create/Edit Facility Activity to create a new facility.
     * This is used when an organizer has no associated facility.
     */
    private void navigateToCreateFacility() {
        Intent intent = new Intent(OrganizerHomeActivity.this, CreateEditFacilityActivity.class);
        startActivity(intent);
        finish();
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
            intent = new Intent(OrganizerHomeActivity.this, NotificationsActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_edit_profile) {
            // Navigate to UserInfoActivity in EDIT mode
            intent = new Intent(OrganizerHomeActivity.this, UserInfoActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
            intent.putExtra("MODE", "EDIT");
        } else if (id == R.id.nav_manage_media) {
            // Navigate to ManageMediaActivity (visible only to admins)
            intent = new Intent(OrganizerHomeActivity.this, ManageMediaActivity.class);
        } else if (id == R.id.nav_manage_users) {
            // Navigate to ManageUsersActivity (visible only to admins)
            intent = new Intent(OrganizerHomeActivity.this, ManageUsersActivity.class);
        } else if (id == R.id.nav_manage_events) {
            intent = new Intent(OrganizerHomeActivity.this, ManageEventsActivity.class);
        } else if (id == R.id.nav_manage_facilities) {
            intent = new Intent(OrganizerHomeActivity.this, ManageFacilitiesActivity.class);
        } else if (id == R.id.action_scan_qr) {
            intent = new Intent(OrganizerHomeActivity.this, QRScanActivity.class);
        } else if (id == R.id.nav_create_event) {
            intent = new Intent(OrganizerHomeActivity.this, CreateEditEventActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_edit_facility) {
            intent = new Intent(OrganizerHomeActivity.this, CreateEditFacilityActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_my_events) {
            Toast.makeText(this, "Already on this page.", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_view_joined_events) {
            intent = new Intent(OrganizerHomeActivity.this, EntrantHomeActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        }

        if (intent != null) {
            startActivity(intent);
        }


        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Handles the back button press to close the navigation drawer if it's open.
     * If the drawer is not open, it performs the default back action.
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

    private void setupFirestoreListener(NavigationView navigationView, ActionBarDrawerToggle toggle) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("Users")
                .document(deviceId)
                .addSnapshotListener(this, (documentSnapshot, e) -> {
                    if (e != null) {
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
