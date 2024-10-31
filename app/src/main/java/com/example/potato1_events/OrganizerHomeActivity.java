package com.example.potato1_events;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class OrganizerHomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private LinearLayout eventsLinearLayout;
    private FirebaseFirestore firestore;

    private String deviceId;
    private ArrayList<Event> eventList = new ArrayList<>(); // To store events

    // Location related variables
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private static final float SEARCH_RADIUS_KM = 10.0f; // Example: 10 km radius

    // Permission launcher
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_home);

        // Get device ID
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Initialize Firebase Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
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

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize permission launcher
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        fetchLocationAndLoadEvents();
                    } else {
                        Toast.makeText(OrganizerHomeActivity.this,
                                "Location permission denied. Unable to load nearby events.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Check and request location permissions
        checkLocationPermissionAndLoadEvents();
    }

    /**
     * Checks if location permissions are granted. If not, requests them.
     */
    private void checkLocationPermissionAndLoadEvents() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted
            fetchLocationAndLoadEvents();
        } else {
            // Request permission
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Fetches the device's current location and loads events based on that location.
     */
    private void fetchLocationAndLoadEvents() {
        try {
            Task<Location> locationTask = fusedLocationClient.getLastLocation();
            locationTask.addOnSuccessListener(location -> {
                if (location != null) {
                    currentLocation = location;
                    loadEventsNearLocation(currentLocation);
                } else {
                    Toast.makeText(OrganizerHomeActivity.this,
                            "Unable to determine current location.",
                            Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(OrganizerHomeActivity.this,
                        "Error fetching location: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            });
        } catch (SecurityException e) {
            // This should not happen as we've already checked permissions
            Toast.makeText(OrganizerHomeActivity.this,
                    "Location permission not granted.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads events from Firestore based on the provided location.
     *
     * @param location The current location of the device.
     */
    private void loadEventsNearLocation(Location location) {
        // Clear existing views and list
        eventsLinearLayout.removeAllViews();
        eventList.clear();

        // First, query Facilities within the SEARCH_RADIUS_KM
        CollectionReference facilitiesRef = firestore.collection("Facilities");

        facilitiesRef.get().addOnSuccessListener(facilitiesSnapshot -> {
            List<String> nearbyFacilityIds = new ArrayList<>();

            for (QueryDocumentSnapshot facilityDoc : facilitiesSnapshot) {
                Facility facility = facilityDoc.toObject(Facility.class);
                facility.setId(facilityDoc.getId());

                double distance = calculateDistance(
                        location.getLatitude(),
                        location.getLongitude(),
                        facility.getLatitude(),
                        facility.getLongitude()
                );

                if (distance <= SEARCH_RADIUS_KM) {
                    nearbyFacilityIds.add(facility.getId());
                }
            }

            if (nearbyFacilityIds.isEmpty()) {
                Toast.makeText(OrganizerHomeActivity.this,
                        "No facilities found within " + SEARCH_RADIUS_KM + " km.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Now, query Events where facilityId is in nearbyFacilityIds
            firestore.collection("Events")
                    .whereIn("facilityId", nearbyFacilityIds)
                    .get()
                    .addOnSuccessListener(eventsSnapshot -> {
                        for (QueryDocumentSnapshot eventDoc : eventsSnapshot) {
                            Event event = eventDoc.toObject(Event.class);
                            event.setId(eventDoc.getId());
                            eventList.add(event);
                            addEventView(event);
                        }

                        if (eventList.isEmpty()) {
                            Toast.makeText(OrganizerHomeActivity.this,
                                    "No events found near your location.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(OrganizerHomeActivity.this,
                                "Error loading events: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });

        }).addOnFailureListener(e -> {
            Toast.makeText(OrganizerHomeActivity.this,
                    "Error loading facilities: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Calculates the distance between two geographic coordinates using the Haversine formula.
     *
     * @param lat1 Latitude of the first point.
     * @param lon1 Longitude of the first point.
     * @param lat2 Latitude of the second point.
     * @param lon2 Longitude of the second point.
     * @return Distance in kilometers.
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371; // Radius of the Earth in kilometers

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                                Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Adds a Button view for each event to the LinearLayout.
     *
     * @param event The Event object to display.
     */
    private void addEventView(Event event) {
        // Create a Button for each event
        Button eventButton = new Button(this);
        eventButton.setText(event.getName()); // Set event name
        eventButton.setTag(event.getId()); // Store event ID

        // Set OnClickListener
        eventButton.setOnClickListener(v -> {
            String eventId = (String) v.getTag();
            Intent intent = new Intent(OrganizerHomeActivity.this, EventDetailsEntrantActivity.class);
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
            Toast.makeText(this, "Notifications feature not implemented yet.", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_create_event) {
            Intent intent = new Intent(OrganizerHomeActivity.this, CreateEditEventActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_my_events_centres) {
            //FIXME Implement this
            // Navigate to my_events_centre
//            Intent intent = new Intent(EntrantHomeActivity.this, NotificationsActivity.class);
//            startActivity(intent);
            Toast.makeText(this, "Events Centres not implemented yet", Toast.LENGTH_SHORT).show();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}
