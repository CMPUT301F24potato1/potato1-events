package com.example.potato1_events;

import android.app.Application;
import androidx.work.*;

import java.util.concurrent.TimeUnit;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        scheduleRandomDrawWorker();
    }

    private void scheduleRandomDrawWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // Requires internet connection
                .build();

        PeriodicWorkRequest randomDrawWorkRequest =
                new PeriodicWorkRequest.Builder(RandomDrawWorker.class, 1, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "RandomDrawWork",
                ExistingPeriodicWorkPolicy.KEEP,
                randomDrawWorkRequest);
    }
}