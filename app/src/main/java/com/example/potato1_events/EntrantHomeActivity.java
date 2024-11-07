// File: EntrantHomeActivity.java
package com.example.potato1_events;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
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

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Activity to display all events for entrants.
 * Entrants can view event details and join or leave waiting lists.
 */
public class EntrantHomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private LinearLayout eventsLinearLayout;
    private FirebaseFirestore firestore;

    private String deviceId;
    private ArrayList<Event> eventList = new ArrayList<>(); // To store events

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_home);

        final boolean isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);

        // Get device ID
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Initialize Firebase Firestore
        firestore = FirebaseFirestore.getInstance();

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
        }

        navigationView.setNavigationItemSelectedListener(this);

        // Load all events
        loadAllEvents();
    }

    /**
     * Loads all events from the "Events" collection in Firestore.
     */
    private void loadAllEvents() {
        // Clear existing views and list
        eventsLinearLayout.removeAllViews();
        eventList.clear();

        firestore.collection("Events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot eventDoc : queryDocumentSnapshots) {
                            Event event = eventDoc.toObject(Event.class);
                            event.setId(eventDoc.getId());
                            eventList.add(event);
                            addEventView(event);
                        }
                    } else {
                        Toast.makeText(EntrantHomeActivity.this,
                                "No events available at the moment.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EntrantHomeActivity.this,
                            "Error loading events: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
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
     * @return True if handled, else false.
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation
        int id = item.getItemId();

        if (id == R.id.nav_notifications) {
            // FIXME: Implement NotificationsActivity
            Toast.makeText(this, "Notifications feature not implemented yet.", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_edit_profile) {
            // Navigate to EditProfileActivity
            Intent intent = new Intent(EntrantHomeActivity.this, UserInfoActivity.class);
            intent.putExtra("USER_TYPE", "Entrant"); // or "Organizer"
            intent.putExtra("MODE", "EDIT");
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
}
