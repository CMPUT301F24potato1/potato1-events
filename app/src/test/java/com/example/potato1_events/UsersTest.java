package com.example.potato1_events;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for the {@link User} class.
 * This class tests the constructors, getters, setters, and ensures proper handling of user data.
 */
public class UsersTest {

    private User user;

    private static final double DELTA = 0.0001;

    /**
     * Sets up the test environment before each test.
     * Initializes a User object with sample data.
     */
    @Before
    public void setUp() {
        user = new User(
                "user123",
                "Entrant",
                "John Doe",
                "john.doe@example.com",
                "123-456-7890",
                "images/profile/user123.jpg",
                true,
                System.currentTimeMillis()
        );
    }

    /**
     * Tests the default constructor of the User class.
     * Verifies initial values and defaults.
     */
    @Test
    public void testDefaultConstructor() {
        User defaultUser = new User();
        assertNotNull(defaultUser);
        assertFalse(defaultUser.isAdmin());
        assertNotNull(defaultUser.getEventsJoined());
        assertTrue(defaultUser.getEventsJoined().isEmpty());
    }

    /**
     * Tests the parameterized constructor of the User class.
     * Verifies that all fields are initialized correctly.
     */
    @Test
    public void testParameterizedConstructor() {
        assertEquals("user123", user.getUserId());
        assertEquals("Entrant", user.getRole());
        assertEquals("John Doe", user.getName());
        assertEquals("john.doe@example.com", user.getEmail());
        assertEquals("123-456-7890", user.getPhoneNumber());
        assertEquals("images/profile/user123.jpg", user.getImagePath());
        assertTrue(user.isNotificationsEnabled());
        assertFalse(user.isAdmin());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getEventsJoined());
        assertEquals(0, user.getEventsJoined().size());
    }

    /**
     * Tests setting and getting the user ID.
     */
    @Test
    public void testSetAndGetUserId() {
        user.setUserId("user456");
        assertEquals("user456", user.getUserId());
    }

    /**
     * Tests setting and getting the user role.
     */
    @Test
    public void testSetAndGetRole() {
        user.setRole("Organizer");
        assertEquals("Organizer", user.getRole());
    }

    /**
     * Tests setting and getting the user's name.
     */
    @Test
    public void testSetAndGetName() {
        user.setName("Jane Smith");
        assertEquals("Jane Smith", user.getName());
    }

    /**
     * Tests setting and getting the user's email.
     */
    @Test
    public void testSetAndGetEmail() {
        user.setEmail("jane.smith@example.com");
        assertEquals("jane.smith@example.com", user.getEmail());
    }

    /**
     * Tests setting and getting the user's phone number.
     */
    @Test
    public void testSetAndGetPhoneNumber() {
        user.setPhoneNumber("098-765-4321");
        assertEquals("098-765-4321", user.getPhoneNumber());
    }

    /**
     * Tests setting and getting the user's profile image path.
     */
    @Test
    public void testSetAndGetImagePath() {
        user.setImagePath("images/profile/user456.jpg");
        assertEquals("images/profile/user456.jpg", user.getImagePath());
    }

    /**
     * Tests setting and checking if notifications are enabled.
     */
    @Test
    public void testSetAndIsNotificationsEnabled() {
        user.setNotificationsEnabled(false);
        assertFalse(user.isNotificationsEnabled());
    }

    /**
     * Tests setting and getting the account creation timestamp.
     */
    @Test
    public void testSetAndGetCreatedAt() {
        long newCreatedAt = System.currentTimeMillis() - 100000;
        user.setCreatedAt(newCreatedAt);
        assertEquals(newCreatedAt, user.getCreatedAt());
    }




    /**
     * Tests checking if the user is an admin.
     * Since setAdmin is private, we cannot change its value.
     */
    @Test
    public void testIsAdmin() {
        assertFalse(user.isAdmin());
    }

    /**
     * Tests setting and getting the user's status.
     */
    @Test
    public void testSetAndGetStatus() {
        user.setStatus("Accepted");
        assertEquals("Accepted", user.getStatus());
    }

    /**
     * Tests setting and getting the list of events the user has joined.
     */
    @Test
    public void testSetAndGetEventsJoined() {
        List<String> events = new ArrayList<>();
        events.add("event1");
        events.add("event2");
        user.setEventsJoined(events);
        assertEquals(2, user.getEventsJoined().size());
        assertEquals("event1", user.getEventsJoined().get(0));
        assertEquals("event2", user.getEventsJoined().get(1));
    }

    /**
     * Tests adding an event to the eventsJoined list.
     */
    @Test
    public void testAddEventJoined() {
        user.getEventsJoined().add("event123");
        assertTrue(user.getEventsJoined().contains("event123"));
    }

    /**
     * Tests removing an event from the eventsJoined list.
     */
    @Test
    public void testRemoveEventJoined() {
        user.getEventsJoined().add("event123");
        user.getEventsJoined().remove("event123");
        assertFalse(user.getEventsJoined().contains("event123"));
    }

    /**
     * Tests setting and getting the user's latitude.
     */
    @Test
    public void testSetAndGetLatitude() {
        user.setLatitude(53.5461);
        assertEquals(53.5461, user.getLatitude(), DELTA);
    }

    /**
     * Tests setting and getting the user's longitude.
     */
    @Test
    public void testSetAndGetLongitude() {
        user.setLongitude(-113.4938);
        assertEquals(-113.4938, user.getLongitude(), DELTA);
    }

    /**
     * Tests that isAdmin cannot be changed via public methods.
     */
    @Test
    public void testSetAdmin() {
        // Since setAdmin is private, we cannot test it directly.
        // We can test that isAdmin remains false and cannot be changed through setters.
        assertFalse(user.isAdmin());
    }

    /**
     * Tests setting null values for certain fields.
     */
    @Test
    public void testNullValues() {
        user.setName(null);
        assertNull(user.getName());

        user.setEmail(null);
        assertNull(user.getEmail());

        user.setPhoneNumber(null);
        assertNull(user.getPhoneNumber());

        user.setImagePath(null);
        assertNull(user.getImagePath());
    }
}
