package com.example.potato1_events;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.provider.Settings;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Rule;
import org.junit.Test;

//import androidx.test.espresso.intent.Intents;
//import androidx.test.espresso.intent.matcher.IntentMatchers;

public class LandingUITest {
//    private FirebaseFirestore firestore;
//    private String deviceId;


    @Rule
    public ActivityScenarioRule<LandingActivity> scenario = new ActivityScenarioRule<LandingActivity>(LandingActivity.class);

    // Test 1: Check whether the activity switches correctly
    @Test
    public void testEntrantButton_ActivitySwitch() {
        // Initialize Firebase Firestore
//        firestore = FirebaseFirestore.getInstance();
        // Get device ID
//        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        //Click on Add City button
        onView(withId(R.id.entrantButton)).perform(click());
        // 1-second delay to wait for activity transition
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        onView(withId(R.id.nameEditText)).check(matches(isDisplayed()));

//        firestore.collection("Entrants").document(deviceId).get()
//                .addOnSuccessListener(documentSnapshot -> {
//                    if (documentSnapshot.exists()) {
//                        onView(withId(R.id.eventsLinearLayout)).check(matches(isDisplayed()));
//                    } else {
//                        // check if activity was changed
//                        onView(withId(R.id.nameEditText)).check(matches(isDisplayed()));
//                    }
//
//    });
    }
    // Test 2: Check whether the activity switches correctly when Organizer
    @Test
    public void testOrganizerButton_ActivitySwitch() {

        //Click on Add City button
        onView(withId(R.id.organizerButton)).perform(click());

        // 1-second delay to wait for activity transition
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // check if activity was changed
        onView(withId(R.id.nameEditText)).check(matches(isDisplayed()));

    }
}

