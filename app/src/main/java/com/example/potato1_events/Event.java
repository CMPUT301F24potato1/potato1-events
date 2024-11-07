package com.example.potato1_events;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
     * current number of Entrants
     */
    private int currentEntrantsNumber;

    /**
     * Maximum number of entrants in the waiting list.
     */
    private int waitingListCapacity;

    /**
     * URL of the event poster image.
     */
    private String posterImageUrl;

    /**
     * Hashed QR code data for the event.
     */
    private String qrCodeHash;

    private Map<String, String> entrants; // Map of entrant IDs to their status


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
     * Switch to let organizers set geolocation status
     */
    private boolean geolocationRequired;

    /**
     * Events location
     */
    private String eventLocation;


    /**
     * Default constructor required for Firebase deserialization.
     */
    public Event() {
        // Default constructor
        this.entrants = new HashMap<>();
    }

    /**
     * Parameterized constructor to create an Event instance.
     *
     * @param id               Unique identifier for the event.
     * @param facilityId       Identifier of the hosting facility.
     * @param name             Name of the event.
     * @param description      Description of the event.
     * @param startDate        Start date and time.
     * @param endDate          End date and time.
     * @param registrationStart Registration start date and time.
     * @param registrationEnd   Registration end date and time.
     * @param price            Price for attending.
     * @param capacity         Maximum number of attendees.
     * @param currentEntrantsNumber Current number of entrants
     * @param waitingListCapacity   Maximum entrants in the waiting list
     * @param posterImageUrl   URL of the poster image.
     * @param qrCodeHash       Hashed QR code data.
     * @param entrants       hashmap of entrants with field describing relation to event
     * @param createdAt        Creation timestamp.
     * @param status           Status of the event.
     * @param geolocationRequired  Switch enabling geolocation requirement.
     * @param eventLocation     Events location
     */
    public Event(String id, String facilityId, String name, String description, Date startDate, Date endDate,
                 Date registrationStart, Date registrationEnd, double price, int capacity, int currentEntrantsNumber,
                 int waitingListCapacity, String posterImageUrl, String qrCodeHash, Map<String, String> entrants,
                 Date createdAt, String status, boolean geolocationRequired, String eventLocation) {
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
        this.entrants = entrants != null ? entrants : new HashMap<>();
        this.createdAt = createdAt;
        this.status = status;
        this.geolocationRequired = geolocationRequired;
        this.eventLocation = eventLocation;
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
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
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
     * Gets the hashed QR code data.
     *
     * @return QR code hash.
     */
    public String getQrCodeHash() {
        return qrCodeHash;
    }

    /**
     * Sets the hashed QR code data.
     *
     * @param qrCodeHash QR code hash.
     */
    public void setQrCodeHash(String qrCodeHash) {
        this.qrCodeHash = qrCodeHash;
    }


    /**
     * gets entrants hashmap.
     *
     * @return entrants  hashmap of entrants with field describing relation to event
     */
    public Map<String, String> getEntrants() {
        return entrants;
    }

    /**
     * sets entrants hashmap.
     *
     * @param entrants hashmap of entrants with field describing relation to event
     */
    public void setEntrants(Map<String, String> entrants) {
        this.entrants = entrants;
    }

    /**
     * Adds or updates an entrant's status.
     *
     * @param entrantId The ID of the entrant.
     * @param status    The status of the entrant (e.g., "enrolled", "selected", "confirmed", "declined").
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
     * Grabs the Event Location.
     *
     */
    public String getEventLocation() {
        return eventLocation;
    }

    /**
     * Sets new location of event
     *
     * @param eventLocation new eventLocation
     */
    public void setEventLocation(String eventLocation) {
        this.eventLocation = eventLocation;
    }


    /**
     * Grabs the gelocation requirement status.
     *
     */
    public boolean isGeolocationRequired() {
        return geolocationRequired;
    }

    /**
     * Sets geolocation reqiurements status
     *
     * @param geolocationRequired changes geolocation requirement
     */
    public void setGeolocationRequired(boolean geolocationRequired) {
        this.geolocationRequired = geolocationRequired;
    }

    /**
     * Grabs capacity number for the waiting list
     *
     */
    public int getWaitingListCapacity() {
        return waitingListCapacity;
    }

    /**
     * Sets geolocation reqiurements status
     *
     * @param waitingListCapacity changes the capcity of the waiting list
     */
    public void setWaitingListCapacity(int waitingListCapacity) {
        this.waitingListCapacity = waitingListCapacity;
    }

    /**
     * Grabs current number of entrants in the event
     *
     */
    public int getCurrentEntrantsNumber() {
        return currentEntrantsNumber;
    }

    /**
     * Sets the current number of entrants in the event
     *
     * @param currentEntrantsNumber changes the current number of entrants
     */
    public void setCurrentEntrantsNumber(int currentEntrantsNumber) {
        this.currentEntrantsNumber = currentEntrantsNumber;
    }
}
