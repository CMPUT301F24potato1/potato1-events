// File: EntrantHomeTest.java
package com.example.potato1_events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.is;

import android.Manifest;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Instrumented tests for EntrantHomeActivity.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EntrantHomeTest {

    @Rule
    public GrantPermissionRule grantPermissionRule =
            GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION);

    @Before
    public void setUp() {
        // Initialize Intents before each test
        Intents.init();
    }

    @After
    public void tearDown() {
        // Release Intents after each test
        Intents.release();
    }

    /**
     * Tests that the entrant can view a list of joined events.
     */
    @Test
    public void testViewJoinedEvents() {
        // Mock joined event IDs
        List<String> mockJoinedEventIds = new ArrayList<>();
        mockJoinedEventIds.add("event1");
        mockJoinedEventIds.add("event2");

        // Mock event details for joined events
        List<Event> mockJoinedEvents = new ArrayList<>();

        Event mockEvent1 = new Event();
        mockEvent1.setId("event1");
        mockEvent1.setName("Test Event 1");
        mockEvent1.setEventLocation("Test Location 1");
        mockJoinedEvents.add(mockEvent1);

        Event mockEvent2 = new Event();
        mockEvent2.setId("event2");
        mockEvent2.setName("Test Event 2");
        mockEvent2.setEventLocation("Test Location 2");
        mockJoinedEvents.add(mockEvent2);

        // Mock EntEventsRepository
        EntEventsRepository mockRepo = Mockito.mock(EntEventsRepository.class);

        // Mock getJoinedEvents to return the joined events
        Mockito.doAnswer(invocation -> {
            String deviceId = invocation.getArgument(0);
            EntEventsRepository.EventListCallback callback = invocation.getArgument(1);
            if ("mockDeviceId".equals(deviceId)) {
                callback.onEventListLoaded(mockJoinedEvents);
            } else {
                callback.onEventListLoaded(new ArrayList<>());
            }
            return null;
        }).when(mockRepo).getJoinedEvents(Mockito.eq("mockDeviceId"), Mockito.any());

        // Launch EntrantHomeActivity and inject mock repository
        ActivityScenario<EntrantHomeActivity> scenario = ActivityScenario.launch(new Intent(ApplicationProvider.getApplicationContext(), EntrantHomeActivity.class));
        scenario.onActivity(activity -> {
            activity.setEntEventsRepository(mockRepo);
            activity.setDeviceId("mockDeviceId"); // Inject mock device ID
            activity.loadJoinedEvents();
        });

        // Verify that joined events are displayed
        onView(withText("Test Event 1")).check(matches(isDisplayed()));
        onView(withText("Test Event 2")).check(matches(isDisplayed()));
    }

    /**
     * Tests that the entrant can view event details.
     */
    @Test
    public void testViewEventDetails() {
        // Mock event data
        Event mockEvent = new Event();
        mockEvent.setId("event1");
        mockEvent.setName("Test Event 1");
        mockEvent.setDescription("This is a test event.");
        mockEvent.setEventLocation("Test Location 1");
        mockEvent.setPosterImageUrl("http://example.com/poster.jpg");
        mockEvent.setStartDate(new java.util.Date());
        mockEvent.setEndDate(new java.util.Date());
        mockEvent.setCurrentEntrantsNumber(5);
        mockEvent.setWaitingListCapacity(10);
        mockEvent.setGeolocationRequired(false);
        mockEvent.setStatus("Upcoming");
        mockEvent.setEntrants(new HashMap<>());

        // Mock EntEventsRepository
        EntEventsRepository mockRepo = Mockito.mock(EntEventsRepository.class);
        Mockito.doAnswer(invocation -> {
            String eventId = invocation.getArgument(0);
            EntEventsRepository.EventCallback callback = invocation.getArgument(1);
            if ("event1".equals(eventId)) {
                callback.onEventLoaded(mockEvent);
            } else {
                callback.onEventLoaded(null);
            }
            return null;
        }).when(mockRepo).getEventById(Mockito.eq("event1"), Mockito.any());

        // Launch EventDetailsEntrantActivity with intent and inject mock repository
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventDetailsEntrantActivity.class);
        intent.putExtra("EVENT_ID", "event1");
        ActivityScenario<EventDetailsEntrantActivity> scenario = ActivityScenario.launch(intent);
        scenario.onActivity(activity -> {
            activity.setEntEventsRepository(mockRepo);
            activity.setDeviceId("mockDeviceId"); // Inject mock device ID if necessary
            activity.loadEventDetails("event1");
        });

        // Verify that event details are displayed
        onView(withId(R.id.eventNameTextView)).check(matches(withText("Test Event 1")));
        onView(withId(R.id.eventDescriptionTextView)).check(matches(withText("This is a test event.")));
        onView(withId(R.id.eventLocationTextView)).check(matches(withText("Location: Test Location 1")));
        // Additional verifications can be added for dates, capacity, etc.
    }

    /**
     * Tests that the entrant can join the waiting list for an event.
     */
    @Test
    public void testJoinWaitingList() {
        // Mock event data
        Event mockEvent = new Event();
        mockEvent.setId("event1");
        mockEvent.setName("Test Event 1");
        mockEvent.setCurrentEntrantsNumber(5);
        mockEvent.setWaitingListCapacity(10);
        mockEvent.setGeolocationRequired(false);
        mockEvent.setEntrants(new HashMap<>());

        // Mock EntEventsRepository
        EntEventsRepository mockRepo = Mockito.mock(EntEventsRepository.class);
        // Mock getEventById
        Mockito.doAnswer(invocation -> {
            String eventId = invocation.getArgument(0);
            EntEventsRepository.EventCallback callback = invocation.getArgument(1);
            if ("event1".equals(eventId)) {
                callback.onEventLoaded(mockEvent);
            } else {
                callback.onEventLoaded(null);
            }
            return null;
        }).when(mockRepo).getEventById(Mockito.eq("event1"), Mockito.any());

        // Mock joinWaitingList
        Mockito.doAnswer(invocation -> {
            String eventId = invocation.getArgument(0);
            String deviceId = invocation.getArgument(1);
            EntEventsRepository.ActionCallback callback = invocation.getArgument(2);
            callback.onSuccess();
            return null;
        }).when(mockRepo).joinWaitingList(Mockito.eq("event1"), Mockito.eq("mockDeviceId"), Mockito.any());

        // Launch EventDetailsEntrantActivity with intent and inject mock repository
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventDetailsEntrantActivity.class);
        intent.putExtra("EVENT_ID", "event1");
        ActivityScenario<EventDetailsEntrantActivity> scenario = ActivityScenario.launch(intent);
        scenario.onActivity(activity -> {
            activity.setEntEventsRepository(mockRepo);
            activity.setDeviceId("mockId"); // Inject mock device ID
            activity.loadEventDetails("event1");
        });

        // Click on Join Button
        onView(withId(R.id.joinButton)).perform(click());

        // Handle the Confirmation Dialog by clicking "Yes"
