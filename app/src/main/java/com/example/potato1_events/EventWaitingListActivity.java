// File: EventWaitingListActivity.java
package com.example.potato1_events;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity to display the list of entrants on the waiting list for an event with filtering capabilities.
 * Allows organizers to view and manage entrants based on their status.
 */
public class EventWaitingListActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, UserAdapter.OnCancelClickListener, OnMapReadyCallback {

    // UI Components

    /**
     * DrawerLayout for the navigation drawer.
     */
    private DrawerLayout drawerLayout;

    /**
     * NavigationView for handling navigation menu items.
     */
    private NavigationView navigationView;

    /**
     * Toolbar for the activity.
     */
    private Toolbar toolbar;

    /**
     * RecyclerView to display the list of entrants.
     */
    private RecyclerView waitingListRecyclerView;

    /**
     * Adapter for the RecyclerView to manage entrant data.
     */
    private UserAdapter userAdapter;

    /**
     * List holding all users fetched from Firestore.
     */
    private List<User> fullUserList; // Holds all users fetched

    /**
     * List holding users after applying filters.
     */
    private List<User> filteredUserList; // Holds users after filtering

    /**
     * Spinner to filter entrants based on their status.
     */
    private Spinner statusFilterSpinner;

    // Firebase Firestore

    /**
     * FirebaseFirestore instance for database interactions.
     */
    private FirebaseFirestore firestore;

    // Event Data

    /**
     * The unique identifier of the event.
     */
    private String eventId;

    /**
     * GoogleMap instance to interact with the map.
     */
    private GoogleMap mMap;

    /**
     * Container for the map fragment to control its visibility.
     */
    private FrameLayout mapContainer;

    /**
     * Map to keep track of markers associated with each entrant using their userId.
     */
    private Map<String, Marker> entrantsMarkersMap = new HashMap<>();

    private boolean isAdmin = false;

    /**
     * Tag for logging.
     */
    private static final String TAG = "EventWaitingListActivity";

    /**
     * Called when the activity is first created.
     * Initializes UI components, Firebase instances, and retrieves event details.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_waiting_list);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI Components
        drawerLayout = findViewById(R.id.drawer_event_waiting_list_layout);
        navigationView = findViewById(R.id.nav_view_waiting_list);
        toolbar = findViewById(R.id.toolbar_event_waiting_list);
        setSupportActionBar(toolbar);
        // Initialize Map Container
        mapContainer = findViewById(R.id.mapContainer);
        // Setup Navigation Drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize RecyclerView
        waitingListRecyclerView = findViewById(R.id.waitingListRecyclerView);
        waitingListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        fullUserList = new ArrayList<>();
        filteredUserList = new ArrayList<>();

        // Initialize Spinner
        statusFilterSpinner = findViewById(R.id.statusFilterSpinner);
        setupStatusFilterSpinner();

        // Initialize Google Map
        initMap();

        // Retrieve EVENT_ID from Intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("EVENT_ID")) {
            eventId = intent.getStringExtra("EVENT_ID");
            Log.d(TAG, "Event ID: " + eventId);
            fetchEntrants(eventId);
        } else {
            Toast.makeText(this, "No Event ID provided.", Toast.LENGTH_SHORT).show();
            finish();
        }

        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);
        // Adjust Navigation Drawer Menu Items Based on isAdmin
        if (isAdmin) {
            navigationView.getMenu().findItem(R.id.nav_manage_media).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_users).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_create_event).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_edit_facility).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_my_events).setVisible(true);
        }
        setupFirestoreListener();
        handleBackPressed();
    }

    /**
     * Sets up the Spinner with status options and defines its behavior.
     * Allows users to filter entrants based on their status.
     */
    private void setupStatusFilterSpinner() {
        // Define status options
        String[] statusOptions = {"All", "Waitlist", "Enrolled", "Canceled", "Chosen"};

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusOptions);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        statusFilterSpinner.setAdapter(adapter);

        // Set the default selection to "All"
        statusFilterSpinner.setSelection(0);

