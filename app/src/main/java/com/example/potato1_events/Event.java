package com.example.potato1_events;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;

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
     * URL of the event poster image.
     */
    private String posterImageUrl;

    /**
     * Hashed QR code data for the event.
     */
    private String qrCodeHash;

    /**
     * List of entrant IDs in the waiting list.
     */
    private List<String> waitingList;

    /**
     * List of entrant IDs who have been selected.
     */
    private List<String> selectedEntrants;

    /**
     * List of entrant IDs who have confirmed their participation.
     */
    private List<String> confirmedEntrants;

    /**
     * List of entrant IDs who have declined the invitation.
     */
    private List<String> declinedEntrants;

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
     * Default constructor required for Firebase deserialization.
     */
    public Event() {
        // Default constructor
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
     * @param posterImageUrl   URL of the poster image.
     * @param qrCodeHash       Hashed QR code data.
     * @param waitingList      List of entrant IDs in waiting.
     * @param selectedEntrants List of selected entrant IDs.
     * @param confirmedEntrants List of confirmed entrant IDs.
     * @param declinedEntrants List of declined entrant IDs.
     * @param createdAt        Creation timestamp.
     * @param status           Status of the event.
     */
    public Event(String id, String facilityId, String name, String description, Date startDate, Date endDate,
                 Date registrationStart, Date registrationEnd, double price, int capacity, String posterImageUrl,
                 String qrCodeHash, List<String> waitingList, List<String> selectedEntrants,
                 List<String> confirmedEntrants, List<String> declinedEntrants, Date createdAt, String status) {
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
        this.posterImageUrl = posterImageUrl;
        this.qrCodeHash = qrCodeHash;
        this.waitingList = waitingList;
        this.selectedEntrants = selectedEntrants;
        this.confirmedEntrants = confirmedEntrants;
        this.declinedEntrants = declinedEntrants;
        this.createdAt = createdAt;
        this.status = status;
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
     * Gets the waiting list of entrant IDs.
     *
     * @return Waiting list.
     */
    public List<String> getWaitingList() {
        return waitingList;
    }

    /**
     * Sets the waiting list of entrant IDs.
     *
     * @param waitingList Waiting list.
     */
    public void setWaitingList(List<String> waitingList) {
        this.waitingList = waitingList;
    }

    /**
     * Gets the list of selected entrant IDs.
     *
     * @return Selected entrants.
     */
    public List<String> getSelectedEntrants() {
        return selectedEntrants;
    }

    /**
     * Sets the list of selected entrant IDs.
     *
     * @param selectedEntrants Selected entrants.
     */
    public void setSelectedEntrants(List<String> selectedEntrants) {
        this.selectedEntrants = selectedEntrants;
    }

    /**
     * Gets the list of confirmed entrant IDs.
     *
     * @return Confirmed entrants.
     */
    public List<String> getConfirmedEntrants() {
        return confirmedEntrants;
    }

    /**
     * Sets the list of confirmed entrant IDs.
     *
     * @param confirmedEntrants Confirmed entrants.
     */
    public void setConfirmedEntrants(List<String> confirmedEntrants) {
        this.confirmedEntrants = confirmedEntrants;
    }

    /**
     * Gets the list of declined entrant IDs.
     *
     * @return Declined entrants.
     */
    public List<String> getDeclinedEntrants() {
        return declinedEntrants;
    }

    /**
     * Sets the list of declined entrant IDs.
     *
     * @param declinedEntrants Declined entrants.
     */
    public void setDeclinedEntrants(List<String> declinedEntrants) {
        this.declinedEntrants = declinedEntrants;
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
     * Adds an entrant ID to the waiting list.
     *
     * @param entrantId Entrant ID to add.
     */
    public void addToWaitingList(String entrantId) {
        this.waitingList.add(entrantId);
    }

    /**
     * Removes an entrant ID from the waiting list.
     *
     * @param entrantId Entrant ID to remove.
     */
    public void removeFromWaitingList(String entrantId) {
        this.waitingList.remove(entrantId);
    }

    /**
     * Adds an entrant ID to the selected entrants list.
     *
     * @param entrantId Entrant ID to add.
     */
    public void addSelectedEntrant(String entrantId) {
        this.selectedEntrants.add(entrantId);
    }

    /**
     * Adds an entrant ID to the confirmed entrants list.
     *
     * @param entrantId Entrant ID to add.
     */
    public void addConfirmedEntrant(String entrantId) {
        this.confirmedEntrants.add(entrantId);
    }

    /**
     * Adds an entrant ID to the declined entrants list.
     *
     * @param entrantId Entrant ID to add.
     */
    public void addDeclinedEntrant(String entrantId) {
        this.declinedEntrants.add(entrantId);
    }

    /**
     * Updates the event status.
     *
     * @param newStatus New status of the event.
     */
    public void updateStatus(String newStatus) {
        this.status = newStatus;
    }
}
