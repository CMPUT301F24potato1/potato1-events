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

public class RandomDrawWorker extends Worker {

    private static final String TAG = "RandomDrawWorker";

    public RandomDrawWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "RandomDrawWorker is running");
        performRandomDraw();
        return Result.success();
    }

    private void performRandomDraw() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Get current time
        Timestamp now = Timestamp.now();

        // Query events where random draw needs to be performed
        firestore.collection("Events")
                .whereLessThanOrEqualTo("registrationEnd", now)
                //FIXME put this back in after field is there
                //.whereEqualTo("randomDrawPerformed", false) // Ensure we process only relevant events
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No events require random draw at this time.");
                        return;
                    }

                    for (DocumentSnapshot eventDoc : queryDocumentSnapshots.getDocuments()) {
                        // Perform random draw for each event
                        processEventRandomDraw(eventDoc);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error querying events for random draw", e);
                });
    }

    private void processEventRandomDraw(DocumentSnapshot eventDoc) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference eventRef = eventDoc.getReference();
        String eventId = eventDoc.getId();

        // Use a transaction to ensure the draw is performed only once
        firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);

            Boolean randomDrawPerformed = snapshot.getBoolean("randomDrawPerformed");
            if (randomDrawPerformed != null && randomDrawPerformed) {
                // Random draw already performed
                Log.d(TAG, "Random draw already performed for event: " + eventId);
                return null;
            }

            Map<String, Object> eventData = snapshot.getData();
            if (eventData == null) {
                Log.e(TAG, "Event data is null for event: " + eventId);
                return null;
            }

            // Get entrants
            Map<String, String> entrantsMap = (Map<String, String>) eventData.get("entrants");
            if (entrantsMap == null || entrantsMap.isEmpty()) {
                Log.d(TAG, "No entrants for event: " + eventId);
                // Mark random draw as performed
               //FIXME put this back in after field is there
                // transaction.update(eventRef, "randomDrawPerformed", true);
                return null;
            }

            // Collect only entrants with status "waitlist"
            List<String> waitlistEntrantIds = new ArrayList<>();
            for (Map.Entry<String, String> entry : entrantsMap.entrySet()) {
                if ("waitlist".equals(entry.getValue())) {
                    waitlistEntrantIds.add(entry.getKey());
                }
            }

            if (waitlistEntrantIds.isEmpty()) {
                Log.d(TAG, "No waitlist entrants for event: " + eventId);
                // Mark random draw as performed
                //FIXME put this back in after field is there
                //transaction.update(eventRef, "randomDrawPerformed", true);
                return null;
            }

            // Shuffle the list randomly
            Collections.shuffle(waitlistEntrantIds, new Random());

            Long capacityLong = snapshot.getLong("capacity");
            Long currentEntrantsNumberLong = snapshot.getLong("currentEntrantsNumber");
            int capacity = capacityLong != null ? capacityLong.intValue() : 0;
            int currentEntrantsNumber = currentEntrantsNumberLong != null ? currentEntrantsNumberLong.intValue() : 0;
            int slotsAvailable = capacity - currentEntrantsNumber;
            int numberToSelect = Math.min(slotsAvailable, waitlistEntrantIds.size());

            List<String> selectedEntrants = waitlistEntrantIds.subList(0, numberToSelect);
            List<String> notSelectedEntrants = waitlistEntrantIds.subList(numberToSelect, waitlistEntrantIds.size());

            // Create a new entrants map
            Map<String, String> newEntrantsMap = new HashMap<>();

            // Copy over entrants who are not on the waitlist
            for (Map.Entry<String, String> entry : entrantsMap.entrySet()) {
                String entrantId = entry.getKey();
                String status = entry.getValue();
                if (!"waitlist".equals(status)) {
                    newEntrantsMap.put(entrantId, status);
                }
            }

            // Add selected entrants with updated status
            for (String entrantId : selectedEntrants) {
                newEntrantsMap.put(entrantId, "Registered");
            }

            // Add not selected entrants with updated status
            for (String entrantId : notSelectedEntrants) {
                newEntrantsMap.put(entrantId, "Not Selected");
            }

            // Update the event's current number of entrants
            int newCurrentEntrantsNumber = currentEntrantsNumber + selectedEntrants.size();

            // Prepare event update
            Map<String, Object> eventUpdates = new HashMap<>();
            eventUpdates.put("entrants", newEntrantsMap);
            eventUpdates.put("currentEntrantsNumber", newCurrentEntrantsNumber);
            //FIXME put this back in after
            //eventUpdates.put("randomDrawPerformed", true); // Ensure this is included

            // Update the event document
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