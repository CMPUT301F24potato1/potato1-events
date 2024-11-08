package com.example.potato1_events;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * Instrumented UI tests for the LandingActivity.
 * <p>
 * These tests verify the behavior of the LandingActivity when the entrant or organizer buttons are clicked,
 * ensuring that the app navigates to the correct activity based on whether the user exists.
 * </p>
 */
@RunWith(AndroidJUnit4.class)
public class LandingActivityTest {

    /**
     * Sets up the testing environment before each test.
     * Initializes Espresso Intents to capture and verify intents.
     */
    @Before
    public void setUp() {
        Intents.init(); // Initialize Espresso Intents
    }

    /**
     * Cleans up the testing environment after each test.
     * Releases Espresso Intents to avoid memory leaks.
     */
    @After
    public void tearDown() {
        Intents.release(); // Release Espresso Intents
    }

    /**
     * Tests that clicking the entrant button navigates to EntrantHomeActivity when the user exists.
     */
    @Test
    public void testEntrantButton_UserExists() {
        // Mock the UserRepository to simulate a user that exists and is not an admin
        UserRepository mockUserRepository = new UserRepository(null) {
            @Override
            public void checkUserExists(String userType, String deviceId, UserExistsCallback callback) {
                // Simulate user exists with isAdmin = false
                UserData userData = new UserData(true, false);
                callback.onResult(userData);
            }
        };

        // Launch the LandingActivity
        ActivityScenario<LandingActivity> scenario = ActivityScenario.launch(LandingActivity.class);

        // Inject the mocked UserRepository into the activity
        scenario.onActivity(activity -> activity.setUserRepository(mockUserRepository));

        // Perform click on the entrant button
        onView(withId(R.id.entrantButton)).perform(click());

        // Verify that EntrantHomeActivity is launched
        intended(hasComponent(EntrantHomeActivity.class.getName()));
    }

    /**
     * Tests that clicking the entrant button navigates to UserInfoActivity when the user does not exist.
     */
    @Test
    public void testEntrantButton_UserDoesNotExist() {
        // Mock the UserRepository to simulate a user that does not exist
        UserRepository mockUserRepository = new UserRepository(null) {
            @Override
            public void checkUserExists(String userType, String deviceId, UserExistsCallback callback) {
                // Simulate user does not exist
                UserData userData = new UserData(false, false);
                callback.onResult(userData);
            }
        };

        // Launch the LandingActivity
        ActivityScenario<LandingActivity> scenario = ActivityScenario.launch(LandingActivity.class);

        // Inject the mocked UserRepository into the activity
        scenario.onActivity(activity -> activity.setUserRepository(mockUserRepository));

        // Perform click on the entrant button
        onView(withId(R.id.entrantButton)).perform(click());

        // Verify that UserInfoActivity is launched
        intended(hasComponent(UserInfoActivity.class.getName()));
    }

    /**
     * Tests that clicking the organizer button navigates to OrganizerHomeActivity when the user exists.
     */
    @Test
    public void testOrganizerButton_UserExists() {
        // Mock the UserRepository to simulate a user that exists
        UserRepository mockUserRepository = new UserRepository(null) {
            @Override
            public void checkUserExists(String userType, String deviceId, UserExistsCallback callback) {
                // Simulate user exists
                UserData userData = new UserData(true, false);
                callback.onResult(userData);
            }
        };

        // Launch the LandingActivity
        ActivityScenario<LandingActivity> scenario = ActivityScenario.launch(LandingActivity.class);

        // Inject the mocked UserRepository into the activity
        scenario.onActivity(activity -> activity.setUserRepository(mockUserRepository));

        // Perform click on the organizer button
        onView(withId(R.id.organizerButton)).perform(click());

        // Verify that OrganizerHomeActivity is launched
        intended(hasComponent(OrganizerHomeActivity.class.getName()));
    }

    /**
     * Tests that clicking the organizer button navigates to UserInfoActivity when the user does not exist.
     */
    @Test
    public void testOrganizerButton_UserDoesNotExist() {
        // Mock the UserRepository to simulate a user that does not exist
        UserRepository mockUserRepository = new UserRepository(null) {
            @Override
            public void checkUserExists(String userType, String deviceId, UserExistsCallback callback) {
                // Simulate user does not exist
                UserData userData = new UserData(false, false);
                callback.onResult(userData);
            }
        };

        // Launch the LandingActivity
        ActivityScenario<LandingActivity> scenario = ActivityScenario.launch(LandingActivity.class);

        // Inject the mocked UserRepository into the activity
        scenario.onActivity(activity -> activity.setUserRepository(mockUserRepository));

        // Perform click on the organizer button
        onView(withId(R.id.organizerButton)).perform(click());

        // Verify that UserInfoActivity is launched
        intended(hasComponent(UserInfoActivity.class.getName()));
    }
}
