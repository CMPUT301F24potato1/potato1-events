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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Activity to display the list of entrants on the waiting list for an event with filtering capabilities.
 * Allows organizers to view and manage entrants based on their status.
 */
public class EventWaitingListActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

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
     * Fetches the list of users from Firestore based on the event ID.
     * Populates the RecyclerView with entrant data.
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
                            if (entrantsMap == null || entrantsMap.isEmpty()) {
                                Toast.makeText(this, "No entrants found.", Toast.LENGTH_SHORT).show();
                                fullUserList.clear();
                                filteredUserList.clear();
                                if (userAdapter != null) {
                                    userAdapter.notifyDataSetChanged();
                                }
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
                                        userAdapter = new UserAdapter(filteredUserList, entrantsMap, this);
                                        waitingListRecyclerView.setAdapter(userAdapter);

                                        userAdapter.notifyDataSetChanged();

                                        if (fullUserList.isEmpty()) {
                                            Toast.makeText(EventWaitingListActivity.this, "No users found.", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(EventWaitingListActivity.this, "Loaded Users: " + fullUserList.size(), Toast.LENGTH_SHORT).show();
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
            // intent = new Intent(EventWaitingListActivity.this, NotificationsActivity.class);
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
}
