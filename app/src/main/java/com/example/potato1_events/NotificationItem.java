// File: NotificationItem.java
package com.example.potato1_events;

/**
 * Represents a notification item within the Potato1 Events application.
 * This class encapsulates all relevant details of a notification, such as its title, message,
 * associated event and user IDs, type, read status, and current status.
 * It is used to display notifications to users and manage their states.
 */
public class NotificationItem {

    /**
     * Unique identifier for the notification.
     */
    private String id;

    /**
     * Title of the notification.
     */
    private String title;

    /**
     * Detailed message of the notification.
     */
    private String message;

    /**
     * Identifier of the associated event.
     */
    private String eventId;

    /**
     * Identifier of the user who receives the notification.
     */
    private String userId;

    /**
     * Type of the notification (e.g., "selection", "acceptance", "decline").
     */
    private String type;

    /**
     * Flag indicating whether the notification has been read.
     */
    private boolean isRead;

    /**
     * Current status of the notification.
     */
    private String status;

    /**
     * Default constructor required for Firestore serialization.
     * Initializes a new instance of NotificationItem with default values.
     */
    public NotificationItem() {
        // Empty constructor needed for Firestore serialization
    }

    /**
     * Retrieves the current status of the notification.
     *
     * @return A string representing the notification's status.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status of the notification.
     *
     * @param status A string representing the new status of the notification.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Retrieves the unique identifier of the notification.
     *
     * @return A string representing the notification's ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the notification.
     *
     * @param id A string representing the new ID of the notification.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Retrieves the title of the notification.
     *
     * @return A string representing the notification's title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the notification.
     *
     * @param title A string representing the new title of the notification.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Retrieves the message content of the notification.
     *
     * @return A string representing the notification's message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message content of the notification.
     *
     * @param message A string representing the new message of the notification.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Retrieves the identifier of the associated event.
     *
     * @return A string representing the event's ID.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the identifier of the associated event.
     *
     * @param eventId A string representing the new event ID.
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Retrieves the identifier of the user who receives the notification.
     *
     * @return A string representing the user's ID.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the identifier of the user who receives the notification.
     *
     * @param userId A string representing the new user ID.
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Retrieves the type of the notification.
     *
     * @return A string representing the notification's type.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the notification.
     *
     * @param type A string representing the new type of the notification.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Checks whether the notification has been read.
     *
     * @return True if the notification has been read; otherwise, false.
     */
    public boolean isRead() {
        return isRead;
    }

    /**
     * Sets the read status of the notification.
     *
     * @param read A boolean indicating the new read status of the notification.
     */
    public void setRead(boolean read) {
        isRead = read;
    }
}
