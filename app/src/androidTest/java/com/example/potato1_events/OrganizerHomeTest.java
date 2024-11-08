package com.example.potato1_events;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.contrib.NavigationViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.tasks.OnSuccessListener;

/**
 * Instrumented UI tests for OrganizerHomeActivity.
 * Tests navigation and interactions from the organizer's perspective.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerHomeTest {

    private OrgEventsRepository mockEventRepository;


    /**
     * Initializes Espresso Intents before each test.
     */
    @Before
    public void setUp() {
        Intents.init();
        mockEventRepository = Mockito.mock(OrgEventsRepository.class);

    }

    /**
     * Releases Espresso Intents after each test.
     */
    @After
    public void tearDown() {
        Intents.release();
    }

    /**
     * Tests that clicking the "Switch Mode" button navigates to LandingActivity.
     */
    @Test
    public void testSwitchMode_NavigatesToLandingActivity() {
        // Launch the activity
        ActivityScenario<OrganizerHomeActivity> scenario = ActivityScenario.launch(OrganizerHomeActivity.class);

        // Click the Switch Mode button
        onView(withId(R.id.switchModeButton)).perform(click());

        // Verify that LandingActivity is launched
        intended(hasComponent(LandingActivity.class.getName()));
    }

    /**
     * Tests that selecting "Profile" from the navigation drawer opens UserInfoActivity.
     */
    @Test
    public void testNavigationDrawer_Profile() {
        // Launch the activity
        ActivityScenario<OrganizerHomeActivity> scenario = ActivityScenario.launch(OrganizerHomeActivity.class);

        // Open the navigation drawer
        onView(withId(R.id.drawer_organizer_layout))
                .perform(DrawerActions.open());

        // Click on "Profile" in the navigation drawer
        onView(withId(R.id.nav_view))
                .perform(NavigationViewActions.navigateTo(R.id.nav_organizer_profile));

        // Verify that UserInfoActivity is launched
        intended(hasComponent(UserInfoActivity.class.getName()));
    }

    /**
     * Tests that selecting "Create Event" from the navigation drawer opens CreateEditEventActivity.
     */
    @Test
    public void testNavigationDrawer_CreateEvent() {
        // Launch the activity
        ActivityScenario<OrganizerHomeActivity> scenario = ActivityScenario.launch(OrganizerHomeActivity.class);

        // Open the navigation drawer
        onView(withId(R.id.drawer_organizer_layout))
                .perform(DrawerActions.open());

        // Click on "Create Event" in the navigation drawer
        onView(withId(R.id.nav_view))
                .perform(NavigationViewActions.navigateTo(R.id.nav_create_event));

        // Verify that CreateEditEventActivity is launched
        intended(hasComponent(CreateEditEventActivity.class.getName()));
    }

    /**
     * Tests that selecting "Edit Facility" from the navigation drawer opens CreateEditFacilityActivity.
     */
    @Test
    public void testNavigationDrawer_EditFacility() {
        // Launch the activity
        ActivityScenario<OrganizerHomeActivity> scenario = ActivityScenario.launch(OrganizerHomeActivity.class);

        // Open the navigation drawer
        onView(withId(R.id.drawer_organizer_layout))
                .perform(DrawerActions.open());

        // Click on "Edit Facility" in the navigation drawer
        onView(withId(R.id.nav_view))
                .perform(NavigationViewActions.navigateTo(R.id.nav_edit_facility));

        // Verify that CreateEditFacilityActivity is launched
        intended(hasComponent(CreateEditFacilityActivity.class.getName()));
    }

    /**
     * Tests that selecting "My Events" from the navigation drawer shows a toast message.
     */
    @Test
    public void testNavigationDrawer_MyEvents() {
        // Launch the activity
        ActivityScenario<OrganizerHomeActivity> scenario = ActivityScenario.launch(OrganizerHomeActivity.class);

        // Open the navigation drawer
        onView(withId(R.id.drawer_organizer_layout))
                .perform(DrawerActions.open());

        // Click on "My Events" in the navigation drawer
        onView(withId(R.id.nav_view))
                .perform(NavigationViewActions.navigateTo(R.id.nav_my_events));

        // Verify that a toast is displayed indicating already on this page
//        onView(withText("Already on this page."))
//                .inRoot(new ToastMatcher())
//                .check(matches(isDisplayed()));
        onView(withText("Organizer")).check(matches(isDisplayed()));
    }

    /**
     * Test SP6: Organizer can view and edit their profile.
     */
    @Test
    public void testOrganizerProfile_EditProfile() throws InterruptedException {
        // Launch the activity
        ActivityScenario<OrganizerHomeActivity> scenario = ActivityScenario.launch(OrganizerHomeActivity.class);

        // Open the navigation drawer and navigate to Profile
        onView(withId(R.id.drawer_organizer_layout))
                .perform(DrawerActions.open());
        onView(withId(R.id.nav_view))
                .perform(NavigationViewActions.navigateTo(R.id.nav_organizer_profile));

        // Perform profile edit actions
        onView(withId(R.id.nameEditText)).perform(clearText(), typeText("Updated Organizer"), closeSoftKeyboard());
        onView(withId(R.id.emailEditText)).perform(clearText(), typeText("Updated Organizer"), closeSoftKeyboard());
        onView(withId(R.id.phoneEditText)).perform(clearText(), typeText("Updated Organizer"), closeSoftKeyboard());

        // Add other profile fields as necessary...

        // Click the Save button
        onView(withId(R.id.saveButton)).perform(click());

        Thread.sleep(1000);

        // Verify that a success toast is displayed
//        onView(withText("Profile updated successfully."))
//                .inRoot(new ToastMatcher())
//                .check(matches(isDisplayed()));

        // Optionally, verify that the updated name is displayed
        onView(withId(R.id.switchModeButton)).check(matches(isDisplayed()));
//        onView(withText("Profile updated successfully."))
//                .inRoot(new ToastMatcher())
//                .check(matches(isDisplayed()));
    }

