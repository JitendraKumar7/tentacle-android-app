package com.sunoray.tentacle.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;

import com.sunoray.tentacle.common.PreferanceUtil;
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
	}
	
    @SuppressLint("NewApi")
	@Override
	public void onStart(Intent intent, int startId) {
    	try {
    		String isServiceStarted = PreferanceUtil.getSharedPreferences(getBaseContext(), PreferanceUtil.isServiceStarted, "false");
        	log.info("BackGroundService Started | BackGroundService Status:" + isServiceStarted);
    		if (isServiceStarted.equalsIgnoreCase("false") || Util.isAfterInterval(PreferanceUtil.getSharedPreferences(getBaseContext(), PreferanceUtil.lastSyncTime, "1970-01-01 00:00:00"), 20)) {
    			
    			PreferanceUtil.setSharedPreferences(getBaseContext(), PreferanceUtil.isServiceStarted, "true");
    			PreferanceUtil.setSharedPreferences(getBaseContext(), PreferanceUtil.lastSyncTime, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Calendar.getInstance().getTime()));
    			
    			AsyncTask<Void, Void, Boolean> aTask = new BulkUploadLoop(getBaseContext());
    			aTask.execute();
    			//stopService(new Intent(this, com.sunoray.tentacle.service.BackGroundService.class));
    		} else {
    			log.info("BackGroundService Already running");
    		}	
		} catch (Exception e) {
			log.debug("Exception occured in onStart: "+e);
		}
	}
    
    @Override
    public void onDestroy() {
    	log.info("BackGroundService Stopped");
    } 
}