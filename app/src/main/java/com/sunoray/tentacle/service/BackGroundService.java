package com.sunoray.tentacle.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.sunoray.tentacle.R;
import com.sunoray.tentacle.StartupActivity;
import com.sunoray.tentacle.application.ApplicationExtension;
import com.sunoray.tentacle.common.PreferenceUtil;
import com.sunoray.tentacle.common.Util;
import com.sunoray.tentacle.network.BulkUploadLoop;

public class BackGroundService extends Service {

	static private final Logger log = LoggerFactory.getLogger(BackGroundService.class);

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public void onCreate() {
		//starting ongoing notification for creating foreground services
		startForeground(3, buildForegroundNotification("Sync calls"));
		log.debug("BackGroundService started");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		try {
			String isServiceStarted = PreferenceUtil.getSharedPreferences(getBaseContext(), PreferenceUtil.isServiceStarted, "false");
			log.info("BackGroundService Started | BackGroundService Status:" + isServiceStarted);
			if (isServiceStarted.equalsIgnoreCase("false") || Util.isAfterInterval(PreferenceUtil.getSharedPreferences(getBaseContext(), PreferenceUtil.lastSyncTime, "1970-01-01 00:00:00"), 20)) {

				PreferenceUtil.setSharedPreferences(getBaseContext(), PreferenceUtil.isServiceStarted, "true");
				PreferenceUtil.setSharedPreferences(getBaseContext(), PreferenceUtil.lastSyncTime, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Calendar.getInstance().getTime()));

				AsyncTask<Void, Void, Boolean> aTask = new BulkUploadLoop(getBaseContext());
				aTask.execute();
				//stopService(new Intent(this, com.sunoray.tentacle.service.BackGroundService.class));
			} else {
				log.info("BackGroundService Already running");
			}
		} catch (Exception e) {
			log.debug("Exception occured in onStart: "+e);
		}
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		stopForeground(true);
		log.info("BackGroundService Stopped");
	}

	private Notification buildForegroundNotification(String filename) {
		Intent notificationIntent = new Intent(getApplicationContext(), StartupActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(
				getApplicationContext(),
				0,
				notificationIntent,
				0);

		//To notify : setting icon in case of recording in background
		Notification builder = new NotificationCompat.Builder(getApplicationContext(),
				ApplicationExtension.BACK_CHANNEL_ID)
				.setOngoing(true)
				.setContentText(filename)
				.setContentIntent(pendingIntent)
				.setSmallIcon(R.drawable.ic_launcher)
				.setTicker("Listening calls")
				.setPriority(NotificationCompat.PRIORITY_MIN)
				.build();

		return builder;
	}
}