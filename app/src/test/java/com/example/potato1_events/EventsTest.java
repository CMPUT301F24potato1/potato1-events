package com.example.potato1_events;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for the {@link Event} class.
 * This class tests the constructors, getters, setters, and methods of the Event class.
 */
public class EventsTest {

    private Event event;
    private Map<String, String> initialEntrants;

    private String id;
    private String facilityId;
    private String name;
    private String description;
    private Date startDate;
    private Date endDate;
    private Date registrationStart;
    private Date registrationEnd;
    private double price;
    private int capacity;
    private int currentEntrantsNumber;
    private Integer waitingListCapacity;
    private String posterImageUrl;
    private String qrCodeHash;
    private Map<String, String> entrants;
    private Date createdAt;
    private String status;
    private boolean geolocationRequired;
    private String eventLocation;

    private static final double DELTA = 0.0001;

    /**
     * Sets up the test environment before each test.
     * Initializes an Event object with sample data.
     */
    @Before
    public void setUp() {
        id = "event123";
        facilityId = "facility456";
        name = "Sample Event";
        description = "This is a sample event.";
        Calendar cal = Calendar.getInstance();
        cal.set(2023, Calendar.SEPTEMBER, 1, 10, 0);
        startDate = cal.getTime();

        cal.set(2023, Calendar.SEPTEMBER, 1, 12, 0);
        endDate = cal.getTime();

        cal.set(2023, Calendar.AUGUST, 25, 0, 0);
        registrationStart = cal.getTime();

        cal.set(2023, Calendar.AUGUST, 31, 23, 59);
        registrationEnd = cal.getTime();

        price = 50.0;
        capacity = 100;
        currentEntrantsNumber = 0;
        waitingListCapacity = 50;
        posterImageUrl = "http://example.com/poster.jpg";
        qrCodeHash = "qrcodehash";
        entrants = new HashMap<>();
        createdAt = new Date();
        status = "Open";
        geolocationRequired = true;
        eventLocation = "123 Event Street";

        event = new Event(id, facilityId, name, description, startDate, endDate, registrationStart,
                registrationEnd, price, capacity, currentEntrantsNumber, waitingListCapacity,
                posterImageUrl, qrCodeHash, entrants, createdAt, status, geolocationRequired,
                eventLocation);
    }

    /**
     * Tests the parameterized constructor of the Event class.
     * Verifies that all fields are initialized correctly.
     */
    @Test
    public void testParameterizedConstructor() {
        assertEquals(id, event.getId());
        assertEquals(facilityId, event.getFacilityId());
        assertEquals(name, event.getName());
        assertEquals(description, event.getDescription());
        assertEquals(startDate, event.getStartDate());
        assertEquals(endDate, event.getEndDate());
        assertEquals(registrationStart, event.getRegistrationStart());
        assertEquals(registrationEnd, event.getRegistrationEnd());
        assertEquals(price, event.getPrice(), DELTA);
        assertEquals(capacity, event.getCapacity());
        assertEquals(currentEntrantsNumber, event.getCurrentEntrantsNumber());
        assertEquals(waitingListCapacity, event.getWaitingListCapacity());
        assertEquals(posterImageUrl, event.getPosterImageUrl());
        assertEquals(qrCodeHash, event.getQrCodeHash());
        assertEquals(entrants, event.getEntrants());
        assertEquals(createdAt, event.getCreatedAt());
        assertEquals(status, event.getStatus());
        assertEquals(geolocationRequired, event.isGeolocationRequired());
        assertEquals(eventLocation, event.getEventLocation());
        assertFalse(event.getRandomDrawPerformed());
    }

    /**
     * Tests setting and getting the event ID.
     */
    @Test
    public void testSetAndGetId() {
        String newId = "newEventId";
        event.setId(newId);
        assertEquals(newId, event.getId());
    }

    /**
     * Tests setting and getting the facility ID.
     */
    @Test
    public void testSetAndGetFacilityId() {
        String newFacilityId = "newFacilityId";
        event.setFacilityId(newFacilityId);
        assertEquals(newFacilityId, event.getFacilityId());
    }

