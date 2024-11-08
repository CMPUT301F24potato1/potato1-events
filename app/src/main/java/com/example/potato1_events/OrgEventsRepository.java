// File: OrgEventsRepository.java
package com.example.potato1_events;

import android.text.TextUtils;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Repository class to handle organizer event-related Firestore interactions.
 */
public class OrgEventsRepository {
    private final FirebaseFirestore firestore;

    /**
     * Callback interface for loading a list of events.
     */
    public interface EventListCallback {
        /**
         * Called when the event list is loaded.
         *
         * @param events List of events, or null if an error occurred.
         */
        void onEventListLoaded(List<Event> events);
    }

    /**
     * Callback interface for loading a single event.
     */
    public interface EventCallback {
        /**
         * Called when the event is loaded.
         *
         * @param event The loaded event, or null if not found.
         */
        void onEventLoaded(Event event);
    }

    /**
     * Callback interface for action results.
     */
    public interface ActionCallback {
        /**
         * Called when the action succeeds.
         */
        void onSuccess();

        /**
         * Called when the action fails.
         *
         * @param e The exception that caused the failure.
         */
        void onFailure(Exception e);
    }

    /**
     * Constructs an OrgEventsRepository with the given Firestore instance.
     *
     * @param firestore FirebaseFirestore instance.
     */
    public OrgEventsRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Retrieves all events associated with a specific facility by fetching the facility's eventIds
     * and then querying the Events collection.
     *
     * @param facilityId The ID of the facility.
     * @param callback   Callback to handle the list of events.
     */
    public void getEventsForFacility(String facilityId, EventListCallback callback) {
        // Fetch the Facility document
        firestore.collection("Facilities").document(facilityId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Facility facility = documentSnapshot.toObject(Facility.class);
                        if (facility != null && facility.getEventIds() != null && !facility.getEventIds().isEmpty()) {
                            List<String> eventIds = facility.getEventIds();
                            // Firestore 'whereIn' can handle up to 10 elements
                            // If more, you need to batch the queries
                            List<String> limitedEventIds = eventIds.size() > 10 ? eventIds.subList(0, 10) : eventIds;
                            firestore.collection("Events")
                                    .whereIn(FieldPath.documentId(), limitedEventIds)
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots -> {
                                        List<Event> eventList = new ArrayList<>();
                                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                            Event event = doc.toObject(Event.class);
                                            if (event != null) {
                                                event.setId(doc.getId());
                                                eventList.add(event);
                                            }
                                        }
                                        callback.onEventListLoaded(eventList);
                                    })
                                    .addOnFailureListener(e -> callback.onEventListLoaded(null));
                        } else {
                            // No events associated with the facility
                            callback.onEventListLoaded(new ArrayList<>());
                        }
                    } else {
                        // Facility does not exist
                        callback.onEventListLoaded(null);
                    }
                })
                .addOnFailureListener(e -> callback.onEventListLoaded(null));
    }

    /**
     * Retrieves an event by its ID.
     *
     * @param eventId  The ID of the event.
     * @param callback Callback to handle the event data.
     */
    public void getEventById(String eventId, EventCallback callback) {
        firestore.collection("Events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Event event = documentSnapshot.toObject(Event.class);
                    if (event != null) {
                        event.setId(documentSnapshot.getId());
                    }
                    callback.onEventLoaded(event);
                })
                .addOnFailureListener(e -> callback.onEventLoaded(null));
    }

    /**
     * Deletes an event by its ID and removes its reference from the associated facility.
     *
     * @param eventId  The ID of the event to delete.
     * @param facilityId The ID of the facility associated with the event.
     * @param callback Callback to handle the result.
     */
    public void deleteEvent(String eventId, String facilityId, ActionCallback callback) {
        // Begin a batch operation to delete the event and update the facility
        firestore.runTransaction(transaction -> {
                    // Delete the event document
                    transaction.delete(firestore.collection("Events").document(eventId));

                    // Remove the eventId from the facility's eventIds array
                    transaction.update(firestore.collection("Facilities").document(facilityId), "eventIds", FieldValue.arrayRemove(eventId));

                    return null;
                }).addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Updates an event's details.
     *
     * @param event    The Event object with updated details.
     * @param callback Callback to handle the result.
     */
    public void updateEvent(Event event, ActionCallback callback) {
        if (event == null || TextUtils.isEmpty(event.getId())) {
            callback.onFailure(new IllegalArgumentException("Event or Event ID cannot be null"));
            return;
        }
        firestore.collection("Events").document(event.getId())
                .set(event)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Adds a new event to the database and updates the facility's eventIds array.
     *
     * @param event    The Event object to add.
     * @param facilityId The ID of the facility to associate with the event.
     * @param callback Callback to handle the result.
     */
    public void addEvent(Event event, String facilityId, ActionCallback callback) {
        if (event == null || TextUtils.isEmpty(event.getName())) {
            callback.onFailure(new IllegalArgumentException("Event or Event Name cannot be null"));
            return;
        }
        // Add the event to Firestore
        firestore.collection("Events").add(event)
                .addOnSuccessListener(documentReference -> {
                    String newEventId = documentReference.getId();
                    // Update the facility's eventIds array
                    firestore.collection("Facilities").document(facilityId)
                            .update("eventIds", FieldValue.arrayUnion(newEventId))
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Add other methods as necessary
}
