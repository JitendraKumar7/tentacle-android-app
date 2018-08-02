package com.sunoray.tentacle.tasks;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.ActivityManager;
import android.content.Context;

import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.Util;

public class BroadcastCallDuration extends Thread {
	
	private static final Logger log = LoggerFactory.getLogger(BroadcastCallDuration.class);

	private Context context;
	int callDuration;
	long dialTime;

	public BroadcastCallDuration(Context context, int callDuration, long dialTime) {
		this.context = context;
		this.callDuration = callDuration;
		this.dialTime = dialTime;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void run() {
		
		try {
			Thread.currentThread().setName("BroadcastCallDur");
			ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
			log.info("sending call duration message to view");
			
			int noOfTried = 0;
			String dialTimeInSec = "" + dialTime/1000L; 
					
			while (noOfTried < AppProperties.MAX_TRY_FOR_BROADCAST) {
				List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
				if (taskInfo.get(0).topActivity.getPackageName().equalsIgnoreCase("com.sunoray.tentacle") && !AppProperties.activeAlertDialog) {
					Util.send2ViewActivity(context, "Recording", "CALL_STATUS",	callDuration + "", dialTimeInSec);
					log.info("message sent to: " + taskInfo.get(0).topActivity.getClassName());
					break;
				} else {
					log.info("message not sent, waiting because of: "+ taskInfo.get(0).topActivity.getPackageName());
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						log.info("Exception in thread of sending msg to view activity=", e);
					}
				}
				noOfTried++;
			}
			
		} catch (Exception e) {
			log.debug("Error Occurred",e);
		}
	}
}