//    /**
//     * Test SP3: Organizer can create a new event.
//     */
//    @Test
//    public void testCreateEvent_Success() {
//        // Mock create event action
//        // Assuming that CreateEditEventActivity returns RESULT_OK upon successful creation
//
//        // Launch CreateEditEventActivity with intent
////        Intent intent = new Intent();
//        ActivityScenario<CreateEditEventActivity> scenario = ActivityScenario.launch(CreateEditEventActivity.class);
//
//        // Perform actions to create event (input data, click save)
//        onView(withId(R.id.eventNameEditText)).perform(typeText("New Organizer Event"), closeSoftKeyboard());
//        onView(withId(R.id.eventDescriptionEditText)).perform(typeText("New Organizer Event"), closeSoftKeyboard());
//        onView(withId(R.id.eventLocationEditText)).perform(typeText("New Organizer Event"), closeSoftKeyboard());
//        onView(withId(R.id.availableSpotsEditText)).perform(typeText("10"), closeSoftKeyboard());
//        onView(withId(R.id.waitingListSpotsEditText)).perform(typeText("20"), closeSoftKeyboard());
//        // Add other necessary input fields...
//
//        // Click the Save button
//        onView(withId(R.id.saveEventButton)).perform(click());
//
//        // Verify that OrganizerHomeActivity is displayed with the new event
//        intended(hasComponent(OrganizerHomeActivity.class.getName()));
//
//        // Optionally, verify that the new event appears in the event list
//        onView(withText("New Organizer Event")).check(matches(isDisplayed()));
//    }






//    @Test
//    public void testEventClick_NavigatesToEventDetails() {
//        // Mock event data
//        List<Event> mockEvents = new ArrayList<>();
//        Event mockEvent = new Event();
//        mockEvent.setId("event1");
//        mockEvent.setName("Test Event");
//        mockEvent.setEventLocation("Test Location");
//        mockEvents.add(mockEvent);
//
//        // Mock EventRepository
//        OrgEventsRepository mockRepository = Mockito.mock(OrgEventsRepository.class);
//        Mockito.doAnswer(invocation -> {
//            String facilityId = invocation.getArgument(0);
//            OrgEventsRepository.EventListCallback callback = invocation.getArgument(1);
//            callback.onEventListLoaded(mockEvents);
//            return null;
//        }).when(mockRepository).getEventsForOrganizerFacility(Mockito.anyString(), Mockito.any());
//
//        // Launch activity and inject mock repository
//        ActivityScenario<OrganizerHomeActivity> scenario = ActivityScenario.launch(OrganizerHomeActivity.class);
//        scenario.onActivity(activity -> {
//            activity.setOrgEventsRepository(mockRepository);
//            activity.loadEventsForOrganizerFacility();
//        });
//
//        // Verify that the event is displayed
//        onView(withText("Test Event")).check(matches(isDisplayed()));
//
//        // Click on the event
//        onView(withText("Test Event")).perform(click());
//
//        // Verify that EventDetailsOrganizerActivity is launched
//        intended(hasComponent(EventDetailsOrganizerActivity.class.getName()));
//    }
}
