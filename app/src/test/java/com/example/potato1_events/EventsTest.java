package com.example.potato1_events;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for the Event class.
 */
public class EventsTest {

    private Event event;
    private Map<String, String> initialEntrants;

    /**
     * Sets up the test environment before each test.
     */
    @Before
    public void setUp() {
        initialEntrants = new HashMap<>();
        initialEntrants.put("entrant1", "confirmed");
        initialEntrants.put("entrant2", "waitlist");

        event = new Event(
                "event1",
                "facility1",
                "Test Event",
                "This is a test event.",
                new Date(2023, 9, 1, 10, 0),
                new Date(2023, 9, 1, 12, 0),
                new Date(2023, 8, 25, 0, 0),
                new Date(2023, 8, 31, 23, 59),
                50.0,
                100,
                2,
                50,
                "http://example.com/poster.jpg",
                "qrCodeHash123",
                initialEntrants,
                new Date(),
                "Open",
                true,
                "123 Event Street"
        );
    }

    /**
     * Tests the getters of the Event class.
     */
    @Test
    public void testEventGetters() {
        assertEquals("event1", event.getId());
        assertEquals("facility1", event.getFacilityId());
        assertEquals("Test Event", event.getName());
        assertEquals("This is a test event.", event.getDescription());
        assertEquals(new Date(2023, 9, 1, 10, 0), event.getStartDate());
        assertEquals(new Date(2023, 9, 1, 12, 0), event.getEndDate());
        assertEquals(new Date(2023, 8, 25, 0, 0), event.getRegistrationStart());
        assertEquals(new Date(2023, 8, 31, 23, 59), event.getRegistrationEnd());
        assertEquals(50.0, event.getPrice(), 0.001);
        assertEquals(100, event.getCapacity());
        assertEquals(2, event.getCurrentEntrantsNumber());
        assertEquals(50, event.getWaitingListCapacity());
        assertEquals("http://example.com/poster.jpg", event.getPosterImageUrl());
        assertEquals("qrCodeHash123", event.getQrCodeHash());
        assertEquals(initialEntrants, event.getEntrants());
        assertNotNull(event.getCreatedAt());
        assertEquals("Open", event.getStatus());
        assertTrue(event.isGeolocationRequired());
        assertEquals("123 Event Street", event.getEventLocation());
        assertFalse(event.getRandomDrawPerformed());
    }

    /**
     * Tests the setters of the Event class.
     */
    @Test
    public void testEventSetters() {
        event.setId("event2");
        assertEquals("event2", event.getId());

        event.setFacilityId("facility2");
        assertEquals("facility2", event.getFacilityId());

        event.setName("Updated Event");
        assertEquals("Updated Event", event.getName());

        event.setDescription("Updated description.");
        assertEquals("Updated description.", event.getDescription());

        Date newStartDate = new Date(2023, 10, 1, 10, 0);
        event.setStartDate(newStartDate);
        assertEquals(newStartDate, event.getStartDate());

        Date newEndDate = new Date(2023, 10, 1, 12, 0);
        event.setEndDate(newEndDate);
        assertEquals(newEndDate, event.getEndDate());

        event.setPrice(75.0);
        assertEquals(75.0, event.getPrice(), 0.001);

        event.setCapacity(150);
        assertEquals(150, event.getCapacity());

        event.setCurrentEntrantsNumber(10);
        assertEquals(10, event.getCurrentEntrantsNumber());

        event.setWaitingListCapacity(30);
        assertEquals(30, event.getWaitingListCapacity());

        event.setPosterImageUrl("http://example.com/new_poster.jpg");
        assertEquals("http://example.com/new_poster.jpg", event.getPosterImageUrl());

        event.setQrCodeHash("newQrCodeHash");
        assertEquals("newQrCodeHash", event.getQrCodeHash());

        Map<String, String> newEntrants = new HashMap<>();
        newEntrants.put("entrant3", "confirmed");
        event.setEntrants(newEntrants);
        assertEquals(newEntrants, event.getEntrants());

        event.setStatus("Closed");
        assertEquals("Closed", event.getStatus());

        event.setGeolocationRequired(false);
        assertFalse(event.isGeolocationRequired());

        event.setEventLocation("456 Updated Street");
        assertEquals("456 Updated Street", event.getEventLocation());

        event.setRandomDrawPerformed(true);
        assertTrue(event.getRandomDrawPerformed());
    }

