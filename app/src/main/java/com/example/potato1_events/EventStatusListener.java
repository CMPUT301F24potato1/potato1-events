// File: EventStatusListener.java
package com.example.potato1_events;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that listens for status changes in events the user has joined and events they have organized.
 */
public class EventStatusListener {

    private static final String TAG = "EventStatusListener";
    private static final String CHANNEL_ID = "event_status_notifications_channel";

    private FirebaseFirestore firestore;
    private String currentUserId;
    private Context context;
    private List<ListenerRegistration> listenerRegistrations;

    // Map to keep track of previous waitingListFilled values for organizer events
    private Map<String, Boolean> previousWaitingListFilledValues;

    public EventStatusListener(Context context) {
        this.context = context.getApplicationContext();
        firestore = FirebaseFirestore.getInstance();
        currentUserId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        listenerRegistrations = new ArrayList<>();
        previousWaitingListFilledValues = new HashMap<>();
        createNotificationChannel();
    }

    /**
     * Starts listening to events the user has joined and organized.
     */
    public void startListening() {
        // List of possible statuses
        String[] statuses = {"Waitlist", "Selected", "Not Selected", "Accepted", "Declined"};

        // Loop through each status and set up a listener for events the user has joined
        for (String status : statuses) {
            ListenerRegistration registration = firestore.collection("Events")
                    .whereEqualTo("entrants." + currentUserId, status)
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null) {
                            Log.w(TAG, "Listen failed for status " + status, e);
                            return;
                        }

                        if (snapshots != null) {
                            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                DocumentSnapshot document = dc.getDocument();
                                handleStatusChange(document);
                            }
                        }
                    });
            listenerRegistrations.add(registration);
        }

        // Set up a listener for events the user has organized
        ListenerRegistration organizerRegistration = firestore.collection("Events")
                .whereEqualTo("facilityId", currentUserId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Organizer listen failed.", e);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            DocumentSnapshot document = dc.getDocument();
                            handleOrganizerEventChange(document);
                        }
                    }
                });
        listenerRegistrations.add(organizerRegistration);
    }

    /**
     * Handles the status change of the user in an event they've joined.
     *
     * @param eventSnapshot The snapshot of the event document.
     */
    private void handleStatusChange(DocumentSnapshot eventSnapshot) {
        String eventId = eventSnapshot.getId();
        String eventName = eventSnapshot.getString("name");

        // Get the user's status in this event
        String status = (String) eventSnapshot.get("entrants." + currentUserId);

        if (status != null) {
            Log.d(TAG, "Status changed to " + status + " for event " + eventId);
            // Create a notification
            createNotification(eventId, eventName, status);
            // Optionally, add to in-app notifications
            saveNotificationToFirestore(eventId, eventName, status);
        }
    }

    /**
     * Handles changes in events the user has organized.
     * Specifically, listens for changes in the 'waitingListFilled' field.
     *
     * @param eventSnapshot The snapshot of the event document.
     */
    private void handleOrganizerEventChange(DocumentSnapshot eventSnapshot) {
        String eventId = eventSnapshot.getId();
        String eventName = eventSnapshot.getString("name");

        // Get the new 'waitingListFilled' value
        Boolean waitingListFilled = eventSnapshot.getBoolean("waitingListFilled");

        // Get the previous value
        Boolean previousValue = previousWaitingListFilledValues.get(eventId);

        // If previousValue is null, this is the first time we see this event
        if (previousValue == null) {
            // Store the current value and do not send a notification
            previousWaitingListFilledValues.put(eventId, waitingListFilled);
            Log.d(TAG, "Initial waitingListFilled value for event " + eventId + ": " + waitingListFilled);
        } else {
            // Compare previous and current values
            if (!previousValue.equals(waitingListFilled)) {
                // Value has changed
                Log.d(TAG, "waitingListFilled changed for event " + eventId + ": " + previousValue + " -> " + waitingListFilled);

                // If waitingListFilled is now true, send a notification
                if (waitingListFilled != null && waitingListFilled) {
                    createOrganizerNotification(eventId, eventName);
                    saveOrganizerNotificationToFirestore(eventId, eventName);
                }

                // Update the previous value
                previousWaitingListFilledValues.put(eventId, waitingListFilled);
            }
        }
    }

    /**
     * Creates a system notification for status changes.
     *
     * @param eventId   The ID of the event.
     * @param eventName The name of the event.
     * @param status    The new status of the user.
     */
    private void createNotification(String eventId, String eventName, String status) {
        Intent intent = new Intent(context, NotificationsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("EVENT_ID", eventId);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = "Event Status Update";
        String message = "Your status for event \"" + eventName + "\" has changed to " + status + ".";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications) // Ensure this icon exists
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        int notificationId = eventId.hashCode(); // Unique ID per event

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this.context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Notification permission not granted.");
                return;
            }
        }
        notificationManager.notify(notificationId, builder.build());
        Log.d(TAG, "Notification displayed for event: " + eventId);
    }

    /**
     * Saves the notification to Firestore for in-app display.
     *
     * @param eventId   The ID of the event.
     * @param eventName The name of the event.
     * @param status    The new status of the user.
     */
    private void saveNotificationToFirestore(String eventId, String eventName, String status) {
        NotificationItem notification = new NotificationItem();
        notification.setTitle("Event Status Update");
        notification.setMessage("Your status for event \"" + eventName + "\" has changed to " + status + ".");
        notification.setEventId(eventId);
        notification.setUserId(currentUserId);
        notification.setType("status_change");
        notification.setRead(false);

        firestore.collection("Notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Notification saved to Firestore with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving notification to Firestore", e);
                });
    }

    /**
     * Creates a system notification for the organizer when the random draw has been performed.
     *
     * @param eventId   The ID of the event.
     * @param eventName The name of the event.
     */
    private void createOrganizerNotification(String eventId, String eventName) {
        Intent intent = new Intent(context, NotificationsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("EVENT_ID", eventId);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = "Random Draw Performed";
        String message = "Random draw has been performed for your event \"" + eventName + "\".";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications) // Ensure this icon exists
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        int notificationId = eventId.hashCode(); // Unique ID per event

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this.context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Notification permission not granted.");
                return;
            }
        }
        notificationManager.notify(notificationId, builder.build());
        Log.d(TAG, "Organizer notification displayed for event: " + eventId);
    }

    /**
     * Saves the organizer notification to Firestore for in-app display.
     *
     * @param eventId   The ID of the event.
     * @param eventName The name of the event.
     */
    private void saveOrganizerNotificationToFirestore(String eventId, String eventName) {
        NotificationItem notification = new NotificationItem();
        notification.setTitle("Random Draw Performed");
        notification.setMessage("Random draw has been performed for your event \"" + eventName + "\".");
        notification.setEventId(eventId);
        notification.setUserId(currentUserId);
        notification.setType("organizer_update");
        notification.setRead(false);

        firestore.collection("Notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Organizer notification saved to Firestore with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving organizer notification to Firestore", e);
                });
    }

    /**
     * Creates a notification channel for Android O and above.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Event Status Notifications";
            String description = "Notifications for event status updates";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Stops listening to events and removes all listeners.
     */
    public void stopListening() {
        for (ListenerRegistration registration : listenerRegistrations) {
            registration.remove();
        }
        listenerRegistrations.clear();
    }
}