    /**
     * Tests setting and getting the event name.
     */
    @Test
    public void testSetAndGetName() {
        String newName = "New Event Name";
        event.setName(newName);
        assertEquals(newName, event.getName());
    }

    /**
     * Tests setting and getting the event description.
     */
    @Test
    public void testSetAndGetDescription() {
        String newDescription = "New Description";
        event.setDescription(newDescription);
        assertEquals(newDescription, event.getDescription());
    }

    /**
     * Tests setting and getting the start date of the event.
     */
    @Test
    public void testSetAndGetStartDate() {
        Date newStartDate = new Date();
        event.setStartDate(newStartDate);
        assertEquals(newStartDate, event.getStartDate());
    }

    /**
     * Tests setting and getting the end date of the event.
     */
    @Test
    public void testSetAndGetEndDate() {
        Date newEndDate = new Date();
        event.setEndDate(newEndDate);
        assertEquals(newEndDate, event.getEndDate());
    }

    /**
     * Tests setting and getting the registration start date.
     */
    @Test
    public void testSetAndGetRegistrationStart() {
        Date newRegistrationStart = new Date();
        event.setRegistrationStart(newRegistrationStart);
        assertEquals(newRegistrationStart, event.getRegistrationStart());
    }

    /**
     * Tests setting and getting the registration end date.
     */
    @Test
    public void testSetAndGetRegistrationEnd() {
        Date newRegistrationEnd = new Date();
        event.setRegistrationEnd(newRegistrationEnd);
        assertEquals(newRegistrationEnd, event.getRegistrationEnd());
    }

    /**
     * Tests setting and getting the event price.
     */
    @Test
    public void testSetAndGetPrice() {
        double newPrice = 75.0;
        event.setPrice(newPrice);
        assertEquals(newPrice, event.getPrice(), DELTA);
    }

    /**
     * Tests setting and getting the event capacity.
     */
    @Test
    public void testSetAndGetCapacity() {
        int newCapacity = 150;
        event.setCapacity(newCapacity);
        assertEquals(newCapacity, event.getCapacity());
    }

    /**
     * Tests setting a negative capacity and expecting an IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetCapacityNegative() {
        event.setCapacity(-10);
    }

    /**
     * Tests setting and getting the current number of entrants.
     */
    @Test
    public void testSetAndGetCurrentEntrantsNumber() {
        int newCurrentEntrantsNumber = 10;
        event.setCurrentEntrantsNumber(newCurrentEntrantsNumber);
        assertEquals(newCurrentEntrantsNumber, event.getCurrentEntrantsNumber());
    }

    /**
     * Tests setting and getting the waiting list capacity.
     */
    @Test
    public void testSetAndGetWaitingListCapacity() {
        Integer newWaitingListCapacity = 60;
        event.setWaitingListCapacity(newWaitingListCapacity);
        assertEquals(newWaitingListCapacity, event.getWaitingListCapacity());
    }

    /**
     * Tests setting a negative waiting list capacity and expecting an IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetWaitingListCapacityNegative() {
        event.setWaitingListCapacity(-5);
    }

    /**
     * Tests setting and getting the poster image URL.
     */
    @Test
    public void testSetAndGetPosterImageUrl() {
        String newPosterImageUrl = "http://example.com/newposter.jpg";
        event.setPosterImageUrl(newPosterImageUrl);
        assertEquals(newPosterImageUrl, event.getPosterImageUrl());
    }

    /**
     * Tests setting and getting the QR code hash.
     */
    @Test
    public void testSetAndGetQrCodeHash() {
        String newQrCodeHash = "newqrcodehash";
        event.setQrCodeHash(newQrCodeHash);
        assertEquals(newQrCodeHash, event.getQrCodeHash());
    }

    /**
     * Tests setting and getting the entrants map.
     */
    @Test
    public void testSetAndGetEntrants() {
        Map<String, String> newEntrants = new HashMap<>();
        newEntrants.put("user789", "Accepted");
        event.setEntrants(newEntrants);
        assertEquals(newEntrants, event.getEntrants());
    }