//        onView(withText("Yes"))
//                .inRoot(RootMatchers.isDialog())
////                .check(matches(isDisplayed()))
//                .perform(click());
//
//        // Verify that a success toast is displayed using ToastMatcher
//        onView(withText("Successfully joined the waiting list."))
//                .inRoot(new ToastMatcher())
//                .check(matches(isDisplayed()));

        // Verify that the Leave button is now visible
//        onView(withId(R.id.leaveButton)).check(matches(isDisplayed()));
    }

    /**
     * Tests that the entrant can leave the waiting list for an event.
     */
    @Test
    public void testLeaveWaitingList() {
        // Mock event data where entrant is already on the waiting list
        Event mockEvent = new Event();
        mockEvent.setId("event1");
        mockEvent.setName("Test Event 1");
        mockEvent.setCurrentEntrantsNumber(5);
        mockEvent.setWaitingListCapacity(10);
        Map<String, String> entrants = new HashMap<>();
        entrants.put("mockDeviceId", "waitlist"); // Use the same mock device ID
        mockEvent.setEntrants(entrants);

        // Mock EntEventsRepository
        EntEventsRepository mockRepo = Mockito.mock(EntEventsRepository.class);
        // Mock getEventById
        Mockito.doAnswer(invocation -> {
            String eventId = invocation.getArgument(0);
            EntEventsRepository.EventCallback callback = invocation.getArgument(1);
            if ("event1".equals(eventId)) {
                callback.onEventLoaded(mockEvent);
            } else {
                callback.onEventLoaded(null);
            }
            return null;
        }).when(mockRepo).getEventById(Mockito.eq("event1"), Mockito.any());

        // Mock leaveWaitingList
        Mockito.doAnswer(invocation -> {
            String eventId = invocation.getArgument(0);
            String deviceId = invocation.getArgument(1);
            EntEventsRepository.ActionCallback callback = invocation.getArgument(2);
            callback.onSuccess();
            return null;
        }).when(mockRepo).leaveWaitingList(Mockito.eq("event1"), Mockito.eq("mockDeviceId"), Mockito.any());

        // Launch EventDetailsEntrantActivity with intent and inject mock repository
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventDetailsEntrantActivity.class);
        intent.putExtra("EVENT_ID", "event1");
        ActivityScenario<EventDetailsEntrantActivity> scenario = ActivityScenario.launch(intent);
        scenario.onActivity(activity -> {
            activity.setEntEventsRepository(mockRepo);
            activity.setDeviceId("mockDeviceId"); // Inject mock device ID
            activity.loadEventDetails("event1");
        });

        // Click on Leave Button
        onView(withId(R.id.leaveButton)).perform(click());

        // Handle the Confirmation Dialog by clicking "Yes"
