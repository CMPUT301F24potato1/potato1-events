// File: RandomDrawListener.java
package com.example.potato1_events;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.Timestamp;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Class that sets up a real-time listener for events eligible for random draws.
 * This ensures immediate processing when such events are detected.
 */
public class RandomDrawListener {

    private static final String TAG = "RandomDrawListener";
    private static final long INITIAL_DELAY = 0;
    private static final long PERIOD = 1; // in minutes

    private FirebaseFirestore firestore;
    private Context context;
    private ListenerRegistration listenerRegistration;
    private ScheduledExecutorService scheduler;

    public RandomDrawListener(Context context) {
        this.context = context.getApplicationContext();
        firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Starts the real-time listener.
     */
    public void startListening() {
        // Listen for events where randomDrawPerformed is false and registrationEnd <= now
        firestore.collection("Events")
                .whereEqualTo("randomDrawPerformed", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            DocumentSnapshot doc = dc.getDocument();
                            handleEventChange(dc.getType(), doc);
                        }
                    }
                });
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                performRandomDraw("SOMETHING");
            }
        }, INITIAL_DELAY, PERIOD, TimeUnit.MINUTES);
        Log.d(TAG, "RandomDrawListener started with schedule every " + PERIOD + " minute(s).");
    }

    /**
     * Handles changes detected by the listener.
     *
     * @param changeType The type of document change.
     * @param doc        The document snapshot.
     */
    private void handleEventChange(DocumentChange.Type changeType, DocumentSnapshot doc) {
        String eventId = doc.getId();
        Timestamp registrationEnd = doc.getTimestamp("registrationEnd");

        if (registrationEnd == null) {
            Log.w(TAG, "registrationEnd is null for event: " + eventId);
            return;
        }

        Timestamp now = Timestamp.now();
        if (registrationEnd.toDate().before(now.toDate()) || registrationEnd.toDate().equals(now.toDate())) {
            switch (changeType) {
                case ADDED:
                case MODIFIED:
                    Log.d(TAG, "Eligible event detected for random draw: " + eventId);
                    performRandomDraw(eventId);
                    break;
                case REMOVED:
                    // No action needed
                    break;
            }
        }
    }

    /**
     * Enqueues a RandomDrawWorker to perform a random draw for the specified event.
     *
     * @param eventId   The ID of the event.
     */
    private void performRandomDraw(String eventId) {
        Log.d(TAG, "Enqueuing RandomDrawWorker for event: " + eventId);

//        // Prepare input data with eventId
//        Data inputData = new Data.Builder()
//                .putString("eventId", eventId)
//                .build();

        // Create a OneTimeWorkRequest with the eventId
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(RandomDrawWorker.class)
                .build();

        // Enqueue the work
        WorkManager.getInstance(context).enqueue(workRequest);
    }

    /**
     * Manually triggers a random draw for a specific event.
     *
     * @param eventId The ID of the event.
     */
    public void triggerManualRandomDraw(String eventId) {
        Log.d(TAG, "Manually triggering RandomDrawWorker for event: " + eventId);

        // Prepare input data with eventId
        Data inputData = new Data.Builder()
                .putString("eventId", eventId)
                .build();

        // Create a OneTimeWorkRequest with the eventId
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(RandomDrawWorker.class)
                .setInputData(inputData)
                .build();

        // Enqueue the work
        WorkManager.getInstance(context).enqueue(workRequest);
    }

    /**
     * Stops the real-time listener.
     */
    public void stopListening() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }
}