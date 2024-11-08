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
        //FIXME runs only once for now figure out the what frequency to run this at
        OneTimeWorkRequest randomDrawWorkRequest = new OneTimeWorkRequest.Builder(RandomDrawWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueue(randomDrawWorkRequest);
    }
//        PeriodicWorkRequest randomDrawWorkRequest =
//                new PeriodicWorkRequest.Builder(RandomDrawWorker.class, 15, TimeUnit.MINUTES)
//                        .setConstraints(constraints)
//                        .build();

//        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
//                "RandomDrawWork",
//                ExistingPeriodicWorkPolicy.KEEP,
//                randomDrawWorkRequest);
//    }
}