package com.example.potato1_events;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an Event that entrants can sign up for.
 * This class is synchronized with the Firebase Firestore "Events" collection.
 */
public class Event {

    /**
     * Unique identifier for the event (Firestore document ID).
     */
    private String id;

    /**
     * Identifier of the facility hosting the event.
     */
    private String facilityId;

    /**
     * Name of the event.
     */
    private String name;

    /**
     * Description of the event.
     */
    private String description;

    /**
     * Start date and time of the event.
     */
    private Date startDate;

    /**
     * End date and time of the event.
     */
    private Date endDate;

    /**
     * Registration start date and time.
     */
    private Date registrationStart;

    /**
     * Registration end date and time.
     */
    private Date registrationEnd;

    /**
     * Price for attending the event.
     */
    private double price;

    /**
     * Maximum number of attendees.
     */
    private int capacity;

    /**
     * Current number of entrants.
     */
    private int currentEntrantsNumber;

    /**
     * Maximum number of entrants in the waiting list.
     */
    private Integer waitingListCapacity;

    /**
     * URL of the event poster image.
     */
    private String posterImageUrl;

    /**
     * QR code hash representing the event ID.
     */
    private String qrCodeHash;

    /**
     * Map of entrant IDs to their status.
     */
    private Map<String, String> entrants;

    /**
     * Timestamp of when the event was created.
     */
    @ServerTimestamp
    private Date createdAt;

    /**
     * Status of the event (e.g., "Open", "Closed", "Completed").
     */
    private String status;

    /**
     * Switch to let organizers set geolocation status.
     */
    private boolean geolocationRequired;

    /**
     * Flag indicating if a random draw has been performed.
     */
    private boolean randomDrawPerformed;

    /**
     * Flag indicating if the waiting list has been filled.
     */
    private boolean waitingListFilled;

    /**
     * Map of entrant IDs to their geolocation points.
     */
    private Map<String, GeoPoint> entrantsLocation;

    /**
     * Event's location.
     */
    private String eventLocation;

    /**
     * Default constructor required for Firebase deserialization.
     */
    public Event() {
        // Initialize the entrants map to prevent NullPointerExceptions
        this.entrants = new HashMap<>();
    }

    /**
     * Parameterized constructor to create an Event instance.
     *
     * @param id                    Unique identifier for the event.
     * @param facilityId            Identifier of the hosting facility.
     * @param name                  Name of the event.
     * @param description           Description of the event.
     * @param startDate             Start date and time.
     * @param endDate               End date and time.
     * @param registrationStart     Registration start date and time.
     * @param registrationEnd       Registration end date and time.
     * @param price                 Price for attending.
     * @param capacity              Maximum number of attendees.
     * @param currentEntrantsNumber Current number of entrants.
     * @param waitingListCapacity   Maximum entrants in the waiting list.
     * @param posterImageUrl        URL of the poster image.
     * @param qrCodeHash            QR code hash representing the event ID.
     * @param entrants              Map of entrants with their status.
     * @param createdAt             Creation timestamp.
     * @param status                Status of the event.
     * @param geolocationRequired   Switch enabling geolocation requirement.
     * @param eventLocation         Event's location.
     */
    public Event(String id, String facilityId, String name, String description, Date startDate, Date endDate,
                 Date registrationStart, Date registrationEnd, double price, int capacity, int currentEntrantsNumber,
                 Integer waitingListCapacity, String posterImageUrl, String qrCodeHash,
                 Map<String, String> entrants, Date createdAt, String status, boolean geolocationRequired,
                 String eventLocation) {
        this.id = id;
        this.facilityId = facilityId;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.registrationStart = registrationStart;
        this.registrationEnd = registrationEnd;
        this.price = price;
        this.capacity = capacity;
        this.currentEntrantsNumber = currentEntrantsNumber;
        this.waitingListCapacity = waitingListCapacity;
        this.posterImageUrl = posterImageUrl;
        this.qrCodeHash = qrCodeHash;
        this.entrants = entrants != null ? entrants : new HashMap<>(); // Initialize entrants map
        this.createdAt = createdAt;
        this.status = status;
        this.geolocationRequired = geolocationRequired;
        this.eventLocation = eventLocation;
        this.randomDrawPerformed = false; // Default value upon creation
        this.waitingListFilled = false; // Default value upon creation
        this.entrantsLocation = new HashMap<>(); // Initialize entrants' geolocation map
    }

