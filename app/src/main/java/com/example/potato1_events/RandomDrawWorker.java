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
                .whereEqualTo("randomDrawPerformed", false)
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
                // Random draw already performed by another device
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
                transaction.update(eventRef, "randomDrawPerformed", true);
                return null;
            }

            List<String> entrantIds = new ArrayList<>(entrantsMap.keySet());

            // Shuffle the list randomly
            Collections.shuffle(entrantIds, new Random());

            Long capacityLong = snapshot.getLong("capacity");
            Long currentEntrantsNumberLong = snapshot.getLong("currentEntrantsNumber");
            int capacity = capacityLong != null ? capacityLong.intValue() : 0;
            int currentEntrantsNumber = currentEntrantsNumberLong != null ? currentEntrantsNumberLong.intValue() : 0;
            int slotsAvailable = capacity - currentEntrantsNumber;
            int numberToSelect = Math.min(slotsAvailable, entrantIds.size());

            List<String> selectedEntrants = entrantIds.subList(0, numberToSelect);
            List<String> canceledEntrants = entrantIds.subList(numberToSelect, entrantIds.size());

            // Update entrants' statuses in the event document
            for (String entrantId : selectedEntrants) {
                entrantsMap.put(entrantId, "Selected to Enroll");
            }
            for (String entrantId : canceledEntrants) {
                entrantsMap.put(entrantId, "Canceled to Enroll");
            }

            // Update the event's current number of entrants
            int newCurrentEntrantsNumber = currentEntrantsNumber + selectedEntrants.size();

            // Prepare event update
            Map<String, Object> eventUpdates = new HashMap<>();
            eventUpdates.put("entrants", entrantsMap);
            eventUpdates.put("currentEntrantsNumber", newCurrentEntrantsNumber);
            eventUpdates.put("randomDrawPerformed", true);

            // Update the event document
            transaction.update(eventRef, eventUpdates);

            // Update users' statuses
            for (String entrantId : selectedEntrants) {
                DocumentReference userRef = firestore.collection("Users").document(entrantId);
                transaction.update(userRef, "eventsJoined." + eventId, "Selected to Enroll");
            }

            for (String entrantId : canceledEntrants) {
                DocumentReference userRef = firestore.collection("Users").document(entrantId);
                transaction.update(userRef, "eventsJoined." + eventId, "Canceled to Enroll");
            }

            Log.d(TAG, "Random draw performed for event: " + eventId);
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Transaction success for event: " + eventId);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Transaction failure for event: " + eventId, e);
        });
    }
}