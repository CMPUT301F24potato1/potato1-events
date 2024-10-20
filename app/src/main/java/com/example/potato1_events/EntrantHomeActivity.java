package com.example.potato1_events;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EntrantHomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private LinearLayout eventsLinearLayout;
    private FirebaseFirestore firestore;

    private String deviceId;
    private ArrayList<Event> eventList = new ArrayList<>(); // To store events

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_home);

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

        navigationView.setNavigationItemSelectedListener(this);

        // Load events from Firestore
        loadEvents();

//        SearchView searchView = findViewById(R.id.searchView);
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                filterEvents(query);
//                return false;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                filterEvents(newText);
//                return false;
//            }
//        });
//
//        private void filterEvents(String query) {
//            eventsLinearLayout.removeAllViews(); // Clear existing views
//
//            for (Event event : eventList) {
//                if (event.getName().toLowerCase().contains(query.toLowerCase())) {
//                    addEventView(event);
//                }
//            }
//        }
    }

    private void loadEvents() {
        firestore.collection("Events").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventsLinearLayout.removeAllViews(); // Clear existing views
                    eventList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        event.setEventId(document.getId()); // Ensure event ID is set
                        eventList.add(event);
                        addEventView(event);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EntrantHomeActivity.this, "Error loading events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addEventView(Event event) {
        // Create a Button for each event
        Button eventButton = new Button(this);
        eventButton.setText(event.getName()); // Set event name
        eventButton.setTag(event.getEventId()); // Store event ID

        // Set OnClickListener
        eventButton.setOnClickListener(v -> {
            String eventId = (String) v.getTag();
            Intent intent = new Intent(EntrantHomeActivity.this, EventDetailsEntrantActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            startActivity(intent);
        });

        // Add the button to the LinearLayout
        eventsLinearLayout.addView(eventButton);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation
        int id = item.getItemId();

        if (id == R.id.nav_notifications) {
            //FIXME Implement this
            // Navigate to NotificationsActivity
//            Intent intent = new Intent(EntrantHomeActivity.this, NotificationsActivity.class);
//            startActivity(intent);
        } else if (id == R.id.nav_edit_profile) {
            //FIXME Implement this
            // Navigate to EditProfileActivity
//            Intent intent = new Intent(EntrantHomeActivity.this, EditProfileActivity.class);
//            startActivity(intent);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}