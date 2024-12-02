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

/**
 * Activity to display and manage user notifications.
 * Allows users to view, accept, decline, and delete notifications.
 * Integrates with Firebase Firestore for real-time data synchronization.
 */
public class NotificationsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, NotificationsAdapter.OnNotificationActionListener {

    private static final String TAG = "NotificationsActivity";

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
     * RecyclerView to display the list of notifications.
     */
    private RecyclerView notificationsRecyclerView;

    /**
     * Adapter for the RecyclerView to manage notification items.
     */
    private NotificationsAdapter notificationsAdapter;

    /**
     * List to hold NotificationItem objects.
     */
    private List<NotificationItem> notificationList;

    // Firebase Firestore

    /**
     * FirebaseFirestore instance for database interactions.
     */
    private FirebaseFirestore firestore;

    /**
     * Identifier for the current user.
     */
    private String currentUserId;

    /**
     * Flag indicating if the user has admin privileges.
     */
    private boolean isAdmin = false;

    /**
     * ListenerRegistration for real-time Firestore updates.
     */
    private ListenerRegistration notificationsListener;

    /**
     * Called when the activity is first created.
     * Initializes UI components, Firebase instances, retrieves event details, and sets up listeners.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Retrieve the current user's unique ID
        currentUserId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Check if the user has admin privileges based on intent extras
        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);

        // Initialize UI Components by finding views by their IDs
        drawerLayout = findViewById(R.id.drawer_notifications_layout);
        navigationView = findViewById(R.id.nav_view_notifications);
        toolbar = findViewById(R.id.toolbar_notifications);
        setSupportActionBar(toolbar); // Set the toolbar as the app bar

        // Adjust Navigation Drawer Menu Items Based on isAdmin
        if (isAdmin) {
            navigationView.getMenu().findItem(R.id.nav_manage_media).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_users).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_create_event).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_edit_facility).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_my_events).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_events).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_manage_facilities).setVisible(true);
        }

        // Setup Navigation Drawer with toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle); // Add toggle as a listener to the drawer
        toggle.syncState(); // Synchronize the state of the drawer toggle
        navigationView.setNavigationItemSelectedListener(this); // Set the navigation item selected listener

        // Initialize RecyclerView for notifications
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this)); // Set layout manager
        notificationList = new ArrayList<>(); // Initialize the notification list
        notificationsAdapter = new NotificationsAdapter(notificationList, this, this); // Initialize the adapter
        notificationsRecyclerView.setAdapter(notificationsAdapter); // Set the adapter to the RecyclerView

        // Attach ItemTouchHelper for swipe actions (accept/decline/delete)
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                // We are not supporting move operation in this case
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Get the position of the swiped item
                int position = viewHolder.getAdapterPosition();
                NotificationItem notification = notificationList.get(position);
                // Handle the swipe action (e.g., delete the notification)
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

        itemTouchHelper.attachToRecyclerView(notificationsRecyclerView); // Attach the ItemTouchHelper to RecyclerView

        // Initialize and handle the push notifications switch
        Switch pushNotificationsSwitch = findViewById(R.id.switch_push_notifications);

        // Access SharedPreferences to retrieve and store push notification settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Load the saved state, default is true (enabled)
        boolean isPushEnabled = prefs.getBoolean("push_notifications_enabled", true);
        pushNotificationsSwitch.setChecked(isPushEnabled); // Set the switch state based on saved preferences

        // Set a listener to handle toggle changes for push notifications
        pushNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("push_notifications_enabled", isChecked); // Save the new state
            editor.apply(); // Apply the changes
            Toast.makeText(this, "Push notifications " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show(); // Inform the user
        });

        // Fetch Notifications from Firestore
        fetchNotifications();

        // Set up additional Firestore listeners if needed
        setupFirestoreListener(navigationView, toggle);
    }

    /**
     * Fetches notifications for the current user from Firestore.
     * Sets up a real-time listener to update the RecyclerView as notifications change.
     */
    private void fetchNotifications() {
        notificationsListener = firestore.collection("Notifications")
                .whereEqualTo("userId", currentUserId) // Query notifications for the current user
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        // Handle the error if fetching notifications fails
                        Toast.makeText(this, "Error fetching notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error fetching notifications", e);
                        return;
                    }

