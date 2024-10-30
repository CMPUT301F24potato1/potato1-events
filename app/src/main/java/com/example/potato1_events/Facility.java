package com.example.potato1_events;

import com.google.firebase.firestore.ServerTimestamp;
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
    private String name;

    /**
     * Address of the facility.
     */
    private String address;

    /**
     * Latitude for geolocation verification.
     */
    private double latitude;

    /**
     * Longitude for geolocation verification.
     */
    private double longitude;

    /**
     * Contact email for the facility.
     */
    private String contactEmail;

    /**
     * Contact phone number for the facility.
     */
    private String contactPhone;

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
        // Default constructor
    }

    /**
     * Parameterized constructor to create a Facility instance.
     *
     * @param id            Unique identifier for the facility.
     * @param name          Name of the facility.
     * @param address       Address of the facility.
     * @param latitude      Latitude for geolocation.
     * @param longitude     Longitude for geolocation.
     * @param contactEmail  Contact email.
     * @param contactPhone  Contact phone number.
     * @param eventIds      List of associated event IDs.
     * @param createdAt     Creation timestamp.
     */
    public Facility(String id, String name, String address, double latitude, double longitude,
                    String contactEmail, String contactPhone, List<String> eventIds, Date createdAt) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.eventIds = eventIds;
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
    public String getName() {
        return name;
    }

    /**
     * Sets the facility name.
     *
     * @param name Facility name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the facility address.
     *
     * @return Facility address.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Sets the facility address.
     *
     * @param address Facility address.
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Gets the latitude for geolocation.
     *
     * @return Latitude.
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Sets the latitude for geolocation.
     *
     * @param latitude Latitude.
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * Gets the longitude for geolocation.
     *
     * @return Longitude.
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Sets the longitude for geolocation.
     *
     * @param longitude Longitude.
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /**
     * Gets the contact email.
     *
     * @return Contact email.
     */
    public String getContactEmail() {
        return contactEmail;
    }

    /**
     * Sets the contact email.
     *
     * @param contactEmail Contact email.
     */
    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    /**
     * Gets the contact phone number.
     *
     * @return Contact phone number.
     */
    public String getContactPhone() {
        return contactPhone;
    }

    /**
     * Sets the contact phone number.
     *
     * @param contactPhone Contact phone number.
     */
    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
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
        this.eventIds = eventIds;
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
        this.eventIds.add(eventId);
    }

    /**
     * Removes an event ID from the facility's event list.
     *
     * @param eventId Event ID to remove.
     */
    public void removeEventId(String eventId) {
        this.eventIds.remove(eventId);
    }
}
