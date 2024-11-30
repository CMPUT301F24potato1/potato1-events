package com.example.potato1_events;

import android.app.Application;
import androidx.work.*;

import java.util.concurrent.TimeUnit;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";
    private RandomDrawListener randomDrawListener;
    private EventStatusListener eventStatusListener;

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize and start EventStatusListener for notifications
        eventStatusListener = new EventStatusListener(this);
        eventStatusListener.startListening();
        scheduleRandomDrawWorker();
        randomDrawListener = new RandomDrawListener(this);
        randomDrawListener.startListening();


    }

    private void scheduleRandomDrawWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // Requires internet connection
                .build();
        //FIXME runs only once for now figure out the what frequency to run this at
        OneTimeWorkRequest randomDrawWorkRequest = new OneTimeWorkRequest.Builder(RandomDrawWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueue(randomDrawWorkRequest);
    }
    @Override
    public void onTerminate() {
        super.onTerminate();
        // Stop the real-time listener
        if (randomDrawListener != null) {
            randomDrawListener.stopListening();
        }
    }
}