    // Getters and Setters

    /**
     * Gets the event ID.
     *
     * @return Event ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the event ID.
     *
     * @param id Event ID.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the facility ID hosting the event.
     *
     * @return Facility ID.
     */
    public String getFacilityId() {
        return facilityId;
    }

    /**
     * Sets the facility ID hosting the event.
     *
     * @param facilityId Facility ID.
     */
    public void setFacilityId(String facilityId) {
        this.facilityId = facilityId;
    }

    /**
     * Gets the event name.
     *
     * @return Event name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the event name.
     *
     * @param name Event name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the event description.
     *
     * @return Event description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the event description.
     *
     * @param description Event description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the start date and time of the event.
     *
     * @return Start date and time.
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Sets the start date and time of the event.
     *
     * @param startDate Start date and time.
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /**
     * Gets the end date and time of the event.
     *
     * @return End date and time.
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Sets the end date and time of the event.
     *
     * @param endDate End date and time.
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    /**
     * Gets the registration start date and time.
     *
     * @return Registration start date and time.
     */
    public Date getRegistrationStart() {
        return registrationStart;
    }

    /**
     * Sets the registration start date and time.
     *
     * @param registrationStart Registration start date and time.
     */
    public void setRegistrationStart(Date registrationStart) {
        this.registrationStart = registrationStart;
    }

    /**
     * Gets the registration end date and time.
     *
     * @return Registration end date and time.
     */
    public Date getRegistrationEnd() {
        return registrationEnd;
    }

    /**
     * Sets the registration end date and time.
     *
     * @param registrationEnd Registration end date and time.
     */
    public void setRegistrationEnd(Date registrationEnd) {
        this.registrationEnd = registrationEnd;
    }

    /**
     * Gets the price for attending the event.
     *
     * @return Price.
     */
    public double getPrice() {
        return price;
    }

