// File: EntEventsRepository.java
package com.example.potato1_events;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class to handle entrant-related Firestore interactions.
 */
public class EntEventsRepository {
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
     * Constructs an EntEventsRepository with the given Firestore instance.
     *
     * @param firestore FirebaseFirestore instance.
     */
    public EntEventsRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Retrieves all available events.
     *
     * @param callback Callback to handle the list of events.
     */
    public void getAllEvents(EventListCallback callback) {
        CollectionReference eventsCollection = firestore.collection("Events");
        eventsCollection.get()
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
     * Retrieves events by their IDs.
     * Handles Firestore's 'whereIn' limitation by batching the requests if necessary.
     *
     * @param eventIds List of event IDs to fetch.
     * @param callback Callback to handle the list of events.
     */
    public void getEventsByIds(List<String> eventIds, EventListCallback callback) {
        if (eventIds == null || eventIds.isEmpty()) {
            callback.onEventListLoaded(new ArrayList<>());
            return;
        }

        // Firestore's 'whereIn' supports up to 10 elements. Batch the requests accordingly.
        int batchSize = 10;
        int total = eventIds.size();
        int batches = (int) Math.ceil((double) total / batchSize);

        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (int i = 0; i < batches; i++) {
            int start = i * batchSize;
            int end = Math.min(start + batchSize, total);
            List<String> batch = eventIds.subList(start, end);

            Task<QuerySnapshot> task = firestore.collection("Events")
                    .whereIn(FieldPath.documentId(), batch)
                    .get();

            tasks.add(task);
        }

        // Use Tasks.whenAllSuccess to wait for all batch requests to complete
        com.google.android.gms.tasks.Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(results -> {
                    List<Event> allEvents = new ArrayList<>();
                    for (Object result : results) {
                        if (result instanceof QuerySnapshot) {
                            QuerySnapshot qs = (QuerySnapshot) result;
                            for (QueryDocumentSnapshot doc : qs) {
                                Event event = doc.toObject(Event.class);
                                if (event != null) {
                                    event.setId(doc.getId());
                                    allEvents.add(event);
                                }
                            }
                        }
                    }
                    callback.onEventListLoaded(allEvents);
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
     * Retrieves events that the entrant has joined.
     *
     * @param deviceId The entrant's device ID.
     * @param callback Callback to handle the list of joined events.
     */
    public void getJoinedEvents(String deviceId, EventListCallback callback) {
        DocumentReference userRef = firestore.collection("Users").document(deviceId);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    List<String> eventsJoined = (List<String>) document.get("eventsJoined");
                    if (eventsJoined != null && !eventsJoined.isEmpty()) {
                        // Fetch events by IDs
                        getEventsByIds(eventsJoined, callback);
                    } else {
                        // No events joined
                        callback.onEventListLoaded(new ArrayList<>());
                    }
                } else {
                    // User profile not found
                    callback.onEventListLoaded(null);
                }
            } else {
                // Error fetching user data
                callback.onEventListLoaded(null);
            }
        });
    }

    /**
     * Adds the entrant to the event's waiting list.
     * Supports unlimited waiting list if waitingListCapacity is null.
     *
     * @param eventId  The ID of the event.
     * @param deviceId The entrant's device ID.
     * @param callback Callback to handle the result.
     */
    public void joinWaitingList(String eventId, String deviceId, ActionCallback callback) {
        final CollectionReference eventsCollection = firestore.collection("Events");
        final CollectionReference usersCollection = firestore.collection("Users");

        firestore.runTransaction((Transaction.Function<Void>) transaction -> {
                    // Fetch event document
                    DocumentSnapshot eventSnapshot = transaction.get(eventsCollection.document(eventId));
                    Event event = eventSnapshot.toObject(Event.class);

                    if (event == null) {
                        throw new FirebaseFirestoreException("Event does not exist.",
                                FirebaseFirestoreException.Code.ABORTED, null);
                    }

                    Map<String, String> entrantsMap = event.getEntrants();
                    if (entrantsMap == null) {
                        entrantsMap = new HashMap<>();
                    }

                    // Check if entrant is already registered or on the waiting list
                    if (entrantsMap.containsKey(deviceId)) {
                        throw new FirebaseFirestoreException("Already registered or on the waiting list.",
                                FirebaseFirestoreException.Code.ABORTED, null);
                    }

                    // Retrieve waitingListCapacity (nullable)
                    Integer waitingListCapacity = event.getWaitingListCapacity();

                    if (waitingListCapacity != null) {
                        // Limited waiting list; enforce capacity
                        long currentWaitingList = entrantsMap.values().stream()
                                .filter(status -> "waitlist".equalsIgnoreCase(status))
                                .count();

                        if (currentWaitingList >= waitingListCapacity) {
                            throw new FirebaseFirestoreException("Waiting list is full.",
                                    FirebaseFirestoreException.Code.ABORTED, null);
                        }
                    }
                    // Else, unlimited waiting list; no capacity checks

                    // Add entrant to waiting list with status "waitlist"
                    transaction.update(eventsCollection.document(eventId), "entrants." + deviceId, "waitlist");

                    // Note: Do NOT increment currentEntrantsNumber for waiting list
                    // Assuming currentEntrantsNumber tracks only main entrants

                    // Add eventId to the user's eventsJoined list using set with merge
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("eventsJoined", FieldValue.arrayUnion(eventId));
                    transaction.set(usersCollection.document(deviceId), userData, SetOptions.merge());

                    return null;
                }).addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Removes the entrant from the event's waiting list.
     *
     * @param eventId  The ID of the event.
     * @param deviceId The entrant's device ID.
     * @param callback Callback to handle the result.
     */
    public void leaveWaitingList(String eventId, String deviceId, ActionCallback callback) {
        final CollectionReference eventsCollection = firestore.collection("Events");
        final CollectionReference usersCollection = firestore.collection("Users");

        firestore.runTransaction((Transaction.Function<Void>) transaction -> {
                    // Fetch event document
                    DocumentSnapshot eventSnapshot = transaction.get(eventsCollection.document(eventId));
                    Event event = eventSnapshot.toObject(Event.class);

                    if (event == null) {
                        throw new FirebaseFirestoreException("Event does not exist.",
                                FirebaseFirestoreException.Code.ABORTED, null);
                    }

                    Map<String, String> entrantsMap = event.getEntrants();
                    if (entrantsMap == null || !entrantsMap.containsKey(deviceId)) {
                        throw new FirebaseFirestoreException("Not on the waiting list.",
                                FirebaseFirestoreException.Code.ABORTED, null);
                    }

                    // Check if the entrant is on the waiting list
                    String status = entrantsMap.get(deviceId);
                    if (!"waitlist".equalsIgnoreCase(status)) {
                        throw new FirebaseFirestoreException("Entrant is not on the waiting list.",
                                FirebaseFirestoreException.Code.ABORTED, null);
                    }

                    // Remove entrant from entrants map
                    transaction.update(eventsCollection.document(eventId), "entrants." + deviceId, FieldValue.delete());

                    // Note: Do NOT decrement currentEntrantsNumber for waiting list removal
                    // Assuming currentEntrantsNumber tracks only main entrants

                    // Remove eventId from the user's eventsJoined list
                    transaction.update(usersCollection.document(deviceId), "eventsJoined", FieldValue.arrayRemove(eventId));

                    return null;
                }).addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}
