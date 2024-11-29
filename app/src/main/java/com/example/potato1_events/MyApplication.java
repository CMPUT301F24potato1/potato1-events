package com.example.potato1_events;

import android.app.Application;
import androidx.work.*;

import java.util.concurrent.TimeUnit;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";

    private EventStatusListener eventStatusListener;

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize and start EventStatusListener for notifications
        eventStatusListener = new EventStatusListener(this);
        eventStatusListener.startListening();
        scheduleRandomDrawWorker();


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
//        PeriodicWorkRequest randomDrawWorkRequest =
//                new PeriodicWorkRequest.Builder(RandomDrawWorker.class, 1, TimeUnit.MINUTES)
//                        .setConstraints(constraints)
//                        .build();

//        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
//                "RandomDrawWork",
//                ExistingPeriodicWorkPolicy.KEEP,
//                randomDrawWorkRequest);
//    }
}