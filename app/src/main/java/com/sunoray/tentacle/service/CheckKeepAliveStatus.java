package com.sunoray.tentacle.service;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.sunoray.tentacle.common.PreferenceUtil;
import com.sunoray.tentacle.common.Util;

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
        showAvailableSpace();
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    private void showAvailableSpace() {
        double per1 = (Util.getAvailableInternalMemorySize() * 1.0 / Util.getTotalInternalMemorySize()) * 100.0;
        Log.d("showAvailableSpace", "\n\n ------ Available Internal Memory : "
                + Util.formatSize(Util.getAvailableInternalMemorySize())
                + "/" + Util.formatSize(Util.getTotalInternalMemorySize())
                + " (" + (float) per1 + " %) "
                + "------ \n\n");

        double per2 = (Util.getAvailableExternalMemorySize() * 1.0 / Util.getTotalExternalMemorySize()) * 100.0;
        Log.d("showAvailableSpace", "\n\n ------ Available External Memory : "
                + Util.formatSize(Util.getAvailableExternalMemorySize())
                + "/" + Util.formatSize(Util.getTotalExternalMemorySize())
                + " (" + (float) per2 + " %) "
                + "------ \n\n");

        double per3 = (Util.getAvilableRamSize(getApplicationContext()) * 1.0 / Util.getTotalRamSize(getApplicationContext())) * 100.0;
        Log.d("showAvailableSpace", "\n\n ------ Available RAM : "
                + Util.formatSize(Util.getAvilableRamSize(getApplicationContext()))
                + "/" + Util.formatSize(Util.getTotalRamSize(getApplicationContext()))
                + " (" + (float) per3 + " %) "
                + " ------- \n\n");
    }
}
