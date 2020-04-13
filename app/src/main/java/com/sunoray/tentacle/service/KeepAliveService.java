package com.sunoray.tentacle.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.telephony.TelephonyManager;

import com.sunoray.tentacle.R;
import com.sunoray.tentacle.StartupActivity;
import com.sunoray.tentacle.application.ApplicationExtension;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeepAliveService extends Service {
  private static final Logger log = LoggerFactory.getLogger(KeepAliveService.class);
  CallBarring callReceiver = new CallBarring();

  @Override
  public void onCreate() {
    //starting ongoing notification for creating foreground services
    startForeground(4, buildForegroundNotification("Monitoring calls"));
    log.debug("NotificationService started");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    // registering call receiver here
    IntentFilter phnIntent = new IntentFilter();
    phnIntent.addAction(TelephonyManager.EXTRA_STATE);
    registerReceiver(callReceiver, phnIntent);

    // If we get killed, after returning from here, restart
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    unregisterReceiver(callReceiver);
    stopForeground(true);
    log.debug("NotificationService ends");
  }

  @Override
  public IBinder onBind(Intent intent) {
    // We don't provide binding, so return null
    return null;
  }

  private Notification buildForegroundNotification(String filename) {
    Intent notificationIntent = new Intent(getApplicationContext(), StartupActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),0, notificationIntent,0);

    //To notify : Setting icon in case of recording in background
    Notification builder = new NotificationCompat.Builder(getApplicationContext(),
            ApplicationExtension.BACK_CHANNEL_ID)
            .setOngoing(true)
            .setContentText(filename)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_launcher)
            .setTicker("Monitoring calls")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build();

    return builder;
  }

}