    /**
     * Tests the updateEntrantStatus method.
     */
    @Test
    public void testUpdateEntrantStatus() {
        event.updateEntrantStatus("entrant3", "confirmed");
        assertEquals("confirmed", event.getEntrants().get("entrant3"));
        assertEquals(3, event.getEntrants().size());

        // Update existing entrant status
        event.updateEntrantStatus("entrant1", "waitlist");
        assertEquals("waitlist", event.getEntrants().get("entrant1"));
    }

    /**
     * Tests the removeEntrant method.
     */
    @Test
    public void testRemoveEntrant() {
        event.removeEntrant("entrant2");
        assertFalse(event.getEntrants().containsKey("entrant2"));
        assertEquals(1, event.getEntrants().size());
    }

    /**
     * Tests the updateStatus method.
     */
    @Test
    public void testUpdateStatus() {
        event.updateStatus("Completed");
        assertEquals("Completed", event.getStatus());
    }

    /**
     * Tests the default constructor.
     */
    @Test
    public void testDefaultConstructor() {
        Event defaultEvent = new Event();
        assertNotNull(defaultEvent.getEntrants());
        assertTrue(defaultEvent.getEntrants().isEmpty());
        assertFalse(defaultEvent.isGeolocationRequired());
        assertFalse(defaultEvent.getRandomDrawPerformed());
    }

    /**
     * Tests setting and getting createdAt timestamp.
     */
    @Test
    public void testCreatedAt() {
        Date creationDate = new Date(2023, 8, 15, 10, 30);
        event.setCreatedAt(creationDate);
        assertEquals(creationDate, event.getCreatedAt());
    }

    /**
     * Tests setting and getting randomDrawPerformed flag.
     */
    @Test
    public void testRandomDrawPerformed() {
        event.setRandomDrawPerformed(true);
        assertTrue(event.getRandomDrawPerformed());
    }

    /**
     * Tests the equality of two Event objects.
     */
    @Test
    public void testEventEquality() {
        Event eventCopy = new Event(
                "event1",
                "facility1",
                "Test Event",
                "This is a test event.",
                new Date(2023, 9, 1, 10, 0),
                new Date(2023, 9, 1, 12, 0),
                new Date(2023, 8, 25, 0, 0),
                new Date(2023, 8, 31, 23, 59),
                50.0,
                100,
                2,
                50,
                "http://example.com/poster.jpg",
                "qrCodeHash123",
                initialEntrants,
                event.getCreatedAt(),
                "Open",
                true,
                "123 Event Street"
        );

        assertEquals(event.getId(), eventCopy.getId());
        assertEquals(event.getFacilityId(), eventCopy.getFacilityId());
        assertEquals(event.getName(), eventCopy.getName());
        // ... Continue comparing other fields as needed
    }

    /**
     * Tests handling of null values in setters.
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

    /**
     * Tests setting negative values for capacity.
     */
    @Test
    public void testNegativeCapacity() {
        event.setCapacity(-10);
        assertEquals(-10, event.getCapacity());

        event.setWaitingListCapacity(-5);
        assertEquals(-5, event.getWaitingListCapacity());
    }

    /**
     * Tests setting invalid dates.
     */
    @Test
    public void testInvalidDates() {
        Date invalidStartDate = new Date(2023, 9, 2, 10, 0);
        Date invalidEndDate = new Date(2023, 9, 1, 12, 0);

        event.setStartDate(invalidStartDate);
        event.setEndDate(invalidEndDate);

        assertEquals(invalidStartDate, event.getStartDate());
        assertEquals(invalidEndDate, event.getEndDate());
    }

    /**
     * Tests the getCurrentEntrantsNumber and setCurrentEntrantsNumber methods.
     */
    @Test
    public void testCurrentEntrantsNumber() {
        event.setCurrentEntrantsNumber(5);
        assertEquals(5, event.getCurrentEntrantsNumber());

        event.setCurrentEntrantsNumber(-1);
        assertEquals(-1, event.getCurrentEntrantsNumber());
    }
}
