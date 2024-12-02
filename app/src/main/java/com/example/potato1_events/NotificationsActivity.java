// File: NotificationsActivity.java
package com.example.potato1_events;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
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
    private ListenerRegistration notificationsListener;

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

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                // We are not supporting move operation in this case
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                NotificationItem notification = notificationList.get(position);
                deleteNotification(notification, position);
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                // Optionally, you can customize the swipe background and icon here
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        });

        itemTouchHelper.attachToRecyclerView(notificationsRecyclerView);
        Switch pushNotificationsSwitch = findViewById(R.id.switch_push_notifications);

        // Access SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Load the saved state, default is true (enabled)
        boolean isPushEnabled = prefs.getBoolean("push_notifications_enabled", true);
        pushNotificationsSwitch.setChecked(isPushEnabled);

        // Set a listener to handle toggle changes
        pushNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("push_notifications_enabled", isChecked);
            editor.apply();
            Toast.makeText(this, "Push notifications " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });
        // Fetch Notifications
        fetchNotifications();

    }

    private void fetchNotifications() {
        notificationsListener = firestore.collection("Notifications")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Error fetching notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error fetching notifications", e);
                        return;
                    }

                    if (snapshots != null) {
                        notificationList.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            NotificationItem notification = doc.toObject(NotificationItem.class);
                            notification.setId(doc.getId());
                            notificationList.add(notification);
                        }
                        notificationsAdapter.notifyDataSetChanged();
                    }
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
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot eventSnapshot) {
                        if (eventSnapshot.exists()) {
                            String organizerId = eventSnapshot.getString("facilityId");
                            String eventName = eventSnapshot.getString("name");
                            if (organizerId != null) {
                                // Fetch entrant's name using the getUserName method
                                getUserName(userId, new NameCallback() {
                                    @Override
                                    public void onNameReceived(String entrantName) {
                                        // Create notification for organizer with entrant's name
                                        NotificationItem notification = new NotificationItem();
                                        notification.setTitle("Entrant " + status);
                                        notification.setMessage(entrantName + " has " + status.toLowerCase() + " the invitation for the event " + eventName);
                                        notification.setEventId(eventId);
                                        notification.setUserId(organizerId);
                                        notification.setType("entrant_response");
                                        notification.setRead(false);

                                        firestore.collection("Notifications").add(notification)
                                                .addOnSuccessListener(docRef -> {
                                                    Log.d(TAG, "Organizer notification saved with ID: " + docRef.getId());
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "Error saving organizer notification", e);
                                                });
                                    }
                                });
                            } else {
                                Log.w(TAG, "Organizer ID is null for event: " + eventId);
                            }
                        } else {
                            Log.w(TAG, "Event document does not exist for eventId: " + eventId);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching event document for eventId: " + eventId, e);
                });
    }

    private void markNotificationAsRead(NotificationItem notification) {
        firestore.collection("Notifications").document(notification.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // No need to manually remove the notification; the listener will handle it
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting notification: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error deleting notification", e);
                });
    }
    private void deleteNotification(NotificationItem notification, int position) {
        firestore.collection("Notifications").document(notification.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // The listener will handle the removal, so no need to update the list here
                    Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // If deletion fails, we need to notify the adapter to rebind the item
                    notificationsAdapter.notifyItemChanged(position);
                    Toast.makeText(this, "Error deleting notification: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error deleting notification", e);
                });
    }
    /**
     * Fetches the user's name from Firestore given their userId.
     *
     * @param userId   The ID of the user whose name is to be fetched.
     * @param callback The callback to handle the retrieved name.
     */
    private void getUserName(String userId, final NameCallback callback) {
        firestore.collection("Users").document(userId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            if (name != null && !name.isEmpty()) {
                                callback.onNameReceived(name);
                            } else {
                                callback.onNameReceived("Unknown User"); // Fallback name
                            }
                        } else {
                            callback.onNameReceived("Unknown User"); // Fallback name
                            Log.w(TAG, "No such document for userId: " + userId);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        callback.onNameReceived("Unknown User"); // Fallback name
                        Log.e(TAG, "Error fetching user name for userId: " + userId, e);
                    }
                });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationsListener != null) {
            notificationsListener.remove();
            notificationsListener = null;
        }
    }
}