package com.example.potato1_events;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for the {@link User} class.
 */
public class UsersTest {

    private User defaultUser;
    private User parameterizedUser;
    private List<String> eventsJoined;

    /**
     * Sets up the test environment before each test case.
     * Initializes default and parameterized User objects.
     */
    @Before
    public void setUp() {
        // Initialize default user
        defaultUser = new User();

        // Initialize parameterized user with sample data
        eventsJoined = new ArrayList<>();
        eventsJoined.add("event1");
        eventsJoined.add("event2");

        parameterizedUser = new User(
                "user123",
                "Entrant",
                "John Doe",
                "john.doe@example.com",
                "1234567890",
                "images/profile_pictures/user123.jpg",
                true,
                1627891200000L // Created at timestamp
        );
        parameterizedUser.setEventsJoined(eventsJoined);
        parameterizedUser.setStatus("Active");
    }

    /**
     * Tests the default constructor of the {@link User} class.
     * Verifies that default values are set correctly.
     */
    @Test
    public void testDefaultConstructor() {
        assertNotNull("Default user should not be null", defaultUser);
        assertFalse("Default user should not be admin", defaultUser.isAdmin());
        assertNotNull("Events joined list should be initialized", defaultUser.getEventsJoined());
        assertTrue("Events joined list should be empty", defaultUser.getEventsJoined().isEmpty());
    }

    /**
     * Tests the parameterized constructor of the {@link User} class.
     * Checks if all fields are initialized correctly.
     */
    @Test
    public void testParameterizedConstructor() {
        assertEquals("User ID should match", "user123", parameterizedUser.getUserId());
        assertEquals("Role should match", "Entrant", parameterizedUser.getRole());
        assertEquals("Name should match", "John Doe", parameterizedUser.getName());
        assertEquals("Email should match", "john.doe@example.com", parameterizedUser.getEmail());
        assertEquals("Phone number should match", "1234567890", parameterizedUser.getPhoneNumber());
        assertEquals("Image path should match", "images/profile_pictures/user123.jpg", parameterizedUser.getImagePath());
        assertTrue("Notifications should be enabled", parameterizedUser.isNotificationsEnabled());
        assertEquals("Creation timestamp should match", 1627891200000L, parameterizedUser.getCreatedAt());
        assertFalse("User should not be admin", parameterizedUser.isAdmin());
        assertEquals("Events joined should match", eventsJoined, parameterizedUser.getEventsJoined());
        assertEquals("Status should match", "Active", parameterizedUser.getStatus());
    }

    /**
     * Tests the getters and setters for all fields in the {@link User} class.
     * Ensures that values are set and retrieved correctly.
     */
    @Test
    public void testGettersAndSetters() {
        defaultUser.setUserId("user456");
        assertEquals("User ID should match", "user456", defaultUser.getUserId());

        defaultUser.setRole("Organizer");
        assertEquals("Role should match", "Organizer", defaultUser.getRole());

        defaultUser.setName("Jane Smith");
        assertEquals("Name should match", "Jane Smith", defaultUser.getName());

        defaultUser.setEmail("jane.smith@example.com");
        assertEquals("Email should match", "jane.smith@example.com", defaultUser.getEmail());

        defaultUser.setPhoneNumber("0987654321");
        assertEquals("Phone number should match", "0987654321", defaultUser.getPhoneNumber());

        defaultUser.setImagePath("images/profile_pictures/user456.jpg");
        assertEquals("Image path should match", "images/profile_pictures/user456.jpg", defaultUser.getImagePath());

        defaultUser.setNotificationsEnabled(false);
        assertFalse("Notifications should be disabled", defaultUser.isNotificationsEnabled());

        defaultUser.setCreatedAt(1627977600000L);
        assertEquals("Creation timestamp should match", 1627977600000L, defaultUser.getCreatedAt());

        defaultUser.setStatus("Pending");
        assertEquals("Status should match", "Pending", defaultUser.getStatus());

        List<String> newEventsJoined = new ArrayList<>();
        newEventsJoined.add("event3");
        defaultUser.setEventsJoined(newEventsJoined);
        assertEquals("Events joined should match", newEventsJoined, defaultUser.getEventsJoined());

        // isAdmin remains false since setAdmin is private
        assertFalse("User should not be admin", defaultUser.isAdmin());
    }

    /**
     * Tests adding events to the user's eventsJoined list.
     * Ensures that events are added correctly.
     */
    @Test
    public void testEventsJoinedList() {
        defaultUser.getEventsJoined().add("event100");
        assertEquals("Events joined list size should be 1", 1, defaultUser.getEventsJoined().size());
        assertEquals("First event should match", "event100", defaultUser.getEventsJoined().get(0));
    }

    /**
     * Tests that the eventsJoined list is modifiable.
     * Verifies that changes to the list are reflected in the user object.
     */
    @Test
    public void testEventsJoinedListMutability() {
        List<String> events = defaultUser.getEventsJoined();
        events.add("event200");
        assertEquals("Events joined list size should be 1", 1, defaultUser.getEventsJoined().size());
    }


    /**
     * Tests the inequality of two User objects with different userIds.
     */
    @Test
    public void testUserEquality_DifferentUserId() {
        User user1 = new User();
        user1.setUserId("user111");

        User user2 = new User();
        user2.setUserId("user222");

        assertNotEquals("Users with different IDs should not be equal", user1, user2);
    }


    /**
     * Tests how the {@link User} class handles null values in setters.
     */
    @Test
    public void testNullValues() {
        defaultUser.setName(null);
        assertNull("Name should be null", defaultUser.getName());

        defaultUser.setEmail(null);
        assertNull("Email should be null", defaultUser.getEmail());

        defaultUser.setPhoneNumber(null);
        assertNull("Phone number should be null", defaultUser.getPhoneNumber());

        defaultUser.setImagePath(null);
        assertNull("Image path should be null", defaultUser.getImagePath());

        defaultUser.setStatus(null);
        assertNull("Status should be null", defaultUser.getStatus());

        defaultUser.setEventsJoined(null);
        assertNull("Events joined list should be null", defaultUser.getEventsJoined());
    }

    /**
     * Tests setting negative timestamps in the {@link User} class.
     */
    @Test
    public void testNegativeTimestamps() {
        defaultUser.setCreatedAt(-1L);
        assertEquals("Created at timestamp should be -1", -1L, defaultUser.getCreatedAt());
    }

    /**
     * Tests the default values of boolean fields in the {@link User} class.
     */
    @Test
    public void testDefaultBooleanValues() {
        assertFalse("Notifications should be disabled by default", defaultUser.isNotificationsEnabled());
        assertFalse("User should not be admin by default", defaultUser.isAdmin());
    }
}
