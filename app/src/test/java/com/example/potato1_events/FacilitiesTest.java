// File: FacilityTest.java
package com.example.potato1_events;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for the {@link Facility} model class.
 * <p>
 * These tests verify the correctness of the {@link Facility} class's constructors,
 * getters, setters, and helper methods.
 * </p>
 */
public class FacilitiesTest { // Ensure the class name matches the filename

    private Facility facility;
    private String facilityId;
    private String facilityName;
    private String facilityAddress;
    private String facilityDescription;
    private String facilityPhotoUrl;
    private List<String> eventIds;
    private Date createdAt;

    /**
     * Sets up the test environment before each test case.
     * <p>
     * Initializes a {@link Facility} instance with predefined test data to be used in subsequent tests.
     * </p>
     */
    @Before
    public void setUp() {
        // Initialize test data
        facilityId = "facility123";
        facilityName = "Grand Hall";
        facilityAddress = "123 Main St, Springfield";
        facilityDescription = "A spacious hall suitable for large events.";
        facilityPhotoUrl = "http://example.com/photos/facility123.jpg";
        eventIds = new ArrayList<>();
        eventIds.add("event1");
        eventIds.add("event2");
        createdAt = new Date();

        // Initialize Facility instance using parameterized constructor
        facility = new Facility(
                facilityId,
                facilityName,
                facilityAddress,
                facilityDescription,
                facilityPhotoUrl,
                eventIds,
                createdAt
        );
    }

    /**
     * Tests the default constructor of the {@link Facility} class.
     * <p>
     * Ensures that the default constructor initializes the {@code eventIds} list to prevent {@link NullPointerException}.
     * </p>
     */
    @Test
    public void testDefaultConstructor() {
        Facility defaultFacility = new Facility();
        assertNotNull("eventIds should be initialized as an empty list", defaultFacility.getEventIds());
        assertTrue("eventIds should be empty", defaultFacility.getEventIds().isEmpty());
    }

    /**
     * Tests the parameterized constructor of the {@link Facility} class.
     * <p>
     * Verifies that all fields are correctly assigned when using the parameterized constructor.
     * </p>
     */
    @Test
    public void testParameterizedConstructor() {
        assertEquals("Facility ID should match", facilityId, facility.getId());
        assertEquals("Facility name should match", facilityName, facility.getFacilityName());
        assertEquals("Facility address should match", facilityAddress, facility.getFacilityAddress());
        assertEquals("Facility description should match", facilityDescription, facility.getFacilityDescription());
        assertEquals("Facility photo URL should match", facilityPhotoUrl, facility.getFacilityPhotoUrl());
        assertEquals("Event IDs should match", eventIds, facility.getEventIds());
        assertEquals("Creation timestamp should match", createdAt, facility.getCreatedAt());
    }

    /**
     * Tests the setter methods of the {@link Facility} class.
     * <p>
     * Ensures that each setter correctly updates the corresponding field.
     * </p>
     */
    @Test
    public void testSetters() {
        // Update fields
        String newFacilityName = "Conference Center";
        String newFacilityAddress = "456 Elm St, Springfield";
        String newFacilityDescription = "Modern conference center with state-of-the-art facilities.";
        String newFacilityPhotoUrl = "http://example.com/photos/facility456.jpg";
        List<String> newEventIds = new ArrayList<>();
        newEventIds.add("event3");
        newEventIds.add("event4");
        Date newCreatedAt = new Date();

        facility.setId("facility456");
        facility.setFacilityName(newFacilityName);
        facility.setFacilityAddress(newFacilityAddress);
        facility.setFacilityDescription(newFacilityDescription);
        facility.setFacilityPhotoUrl(newFacilityPhotoUrl);
        facility.setEventIds(newEventIds);
        facility.setCreatedAt(newCreatedAt);

        // Assertions
        assertEquals("Facility ID should be updated", "facility456", facility.getId());
        assertEquals("Facility name should be updated", newFacilityName, facility.getFacilityName());
        assertEquals("Facility address should be updated", newFacilityAddress, facility.getFacilityAddress());
        assertEquals("Facility description should be updated", newFacilityDescription, facility.getFacilityDescription());
        assertEquals("Facility photo URL should be updated", newFacilityPhotoUrl, facility.getFacilityPhotoUrl());
        assertEquals("Event IDs should be updated", newEventIds, facility.getEventIds());
        assertEquals("Creation timestamp should be updated", newCreatedAt, facility.getCreatedAt());
    }

    /**
     * Tests the {@link Facility#addEventId(String)} method.
     * <p>
     * Verifies that a new event ID is successfully added to the {@code eventIds} list.
     * </p>
     */
    @Test
    public void testAddEventId() {
        String newEventId = "event3";
        facility.addEventId(newEventId);
        assertTrue("Event IDs should contain the newly added event", facility.getEventIds().contains(newEventId));
        assertEquals("Event IDs size should increment", 3, facility.getEventIds().size());
    }

    /**
     * Tests the {@link Facility#removeEventId(String)} method.
     * <p>
     * Ensures that an existing event ID is correctly removed from the {@code eventIds} list.
     * </p>
     */
    @Test
    public void testRemoveEventId() {
        String existingEventId = "event1";
        facility.removeEventId(existingEventId);
        assertFalse("Event IDs should not contain the removed event", facility.getEventIds().contains(existingEventId));
        assertEquals("Event IDs size should decrement", 1, facility.getEventIds().size());
    }

    /**
     * Tests the {@link Facility#removeEventId(String)} method when removing a non-existent event ID.
     * <p>
     * Verifies that attempting to remove an event ID that does not exist does not affect the {@code eventIds} list.
     * </p>
     */
    @Test
    public void testRemoveNonExistentEventId() {
        String nonExistentEventId = "event999";
        facility.removeEventId(nonExistentEventId);
        assertEquals("Event IDs size should remain unchanged", 2, facility.getEventIds().size());
        assertFalse("Event IDs should not contain the non-existent event", facility.getEventIds().contains(nonExistentEventId));
    }

    /**
     * Tests the {@link Facility#setEventIds(List)} method when setting the event IDs to {@code null}.
     * <p>
     * Ensures that {@code eventIds} is initialized as an empty list to maintain data integrity.
     * </p>
     */
    @Test
    public void testSetEventIds_Null() {
        facility.setEventIds(null);
        assertNotNull("eventIds should not be null after setting to null", facility.getEventIds());
        assertTrue("eventIds should be empty after setting to null", facility.getEventIds().isEmpty());
    }

    /**
     * Tests that the {@code createdAt} field can be {@code null} when not explicitly set.
     * <p>
     * Verifies that a new {@link Facility} instance without a creation timestamp has {@code createdAt} as {@code null}.
     * </p>
     */
    @Test
    public void testCreatedAt_Null() {
        Facility newFacility = new Facility();
        assertNull("createdAt should be null by default", newFacility.getCreatedAt());
    }




}
