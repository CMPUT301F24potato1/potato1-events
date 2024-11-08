package com.example.potato1_events;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.view.menu.ShowableListMenu;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.rule.GrantPermissionRule;
//import androidx.test.uiautomator.UiDevice;

//import androidx.test.espresso.intent.Intents;
//import androidx.test.espresso.intent.matcher.IntentMatchers;


import android.Manifest;
import android.app.ActionBar;
import android.app.Application;
import android.content.Intent;
import android.provider.Settings;
//import android.support.test.InstrumentationRegistry;


import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;


public class UserInfoTest {
    @Rule
    public ActivityScenarioRule<EntrantHomeActivity> scenario = new ActivityScenarioRule<EntrantHomeActivity>(EntrantHomeActivity.class);

    @Rule
    public GrantPermissionRule grantPermissionRule =
            GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION);

    @Test
    public void testProfilePicture(){
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), CreateEditEventActivity.class);


    }

}
