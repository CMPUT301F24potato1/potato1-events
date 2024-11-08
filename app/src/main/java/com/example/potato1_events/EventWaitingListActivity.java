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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity to display the list of entrants on the waiting list for an event with filtering capabilities.
 */
public class EventWaitingListActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // UI Components
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    private RecyclerView waitingListRecyclerView;
    private UserAdapter userAdapter;
    private List<User> fullUserList; // Holds all users fetched
    private List<User> filteredUserList; // Holds users after filtering

    private Spinner statusFilterSpinner;

    // Firebase Firestore
    private FirebaseFirestore firestore;

    // Event Data
    private String eventId;

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
            fetchEntrants(eventId);
        } else {
            Toast.makeText(this, "No Event ID provided.", Toast.LENGTH_SHORT).show();
            finish();
        }

        handleBackPressed();
    }

    /**
     * Sets up the Spinner with status options and defines its behavior.
     */
    private void setupStatusFilterSpinner() {
        // Define status options
        String[] statusOptions = {"All", "Waitlist", "Enrolled"};

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

                            List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();

                            for (Map.Entry<String, String> entry : entrantsMap.entrySet()) {
                                String entrantId = entry.getKey();
                                String status = entry.getValue();

                                // Fetch all entrants regardless of status
                                Task<DocumentSnapshot> userTask = firestore.collection("Entrants").document(entrantId).get();
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
                                    });
                        } else {
                            Toast.makeText(this, "Error parsing event data.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Event not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching event data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    /**
     * Filters the list of entrants based on the selected status.
     *
     * @param status The status to filter by ("All", "Waitlist", "Enrolled").
     */
    private void filterEntrantsByStatus(String status) {
        if (userAdapter == null) {
            return;
        }

        filteredUserList.clear();

        if ("All".equalsIgnoreCase(status)) {
            filteredUserList.addAll(fullUserList);
        } else {
            for (User user : fullUserList) {
                // Retrieve the entrant's status from the entrants map
                String entrantStatus = userAdapter.getEntrantStatus(user.getUserId());

                if (status.equalsIgnoreCase("Waitlist") && "waitlist".equalsIgnoreCase(entrantStatus)) {
                    filteredUserList.add(user);
                } else if (status.equalsIgnoreCase("Enrolled") && "enrolled".equalsIgnoreCase(entrantStatus)) {
                    filteredUserList.add(user);
                }
            }
        }

        userAdapter.notifyDataSetChanged();

        Toast.makeText(this, "Filtered Users: " + filteredUserList.size(), Toast.LENGTH_SHORT).show();
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

        if (id == R.id.nav_organizer_profile) {
            Intent intent = new Intent(EventWaitingListActivity.this, UserInfoActivity.class);
            intent.putExtra("USER_TYPE", "Organizer");
            intent.putExtra("MODE", "EDIT");
            startActivity(intent);
        } else if (id == R.id.nav_create_event) {
            Intent intent = new Intent(EventWaitingListActivity.this, CreateEditEventActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_edit_facility) {
            Intent intent = new Intent(EventWaitingListActivity.this, CreateEditFacilityActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_my_events) {
            Intent intent = new Intent(EventWaitingListActivity.this, OrganizerHomeActivity.class);
            startActivity(intent);
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
