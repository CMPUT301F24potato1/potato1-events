package com.example.potato1_events;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Repository class to handle event-related Firestore interactions.
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
     * Constructs an EventRepository with the given Firestore instance.
     *
     * @param firestore FirebaseFirestore instance.
     */
    public OrgEventsRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Retrieves all events associated with a specific facility.
     *
     * @param facilityId The ID of the facility.
     * @param callback   Callback to handle the list of events.
     */
    public void getEventsForOrganizerFacility(String facilityId, EventListCallback callback) {
        CollectionReference eventsCollection = firestore.collection("Events");
        eventsCollection.whereEqualTo("facilityId", facilityId)
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
     * Adds the entrant to the event's waiting list.
     *
     * @param eventId   The ID of the event.
     * @param deviceId  The entrant's device ID.
     * @param callback  Callback to handle the result.
     */
    public void joinWaitingList(String eventId, String deviceId, ActionCallback callback) {
        final CollectionReference eventsCollection = firestore.collection("Events");
        final CollectionReference entrantsCollection = firestore.collection("Entrants");

        firestore.runTransaction((Transaction.Function<Void>) transaction -> {
                    // Fetch event
                    Event event = transaction.get(eventsCollection.document(eventId)).toObject(Event.class);

                    if (event == null) {
                        try {
                            throw new Exception("Event does not exist.");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    Map<String, String> entrantsMap = event.getEntrants();

                    // Check if entrant is already in the entrants map
                    if (entrantsMap.containsKey(deviceId)) {
                        try {
                            throw new Exception("Already on the waiting list.");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    // Check if waiting list is full
                    if (event.getCurrentEntrantsNumber() >= event.getWaitingListCapacity()) {
                        try {
                            throw new Exception("Waiting list is full.");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    // Update event data
                    entrantsMap.put(deviceId, "waitlist");
                    event.setCurrentEntrantsNumber(event.getCurrentEntrantsNumber() + 1);

                    // Commit updates
                    transaction.set(eventsCollection.document(eventId), event);

                    // Update entrant's eventsJoined list
                    transaction.update(entrantsCollection.document(deviceId), "eventsJoined", FieldValue.arrayUnion(eventId));

                    return null;
                }).addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Removes the entrant from the event's waiting list.
     *
     * @param eventId   The ID of the event.
     * @param deviceId  The entrant's device ID.
     * @param callback  Callback to handle the result.
     */
    public void leaveWaitingList(String eventId, String deviceId, ActionCallback callback) {
        final CollectionReference eventsCollection = firestore.collection("Events");
        final CollectionReference entrantsCollection = firestore.collection("Entrants");

        firestore.runTransaction((Transaction.Function<Void>) transaction -> {
                    // Fetch event
                    Event event = transaction.get(eventsCollection.document(eventId)).toObject(Event.class);

                    if (event == null) {
                        try {
                            throw new Exception("Event does not exist.");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    Map<String, String> entrantsMap = event.getEntrants();

                    // Check if entrant is in the entrants map
                    if (!entrantsMap.containsKey(deviceId)) {
                        try {
                            throw new Exception("Not on the waiting list.");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    // Update event data
                    entrantsMap.remove(deviceId);
                    event.setCurrentEntrantsNumber(event.getCurrentEntrantsNumber() - 1);

                    // Commit updates
                    transaction.set(eventsCollection.document(eventId), event);

                    // Update entrant's eventsJoined list
                    transaction.update(entrantsCollection.document(deviceId), "eventsJoined", FieldValue.arrayRemove(eventId));

                    return null;
                }).addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}