    /**
     * Tests updating an entrant's status.
     */
    @Test
    public void testUpdateEntrantStatus() {
        String entrantId = "user123";
        String status = "Accepted";
        event.updateEntrantStatus(entrantId, status);
        assertEquals(status, event.getEntrants().get(entrantId));
    }

    /**
     * Tests removing an entrant from the entrants map.
     */
    @Test
    public void testRemoveEntrant() {
        String entrantId = "user123";
        event.updateEntrantStatus(entrantId, "Accepted");
        event.removeEntrant(entrantId);
        assertFalse(event.getEntrants().containsKey(entrantId));
    }

    /**
     * Tests setting and getting the creation timestamp.
     */
    @Test
    public void testSetAndGetCreatedAt() {
        Date newCreatedAt = new Date();
        event.setCreatedAt(newCreatedAt);
        assertEquals(newCreatedAt, event.getCreatedAt());
    }

    /**
     * Tests setting and getting the event status.
     */
    @Test
    public void testSetAndGetStatus() {
        String newStatus = "Closed";
        event.setStatus(newStatus);
        assertEquals(newStatus, event.getStatus());
    }

    /**
     * Tests updating the event status.
     */
    @Test
    public void testUpdateStatus() {
        String newStatus = "Completed";
        event.updateStatus(newStatus);
        assertEquals(newStatus, event.getStatus());
    }

    /**
     * Tests setting and checking if geolocation is required.
     */
    @Test
    public void testSetAndIsGeolocationRequired() {
        event.setGeolocationRequired(false);
        assertFalse(event.isGeolocationRequired());
    }

    /**
     * Tests setting and getting the random draw performed flag.
     */
    @Test
    public void testSetAndGetRandomDrawPerformed() {
        event.setRandomDrawPerformed(true);
        assertTrue(event.getRandomDrawPerformed());
    }

    /**
     * Tests setting and getting the event location.
     */
    @Test
    public void testSetAndGetEventLocation() {
        String newLocation = "456 New Street";
        event.setEventLocation(newLocation);
        assertEquals(newLocation, event.getEventLocation());
    }

    /**
     * Tests setting and checking if the waiting list is filled.
     */
    @Test
    public void testIsAndSetWaitingListFilled() {
        event.setWaitingListFilled(true);
        assertTrue(event.isWaitingListFilled());
        event.setWaitingListFilled(false);
        assertFalse(event.isWaitingListFilled());
    }

    /**
     * Tests the calculation of available capacity.
     * Ensures it accurately reflects accepted and selected entrants.
     */
    @Test
    public void testGetAvailableCapacity() {
        event.setCapacity(5);
        Map<String, String> entrants = new HashMap<>();
        entrants.put("user1", "Accepted");
        entrants.put("user2", "Selected");
        entrants.put("user3", "Declined");
        entrants.put("user4", "Not Selected");
        event.setEntrants(entrants);

        assertEquals("3", event.getAvailableCapacity());
    }

    /**
     * Tests getAvailableCapacity when there are no entrants.
     */
    @Test
    public void testGetAvailableCapacityNoEntrants() {
        event.setCapacity(10);
        event.setEntrants(null);
        assertEquals("10", event.getAvailableCapacity());
    }

    /**
     * Tests getAvailableCapacity when all spots are taken.
     */
    @Test
    public void testGetAvailableCapacityAllAccepted() {
        event.setCapacity(2);
        Map<String, String> entrants = new HashMap<>();
        entrants.put("user1", "Accepted");
        entrants.put("user2", "Accepted");
        event.setEntrants(entrants);

        assertEquals("0", event.getAvailableCapacity());
    }

    /**
     * Tests setting null values in setters.
     */
    @Test
    public void testNullValues() {
        event.setName(null);
        assertNull(event.getName());

        event.setDescription(null);
        assertNull(event.getDescription());

        event.setPosterImageUrl(null);
        assertNull(event.getPosterImageUrl());

        event.setEventLocation(null);
        assertNull(event.getEventLocation());
    }
}
