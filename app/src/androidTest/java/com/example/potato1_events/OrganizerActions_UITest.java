package com.example.potato1_events;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withResourceName;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

import android.Manifest;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;


import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;


public class OrganizerActions_UITest {
    @Rule
    public GrantPermissionRule grantPermissionRule =
            GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION);


    @Rule
    public ActivityScenarioRule<LandingActivity> scenario = new ActivityScenarioRule<LandingActivity>(LandingActivity.class);

    public void navigateToOrgHome(){
        onView(withId(R.id.organizerButton)).perform(click());

        // 1-second delay to wait for activity transition
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // test to check how an event is opened after click to see its details
    @Test
    public void testViewEvent(){
        navigateToOrgHome();
        onView(withText("party")).perform(click());
        onView(withId(R.id.eventDescriptionTextView)).check(matches(isDisplayed()));

    }
    // Test to check the waiting list of an event
    @Test
    public void testViewWaitlist(){
        navigateToOrgHome();
        onView(withText("party")).perform(click());
        onView(withId(R.id.entrantsListButton)).perform(click());
        onView(withText("Waiting List")).check(matches(isDisplayed()));

    }

    // test to check the edit event button takes us to the edit event page
    @Test
    public void testEditEventIntent(){
        navigateToOrgHome();
        onView(withText("party")).perform(click());
        onView(withId(R.id.editButton)).perform(click());
        onView(withId(R.id.eventNameEditText)).check(matches(isDisplayed()));


    }

    @Test
    public void testManageProfile() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), CreateEditFacilityActivity.class);
        navigateToOrgHome();
        try (ActivityScenario<CreateEditFacilityActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.facilityNameEditText)).check(matches(isDisplayed()));

        }
    }
}