                    if (snapshots != null) {
                        notificationList.clear(); // Clear the existing list to avoid duplicates
                        for (QueryDocumentSnapshot doc : snapshots) {
                            NotificationItem notification = doc.toObject(NotificationItem.class); // Convert document to NotificationItem
                            notification.setId(doc.getId()); // Set the notification ID
                            notificationList.add(notification); // Add to the list
                        }
                        notificationsAdapter.notifyDataSetChanged(); // Notify the adapter of data changes
                    }
                });
    }

    /**
     * Handles navigation menu item selections.
     * Navigates to the corresponding activity based on the selected menu item.
     *
     * @param item The selected menu item.
     * @return True if the event was handled, false otherwise.
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Get the ID of the selected menu item
        int id = item.getItemId();
        Intent intent = null;

        // Determine which menu item was selected and create the corresponding Intent
        if (id == R.id.nav_notifications) {
            // Navigate to NotificationsActivity
            // Since we're already in NotificationsActivity, inform the user
            Toast.makeText(this, "Already on this page.", Toast.LENGTH_SHORT).show();
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
        } else if (id == R.id.nav_manage_events) {
            // Navigate to ManageEventsActivity (visible only to admins)
            intent = new Intent(NotificationsActivity.this, ManageEventsActivity.class);
        } else if (id == R.id.nav_manage_facilities) {
            // Navigate to ManageFacilitiesActivity (visible only to admins)
            intent = new Intent(NotificationsActivity.this, ManageFacilitiesActivity.class);
        } else if (id == R.id.action_scan_qr) {
            // Handle QR code scanning by navigating to QRScanActivity
            intent = new Intent(NotificationsActivity.this, QRScanActivity.class);
        } else if (id == R.id.nav_create_event) {
            // Navigate to CreateEditEventActivity for creating a new event
            intent = new Intent(NotificationsActivity.this, CreateEditEventActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_edit_facility) {
            // Navigate to CreateEditFacilityActivity for editing facilities
            intent = new Intent(NotificationsActivity.this, CreateEditFacilityActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_my_events) {
            // Navigate to OrganizerHomeActivity to view/manage own events
            intent = new Intent(NotificationsActivity.this, OrganizerHomeActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        } else if (id == R.id.nav_view_joined_events) {
            // Navigate to EntrantHomeActivity to view joined events
            intent = new Intent(NotificationsActivity.this, EntrantHomeActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
        }

        // Start the intended activity if an intent was created
        if (intent != null) {
            startActivity(intent);
        }

        // Close the navigation drawer after selection
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Callback method when a notification is accepted.
     * Updates the entrant's status to "Accepted" in Firestore.
     *
     * @param notification The notification item that was accepted.
     */
    @Override
    public void onAccept(NotificationItem notification) {
        // Handle accept action by updating entrant status
        updateEntrantStatus(notification, "Accepted");
    }

    /**
     * Callback method when a notification is declined.
     * Updates the entrant's status to "Declined" in Firestore.
     *
     * @param notification The notification item that was declined.
     */
    @Override
    public void onDecline(NotificationItem notification) {
        // Handle decline action by updating entrant status
        updateEntrantStatus(notification, "Declined");
    }

    /**
     * Updates the entrant's status in Firestore and sends a notification to the event organizer.
     *
     * @param notification The notification item being acted upon.
     * @param status       The new status ("Accepted" or "Declined").
     */
    private void updateEntrantStatus(NotificationItem notification, String status) {
        // Retrieve event ID and user ID from the notification
        String eventId = notification.getEventId();
        String userId = notification.getUserId();

        // Update the entrant's status in the "Events" collection
        firestore.collection("Events").document(eventId)
                .update("entrants." + userId, status) // Update the specific entrant's status
                .addOnSuccessListener(aVoid -> {
                    // Inform the user of successful status update
                    Toast.makeText(this, "You have " + status.toLowerCase() + " the invitation.", Toast.LENGTH_SHORT).show();
                    // Send a notification to the event organizer about the entrant's response
                    sendOrganizerNotification(eventId, userId, status);
                    // Mark the notification as read (delete it)
                    markNotificationAsRead(notification);
                })
                .addOnFailureListener(e -> {
                    // Handle failure scenarios and inform the user
                    Toast.makeText(this, "Error updating status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating entrant status", e);
                });
    }

    /**
     * Sends a notification to the event organizer indicating the entrant's response.
     *
     * @param eventId The ID of the event.
     * @param userId  The ID of the user who responded.
     * @param status  The status response ("Accepted" or "Declined").
     */
    private void sendOrganizerNotification(String eventId, String userId, String status) {
        // Fetch the event document to retrieve organizer details
        firestore.collection("Events").document(eventId)
                .get()
                .addOnSuccessListener(eventSnapshot -> {
                    if (eventSnapshot.exists()) {
                        // Retrieve the organizer's user ID (assuming it's stored as "facilityId")
                        String organizerId = eventSnapshot.getString("facilityId");
                        String eventName = eventSnapshot.getString("name");
                        if (organizerId != null) {
                            // Fetch entrant's name using the getUserName method
                            getUserName(userId, entrantName -> {
                                // Create a new notification for the organizer
                                NotificationItem notification = new NotificationItem();
                                notification.setTitle("Entrant " + status);
                                notification.setMessage(entrantName + " has " + status.toLowerCase() + " the invitation for the event " + eventName);
                                notification.setEventId(eventId);
                                notification.setUserId(organizerId);
                                notification.setType("entrant_response");
                                notification.setRead(false);

                                // Add the notification to Firestore
                                firestore.collection("Notifications").add(notification)
                                        .addOnSuccessListener(docRef -> {
                                            Log.d(TAG, "Organizer notification saved with ID: " + docRef.getId());
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error saving organizer notification", e);
                                        });
                            });
                        } else {
                            Log.w(TAG, "Organizer ID is null for event: " + eventId);
                        }
                    } else {
                        Log.w(TAG, "Event document does not exist for eventId: " + eventId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching event document for eventId: " + eventId, e);
                });
    }

    /**
     * Marks a notification as read by deleting it from Firestore.
     *
     * @param notification The notification item to mark as read.
     */
    private void markNotificationAsRead(NotificationItem notification) {
        // Delete the notification document from Firestore
        firestore.collection("Notifications").document(notification.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // No need to manually remove the notification from the list; the listener will handle it
                })
                .addOnFailureListener(e -> {
                    // Inform the user if deletion fails
                    Toast.makeText(this, "Error deleting notification: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error deleting notification", e);
                });
    }

    /**
     * Deletes a notification from Firestore.
     *
     * @param notification The notification item to delete.
     * @param position     The position of the notification in the RecyclerView.
     */
    private void deleteNotification(NotificationItem notification, int position) {
        // Delete the notification document from Firestore
        firestore.collection("Notifications").document(notification.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Inform the user of successful deletion
                    Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show();
                    // The real-time listener will handle removing the notification from the list
                })
                .addOnFailureListener(e -> {
                    // If deletion fails, notify the adapter to rebind the item and inform the user
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
        // Retrieve the user document from Firestore
        firestore.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get the 'name' field from the user document
                        String name = documentSnapshot.getString("name");
                        if (name != null && !name.isEmpty()) {
                            callback.onNameReceived(name); // Return the retrieved name
                        } else {
                            callback.onNameReceived("Unknown User"); // Fallback name if not available
                        }
                    } else {
                        callback.onNameReceived("Unknown User"); // Fallback name if document doesn't exist
                        Log.w(TAG, "No such document for userId: " + userId);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle failure in fetching user name
                    callback.onNameReceived("Unknown User"); // Fallback name
                    Log.e(TAG, "Error fetching user name for userId: " + userId, e);
                });
    }

    /**
     * Interface for handling asynchronous name retrieval.
     */
    private interface NameCallback {
        /**
         * Called when the user's name has been retrieved.
         *
         * @param name The retrieved name of the user.
         */
        void onNameReceived(String name);
    }

    /**
     * Cleans up the Firestore listener when the activity is destroyed to prevent memory leaks.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationsListener != null) {
            notificationsListener.remove(); // Remove the listener to stop receiving updates
            notificationsListener = null; // Nullify the listener reference
        }
    }

    /**
     * Sets up a real-time listener to Firestore to listen for changes in the user's admin status.
     * Updates the navigation menu based on admin status changes.
     *
     * @param navigationView The NavigationView to update menu items.
     * @param toggle         The ActionBarDrawerToggle to sync state.
     */
    private void setupFirestoreListener(NavigationView navigationView, ActionBarDrawerToggle toggle) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Attach a snapshot listener to the current user's document
        firestore.collection("Users")
                .document(currentUserId)
                .addSnapshotListener(this, (documentSnapshot, e) -> {
                    if (e != null) {
                        // Handle the error if listening fails
                        Log.e(TAG, "Listen failed.", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // Retrieve the 'admin' field from the user document
                        Boolean admin = documentSnapshot.getBoolean("admin");

                        if (admin != null && admin != isAdmin) {
                            // Update the isAdmin variable if there's a change
                            isAdmin = admin;

                            // Update the navigation menu based on the new isAdmin value
                            runOnUiThread(() -> {
                                if (isAdmin) {
                                    // Show admin-specific menu items
                                    navigationView.getMenu().findItem(R.id.nav_manage_media).setVisible(true);
                                    navigationView.getMenu().findItem(R.id.nav_manage_users).setVisible(true);
                                    navigationView.getMenu().findItem(R.id.nav_manage_facilities).setVisible(true);
                                    navigationView.getMenu().findItem(R.id.nav_manage_events).setVisible(true);
                                } else {
                                    // Hide admin-specific menu items
                                    navigationView.getMenu().findItem(R.id.nav_manage_media).setVisible(false);
                                    navigationView.getMenu().findItem(R.id.nav_manage_users).setVisible(false);
                                    navigationView.getMenu().findItem(R.id.nav_manage_facilities).setVisible(false);
                                    navigationView.getMenu().findItem(R.id.nav_manage_events).setVisible(false);
                                }
                                // Sync the toggle state to reflect menu changes
                                toggle.syncState();
                            });
                        }
                    }
                });
    }
}
