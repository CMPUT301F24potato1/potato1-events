package com.example.potato1_events;

import android.os.IBinder;
import android.view.WindowManager.LayoutParams;

import androidx.test.espresso.Root;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * A custom matcher to check for Toast messages in Espresso tests.
 */
public class ToastMatcher extends TypeSafeMatcher<Root> {

    /**
     * Describes the matcher to the given description.
     *
     * @param description The description to append details to.
     */
    @Override
    public void describeTo(Description description) {
        description.appendText("is toast");
    }

    /**
     * Determines if the given root matches a Toast message.
     *
     * @param root The root to check.
     * @return True if the root matches a Toast message, false otherwise.
     */
    @Override
    public boolean matchesSafely(Root root) {
        int type = root.getWindowLayoutParams().get().type;
        if (type == LayoutParams.TYPE_TOAST || type == LayoutParams.FIRST_APPLICATION_WINDOW + 5) {
            IBinder windowToken = root.getDecorView().getWindowToken();
            IBinder appToken = root.getDecorView().getApplicationWindowToken();
            // Toast's window token and app token are the same
            return windowToken == appToken;
        }
        return false;
    }
}

