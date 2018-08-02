package com.sunoray.tentacle.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class BetteryHelper {
	
	private static final Logger log = LoggerFactory.getLogger(BetteryHelper.class);
	
	public static String getBatteryLevel(Context context) {
    	try{
	        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
	        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
	
	        // Error checking that probably isn't needed but I added just in case.
	        if(level == -1 || scale == -1) {
	            return String.valueOf( 50 );  
	        }
	        return String.valueOf( ((float)level / (float)scale) * 100.0f );
    	}catch (Exception e) {
    		log.info(" :"+ e);
		}
    	return "-1";
    }
}