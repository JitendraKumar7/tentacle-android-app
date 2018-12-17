package com.sunoray.tentacle.service;

import android.app.NotificationManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.sunoray.tentacle.common.PreferenceUtil;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CheckNotificationStatus extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d("notification-killer", "canceling notification");

        // stopping notification service here
        if (PreferenceUtil.getSharedPreferences(getBaseContext(),
                PreferenceUtil.TrackInboundOption, "1").equalsIgnoreCase("0")) {
            Intent serviceIntent = new Intent(this, NotificationService.class);
            ContextCompat.startForegroundService(this, serviceIntent);
        } else {
            Intent serviceIntent = new Intent(getApplicationContext(), NotificationService.class);
            stopService(serviceIntent);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
