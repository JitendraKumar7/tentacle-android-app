package com.sunoray.tentacle.service;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;

import com.sunoray.tentacle.common.PreferenceUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CheckKeepAliveStatus extends JobService {
    private static final Logger log = LoggerFactory.getLogger(CheckKeepAliveStatus.class);

    @Override
    public boolean onStartJob(JobParameters params) {
        log.debug("notification-killer", "canceling notification");

        // stopping notification service here
        if (PreferenceUtil.getSharedPreferences(getBaseContext(),
                PreferenceUtil.TrackInboundOption, "1").equalsIgnoreCase("0")) {
            Intent serviceIntent = new Intent(this, KeepAliveService.class);
            ContextCompat.startForegroundService(this, serviceIntent);
        } else {
            Intent serviceIntent = new Intent(getApplicationContext(), KeepAliveService.class);
            stopService(serviceIntent);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

}
