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

        eventRepository = new OrgEventsRepository(FirebaseFirestore.getInstance());



        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);

        // Initialize UI Components
        drawerLayout = findViewById(R.id.drawer_organizer_layout);
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

        navigationView.setNavigationItemSelectedListener(this);

        // Set Click Listener for Switch Mode Button
        switchModeButton.setOnClickListener(v -> switchMode());

        if (isAdmin) {
            navigationView.getMenu().findItem(R.id.nav_manage_media).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_users).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_create_event).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_edit_facility).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_my_events).setVisible(true);
        }

        // Load events associated with the organizer's facility
        loadEventsForOrganizerFacility();
        handleBackPressed();
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

    /**
     * Loads all events associated with the organizer's facility from Firestore.
     * Populates the eventsLinearLayout with the fetched events.
     */

//    public void loadEventsForOrganizerFacility() {
//        // Clear existing views and list
//        CollectionReference facilitiesRef = firestore.collection("Facilities");
//        eventsLinearLayout.removeAllViews();
//        eventList.clear();
//
//        // Use facilityId (e.g., retrieved from deviceId or user session)
//        String currentFacilityId = facilityId; // Replace with actual retrieval logic
//
//        eventRepository.getEventsForOrganizerFacility(currentFacilityId, new OrgEventsRepository.EventListCallback() {
//            @Override
//            public void onEventListLoaded(List<Event> events) {
//                if (events != null && !events.isEmpty()) {
//                    eventList.addAll(events);
//                    eventsLinearLayout.removeAllViews(); // Clear existing views
//                    // Update UI with eventList
//                    for (Event event : eventList) {
//                        addEventView(event);
//                    }
//                } else {
//                    Toast.makeText(OrganizerHomeActivity.this,
//                            "No events found for your facility.",
//                            Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
//    }
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
            // intent = new Intent(OrganizerHomeActivity.this, NotificationsActivity.class);
        } else if (id == R.id.nav_edit_profile) {
            // Navigate to UserInfoActivity
            intent = new Intent(OrganizerHomeActivity.this, UserInfoActivity.class);
            intent.putExtra("USER_TYPE", "Organizer"); // or "Entrant" based on context
            intent.putExtra("MODE", "EDIT");
            intent.putExtra("IS_ADMIN", isAdmin); // Pass isAdmin flag
        } else if (id == R.id.nav_manage_media) {
            // Navigate to ManageMediaActivity (visible only to admins)
            if (isAdmin) {
                intent = new Intent(OrganizerHomeActivity.this, ManageMediaActivity.class);
                intent.putExtra("IS_ADMIN", isAdmin); // Pass isAdmin flag
            } else {
                // Access denied message (optional as menu item is hidden)
                Toast.makeText(this, "Access Denied: Admins Only", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.nav_manage_users) {
            // Navigate to ManageUsersActivity (visible only to admins)
            if (isAdmin) {
                intent = new Intent(OrganizerHomeActivity.this, ManageUsersActivity.class);
                intent.putExtra("IS_ADMIN", isAdmin); // Pass isAdmin flag
            } else {
                // Access denied message (optional as menu item is hidden)
                Toast.makeText(this, "Access Denied: Admins Only", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.action_scan_qr) {
            // Handle QR code scanning
            intent = new Intent(OrganizerHomeActivity.this, QRScanActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin); // Pass isAdmin flag
        } else if (id == R.id.nav_create_event) {
            // Navigate to CreateEditEventActivity and pass isAdmin flag
            intent = new Intent(OrganizerHomeActivity.this, CreateEditEventActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin); // Pass the isAdmin flag
        } else if (id == R.id.nav_edit_facility) {
            // Navigate to CreateEditFacilityActivity and pass isAdmin flag
            intent = new Intent(OrganizerHomeActivity.this, CreateEditFacilityActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin); // Pass isAdmin flag
        } else if (id == R.id.nav_my_events) {
            // Navigate to OrganizerHomeActivity and pass isAdmin flag
            intent = new Intent(OrganizerHomeActivity.this, OrganizerHomeActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin); // Pass isAdmin flag
        } else if (id == R.id.nav_view_joined_events) {
            // Navigate to EntrantHomeActivity and pass isAdmin flag
            intent = new Intent(OrganizerHomeActivity.this, EntrantHomeActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin); // Pass isAdmin flag
        }

        if (intent != null) {
            startActivity(intent);
        } else {
            if (id != R.id.nav_notifications) { // Assuming notifications are handled separately
                Toast.makeText(this, "Invalid option selected", Toast.LENGTH_SHORT).show();
            }
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
}