        // Set up the listener for selection changes
        statusFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedStatus = statusOptions[position];
                Log.d(TAG, "Selected Status Filter: " + selectedStatus);
                filterEntrantsByStatus(selectedStatus);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Default behavior when nothing is selected
                filterEntrantsByStatus("All");
            }
        });
    }

    /**
     * Initializes the Google Map by setting up the SupportMapFragment and registering the callback.
     */
    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this); // Register the callback
        } else {
            Log.e(TAG, "Map Fragment is null!");
            Toast.makeText(this, "Error initializing map.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Callback method when the Google Map is ready to be used.
     *
     * @param googleMap The GoogleMap instance.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Optional: Customize map settings
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        // Optionally, set a default location and zoom
        LatLng defaultLocation = new LatLng(0, 0); // Equator
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 2));

    }

    /**
     * Adds markers to the Google Map for each entrant based on their geopoint.
     *
     * @param entrantsLocationMap The map containing entrant IDs and their corresponding geopoints.
     * @param entrantsMap         The map containing entrant IDs and their statuses.
     */
    /**
     * Adds markers to the Google Map for each entrant based on their geopoint.
     *
     * @param entrantsLocationMap The map containing entrant IDs and their corresponding geopoints.
     * @param entrantsMap         The map containing entrant IDs and their statuses.
     */
    private void addEntrantsMarkers(Map<String, GeoPoint> entrantsLocationMap, Map<String, String> entrantsMap) {
        if (entrantsLocationMap == null || entrantsLocationMap.isEmpty()) {
            Log.w(TAG, "EntrantsLocationMap is null or empty.");
            return;
        }

        for (Map.Entry<String, GeoPoint> entry : entrantsLocationMap.entrySet()) {
            String entrantId = entry.getKey();
            GeoPoint geoPointObj = entry.getValue();

            if (geoPointObj instanceof com.google.firebase.firestore.GeoPoint) {
                com.google.firebase.firestore.GeoPoint geoPoint = (com.google.firebase.firestore.GeoPoint) geoPointObj;
                // Find the user in fullUserList
                User user = findUserById(entrantId);
                if (user != null) {
                    String name = user.getName() != null ? user.getName() : "Unnamed Entrant";

                    LatLng position = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());

                    // Add marker to the map
                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(position)
                            .title(name));

                    if (marker != null) {
                        // Store the marker in the map with entrantId as the key
                        entrantsMarkersMap.put(entrantId, marker);
                    }
                }
            } else {
                Log.w(TAG, "Invalid GeoPoint for entrantId: " + entrantId);
            }
        }

        // Optionally, move camera to show all markers
        moveCameraToShowAllMarkers();
    }

    /**
     * Finds a user in the fullUserList by their userId.
     *
     * @param userId The userId to search for.
     * @return The User object if found, else null.
     */
    private User findUserById(String userId) {
        for (User user : fullUserList) {
            if (user.getUserId().equals(userId)) {
                return user;
            }
        }
        return null;
    }

    /**
     * Moves the camera to show all markers on the map.
     * Adjusts the zoom level to encompass all entrants.
     */
    private void moveCameraToShowAllMarkers() {
        if (entrantsMarkersMap.isEmpty()) {
            Log.w(TAG, "No markers to display on the map.");
            return;
        }

        // Calculate the bounds
        double minLat = Double.MAX_VALUE;
        double minLng = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double maxLng = -Double.MAX_VALUE;

        for (Marker marker : entrantsMarkersMap.values()) {
            LatLng position = marker.getPosition();
            if (position.latitude < minLat) minLat = position.latitude;
            if (position.longitude < minLng) minLng = position.longitude;
            if (position.latitude > maxLat) maxLat = position.latitude;
            if (position.longitude > maxLng) maxLng = position.longitude;
        }

        // Compute the center
        double centerLat = (minLat + maxLat) / 2;
        double centerLng = (minLng + maxLng) / 2;

        // Move camera to center with appropriate zoom
        LatLng center = new LatLng(centerLat, centerLng);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 5)); // Adjust zoom level as needed
    }

    /**
     * Removes an entrant's marker from the map based on their userId.
     *
     * @param userId The userId of the entrant whose marker should be removed.
     */
    private void removeEntrantMarker(String userId) {
        Marker marker = entrantsMarkersMap.get(userId);
        if (marker != null) {
            marker.remove();
            entrantsMarkersMap.remove(userId);
            Log.d(TAG, "Removed marker for userId: " + userId);
        } else {
            Log.w(TAG, "No marker found for userId: " + userId);
        }
    }

    /**
     * Fetches the list of users from Firestore based on the event ID.
     * Populates the RecyclerView with entrant data and adds markers to the map.
     *
     * @param eventId The ID of the event.
     */
    private void fetchEntrants(String eventId) {
        firestore.collection("Events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        if (event != null) {
                            Map<String, String> entrantsMap = event.getEntrants();
                            Map<String, GeoPoint> entrantsLocationMap = event.getEntrantsLocation();

                            if (entrantsMap == null || entrantsMap.isEmpty()) {
                                Toast.makeText(this, "No entrants found.", Toast.LENGTH_SHORT).show();
                                fullUserList.clear();
                                filteredUserList.clear();
                                if (userAdapter != null) {
                                    userAdapter.notifyDataSetChanged();
                                }
                                if (mMap != null) {
                                    mMap.clear(); // Clear any existing markers
                                }
                                // Hide the map if geolocation is not required
                                mapContainer.setVisibility(View.GONE);
                                return;
                            }

                            // Log entrantsMap contents for debugging
                            Log.d(TAG, "Entrants Map:");
                            for (Map.Entry<String, String> entry : entrantsMap.entrySet()) {
                                Log.d(TAG, "Entrant ID: " + entry.getKey() + ", Status: " + entry.getValue());
                            }

                            List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();

                            for (Map.Entry<String, String> entry : entrantsMap.entrySet()) {
                                String entrantId = entry.getKey();
                                String status = entry.getValue();

                                // Fetch all entrants regardless of status
                                Task<DocumentSnapshot> userTask = firestore.collection("Users").document(entrantId).get();
                                userTasks.add(userTask);
                            }

                            // Wait for all user fetch tasks to complete
                            Tasks.whenAllSuccess(userTasks)
                                    .addOnSuccessListener(results -> {
                                        fullUserList.clear();
                                        for (Object result : results) {
                                            if (result instanceof DocumentSnapshot) {
                                                DocumentSnapshot userSnapshot = (DocumentSnapshot) result;
                                                User user = userSnapshot.toObject(User.class);
                                                if (user != null) {
                                                    user.setUserId(userSnapshot.getId());
                                                    fullUserList.add(user);
                                                    Log.d(TAG, "Fetched User: " + user.getUserId() + ", Name: " + user.getName());
                                                } else {
                                                    Log.w(TAG, "User document is null for ID: " + userSnapshot.getId());
                                                }
                                            }
                                        }

                                        // Initially, set filtered list to full list
                                        filteredUserList.clear();
                                        filteredUserList.addAll(fullUserList);

                                        // Initialize the UserAdapter
                                        userAdapter = new UserAdapter(filteredUserList, entrantsMap, this, this);
                                        waitingListRecyclerView.setAdapter(userAdapter);

                                        userAdapter.notifyDataSetChanged();

                                        if (fullUserList.isEmpty()) {
                                            Toast.makeText(EventWaitingListActivity.this, "No users found.", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(EventWaitingListActivity.this, "Loaded Users: " + fullUserList.size(), Toast.LENGTH_SHORT).show();
                                        }

                                        // After users are fetched, handle map visibility based on geolocationRequired
                                        if (event.isGeolocationRequired()) {
                                            mapContainer.setVisibility(View.VISIBLE);
                                            addEntrantsMarkers(entrantsLocationMap, entrantsMap);
                                        } else {
                                            mapContainer.setVisibility(View.GONE);
                                            if (mMap != null) {
                                                mMap.clear(); // Ensure map is clear if geolocation not required
                                            }
                                            entrantsMarkersMap.clear();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(EventWaitingListActivity.this, "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "Error fetching users: ", e);
                                    });
                        } else {
                            Toast.makeText(this, "Error parsing event data.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Event parsing returned null.");
                        }
                    } else {
                        Toast.makeText(this, "Event not found.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Event document does not exist.");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching event data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching event data: ", e);
                });
    }


    /**
     * Filters the list of entrants based on the selected status.
     *
     * @param status The status to filter by ("All", "Waitlist", "Enrolled", "Canceled", "Chosen").
     */
    private void filterEntrantsByStatus(String status) {
        if (userAdapter == null) {
            Log.w(TAG, "UserAdapter is null. Cannot filter entrants.");
            return;
        }

        filteredUserList.clear();

        if ("All".equalsIgnoreCase(status)) {
            filteredUserList.addAll(fullUserList);
        } else {
            for (User user : fullUserList) {
                // Retrieve the entrant's status from the entrants map
                String entrantStatus = userAdapter.getEntrantStatus(user.getUserId());

                if ("Waitlist".equalsIgnoreCase(status) && "waitlist".equalsIgnoreCase(entrantStatus)) {
                    filteredUserList.add(user);
                } else if ("Enrolled".equalsIgnoreCase(status) && "enrolled".equalsIgnoreCase(entrantStatus)) {
                    filteredUserList.add(user);
                } else if ("Canceled".equalsIgnoreCase(status) && "canceled".equalsIgnoreCase(entrantStatus)) {
                    filteredUserList.add(user);
                } else if ("Chosen".equalsIgnoreCase(status) && "chosen".equalsIgnoreCase(entrantStatus)) {
                    filteredUserList.add(user);
                }
            }
        }

        userAdapter.notifyDataSetChanged();

        Log.d(TAG, "Filtered Users Count: " + filteredUserList.size());
        Toast.makeText(this, "Filtered Users: " + filteredUserList.size(), Toast.LENGTH_SHORT).show();
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
            intent = new Intent(EventWaitingListActivity.this, NotificationsActivity.class);
        } else if (id == R.id.nav_edit_profile) {
            // Navigate to UserInfoActivity
            intent = new Intent(EventWaitingListActivity.this, UserInfoActivity.class);
            intent.putExtra("MODE", "EDIT");
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_manage_media) {

            intent = new Intent(EventWaitingListActivity.this, ManageMediaActivity.class);

        } else if (id == R.id.nav_manage_users) {

            intent = new Intent(EventWaitingListActivity.this, ManageUsersActivity.class);

        } else if (id == R.id.action_scan_qr) {
            // Handle QR code scanning
            intent = new Intent(EventWaitingListActivity.this, QRScanActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_create_event) {
            // Navigate to CreateEditEventActivity
            intent = new Intent(EventWaitingListActivity.this, CreateEditEventActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_edit_facility) {
            intent = new Intent(EventWaitingListActivity.this, CreateEditFacilityActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_my_events) {
            // Navigate to OrganizerHomeActivity and pass isAdmin flag
            intent = new Intent(EventWaitingListActivity.this, OrganizerHomeActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_view_joined_events) {
            // Navigate to EntrantHomeActivity and pass isAdmin flag
            intent = new Intent(EventWaitingListActivity.this, EntrantHomeActivity.class);
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
    /**
     * Handles the cancel action when the "Cancel" button is clicked in the UserAdapter.
     *
     * @param user The user to cancel.
     */
    @Override
    public void onCancelClick(User user) {
        // Show a confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Cancel Entrant")
                .setMessage("Are you sure you want to cancel this entrant?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    cancelEntrant(user);
                })
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Cancels the entrant by updating their status in Firestore and removes their marker from the map.
     *
     * @param user The user to cancel.
     */
    private void cancelEntrant(User user) {
        // Update the entrant's status in Firestore
        DocumentReference eventRef = firestore.collection("Events").document(eventId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("entrants." + user.getUserId(), "Canceled");
        updates.put("waitingListFilled", false); // Optionally set to false to refill the spot

        eventRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Entrant canceled successfully.", Toast.LENGTH_SHORT).show();
                    // Remove entrant's marker from the map
                    removeEntrantMarker(user.getUserId());
                    // Refresh the entrants list
                    fetchEntrants(eventId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error canceling entrant: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error canceling entrant: ", e);
                });
    }

    /**
     * Sets up a real-time listener to Firestore to listen for changes in entrants and update the map accordingly.
     */
    private void setupFirestoreListener() {
        firestore.collection("Events")
                .document(eventId)
                .addSnapshotListener(this, (documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed.", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Event updatedEvent = documentSnapshot.toObject(Event.class);
                        if (updatedEvent != null) {
                            Map<String, String> updatedEntrantsMap = updatedEvent.getEntrants();
                            Map<String, GeoPoint> updatedEntrantsLocationMap = updatedEvent.getEntrantsLocation();

                            // Update UI and Map by re-fetching entrants
                            fetchEntrants(eventId);
                        }
                    } else {
                        Log.d(TAG, "Current data: null");
                    }
                });
    }
}
