package com.example.potato1_events;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user within the Potato1 Events application.
 * This class includes all necessary information for entrants, organizers, and administrators.
 */
@IgnoreExtraProperties // Ensures Firestore ignores any extra fields not mapped in this class
public class User {
    private String userId; // Firebase Authentication UID
    private String role; // Entrant, Organizer, Admin
    private String name;
    private String email;
    private String phoneNumber;
    private String imagePath; // Firebase Storage path for profile picture
    private boolean notificationsEnabled; // For opting in/out of notifications
    private long createdAt; // Timestamp of account creation
    private long updatedAt; // Timestamp of last update
    private boolean isActive; // Indicates if the user account is active
    private boolean isAdmin = false; // Indicates if the user has administrative privileges
    private String status; // Status related to event participation (e.g., Waiting List, Accepted, Rejected)

    private List<String> eventsJoined; // List of event document IDs

    private Double latitude;  // User's current latitude
    private Double longitude; // User's current longitude

    /**
     * Default constructor required for Firestore serialization.
     * Initializes isAdmin to false by default.
     */
    public User() {
        this.isAdmin = false;
        this.eventsJoined = new ArrayList<>();
    }

    /**
     * Parameterized constructor to create a User object.
     *
     * @param userId               The unique identifier (UID) from Firebase Authentication.
     * @param role                 The role of the user (Entrant, Organizer, Admin).
     * @param name                 The user's full name.
     * @param email                 The user's email address.
     * @param phoneNumber          The user's phone number.
     * @param imagePath            The Firebase Storage path to the user's profile picture.
     * @param notificationsEnabled Whether the user has opted in for notifications.
     * @param createdAt            Timestamp of when the user was created.
     */
    public User(String userId, String role, String name, String email, String phoneNumber,
                String imagePath, boolean notificationsEnabled, long createdAt) {
        this.userId = userId;
        this.role = role;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.imagePath = imagePath;
        this.notificationsEnabled = notificationsEnabled;
        this.createdAt = createdAt;
        this.isAdmin = false; // Ensure isAdmin is false by default
        this.eventsJoined = new ArrayList<>();
    }

    // Getters and Setters

    /**
     * Gets the user's unique identifier (UID).
     *
     * @return The user's UID.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user's unique identifier (UID).
     *
     * @param userId The user's UID.
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the role of the user.
     *
     * @return The user's role.
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the role of the user.
     *
     * @param role The user's role.
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Gets the user's full name.
     *
     * @return The user's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user's full name.
     *
     * @param name The user's name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the user's email address.
     *
     * @return The user's email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address.
     *
     * @param email The user's email.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the user's phone number.
     *
     * @return The user's phone number.
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Sets the user's phone number.
     *
     * @param phoneNumber The user's phone number.
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Gets the Firebase Storage path of the user's profile picture.
     *
     * @return The profile picture storage path.
     */
    public String getImagePath() {
        return imagePath;
    }

    /**
     * Sets the Firebase Storage path of the user's profile picture.
     *
     * @param imagePath The profile picture storage path.
     */
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    /**
     * Checks if the user has enabled notifications.
     *
     * @return True if notifications are enabled, false otherwise.
     */
    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    /**
     * Sets the user's notification preference.
     *
     * @param notificationsEnabled True to enable notifications, false to disable.
     */
    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    /**
     * Gets the timestamp of when the user account was created.
     *
     * @return The creation timestamp.
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the timestamp of when the user account was created.
     *
     * @param createdAt The creation timestamp.
     */
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    /**
     * Checks if the user has administrative privileges.
     * This field is intended to be managed directly in the database.
     *
     * @return True if the user is an admin, false otherwise.
     */
    public boolean isAdmin() {
        return isAdmin;
    }

    /**
     * Sets the administrative status of the user.
     * This method is private to prevent modification from application code.
     * Only the database can modify this field.
     *
     * @param admin True to grant admin privileges, false to revoke.
     */
    @Exclude // Prevents this setter from being used by Firestore serialization
    public void setAdmin(boolean admin) {
        this.isAdmin = admin;
    }

    /**
     * Gets the entrant's status related to event participation.
     *
     * @return The entrant's status (e.g., Waiting List, Accepted, Rejected).
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the entrant's status related to event participation.
     *
     * @param status The entrant's status (e.g., Waiting List, Accepted, Rejected).
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the list of event IDs that the user has joined.
     *
     * @return A list of event document IDs.
     */
    public List<String> getEventsJoined() {
        return eventsJoined;
    }

    /**
     * Sets the list of event IDs that the user has joined.
     *
     * @param eventsJoined A list of event document IDs.
     */
    public void setEventsJoined(List<String> eventsJoined) {
        this.eventsJoined = eventsJoined;
    }

    /**
     * Gets the user's latitude.
     *
     * @return The user's current latitude.
     */
    public Double getLatitude() {
        return latitude;
    }

    /**
     * Sets the user's latitude.
     *
     * @param latitude The user's current latitude.
     */
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    /**
     * Gets the user's longitude.
     *
     * @return The user's current longitude.
     */
    public Double getLongitude() {
        return longitude;
    }

    /**
     * Sets the user's longitude.
     *
     * @param longitude The user's current longitude.
     */
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
