package com.example.potato1_events;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Rule;
import org.junit.Test;

//import androidx.test.espresso.intent.Intents;
//import androidx.test.espresso.intent.matcher.IntentMatchers;

public class LandingUITest {
    @Rule
    public ActivityScenarioRule<LandingActivity> scenario = new ActivityScenarioRule<LandingActivity>(LandingActivity.class);

    // Test 1: Check whether the activity switches correctly
    @Test
    public void testEntrantButton_ActivitySwitch() {

        //Click on Add City button
        onView(withId(R.id.entrantButton)).perform(click());

        // 1-second delay to wait for activity transition
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // check if activity was changed
        onView(withId(R.id.nameEditText)).check(matches(isDisplayed()));

    }
}

