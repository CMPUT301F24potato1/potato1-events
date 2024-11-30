// File: RandomDrawWorker.java
package com.example.potato1_events;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Worker class responsible for performing random draws for events.
 * It can process either a specific event (if eventId is provided) or all eligible events.
 */
public class RandomDrawWorker extends Worker {

    private static final String TAG = "RandomDrawWorker";
    private static final String KEY_EVENT_ID = "eventId";

    /**
     * Constructs a new RandomDrawWorker.
     *
     * @param context The application context.
     * @param params  Parameters for the worker.
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
        String eventId = getInputData().getString(KEY_EVENT_ID);

        if (eventId != null && !eventId.isEmpty()) {
            // Process a specific event
            Log.d(TAG, "Processing specific event: " + eventId);
            performRandomDrawForEvent(eventId);
        } else {
            // Process all eligible events
            Log.d(TAG, "Processing all eligible events");
            performRandomDrawForAllEvents();
        }

        return Result.success();
    }

    /**
     * Performs the random draw for a specific event.
     *
     * @param eventId The ID of the event to process.
     */
    private void performRandomDrawForEvent(String eventId) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference eventRef = firestore.collection("Events").document(eventId);

        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);

            if (!snapshot.exists()) {
                Log.e(TAG, "Event document does not exist: " + eventId);
                return null;
            }

            Map<String, Object> eventData = snapshot.getData();
            if (eventData == null) {
                Log.e(TAG, "Event data is null for event: " + eventId);
                return null;
            }

            // Check if random draw is already performed
            Boolean randomDrawPerformed = (Boolean) eventData.get("randomDrawPerformed");
            if (randomDrawPerformed != null && randomDrawPerformed) {
                Log.d(TAG, "Random draw already performed for event: " + eventId);
                return null;
            }

            // Check if registration period has ended
            Timestamp registrationEnd = snapshot.getTimestamp("registrationEnd");
            if (registrationEnd == null || registrationEnd.toDate().after(new java.util.Date())) {
                Log.d(TAG, "Registration period not ended for event: " + eventId);
                return null;
            }

            // Retrieve entrants map
            Map<String, String> entrantsMap = (Map<String, String>) eventData.get("entrants");
            if (entrantsMap == null || entrantsMap.isEmpty()) {
                Log.d(TAG, "No entrants for event: " + eventId);
                // Mark randomDrawPerformed to avoid reprocessing
                transaction.update(eventRef, "randomDrawPerformed", true);
                return null;
            }

            // Get event capacity
            Long capacityLong = snapshot.getLong("capacity");
            int capacity = capacityLong != null ? capacityLong.intValue() : 0;

            // Calculate the number of entrants who have accepted or are selected
            int nonEligibleEntrants = 0;
            int acceptedEntrants = 0;
            for (String status : entrantsMap.values()) {
                if ("Selected".equalsIgnoreCase(status) || "Accepted".equalsIgnoreCase(status)) {
                    nonEligibleEntrants++;
                }
                if ("Accepted".equalsIgnoreCase(status)) {
                    acceptedEntrants++;
                }
            }

            if (acceptedEntrants >= capacity) {
                Log.d(TAG, "All entrants accepted for event: " + eventId);
                // Update randomDrawPerformed and waitingListFilled
                transaction.update(eventRef, "randomDrawPerformed", true);
                transaction.update(eventRef, "waitingListFilled", true);
                return null;
            }

            // Determine available slots
            int slotsAvailable = capacity - nonEligibleEntrants;

            if (slotsAvailable <= 0) {
                Log.d(TAG, "No available slots for event: " + eventId);
                // Update randomDrawPerformed and waitingListFilled
                transaction.update(eventRef, "randomDrawPerformed", true);
                transaction.update(eventRef, "waitingListFilled", true);
                return null;
            }

            // Collect eligible entrant IDs (status is "Not Selected" or "Waitlist")
            List<String> eligibleEntrantIds = new ArrayList<>();
            for (Map.Entry<String, String> entry : entrantsMap.entrySet()) {
                String status = entry.getValue();
                if ("Not Selected".equalsIgnoreCase(status) || "Waitlist".equalsIgnoreCase(status)) {
                    eligibleEntrantIds.add(entry.getKey());
                }
            }

            if (eligibleEntrantIds.isEmpty()) {
                Log.d(TAG, "No eligible entrants to fill available slots for event: " + eventId);
                // Update randomDrawPerformed and waitingListFilled
                transaction.update(eventRef, "randomDrawPerformed", true);
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

            // Mark randomDrawPerformed and waitingListFilled
            transaction.update(eventRef, "randomDrawPerformed", true);
            transaction.update(eventRef, "waitingListFilled", true);

            Log.d(TAG, "Random draw performed for event: " + eventId);
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Transaction success for event: " + eventId);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Transaction failure for event: " + eventId, e);
        });
    }

    /**
     * Performs random draw for all eligible events.
     */
    private void performRandomDrawForAllEvents() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Timestamp now = Timestamp.now();

        // Query events that need random draw
        firestore.collection("Events")
                .whereLessThanOrEqualTo("registrationEnd", now)
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
     * This method is similar to performRandomDrawForEvent but used internally.
     *
     * @param eventDoc The DocumentSnapshot representing the event to process.
     */
    private void processEventRandomDraw(DocumentSnapshot eventDoc) {
        String eventId = eventDoc.getId();
        String eventName = eventDoc.getString("name");

        performRandomDrawForEvent(eventId);
    }
}