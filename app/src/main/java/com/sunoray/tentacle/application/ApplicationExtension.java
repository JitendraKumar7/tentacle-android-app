package com.sunoray.tentacle.application;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;


public class ApplicationExtension extends Application {

    public static final String BACK_CHANNEL_ID = "tentacle_calls";
    public static final String ALERT_CHANNEL_ID = "tentacle_alert";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        createAlertNotificationChannel();
    }

    private void createNotificationChannel() {
        try {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        BACK_CHANNEL_ID,
                        "Call Listener",
                        NotificationManager.IMPORTANCE_NONE);
                channel.setDescription("Background Notifications");
                // Change Badge to false to avoid badge in the app
                channel.setShowBadge(false);
                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
        } catch (Exception e) {
            Log.e("ApplicationExtension", "Exception in createNotificationChannel" + e);
        }
    }

    private void createAlertNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        ALERT_CHANNEL_ID,
                        "Alert",
                        NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Notifications");
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
        } catch (Exception e) {
            Log.e("ApplicationExtension", "Exception in createNotificationChannel" + e);
        }
    }
}
