// File: RandomDrawWorker.java
package com.example.potato1_events; // Replace with your actual package name

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;

import java.util.*;

/**
 * Worker class responsible for performing random draws for events.
 * It checks for events that have ended their registration period and haven't had a random draw performed yet.
 * For each such event, it selects entrants from the waiting list based on available slots and updates their status.
 */
public class RandomDrawWorker extends Worker {

    // Tag for logging purposes
    private static final String TAG = "RandomDrawWorker";

    /**
     * Constructs a new RandomDrawWorker.
     *
     * @param context      The application context.
     * @param params Parameters for the worker.
     */
    public RandomDrawWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    /**
     * Executes the work to perform random draws for eligible events.
     *
     * @return The result of the work, indicating success or failure.
     */
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "RandomDrawWorker is running");
        performRandomDraw();
        return Result.success();
    }

    /**
     * Initiates the random draw process by querying eligible events and processing each one.
     * An eligible event is one where the registration period has ended and a random draw hasn't been performed yet.
     */
    private void performRandomDraw() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Get current timestamp
        Timestamp now = Timestamp.now();

        // Query events where registration has ended and random draw hasn't been performed
        firestore.collection("Events")
                .whereLessThanOrEqualTo("registrationEnd", now)
                .whereEqualTo("randomDrawPerformed", false) // Ensure only relevant events are processed
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No events require random draw at this time.");
                        return;
                    }

                    for (DocumentSnapshot eventDoc : queryDocumentSnapshots.getDocuments()) {
                        // Perform random draw for each eligible event
                        processEventRandomDraw(eventDoc);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error querying events for random draw", e);
                });
    }

    /**
     * Processes the random draw for a specific event.
     * Updates entrant statuses based on available slots and marks the random draw as performed.
     *
     * @param eventDoc The DocumentSnapshot representing the event to process.
     */
    private void processEventRandomDraw(DocumentSnapshot eventDoc) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference eventRef = eventDoc.getReference();
        String eventId = eventDoc.getId();

        // Use a transaction to ensure atomicity of the random draw operation
        firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);

            Boolean randomDrawPerformed = snapshot.getBoolean("randomDrawPerformed");
            if (randomDrawPerformed != null && randomDrawPerformed) {
                // Random draw already performed for this event
                Log.d(TAG, "Random draw already performed for event: " + eventId);
                return null;
            }

            Map<String, Object> eventData = snapshot.getData();
            if (eventData == null) {
                Log.e(TAG, "Event data is null for event: " + eventId);
                return null;
            }

            // Retrieve entrants map from event data
            Map<String, String> entrantsMap = (Map<String, String>) eventData.get("entrants");
            if (entrantsMap == null || entrantsMap.isEmpty()) {
                Log.d(TAG, "No entrants for event: " + eventId);

                // Mark random draw as performed even if there are no entrants
                transaction.update(eventRef, "randomDrawPerformed", true);
                return null;
            }

            // Collect entrant IDs with status "On Waiting List"
            List<String> waitlistEntrantIds = new ArrayList<>();
            for (Map.Entry<String, String> entry : entrantsMap.entrySet()) {
                if ("On Waiting List".equalsIgnoreCase(entry.getValue())) {
                    waitlistEntrantIds.add(entry.getKey());
                }
            }

            if (waitlistEntrantIds.isEmpty()) {
                Log.d(TAG, "No waitlist entrants for event: " + eventId);
                // Mark random draw as performed since there are no waitlist entrants
                transaction.update(eventRef, "randomDrawPerformed", true);
                return null;
            }

            // Shuffle the list randomly to ensure fair selection
            Collections.shuffle(waitlistEntrantIds, new Random());

            // Retrieve event capacity and current number of entrants
            Long capacityLong = snapshot.getLong("capacity");
            Long currentEntrantsNumberLong = snapshot.getLong("currentEntrantsNumber");
            int capacity = capacityLong != null ? capacityLong.intValue() : 0;
            int currentEntrantsNumber = currentEntrantsNumberLong != null ? currentEntrantsNumberLong.intValue() : 0;
            int slotsAvailable = capacity - currentEntrantsNumber;

            if (slotsAvailable <= 0) {
                Log.d(TAG, "No available slots for event: " + eventId);
                // Mark random draw as performed since there are no available slots
                transaction.update(eventRef, "randomDrawPerformed", true);
                return null;
            }

            // Determine the number of entrants to select based on available slots
            int numberToSelect = Math.min(slotsAvailable, waitlistEntrantIds.size());

            // Select entrants to be moved from waitlist to enrolled
            List<String> selectedEntrants = waitlistEntrantIds.subList(0, numberToSelect);
            List<String> notSelectedEntrants = waitlistEntrantIds.subList(numberToSelect, waitlistEntrantIds.size());

            // Create a new entrants map with updated statuses
            Map<String, String> newEntrantsMap = new HashMap<>();

            // Retain entrants who are not on the waitlist
            for (Map.Entry<String, String> entry : entrantsMap.entrySet()) {
                String entrantId = entry.getKey();
                String status = entry.getValue();
                if (!"On Waiting List".equalsIgnoreCase(status)) {
                    newEntrantsMap.put(entrantId, status);
                }
            }

            // Update statuses for selected entrants to "Enrolled"
            for (String entrantId : selectedEntrants) {
                newEntrantsMap.put(entrantId, "Enrolled");
            }

            // Ensure not selected entrants remain on the waitlist
            for (String entrantId : notSelectedEntrants) {
                newEntrantsMap.put(entrantId, "On Waiting List");
            }

            // Update the current number of entrants
            int newCurrentEntrantsNumber = currentEntrantsNumber + selectedEntrants.size();

            // Prepare the updates for the event document
            Map<String, Object> eventUpdates = new HashMap<>();
            eventUpdates.put("entrants", newEntrantsMap);
            eventUpdates.put("currentEntrantsNumber", newCurrentEntrantsNumber);
            eventUpdates.put("randomDrawPerformed", true);

            // Apply the updates within the transaction
            transaction.update(eventRef, eventUpdates);

            Log.d(TAG, "Random draw performed for event: " + eventId);
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Transaction success for event: " + eventId);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Transaction failure for event: " + eventId, e);
        });
    }
}
