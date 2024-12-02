package com.example.potato1_events;

import android.app.Application;
import androidx.work.*;

import java.util.concurrent.TimeUnit;

/**
 * Custom Application class for the Potato1 Events application.
 * Initializes essential listeners and schedules background workers upon application start.
 * Ensures that listeners are properly stopped when the application is terminated.
 */
public class MyApplication extends Application {

    /**
     * Tag for logging purposes.
     */
    private static final String TAG = "MyApplication";

    /**
     * Listener for handling random draw operations related to events.
     */
    private RandomDrawListener randomDrawListener;

    /**
     * Listener for monitoring and updating event statuses in real-time.
     */
    private EventStatusListener eventStatusListener;

    /**
     * Called when the application is starting, before any other application objects have been created.
     * Initializes listeners and schedules background workers.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize and start EventStatusListener for handling real-time event status updates
        eventStatusListener = new EventStatusListener(this);
        eventStatusListener.startListening();

        // Schedule the RandomDrawWorker to handle periodic random draw operations
        scheduleRandomDrawWorker();

        // Initialize and start RandomDrawListener for handling random draw-related events
        randomDrawListener = new RandomDrawListener(this);
        randomDrawListener.startListening();
    }

    /**
     * Schedules a background worker to perform random draw operations.
     * Currently set to run only once, but the frequency can be adjusted as needed.
     * Ensures that the worker runs only when the device has an active internet connection.
     */
    private void scheduleRandomDrawWorker() {
        // Define constraints for the worker: requires an active internet connection
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // Requires internet connection
                .build();

        // FIXME runs only once for now figure out the what frequency to run this at
        // Create a OneTimeWorkRequest for the RandomDrawWorker with the specified constraints
        OneTimeWorkRequest randomDrawWorkRequest = new OneTimeWorkRequest.Builder(RandomDrawWorker.class)
                .setConstraints(constraints)
                .build();

        // Enqueue the work request to be executed by WorkManager
        WorkManager.getInstance(this).enqueue(randomDrawWorkRequest);
    }

    /**
     * Called when the application is terminating.
     * Ensures that all active listeners are properly stopped to prevent memory leaks.
     */
    @Override
    public void onTerminate() {
        super.onTerminate();

        // Stop the RandomDrawListener if it is active
        if (randomDrawListener != null) {
            randomDrawListener.stopListening();
        }

        // Stop the EventStatusListener if it is active
        if (eventStatusListener != null) {
            eventStatusListener.stopListening();
        }
    }
}