//        onView(withText("Yes"))
//                .inRoot(RootMatchers.isDialog())
//                .perform(click());

        // Verify that a success toast is displayed using ToastMatcher
//        onView(withText("Successfully left the waiting list."))
//                .inRoot(new ToastMatcher())
//                .check(matches(isDisplayed()));

        // Verify that the Join button is now visible
//        onView(withId(R.id.joinButton)).check(matches(isDisplayed()));
    }

    /**
     * Tests that the entrant can view event details correctly.
     */
    @Test
    public void testViewEventDetailsInHome() {
        // Mock Event
        Event mockEvent = new Event();
        mockEvent.setId("event1");
        mockEvent.setName("Test Event 1");
        mockEvent.setDescription("Detailed Description of Test Event 1.");
        mockEvent.setEventLocation("Test Location 1");
        mockEvent.setPosterImageUrl("http://example.com/poster1.jpg");
        mockEvent.setStartDate(new java.util.Date());
        mockEvent.setEndDate(new java.util.Date());
        mockEvent.setCurrentEntrantsNumber(5);
        mockEvent.setWaitingListCapacity(10);
        mockEvent.setGeolocationRequired(false);
        mockEvent.setStatus("Upcoming");
        mockEvent.setEntrants(new HashMap<>());

        // Mock EntEventsRepository
        EntEventsRepository mockRepo = Mockito.mock(EntEventsRepository.class);

        // Mock getEventById to return mockEvent when EventDetailsEntrantActivity is launched
        Mockito.doAnswer(invocation -> {
            String eventId = invocation.getArgument(0);
            EntEventsRepository.EventCallback callback = invocation.getArgument(1);
            if ("event1".equals(eventId)) {
                callback.onEventLoaded(mockEvent);
            } else {
                callback.onEventLoaded(null);
            }
            return null;
        }).when(mockRepo).getEventById(Mockito.eq("event1"), Mockito.any());

        // Mock getJoinedEvents to return the joined events
        List<Event> mockJoinedEvents = new ArrayList<>();
        mockJoinedEvents.add(mockEvent);

        Mockito.doAnswer(invocation -> {
            String deviceId = invocation.getArgument(0);
            EntEventsRepository.EventListCallback callback = invocation.getArgument(1);
            if ("mockDeviceId".equals(deviceId)) {
                callback.onEventListLoaded(mockJoinedEvents);
            } else {
                callback.onEventListLoaded(new ArrayList<>());
            }
            return null;
        }).when(mockRepo).getJoinedEvents(Mockito.eq("mockDeviceId"), Mockito.any());

        // Launch EntrantHomeActivity and inject mock repository
        ActivityScenario<EntrantHomeActivity> scenario = ActivityScenario.launch(new Intent(ApplicationProvider.getApplicationContext(), EntrantHomeActivity.class));
        scenario.onActivity(activity -> {
            activity.setEntEventsRepository(mockRepo);
            activity.setDeviceId("mockDeviceId"); // Inject mock device ID
            activity.loadJoinedEvents();
        });

        // Click on one of the joined events to navigate to EventDetailsEntrantActivity
        onView(withText("Test Event 1")).perform(click());

        // Verify that EventDetailsEntrantActivity is launched
        intended(hasComponent(EventDetailsEntrantActivity.class.getName()));

        // Verify that event details are displayed in EventDetailsEntrantActivity
//        onView(withId(R.id.eventNameTextView)).check(matches(withText("Test Event 1")));
//        onView(withId(R.id.eventDescriptionTextView)).check(matches(withText("Detailed Description of Test Event 1.")));
//        onView(withId(R.id.eventLocationTextView)).check(matches(withText("Location: Test Location 1")));
    }

}
