// File: EntrantHomeTest.java
package com.example.potato1_events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
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
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.matcher.IntentMatchers;
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
    @Rule
    public GrantPermissionRule cameraPermissionRule =
            GrantPermissionRule.grant(Manifest.permission.CAMERA);


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
     * Tests that the entrant is prompted for geolocation permissions when required by an event.
     * US 01.08.01
     */
    @Test
    public void testGeolocationPermissionPrompt() {
        // Mock event details
        Event mockEvent = new Event();
        mockEvent.setId("event123");
        mockEvent.setName("Music Concert");
        mockEvent.setEventLocation("City Hall");
        mockEvent.setGeolocationRequired(true); // true geolocation
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


        // Verify that the geolocation alert is displayed
        onView(withText("Geolocation Required")).check(matches(isDisplayed()));
        onView(withText("OK")).perform(click());

        // Verify that the join confirmation dialog is displayed
        onView(withText("Join Waiting List")).check(matches(isDisplayed()));
        onView(withText("Yes")).perform(click());

//        // Verify that the success message is displayed
//        onView(withText("Successfully joined the waiting list.")).inRoot(new ToastMatcher())
//                .check(matches(isDisplayed()));
    }


    /**
     * Tests that the entrant can reach the window to edit profile and edit the profile
     * Including name, email, picture
     * US 01.02.02, US 01.03.01, US 01.03.02, US 01.03.02
     */
    @Test
    public void testNavigateToEditProfile() {
        // Launch EntrantHomeActivity
        ActivityScenario.launch(EntrantHomeActivity.class);

        // Open the navigation drawer using DrawerActions from Espresso-Contrib
        onView(withId(R.id.drawer_layout))
                .perform(DrawerActions.open());

        // Click on the "Edit Profile" menu item
        onView(withText("Edit Profile"))
                .perform(click());

        // Verify that an intent was sent to start UserInfoActivity
        intended(IntentMatchers.hasComponent(UserInfoActivity.class.getName()));

        onView(withId(R.id.nameEditText)).perform(clearText()).perform(typeText("Xavier"));
        onView(withId(R.id.saveButton)).perform(click());

        intended(IntentMatchers.hasComponent(EntrantHomeActivity.class.getName()));
    }

    /**
     * Tests that the entrant can reach the window to see notifications
     * US 01.04.01,02
     */
    @Test
    public void testNavigateToNotifications() {
        // Launch EntrantHomeActivity
        ActivityScenario.launch(EntrantHomeActivity.class);

        // Open the navigation drawer using DrawerActions from Espresso-Contrib
        onView(withId(R.id.drawer_layout))
                .perform(DrawerActions.open());

        // Click on the "Notifications" menu item
        onView(withText("Notifications"))
                .perform(click());

        // Verify that an intent was sent to start Notifications
        intended(IntentMatchers.hasComponent(NotificationsActivity.class.getName()));

    }

    /**
     * Tests that the entrant can Scan a QR code, run and manually scan the QR code in 5s
     * US 01.06.02
     * the test can fail if there is an issue with the 3D-simulator
     * Note: it is hard to simulate this US
     */
    @Test
    public void testScanQR() throws InterruptedException {
        // Launch EntrantHomeActivity
        ActivityScenario.launch(EntrantHomeActivity.class);

        // Open the navigation drawer using DrawerActions from Espresso-Contrib
        onView(withId(R.id.drawer_layout))
                .perform(DrawerActions.open());

        // Click on the "Notifications" menu item
        onView(withText("Scan QR Code"))
                .perform(click());
        // scan the QR in 5 seconds
        Thread.sleep(5000);

        intended(IntentMatchers.hasComponent(EventDetailsEntrantActivity.class.getName()));


    }


}
