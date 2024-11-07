package com.example.potato1_events;

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
 * Activity to display the list of entrants on the waiting list for an event.
 */
public class EventWaitingListActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // UI Components
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    private RecyclerView waitingListRecyclerView;
    private UserAdapter userAdapter;
    private List<User> userList;

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
        userList = new ArrayList<>();



        // Retrieve EVENT_ID from Intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("EVENT_ID")) {
            eventId = intent.getStringExtra("EVENT_ID");
            fetchWaitingListUsers(eventId);
        } else {
            Toast.makeText(this, "No Event ID provided.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Fetches the list of users on the waiting list from Firestore.
     *
     * @param eventId The ID of the event.
     */
    private void fetchWaitingListUsers(String eventId) {
        firestore.collection("Events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        if (event != null) {
                            Map<String, String> entrantsMap = event.getEntrants();
                            if (entrantsMap == null || entrantsMap.isEmpty()) {
                                Toast.makeText(this, "No entrants on the waiting list.", Toast.LENGTH_SHORT).show();
                                userList.clear();
                                if (userAdapter != null) {
                                    userAdapter.notifyDataSetChanged();
                                }
                                return;
                            }

                            List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();

                            for (Map.Entry<String, String> entry : entrantsMap.entrySet()) {
                                String entrantId = entry.getKey();
                                String status = entry.getValue();

                                // Only include entrants with the desired status
                                if ("waitlist".equals(status)) {
                                    Task<DocumentSnapshot> userTask = firestore.collection("Entrants").document(entrantId).get();
                                    userTasks.add(userTask);
                                }
                            }

                            if (userTasks.isEmpty()) {
                                Toast.makeText(this, "No entrants on the waiting list.", Toast.LENGTH_SHORT).show();
                                userList.clear();
                                if (userAdapter != null) {
                                    userAdapter.notifyDataSetChanged();
                                }
                                return;
                            }

                            // Wait for all user fetch tasks to complete
                            Tasks.whenAllSuccess(userTasks)
                                    .addOnSuccessListener(results -> {
                                        userList.clear();
                                        for (Object result : results) {
                                            if (result instanceof DocumentSnapshot) {
                                                DocumentSnapshot userSnapshot = (DocumentSnapshot) result;
                                                User user = userSnapshot.toObject(User.class);
                                                if (user != null) {
                                                    user.setUserId(userSnapshot.getId());
                                                    userList.add(user);
                                                }
                                            }
                                        }

                                        // Initialize or update the UserAdapter with entrantsMap
                                        userAdapter = new UserAdapter(userList, entrantsMap, this);
                                        waitingListRecyclerView.setAdapter(userAdapter);

                                        userAdapter.notifyDataSetChanged();

                                        if (userList.isEmpty()) {
                                            Toast.makeText(EventWaitingListActivity.this, "No users found.", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(EventWaitingListActivity.this, "Loaded Users: " + userList.size(), Toast.LENGTH_SHORT).show();
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
     * Handles the back button press to close the drawer if it's open.
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
