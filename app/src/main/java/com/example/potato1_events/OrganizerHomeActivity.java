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

public class OrganizerHomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private LinearLayout eventsLinearLayout;
    private FirebaseFirestore firestore;

    private String deviceId;
    private ArrayList<Event> eventList = new ArrayList<>(); // To store events

    private Button switchModeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_home);

        // Get device ID
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
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

        // Load events associated with the organizer's facility
        loadEventsForOrganizerFacility();
        handleBackPressed();
    }

    /**
     * Navigates back to LandingActivity when Switch Mode button is clicked.
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
     * Loads all events associated with the organizer's facility.
     */
    private void loadEventsForOrganizerFacility() {
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
                                                Toast.makeText(OrganizerHomeActivity.this,
                                                        "No events found for your facility.",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        // Case if there was a failure when loading events
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(OrganizerHomeActivity.this,
                                                    "Error loading events: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                Toast.makeText(OrganizerHomeActivity.this,
                                        "No events associated with your facility.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        // Facility does not exist; prompt the organizer to create one
                        Toast.makeText(OrganizerHomeActivity.this,
                                "No facility found. Please create a facility first.",
                                Toast.LENGTH_SHORT).show();
                        navigateToCreateFacility();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(OrganizerHomeActivity.this,
                            "Error loading facility data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Adds an event view to the LinearLayout for the added event.
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

        // Adding the information desired to the event view (Need to add deadline for waitlist too)
        eventNameTextView.setText(event.getName());
        eventLocationTextView.setText(event.getEventLocation());

        if (!TextUtils.isEmpty(event.getPosterImageUrl())) {
            Picasso.get()
                    .load(event.getPosterImageUrl())
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_error_image)
                    .into(eventPosterImageView);
        } else {
            eventPosterImageView.setImageResource(R.drawable.ic_placeholder_image);
        }

        // Listener to switch to event details if clicked
        eventCardView.setOnClickListener(v -> {
            Intent intent = new Intent(OrganizerHomeActivity.this, EventDetailsOrganizerActivity.class);
            intent.putExtra("EVENT_ID", event.getId());
            startActivity(intent);
        });

        // Finally add the new view to the linear view
        eventsLinearLayout.addView(eventView);
    }

    /**
     * Navigates the organizer to the Create/Edit Facility Activity to create a new facility.
     * This is for when an organizer is first chosen (Will probably be removed if we combine organizer and entrant, instead having a warning for creating an event)
     */
    private void navigateToCreateFacility() {
        Intent intent = new Intent(OrganizerHomeActivity.this, CreateEditFacilityActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Used to control changing between different pages in the side bar
     *
     * @param item The option clicked on by the user in the side bar
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation
        int id = item.getItemId();

        if (id == R.id.nav_organizer_profile) {
            Intent intent = new Intent(OrganizerHomeActivity.this, UserInfoActivity.class);
            intent.putExtra("USER_TYPE", "Organizer");
            intent.putExtra("MODE", "EDIT");
            startActivity(intent);
        } else if (id == R.id.nav_create_event) {
            Intent intent = new Intent(OrganizerHomeActivity.this, CreateEditEventActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_edit_facility) {
            Intent intent = new Intent(OrganizerHomeActivity.this, CreateEditFacilityActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_my_events) {
            Toast.makeText(this, "Already on this page.", Toast.LENGTH_SHORT).show();
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
