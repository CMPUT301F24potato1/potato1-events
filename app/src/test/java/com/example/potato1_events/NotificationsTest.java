package com.example.potato1_events;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the {@link NotificationItem} class.
 * This class tests the constructors, getters, setters, and ensures proper handling of notification data.
 */
public class NotificationsTest {

    private NotificationItem notificationItem;

    /**
     * Sets up the test environment before each test.
     * Initializes a NotificationItem object with sample data.
     */
    @Before
    public void setUp() {
        notificationItem = new NotificationItem();
        notificationItem.setId("notif123");
        notificationItem.setTitle("Event Update");
        notificationItem.setMessage("You have been selected for the event.");
        notificationItem.setEventId("event456");
        notificationItem.setUserId("user789");
        notificationItem.setType("selection");
        notificationItem.setRead(false);
        notificationItem.setStatus("unread");
    }

    /**
     * Tests the default constructor of the NotificationItem class.
     * Verifies that the object is initialized.
     */
    @Test
    public void testDefaultConstructor() {
        NotificationItem defaultNotification = new NotificationItem();
        assertNotNull(defaultNotification);
    }

    /**
     * Tests setting and getting the notification ID.
     */
    @Test
    public void testSetAndGetId() {
        String newId = "notif456";
        notificationItem.setId(newId);
        assertEquals(newId, notificationItem.getId());
    }

    /**
     * Tests setting and getting the notification title.
     */
    @Test
    public void testSetAndGetTitle() {
        String newTitle = "New Event Notification";
        notificationItem.setTitle(newTitle);
        assertEquals(newTitle, notificationItem.getTitle());
    }

    /**
     * Tests setting and getting the notification message.
     */
    @Test
    public void testSetAndGetMessage() {
        String newMessage = "Your event has been updated.";
        notificationItem.setMessage(newMessage);
        assertEquals(newMessage, notificationItem.getMessage());
    }

    /**
     * Tests setting and getting the event ID associated with the notification.
     */
    @Test
    public void testSetAndGetEventId() {
        String newEventId = "event789";
        notificationItem.setEventId(newEventId);
        assertEquals(newEventId, notificationItem.getEventId());
    }

    /**
     * Tests setting and getting the user ID associated with the notification.
     */
    @Test
    public void testSetAndGetUserId() {
        String newUserId = "user123";
        notificationItem.setUserId(newUserId);
        assertEquals(newUserId, notificationItem.getUserId());
    }

    /**
     * Tests setting and getting the notification type.
     */
    @Test
    public void testSetAndGetType() {
        String newType = "acceptance";
        notificationItem.setType(newType);
        assertEquals(newType, notificationItem.getType());
    }

    /**
     * Tests setting and checking if the notification is read.
     */
    @Test
    public void testSetAndIsRead() {
        notificationItem.setRead(true);
        assertTrue(notificationItem.isRead());
        notificationItem.setRead(false);
        assertFalse(notificationItem.isRead());
    }

    /**
     * Tests setting and getting the notification status.
     */
    @Test
    public void testSetAndGetStatus() {
        String newStatus = "read";
        notificationItem.setStatus(newStatus);
        assertEquals(newStatus, notificationItem.getStatus());
    }

    /**
     * Tests setting null values for certain fields.
     */
    @Test
    public void testSetAndGetNullValues() {
        notificationItem.setTitle(null);
        assertNull(notificationItem.getTitle());

        notificationItem.setMessage(null);
        assertNull(notificationItem.getMessage());

        notificationItem.setEventId(null);
        assertNull(notificationItem.getEventId());

        notificationItem.setUserId(null);
        assertNull(notificationItem.getUserId());

        notificationItem.setType(null);
        assertNull(notificationItem.getType());

        notificationItem.setStatus(null);
        assertNull(notificationItem.getStatus());
    }
}

