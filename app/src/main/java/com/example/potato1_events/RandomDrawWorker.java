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
        Timestamp now = Timestamp.now();

        // Query events that need random draw
        firestore.collection("Events")
                .whereLessThanOrEqualTo("registrationEnd", now)
                //.whereEqualTo("waitingListFilled", false)
                .whereEqualTo("randomDrawPerformed", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No events require random draw at this time.");
                        return;
                    }

                    for (DocumentSnapshot eventDoc : queryDocumentSnapshots.getDocuments()) {
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

        firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);

            Map<String, Object> eventData = snapshot.getData();
            if (eventData == null) {
                Log.e(TAG, "Event data is null for event: " + eventId);
                return null;
            }

            // Retrieve entrants map
            Map<String, String> entrantsMap = (Map<String, String>) eventData.get("entrants");
            if (entrantsMap == null || entrantsMap.isEmpty()) {
                Log.d(TAG, "No entrants for event: " + eventId);
                // No entrants to process
                return null;
            }

            // Get event capacity
            Long capacityLong = snapshot.getLong("capacity");
            int capacity = capacityLong != null ? capacityLong.intValue() : 0;

            // Calculate the number of entrants who have accepted or are selected
            int noneligibleEntrants = 0;
            int acceptedEntrants = 0;
            for (String status : entrantsMap.values()) {
                if ("Selected".equalsIgnoreCase(status) || "Accepted".equalsIgnoreCase(status)) {
                    noneligibleEntrants++;
                }
                if ("Accepted".equalsIgnoreCase(status)) {
                    acceptedEntrants++;
                }
            }

            if (acceptedEntrants == capacity){
                Log.d(TAG, "All entrants accepted for event: " + eventId);
                // Update waitingListFilled
                transaction.update(eventRef, "waitingListFilled", true);
                transaction.update(eventRef, "randomDrawPerformed", true);
                return null;
            }

            // Determine available slots
            int slotsAvailable = capacity - noneligibleEntrants;

            if (slotsAvailable <= 0) {
                Log.d(TAG, "No available slots for event: " + eventId);
                // Update waitingListFilled
                transaction.update(eventRef, "waitingListFilled", true);
                return null;
            }

            // Collect eligible entrant IDs (status is "Not Selected" or "waitlist")
            List<String> eligibleEntrantIds = new ArrayList<>();
            for (Map.Entry<String, String> entry : entrantsMap.entrySet()) {
                String status = entry.getValue();
                if ("Not Selected".equalsIgnoreCase(status) || "waitlist".equalsIgnoreCase(status)) {
                    eligibleEntrantIds.add(entry.getKey());
                }
            }

            if (eligibleEntrantIds.isEmpty()) {
                Log.d(TAG, "No eligible entrants to fill available slots for event: " + eventId);
                // Update waitingListFilled
                transaction.update(eventRef, "waitingListFilled", true);
                return null;
            }

            // Shuffle the list to randomly select entrants
            Collections.shuffle(eligibleEntrantIds, new Random());

            int numberToSelect = Math.min(slotsAvailable, eligibleEntrantIds.size());
            List<String> selectedEntrants = eligibleEntrantIds.subList(0, numberToSelect);
            List<String> notSelectedEntrants = eligibleEntrantIds.subList(numberToSelect, eligibleEntrantIds.size());

            // Update entrant statuses
            for (String entrantId : selectedEntrants) {
                entrantsMap.put(entrantId, "Selected");
            }
            for (String entrantId : notSelectedEntrants) {
                entrantsMap.put(entrantId, "Not Selected");
            }

            // Update the event document with the new entrants map
            transaction.update(eventRef, "entrants", entrantsMap);

            // Set waitingListFilled to true
            transaction.update(eventRef, "waitingListFilled", true);

            Log.d(TAG, "Random draw performed for event: " + eventId);
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Transaction success for event: " + eventId);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Transaction failure for event: " + eventId, e);
        });
    }
}
