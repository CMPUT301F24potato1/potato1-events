package com.example.potato1_events;

import android.Manifest;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.typeText;
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
//@RunWith(AndroidJUnit4.class)
public class LandingActivityTest {

    @Rule
    public GrantPermissionRule grantPermissionRule =
            GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION);
    @Rule
    public GrantPermissionRule notificationPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS);


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
     * Tests that clicking the entrant button navigates to UserInfoActivity when the user does not exist.
     * US 01.02.01
     */
    @Test
    public void testEntrantButton_UserDoesNotExist() {
        // Mock the UserRepository to simulate a user that does not exist
        UserRepository mockUserRepository = new UserRepository(null) {
            @Override
            public void checkUserExists(String deviceId, UserExistsCallback callback) {
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
        onView(withId(R.id.nameEditText)).perform(clearText()).perform(typeText("Xavier"));
        onView(withId(R.id.emailEditText)).perform(clearText()).perform(typeText("x@hotmail"));
        onView(withId(R.id.phoneEditText)).perform(clearText()).perform(typeText("123"));

        onView(withId(R.id.saveButton)).perform(click());
    }
}