// File: EntrantHomeTest.java
package com.example.potato1_events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import com.google.firebase.firestore.GeoPoint;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
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
    @Rule
    public GrantPermissionRule notificationPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS);

    @Before
    public void setUp() {
        // Initialize Intents before each test
        Intents.init();
    }

    @After
    public void tearDown() {
        // Release Intents after each test
        Intents.release();
        // Reset the repository to its default instance to avoid side effects on other tests
        EntEventsRepository.setInstance(null);
    }
    /**
     * Utility method to set private fields via reflection.
     */
    public static void setPrivateField(Object object, String fieldName, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }


    /**
     * Tests that the entrant can view a list of joined events.
     */
    @Test
    public void testViewJoinedEvents() {

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
     * Tests that the entrant can successfully join the waiting list for an event.
     */
    @Test
    public void testJoinWaitingList_Success() {
        // Mock event details
        Event mockEvent = new Event();
        mockEvent.setId("event123");
        mockEvent.setName("Music Concert");
        mockEvent.setEventLocation("City Hall");
        mockEvent.setGeolocationRequired(false);
        mockEvent.setCapacity(100);
        mockEvent.setCurrentEntrantsNumber(50); // Initially 50 entrants
        mockEvent.setStatus("Open");
        mockEvent.setEntrants(new HashMap<>()); // Empty entrants map

        // Mock EntEventsRepository
        EntEventsRepository mockRepo = Mockito.mock(EntEventsRepository.class);

        // Mock getEventById to return the mockEvent
        Mockito.doAnswer(invocation -> {
            String eventId = invocation.getArgument(0);
            EntEventsRepository.EventCallback callback = invocation.getArgument(1);
            if ("event123".equals(eventId)) {
                callback.onEventLoaded(mockEvent);
            } else {
                callback.onEventLoaded(null);
            }
            return null;
        }).when(mockRepo).getEventById(Mockito.eq("event123"), Mockito.any());

        // Mock joinWaitingList to simulate successful join
        Mockito.doAnswer(invocation -> {
            String eventId = invocation.getArgument(0);
            String entrantId = invocation.getArgument(1);
            GeoPoint geoPoint = invocation.getArgument(2);
            EntEventsRepository.ActionCallback callback = invocation.getArgument(3);
            // Simulate adding entrant to waiting list
            mockEvent.getEntrants().put(entrantId, "Waitlist");
            mockEvent.setCurrentEntrantsNumber(mockEvent.getCurrentEntrantsNumber() + 1);
            callback.onSuccess();
            return null;
        }).when(mockRepo).joinWaitingList(Mockito.eq("event123"), Mockito.anyString(), Mockito.any(), Mockito.any());

        // Inject the mocked repository into the Singleton before launching the activity
        EntEventsRepository.setInstance(mockRepo);

        // Create an Intent with the EVENT_ID extra
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventDetailsEntrantActivity.class);
        intent.putExtra("EVENT_ID", "event123");

        // Launch EventDetailsEntrantActivity with the intent
        ActivityScenario<EventDetailsEntrantActivity> scenario = ActivityScenario.launch(intent);
        scenario.onActivity(activity -> {
            // Set device ID
            activity.setDeviceId("entrantDevice123"); // Assuming deviceId is a public or package-private field
            // Load event details (this should already be handled in onCreate)
            // activity.loadEventDetails("event123"); // Uncomment if needed
        });

        // Click on "Join Waiting List" button
        onView(withId(R.id.joinButton)).perform(click());

        // Confirm joining the waiting list
        onView(withText("Yes")).inRoot(RootMatchers.isDialog()).perform(click());

        // Verify that joinWaitingList was called with correct parameters
        verify(mockRepo).joinWaitingList(Mockito.eq("event123"), Mockito.eq("entrantDevice123"), Mockito.any(), Mockito.any());

        // Verify that the UI updates to show "Leave Waiting List" button and hide "Join" button
        onView(withId(R.id.joinButton)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        onView(withId(R.id.leaveButton)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }

    /**
     * Helper method to get the current activity's decor view for Toast verification.
     *
     * @param scenario The ActivityScenario instance.
     * @return The decor view of the current activity.
     */
    private android.view.View getCurrentActivityDecorView(ActivityScenario<EventDetailsEntrantActivity> scenario) {
        final android.view.View[] decorView = new android.view.View[1];
        scenario.onActivity(activity -> decorView[0] = activity.getWindow().getDecorView());
        return decorView[0];
    }


    /**
     * Tests that the entrant can successfully leave the waiting list for an event.
     */
    @Test
    public void testLeaveWaitingList_Success() throws InterruptedException {
        // Mock event details where entrant is already on the waiting list
        Event mockEvent = new Event();
        mockEvent.setId("event123");
        mockEvent.setName("Music Concert");
        mockEvent.setEventLocation("City Hall");
        mockEvent.setGeolocationRequired(false);
        mockEvent.setCapacity(100);
        mockEvent.setCurrentEntrantsNumber(51); // Initially 50 entrants + 1 entrant on waitlist
        mockEvent.setStatus("Open");
        mockEvent.setEntrants(new HashMap<>()); // Initialize entrants map
        mockEvent.getEntrants().put("entrantDevice123", "Waitlist"); // Add entrant to waitlist

        // Mock EntEventsRepository
        EntEventsRepository mockRepo = Mockito.mock(EntEventsRepository.class);

        // Mock getEventById to return the mockEvent
        Mockito.doAnswer(invocation -> {
            String eventId = invocation.getArgument(0);
            EntEventsRepository.EventCallback callback = invocation.getArgument(1);
            if ("event123".equals(eventId)) {
                callback.onEventLoaded(mockEvent);
            } else {
                callback.onEventLoaded(null);
            }
            return null;
        }).when(mockRepo).getEventById(Mockito.eq("event123"), Mockito.any());

        // Mock joinWaitingList to simulate successful join
        Mockito.doAnswer(invocation -> {
            String eventId = invocation.getArgument(0);
            String entrantId = invocation.getArgument(1);
            GeoPoint geoPoint = invocation.getArgument(2);
            EntEventsRepository.ActionCallback callback = invocation.getArgument(3);
            // Simulate adding entrant to waiting list
            mockEvent.getEntrants().put(entrantId, "Waitlist");
            mockEvent.setCurrentEntrantsNumber(mockEvent.getCurrentEntrantsNumber() + 1);
            callback.onSuccess();
            return null;
        }).when(mockRepo).joinWaitingList(Mockito.eq("event123"), Mockito.anyString(), Mockito.any(), Mockito.any());


        // Mock leaveWaitingList to simulate successful leave
        Mockito.doAnswer(invocation -> {
            String eventId = invocation.getArgument(0);
            String entrantId = invocation.getArgument(1);
            EntEventsRepository.ActionCallback callback = invocation.getArgument(2);
            // Simulate removing entrant from waiting list
            mockEvent.getEntrants().remove(entrantId);
            mockEvent.setCurrentEntrantsNumber(mockEvent.getCurrentEntrantsNumber() - 1);
            callback.onSuccess();
            return null;
        }).when(mockRepo).leaveWaitingList(Mockito.eq("event123"), Mockito.eq("entrantDevice123"), Mockito.any());

        // Inject the mocked repository into the Singleton before launching the activity
        EntEventsRepository.setInstance(mockRepo);

        // Create an Intent with the EVENT_ID extra
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventDetailsEntrantActivity.class);
        intent.putExtra("EVENT_ID", "event123");

        // Launch EventDetailsEntrantActivity with the intent
        ActivityScenario<EventDetailsEntrantActivity> scenario = ActivityScenario.launch(intent);
        scenario.onActivity(activity -> {
            // Set device ID
            activity.setDeviceId("entrantDevice123"); // Assuming deviceId is a public or package-private field
        });

        // Perform click on "Leave Waiting List" button
        onView(withId(R.id.joinButton)).perform(click());
        onView(withText("Yes")).inRoot(RootMatchers.isDialog()).perform(click());
        onView(withId(R.id.leaveButton)).perform(click());

        // Confirm leaving in the dialog
        onView(withText("Yes")).inRoot(RootMatchers.isDialog()).perform(click());

//        // Verify that a confirmation message is displayed (assuming it's a Toast)
//        onView(withText("Successfully left the waiting list."))
//                .inRoot(withDecorView(not(is(getCurrentActivityDecorView(scenario)))))
//                .check(matches(isDisplayed()));

        // Verify that leaveWaitingList was called with correct parameters
        verify(mockRepo).leaveWaitingList(Mockito.eq("event123"), Mockito.eq("entrantDevice123"), Mockito.any());

        // Verify that the UI updates to show "Join Waiting List" button and hide "Leave Waiting List" button
        onView(withId(R.id.joinButton)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withId(R.id.leaveButton)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }

    /**
     * Tests that the entrant can view event details correctly.
     */
//    @Test
//    public void testViewEventDetailsInHome() throws InterruptedException {
//        // Mock Event
//        Event mockEvent = new Event();
//        mockEvent.setId("event1");
//        mockEvent.setName("Test Event 1");
//        mockEvent.setDescription("Detailed Description of Test Event 1.");
//        mockEvent.setEventLocation("Test Location 1");
//        mockEvent.setPosterImageUrl("http://example.com/poster1.jpg");
//        mockEvent.setStartDate(new java.util.Date());
//        mockEvent.setEndDate(new java.util.Date());
//        mockEvent.setCurrentEntrantsNumber(5);
//        mockEvent.setWaitingListCapacity(10);
//        mockEvent.setGeolocationRequired(false);
//        mockEvent.setStatus("Upcoming");
//        mockEvent.setEntrants(new HashMap<>());
//
//        // Mock EntEventsRepository
//        EntEventsRepository mockRepo = Mockito.mock(EntEventsRepository.class);
//
//        // Mock getEventById to return mockEvent when EventDetailsEntrantActivity is launched
//        Mockito.doAnswer(invocation -> {
//            String eventId = invocation.getArgument(0);
//            EntEventsRepository.EventCallback callback = invocation.getArgument(1);
//            if ("event1".equals(eventId)) {
//                callback.onEventLoaded(mockEvent);
//            } else {
//                callback.onEventLoaded(null);
//            }
//            return null;
//        }).when(mockRepo).getEventById(Mockito.eq("event1"), Mockito.any());
//
//        // Mock getJoinedEvents to return the joined events
//        List<Event> mockJoinedEvents = new ArrayList<>();
//        mockJoinedEvents.add(mockEvent);
//
//        Mockito.doAnswer(invocation -> {
//            String deviceId = invocation.getArgument(0);
//            EntEventsRepository.EventListCallback callback = invocation.getArgument(1);
//            if ("mockDeviceId".equals(deviceId)) {
//                callback.onEventListLoaded(mockJoinedEvents);
//            } else {
//                callback.onEventListLoaded(new ArrayList<>());
//            }
//            return null;
//        }).when(mockRepo).getJoinedEvents(Mockito.eq("mockDeviceId"), Mockito.any());
//
//        // Launch EntrantHomeActivity and inject mock repository
//        ActivityScenario<EntrantHomeActivity> scenario = ActivityScenario.launch(new Intent(ApplicationProvider.getApplicationContext(), EntrantHomeActivity.class));
//        scenario.onActivity(activity -> {
//            activity.setEntEventsRepository(mockRepo);
//            activity.setDeviceId("mockDeviceId"); // Inject mock device ID
//            activity.loadJoinedEvents();
//        });
//        onView(withId(R.id.eventNameTextView)).check(matches(withText("Test Event 1")));
//
//        // Click on one of the joined events to navigate to EventDetailsEntrantActivity
//        onView(withText("Test Event 1")).perform(click());
//
//        // Verify that EventDetailsEntrantActivity is launched
//        intended(hasComponent(EventDetailsEntrantActivity.class.getName()));
////        Thread.sleep(2000);
//
//
////        // Verify that event details are displayed in EventDetailsEntrantActivity
////        onView(withId(R.id.eventNameTextView)).check(matches(withText("Test Event 1")));
////        onView(withId(R.id.eventDescriptionTextView)).check(matches(withText("Detailed Description of Test Event 1.")));
////        onView(withId(R.id.eventLocationTextView)).check(matches(withText("Location: Test Location 1")));
//    }

    /**
     * Tests that the entrant is prompted for geolocation permissions when required by an event.
     */
//    @Test
//    public void testGeolocationPermissionPrompt() {
//        // Initialize the mock repository
//        EntEventsRepository mockRepository = Mockito.mock(EntEventsRepository.class);
//
//        // Create a mock event that requires geolocation
//        Event mockEvent = new Event();
//        mockEvent.setId("event1");
//        mockEvent.setName("Sample Event 1");
//        mockEvent.setDescription("This is a sample event description.");
//        mockEvent.setEventLocation("Location 1");
//        mockEvent.setPosterImageUrl("");
//        mockEvent.setStartDate(new Date());
//        mockEvent.setEndDate(new Date());
//        mockEvent.setCurrentEntrantsNumber(0);
//        mockEvent.setWaitingListCapacity(100);
//        mockEvent.setGeolocationRequired(true);
//        mockEvent.setStatus("Open");
//        mockEvent.setEntrants(new HashMap<>());
//
//        // Mock the getEventById method
//        Mockito.doAnswer(invocation -> {
//            EntEventsRepository.EventCallback callback = invocation.getArgument(1);
//            callback.onEventLoaded(mockEvent);
//            return null;
//        }).when(mockRepository).getEventById(anyString(), Mockito.any());
//
//        // Mock joinWaitingList to simulate success
//        Mockito.doAnswer(invocation -> {
//            EntEventsRepository.ActionCallback callback = invocation.getArgument(2);
//            callback.onSuccess();
//            return null;
//        }).when(mockRepository).joinWaitingList(anyString(), anyString(), Mockito.any());
//
//        // Create an Intent with the EVENT_ID extra
//        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventDetailsEntrantActivity.class);
//        intent.putExtra("EVENT_ID", "event1");
//
//        // Launch the activity
//        ActivityScenario<EventDetailsEntrantActivity> scenario = ActivityScenario.launch(intent);
//
//        scenario.onActivity(activity -> {
//            activity.setEntEventsRepository(mockRepository);
//            activity.setDeviceId("testDeviceId");
//            activity.loadEventDetails("event1");
//        });
//
//        // Click the join button
//        onView(withId(R.id.joinButton)).perform(click());
//
//        // Verify that the geolocation alert is displayed
//        onView(withText("Geolocation Required")).check(matches(isDisplayed()));
//        onView(withText("OK")).perform(click());
//
//        // Verify that the join confirmation dialog is displayed
//        onView(withText("Join Waiting List")).check(matches(isDisplayed()));
//        onView(withText("Yes")).perform(click());
//
//        // Verify that the success message is displayed
//        onView(withText("Successfully joined the waiting list.")).inRoot(new ToastMatcher())
//                .check(matches(isDisplayed()));
//    }


}