    /**
     * Sets the price for attending the event.
     *
     * @param price Price.
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * Gets the maximum number of attendees.
     *
     * @return Capacity.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Sets the maximum number of attendees.
     *
     * @param capacity Capacity.
     * @throws IllegalArgumentException if capacity is negative.
     */
    public void setCapacity(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative");
        }
        this.capacity = capacity;
    }

    /**
     * Gets the current number of entrants in the event.
     *
     * @return Current number of entrants.
     */
    public int getCurrentEntrantsNumber() {
        return currentEntrantsNumber;
    }

    /**
     * Sets the current number of entrants in the event.
     *
     * @param currentEntrantsNumber Current number of entrants.
     */
    public void setCurrentEntrantsNumber(int currentEntrantsNumber) {
        this.currentEntrantsNumber = currentEntrantsNumber;
    }

    /**
     * Gets the maximum capacity of the waiting list.
     *
     * @return Waiting list capacity, or null if unlimited.
     */
    public Integer getWaitingListCapacity() {
        return waitingListCapacity;
    }

    /**
     * Sets the maximum capacity of the waiting list.
     *
     * @param waitingListCapacity Waiting list capacity, or null for unlimited.
     * @throws IllegalArgumentException if waitingListCapacity is negative.
     */
    public void setWaitingListCapacity(Integer waitingListCapacity) {
        if (waitingListCapacity != null && waitingListCapacity < 0) {
            throw new IllegalArgumentException("Waiting list capacity cannot be negative");
        }
        this.waitingListCapacity = waitingListCapacity;
    }

    /**
     * Gets the URL of the event poster image.
     *
     * @return Poster image URL.
     */
    public String getPosterImageUrl() {
        return posterImageUrl;
    }

    /**
     * Sets the URL of the event poster image.
     *
     * @param posterImageUrl Poster image URL.
     */
    public void setPosterImageUrl(String posterImageUrl) {
        this.posterImageUrl = posterImageUrl;
    }

    /**
     * Gets the QR code hash representing the event ID.
     *
     * @return QR code hash.
     */
    public String getQrCodeHash() {
        return qrCodeHash;
    }

    /**
     * Sets the QR code hash representing the event ID.
     *
     * @param qrCodeHash QR code hash.
     */
    public void setQrCodeHash(String qrCodeHash) {
        this.qrCodeHash = qrCodeHash;
    }

    /**
     * Gets the entrants map.
     *
     * @return Map of entrant IDs to their status.
     */
    public Map<String, String> getEntrants() {
        return entrants;
    }

    /**
     * Sets the entrants map.
     *
     * @param entrants Map of entrant IDs to their status.
     */
    public void setEntrants(Map<String, String> entrants) {
        this.entrants = entrants;
    }

    /**
     * Adds or updates an entrant's status.
     *
     * @param entrantId The ID of the entrant.
     * @param status    The status of the entrant (e.g., "Accepted", "Selected", "Not Selected", "Declined", "Waitlist").
     */
    public void updateEntrantStatus(String entrantId, String status) {
        this.entrants.put(entrantId, status);
    }

    /**
     * Removes an entrant from the entrants map.
     *
     * @param entrantId The ID of the entrant to remove.
     */
    public void removeEntrant(String entrantId) {
        this.entrants.remove(entrantId);
    }

    /**
     * Gets the creation timestamp.
     *
     * @return Creation timestamp.
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp.
     *
     * @param createdAt Creation timestamp.
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the status of the event.
     *
     * @return Event status.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status of the event.
     *
     * @param status Event status.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Updates the event status.
     *
     * @param newStatus New status of the event.
     */
    public void updateStatus(String newStatus) {
        this.status = newStatus;
    }

    /**
     * Gets the geolocation requirement status.
     *
     * @return True if geolocation is required, false otherwise.
     */
    public boolean isGeolocationRequired() {
        return geolocationRequired;
    }

    /**
     * Sets the geolocation requirement status.
     *
     * @param geolocationRequired True to require geolocation, false otherwise.
     */
    public void setGeolocationRequired(boolean geolocationRequired) {
        this.geolocationRequired = geolocationRequired;
    }

    /**
     * Gets whether a random draw has been performed.
     *
     * @return True if a random draw has been performed, false otherwise.
     */
    public boolean getRandomDrawPerformed() {
        return randomDrawPerformed;
    }

    /**
     * Sets whether a random draw has been performed.
     *
     * @param randomDrawPerformed True if a random draw has been performed, false otherwise.
     */
    public void setRandomDrawPerformed(boolean randomDrawPerformed) {
        this.randomDrawPerformed = randomDrawPerformed;
    }

    /**
     * Gets the map of entrants' geolocations.
     *
     * @return Map of entrant IDs to their GeoPoint.
     */
    public Map<String, GeoPoint> getEntrantsLocation() {
        return entrantsLocation;
    }

    /**
     * Sets the map of entrants' geolocations.
     *
     * @param entrantsLocation Map of entrant IDs to their GeoPoint.
     */
    public void setEntrantsLocation(Map<String, GeoPoint> entrantsLocation) {
        this.entrantsLocation = entrantsLocation;
    }

    /**
     * Gets the event location.
     *
     * @return Event location.
     */
    public String getEventLocation() {
        return eventLocation;
    }

    /**
     * Sets the event location.
     *
     * @param eventLocation Event location.
     */
    public void setEventLocation(String eventLocation) {
        this.eventLocation = eventLocation;
    }

    /**
     * Checks if the waiting list is filled.
     *
     * @return True if the waiting list is filled, false otherwise.
     */
    public boolean isWaitingListFilled() {
        return waitingListFilled;
    }

    /**
     * Sets the waiting list filled status.
     *
     * @param waitingListFilled True if the waiting list is filled, false otherwise.
     */
    public void setWaitingListFilled(boolean waitingListFilled) {
        this.waitingListFilled = waitingListFilled;
    }

    /**
     * Calculates the available capacity based on the current number of accepted entrants.
     *
     * @return Available capacity as a String.
     */
    public String getAvailableCapacity() {
        int acceptedEntrants = 0;

        if (entrants != null && !entrants.isEmpty()) {
            for (String status : entrants.values()) {
                if ("Selected".equalsIgnoreCase(status) || "Accepted".equalsIgnoreCase(status)) {
                    acceptedEntrants++;
                }
            }
        }

        int availableCapacity = capacity - acceptedEntrants;
        return String.valueOf(availableCapacity);
    }

    /**
     * Retrieves the number of entrants with the status "accepted".
     *
     * @return The count of accepted entrants.
     */
    public int getAcceptedCount() {
        if (entrants == null || entrants.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String status : entrants.values()) {
            if ("accepted".equalsIgnoreCase(status)) {
                count++;
            }
        }
        return count;
    }
}
