package com.example.potato1_events;

import com.google.firebase.firestore.Exclude;

/**
 * Represents a user within the Potato1 Events application.
 * This class includes all necessary information for entrants, organizers, and administrators.
 */
public class User {
    private String userId; // Firebase Authentication UID
    private String role; // Entrant, Organizer, Admin
    private String name;
    private String email;
    private String phoneNumber;
    private String profilePictureUrl;
    private boolean notificationsEnabled; // For opting in/out of notifications
    private long createdAt; // Timestamp of account creation

    /**
     * Default constructor required for Firestore serialization.
     */
    public User() {
    }

    /**
     * Parameterized constructor to create a User object.
     *
     * @param userId             The unique identifier (UID) from Firebase Authentication.
     * @param role               The role of the user (Entrant, Organizer, Admin).
     * @param name               The user's full name.
     * @param email              The user's email address.
     * @param phoneNumber        The user's phone number.
     * @param profilePictureUrl  The URL to the user's profile picture.
     * @param notificationsEnabled Whether the user has opted in for notifications.
     * @param createdAt          Timestamp of when the user was created.
     */
    public User(String userId, String role, String name, String email, String phoneNumber,
                String profilePictureUrl, boolean notificationsEnabled, long createdAt) {
        this.userId = userId;
        this.role = role;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.profilePictureUrl = profilePictureUrl;
        this.notificationsEnabled = notificationsEnabled;
        this.createdAt = createdAt;
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
     * Gets the URL of the user's profile picture.
     *
     * @return The profile picture URL.
     */
    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    /**
     * Sets the URL of the user's profile picture.
     *
     * @param profilePictureUrl The profile picture URL.
     */
    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
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
}
