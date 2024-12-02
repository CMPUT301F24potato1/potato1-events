// File: OrganizerHomeActivityTest.java
package com.example.potato1_events;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.contrib.PickerActions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.matcher.IntentMatchers;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.ArrayList;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;

@LargeTest
public class OrganizerHomeTest {
    @Rule
    public GrantPermissionRule grantPermissionRule =
            GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION);
    @Rule
    public GrantPermissionRule notificationPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS);


    private OrgEventsRepository mockOrgEventsRepository;

    @Before
    public void setUp() {
        // Initialize Espresso Intents
        Intents.init();

        // Create a mock OrgEventsRepository
        mockOrgEventsRepository = Mockito.mock(OrgEventsRepository.class);

        // Inject the mock into the singleton
        OrgEventsRepository.setInstance(mockOrgEventsRepository);
    }

    @After
    public void tearDown() {
        // Release Espresso Intents
        Intents.release();

        // Reset the singleton instance
        OrgEventsRepository.resetInstance();
    }


    /**
     * Test that an organizer can create/edit the facility.
     * US 02.01.03
     */
    @Test
    public void testCreateAFacility() throws WriterException, InterruptedException {
        // Define sample event data
        String name = "Laurent3";
        String location = "112 St";
        String description = "A student housing";

        // Launch OrganizerHomeActivity with IS_ADMIN = true
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), OrganizerHomeActivity.class);
        intent.putExtra("IS_ADMIN", true);
        ActivityScenario<EntrantHomeActivity> scenario = ActivityScenario.launch(EntrantHomeActivity.class);

        scenario.onActivity(activity -> {
            // Mock the deviceId
            activity.setDeviceId("deviceId123");
        });

        // Open the navigation drawer using DrawerActions from Espresso-Contrib
        onView(withId(R.id.drawer_layout))
                .perform(DrawerActions.open());

        // Click on the "Notifications" menu item
        onView(withText("My Facility Profile"))
                .perform(click());

        // Fill in the event creation form
        onView(withId(R.id.facilityNameEditText)).perform(clearText())
                .perform(typeText(name));

        onView(withId(R.id.facilityAddressEditText)).perform(clearText())
                .perform(typeText(location));

        onView(withId(R.id.facilityDescriptionEditText)).perform(clearText())
                .perform(typeText(description));

        onView(withId(R.id.save)).perform(click());

        intended(IntentMatchers.hasComponent(EntrantHomeActivity.class.getName()));
    }


        /**
         * Test that an organizer can create a new event and generate a QR code.
         * US 02.01.01, 02.02.03, 02.03.01, 02.04.01, 02.04.02
         */
    @Test
    public void testCreateEvent() throws WriterException {
        // Define sample event data
        String eventName = "Test Event";
        String eventDescription = "This is a test event.";
        String eventLocation = "Test Location";
        String posterUrl = "https://example.com/poster.jpg";
        String eventId = "testEventId123";

        // Mock adding event to Firestore
        Mockito.doAnswer(invocation -> {
            // Capture the event and facilityId
            Event event = invocation.getArgument(0);
            String facilityId = invocation.getArgument(1);
            OrgEventsRepository.ActionCallback callback = invocation.getArgument(2);
            // Simulate successful addition
            callback.onSuccess();
            return null;
        }).when(mockOrgEventsRepository).addEvent(Mockito.any(Event.class), Mockito.anyString(), Mockito.any(OrgEventsRepository.ActionCallback.class));

        // Launch OrganizerHomeActivity with IS_ADMIN = true
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), OrganizerHomeActivity.class);
        intent.putExtra("IS_ADMIN", true);
        ActivityScenario<EntrantHomeActivity> scenario = ActivityScenario.launch(EntrantHomeActivity.class);

        scenario.onActivity(activity -> {
            // Mock the deviceId
            activity.setDeviceId("deviceId123");
        });

        // Open the navigation drawer using DrawerActions from Espresso-Contrib
        onView(withId(R.id.drawer_layout))
                .perform(DrawerActions.open());

        // Click on the "Notifications" menu item
        onView(withText("Create an Event"))
                .perform(click());

        // Fill in the event creation form
        onView(withId(R.id.eventNameEditText))
                .perform(typeText(eventName));

        onView(withId(R.id.eventDescriptionEditText))
                .perform(typeText(eventDescription));



        onView(withId(R.id.startDateButton))
                .perform(click());

        // Perform actions on the DatePickerDialog to set the date to December 20, 2024
        onView(withClassName(Matchers.equalTo(android.widget.DatePicker.class.getName())))
                .perform(PickerActions.setDate(2024, 12, 20));
        onView(withText("OK")).inRoot(RootMatchers.isDialog()).perform(click());
        onView(withText("OK")).inRoot(RootMatchers.isDialog()).perform(click());


        onView(withId(R.id.endDateButton))
                .perform(click());
        // Perform actions on the DatePickerDialog to set the date to December 23, 2024
        onView(withClassName(Matchers.equalTo(android.widget.DatePicker.class.getName())))
                .perform(PickerActions.setDate(2024, 12, 23));
        onView(withText("OK")).inRoot(RootMatchers.isDialog()).perform(click());
        onView(withText("OK")).inRoot(RootMatchers.isDialog()).perform(click());


        onView(withId(R.id.waitingListDeadlineButton))
                .perform(click());
        // Perform actions on the DatePickerDialog to set the date to December 15, 2024
        onView(withClassName(Matchers.equalTo(android.widget.DatePicker.class.getName())))
                .perform(PickerActions.setDate(2024, 12, 15));
        onView(withText("OK")).inRoot(RootMatchers.isDialog()).perform(click());
        onView(withText("OK")).inRoot(RootMatchers.isDialog()).perform(click());

        onView(withId(R.id.eventLocationEditText))
                .perform(typeText(eventLocation));

        onView(withId(R.id.availableSpotsEditText))
                .perform(typeText("30"));

        onView(withId(R.id.waitingListSpotsEditText))
                .perform(typeText("40"));

        onView(withId(R.id.scrollView)).perform(swipeUp());



        // Click on Select Poster button
        onView(withId(R.id.saveEventButton))
                .perform(click());

        intended(IntentMatchers.hasComponent(EntrantHomeActivity.class.getName()));




    }

    @Test
    public void testViewCreatedEvents(){
        // Launch OrganizerHomeActivity with IS_ADMIN = true
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantHomeActivity.class);
        ActivityScenario<EntrantHomeActivity> scenario = ActivityScenario.launch(intent);

        // Open the navigation drawer using DrawerActions from Espresso-Contrib
        onView(withId(R.id.drawer_layout))
                .perform(DrawerActions.open());

        // Click on the "Notifications" menu item
        onView(withText("My Created Events"))
                .perform(click());

        intended(IntentMatchers.hasComponent(EntrantHomeActivity.class.getName()));

    }


    /**
     * Helper method to get the current activity's decor view for Toast verification.
     *
     * @param scenario The ActivityScenario instance.
     * @return The decor view of the current activity.
     */
    private View getCurrentActivityDecorView(ActivityScenario<?> scenario) {
        final View[] decorView = new View[1];
        scenario.onActivity(activity -> decorView[0] = activity.getWindow().getDecorView());
        return decorView[0];
    }
}
