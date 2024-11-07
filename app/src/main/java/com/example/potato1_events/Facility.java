// File: Facility.java
package com.example.potato1_events;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents a Facility where events are hosted.
 * This class is synchronized with the Firebase Firestore "Facilities" collection.
 */
public class Facility {

    /**
     * Unique identifier for the facility (Firestore document ID).
     */
    private String id;

    /**
     * Name of the facility.
     */
    private String facilityName;

    /**
     * Address of the facility.
     */
    private String facilityAddress;

    /**
     * Description of the facility.
     */
    private String facilityDescription;

    /**
     * URL of the facility photo.
     */
    private String facilityPhotoUrl;

    /**
     * List of event IDs associated with this facility.
     */
    private List<String> eventIds;

    /**
     * Timestamp of when the facility was created.
     */
    @ServerTimestamp
    private Date createdAt;

    /**
     * Default constructor required for Firebase deserialization.
     */
    public Facility() {
        // Initialize eventIds to prevent NullPointerException
        this.eventIds = new ArrayList<>();
    }

    /**
     * Parameterized constructor to create a Facility instance.
     *
     * @param id                   Unique identifier for the facility.
     * @param facilityName         Name of the facility.
     * @param facilityAddress      Address of the facility.
     * @param facilityDescription  Description of the facility.
     * @param facilityPhotoUrl     URL of the facility photo.
     * @param eventIds             List of associated event IDs.
     * @param createdAt            Creation timestamp.
     */
    public Facility(String id, String facilityName, String facilityAddress, String facilityDescription,
                    String facilityPhotoUrl, List<String> eventIds, Date createdAt) {
        this.id = id;
        this.facilityName = facilityName;
        this.facilityAddress = facilityAddress;
        this.facilityDescription = facilityDescription;
        this.facilityPhotoUrl = facilityPhotoUrl;
        this.eventIds = eventIds != null ? eventIds : new ArrayList<>();
        this.createdAt = createdAt;
    }

    // Getters and Setters

    /**
     * Gets the facility ID.
     *
     * @return Facility ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the facility ID.
     *
     * @param id Facility ID.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the facility name.
     *
     * @return Facility name.
     */
    public String getFacilityName() {
        return facilityName;
    }

    /**
     * Sets the facility name.
     *
     * @param facilityName Facility name.
     */
    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    /**
     * Gets the facility address.
     *
     * @return Facility address.
     */
    public String getFacilityAddress() {
        return facilityAddress;
    }

    /**
     * Sets the facility address.
     *
     * @param facilityAddress Facility address.
     */
    public void setFacilityAddress(String facilityAddress) {
        this.facilityAddress = facilityAddress;
    }

    /**
     * Gets the facility description.
     *
     * @return Facility description.
     */
    public String getFacilityDescription() {
        return facilityDescription;
    }

    /**
     * Sets the facility description.
     *
     * @param facilityDescription Facility description.
     */
    public void setFacilityDescription(String facilityDescription) {
        this.facilityDescription = facilityDescription;
    }

    /**
     * Gets the facility photo URL.
     *
     * @return Facility photo URL.
     */
    public String getFacilityPhotoUrl() {
        return facilityPhotoUrl;
    }

    /**
     * Sets the facility photo URL.
     *
     * @param facilityPhotoUrl Facility photo URL.
     */
    public void setFacilityPhotoUrl(String facilityPhotoUrl) {
        this.facilityPhotoUrl = facilityPhotoUrl;
    }

    /**
     * Gets the list of associated event IDs.
     *
     * @return List of event IDs.
     */
    public List<String> getEventIds() {
        return eventIds;
    }

    /**
     * Sets the list of associated event IDs.
     *
     * @param eventIds List of event IDs.
     */
    public void setEventIds(List<String> eventIds) {
        this.eventIds = eventIds != null ? eventIds : new ArrayList<>();
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
     * Adds an event ID to the facility's event list.
     *
     * @param eventId Event ID to add.
     */
    public void addEventId(String eventId) {
        if (this.eventIds == null) {
            this.eventIds = new ArrayList<>();
        }
        this.eventIds.add(eventId);
    }

    /**
     * Removes an event ID from the facility's event list.
     *
     * @param eventId Event ID to remove.
     */
    public void removeEventId(String eventId) {
        if (this.eventIds != null) {
            this.eventIds.remove(eventId);
        }
    }
}
