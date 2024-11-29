// File: NotificationsActivity.java
package com.example.potato1_events;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, NotificationsAdapter.OnNotificationActionListener {

    private static final String TAG = "NotificationsActivity";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    private RecyclerView notificationsRecyclerView;
    private NotificationsAdapter notificationsAdapter;
    private List<NotificationItem> notificationList;

    private FirebaseFirestore firestore;
    private String currentUserId;

    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();
        currentUserId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);
        // Initialize UI Components
        drawerLayout = findViewById(R.id.drawer_notifications_layout);
        navigationView = findViewById(R.id.nav_view_notifications);
        toolbar = findViewById(R.id.toolbar_notifications);
        setSupportActionBar(toolbar);

        if (isAdmin) {
            navigationView.getMenu().findItem(R.id.nav_manage_media).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_users).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_create_event).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_edit_facility).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_my_events).setVisible(true);
        }

        // Setup Navigation Drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize RecyclerView
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationList = new ArrayList<>();
        notificationsAdapter = new NotificationsAdapter(notificationList, this, this);
        notificationsRecyclerView.setAdapter(notificationsAdapter);

        // Fetch Notifications
        fetchNotifications();
    }

    private void fetchNotifications() {
        firestore.collection("Notifications")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        NotificationItem notification = doc.toObject(NotificationItem.class);
                        notification.setId(doc.getId());
                        notificationList.add(notification);
                    }
                    notificationsAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching notifications", e);
                });
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

        if (id == R.id.nav_view_joined_events) {
            // Navigate to NotificationsActivity
            // Uncomment and implement if NotificationsActivity exists
            intent = new Intent(NotificationsActivity.this, EntrantHomeActivity.class);
        } else if (id == R.id.nav_edit_profile) {
            // Navigate to UserInfoActivity in EDIT mode
            intent = new Intent(NotificationsActivity.this, UserInfoActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
            intent.putExtra("MODE", "EDIT");
        } else if (id == R.id.nav_manage_media) {
            // Navigate to ManageMediaActivity (visible only to admins)
            intent = new Intent(NotificationsActivity.this, ManageMediaActivity.class);
        } else if (id == R.id.nav_manage_users) {
            // Navigate to ManageUsersActivity (visible only to admins)
            intent = new Intent(NotificationsActivity.this, ManageUsersActivity.class);
        } else if (id == R.id.action_scan_qr) {
            intent = new Intent(NotificationsActivity.this, QRScanActivity.class);
        } else if (id == R.id.nav_create_event) {

            intent = new Intent(NotificationsActivity.this, CreateEditEventActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_edit_facility) {

            intent = new Intent(NotificationsActivity.this, CreateEditFacilityActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_my_events) {

            intent = new Intent(NotificationsActivity.this, OrganizerHomeActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_notifications) {
            Toast.makeText(this, "Already on this page.", Toast.LENGTH_SHORT).show();
        }

        if (intent != null) {
            startActivity(intent);
        }


        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onAccept(NotificationItem notification) {
        // Handle accept action
        updateEntrantStatus(notification, "Accepted");
    }

    @Override
    public void onDecline(NotificationItem notification) {
        // Handle decline action
        updateEntrantStatus(notification, "Declined");
    }

    private void updateEntrantStatus(NotificationItem notification, String status) {
        // Update entrant status in Firestore
        String eventId = notification.getEventId();
        String userId = notification.getUserId();

        firestore.collection("Events").document(eventId)
                .update("entrants." + userId, status)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "You have " + status.toLowerCase() + " the invitation.", Toast.LENGTH_SHORT).show();
                    // Send notification to organizer
                    sendOrganizerNotification(eventId, userId, status);
                    // Mark notification as read
                    markNotificationAsRead(notification);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating entrant status", e);
                });
    }

    private void sendOrganizerNotification(String eventId, String userId, String status) {
        // Get event organizer's userId
        firestore.collection("Events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String organizerId = documentSnapshot.getString("organizerId");
                        if (organizerId != null) {
                            // Create notification for organizer
                            NotificationItem notification = new NotificationItem();
                            notification.setTitle("Entrant " + status);
                            notification.setMessage("User has " + status.toLowerCase() + " the invitation.");
                            notification.setEventId(eventId);
                            notification.setUserId(organizerId);
                            notification.setType("entrant_response");
                            notification.setRead(false);

                            firestore.collection("Notifications").add(notification);
                        }
                    }
                });
    }

    private void markNotificationAsRead(NotificationItem notification) {
        firestore.collection("Notifications").document(notification.getId())
                .update("read", true)
                .addOnSuccessListener(aVoid -> {
                    notification.setRead(true);
                    notificationsAdapter.notifyDataSetChanged();
                });
    }
}