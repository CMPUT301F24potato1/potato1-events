package com.example.potato1_events;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Unit tests for the {@link Facility} class.
 * This class tests the constructors, getters, setters, and methods of the Facility class.
 */
public class FacilitiesTest {

    private Facility facility;
    private String id;
    private String facilityName;
    private String facilityAddress;
    private String facilityDescription;
    private String facilityPhotoUrl;
    private List<String> eventIds;
    private Date createdAt;

    /**
     * Sets up the test environment before each test.
     * Initializes a Facility object with sample data.
     */
    @Before
    public void setUp() {
        id = "facility123";
        facilityName = "Community Center";
        facilityAddress = "123 Main St";
        facilityDescription = "A local community center.";
        facilityPhotoUrl = "http://example.com/facility.jpg";
        eventIds = new ArrayList<>();
        eventIds.add("event1");
        eventIds.add("event2");
        createdAt = new Date();

        facility = new Facility(id, facilityName, facilityAddress, facilityDescription,
                facilityPhotoUrl, eventIds, createdAt);
    }

    /**
     * Tests the default constructor of the Facility class.
     * Verifies that eventIds is initialized.
     */
    @Test
    public void testDefaultConstructor() {
        Facility defaultFacility = new Facility();
        assertNotNull(defaultFacility);
        assertNotNull(defaultFacility.getEventIds());
        assertTrue(defaultFacility.getEventIds().isEmpty());
    }

    /**
     * Tests the parameterized constructor of the Facility class.
     * Verifies that all fields are initialized correctly.
     */
    @Test
    public void testParameterizedConstructor() {
        assertEquals(id, facility.getId());
        assertEquals(facilityName, facility.getFacilityName());
        assertEquals(facilityAddress, facility.getFacilityAddress());
        assertEquals(facilityDescription, facility.getFacilityDescription());
        assertEquals(facilityPhotoUrl, facility.getFacilityPhotoUrl());
        assertEquals(eventIds, facility.getEventIds());
        assertEquals(createdAt, facility.getCreatedAt());
    }

    /**
     * Tests setting and getting the facility ID.
     */
    @Test
    public void testSetAndGetId() {
        String newId = "facility456";
        facility.setId(newId);
        assertEquals(newId, facility.getId());
    }

    /**
     * Tests setting and getting the facility name.
     */
    @Test
    public void testSetAndGetFacilityName() {
        String newName = "New Community Center";
        facility.setFacilityName(newName);
        assertEquals(newName, facility.getFacilityName());
    }

    /**
     * Tests setting and getting the facility address.
     */
    @Test
    public void testSetAndGetFacilityAddress() {
        String newAddress = "456 Elm St";
        facility.setFacilityAddress(newAddress);
        assertEquals(newAddress, facility.getFacilityAddress());
    }

    /**
     * Tests setting and getting the facility description.
     */
    @Test
    public void testSetAndGetFacilityDescription() {
        String newDescription = "An updated description.";
        facility.setFacilityDescription(newDescription);
        assertEquals(newDescription, facility.getFacilityDescription());
    }

    /**
     * Tests setting and getting the facility photo URL.
     */
    @Test
    public void testSetAndGetFacilityPhotoUrl() {
        String newPhotoUrl = "http://example.com/newfacility.jpg";
        facility.setFacilityPhotoUrl(newPhotoUrl);
        assertEquals(newPhotoUrl, facility.getFacilityPhotoUrl());
    }

    /**
     * Tests setting and getting the list of event IDs.
     */
    @Test
    public void testSetAndGetEventIds() {
        List<String> newEventIds = new ArrayList<>();
        newEventIds.add("event3");
        newEventIds.add("event4");
        facility.setEventIds(newEventIds);
        assertEquals(newEventIds, facility.getEventIds());
    }

    /**
     * Tests setting null event IDs list.
     * Ensures it initializes to an empty list.
     */
    @Test
    public void testSetEventIdsNull() {
        facility.setEventIds(null);
        assertNotNull(facility.getEventIds());
        assertTrue(facility.getEventIds().isEmpty());
    }

    /**
     * Tests adding an event ID to the facility's event list.
     */
    @Test
    public void testAddEventId() {
        String newEventId = "event5";
        facility.addEventId(newEventId);
        assertTrue(facility.getEventIds().contains(newEventId));
    }

    /**
     * Tests removing an event ID from the facility's event list.
     */
    @Test
    public void testRemoveEventId() {
        String eventIdToRemove = "event1";
        facility.removeEventId(eventIdToRemove);
        assertFalse(facility.getEventIds().contains(eventIdToRemove));
    }

    /**
     * Tests setting and getting the creation timestamp.
     */
    @Test
    public void testSetAndGetCreatedAt() {
        Date newCreatedAt = new Date();
        facility.setCreatedAt(newCreatedAt);
        assertEquals(newCreatedAt, facility.getCreatedAt());
    }

    /**
     * Tests setting and getting null values for certain fields.
     */
    @Test
    public void testSetAndGetNullValues() {
        facility.setFacilityName(null);
        assertNull(facility.getFacilityName());

        facility.setFacilityAddress(null);
        assertNull(facility.getFacilityAddress());

        facility.setFacilityDescription(null);
        assertNull(facility.getFacilityDescription());

        facility.setFacilityPhotoUrl(null);
        assertNull(facility.getFacilityPhotoUrl());
    }

    /**
     * Tests that eventIds list is initialized when adding an event ID to a null list.
     */
    @Test
    public void testAddEventIdToNullList() {
        facility.setEventIds(null);
        facility.addEventId("event6");
        assertNotNull(facility.getEventIds());
        assertTrue(facility.getEventIds().contains("event6"));
    }

    /**
     * Tests that removing an event ID from a null list does not throw an exception.
     */
    @Test
    public void testRemoveEventIdFromNullList() {
        facility.setEventIds(null);
        // Should not throw an exception
        facility.removeEventId("event1");
        assertNotNull(facility.getEventIds());
        assertTrue(facility.getEventIds().isEmpty());
    }
}
