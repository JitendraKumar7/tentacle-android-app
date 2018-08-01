package com.sunoray.tentacle.common;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferanceUtil {
	public static final String PINID = "PINID";							//	""
	public static final String REGID = "REGID";							//	""
	public static final String AUTHTOKEN = "AUTHTOKEN";					//	""
	public static final String isServiceStarted = "isServiceStarted";	//	"false"
	public static final String lastURL = "lastURL";						// AppProperties.webAppURL + "/campaigns"
	public static final String AudioSource = "AudioSource";				// 	0
	public static final String SyncOption = "SyncOption";				//	0
	public static final String RecordingOption = "RECORDING";			//	0
	public static final String lastSyncTime = "lastSyncTime";			// 1970-1-1 00:00:00
	public static final String locationAccess = "false";				// true/false/ignore
	public static final String userRole = "userRole";
	public static final String locationAlert = "locationAlert";
	public static final String userUniqueID = "userUniqueID";
	public static final String userID = "userID";
	public static final String storageDrive = "storageDrive";
	public static final String MIN_DISTANCE_CHANGE_FOR_UPDATES = "minDistance";	// The minimum distance to change Updates in meters
	public static final String MIN_TIME_BW_UPDATES = "minTime" ;	
	
	private static final String[] AUDIOSOURCE_ITEM = {"MIC","VOICE CALL"};		// Always keep MIC at position 0
	private static final String[] SyncOption_ITEM = {"Sync Anytime"+"\n"+"Data charges may apply.","Sync over Wi-Fi only"};
	private static final String[] RECORDING_ITEM ={"On","Off"};
		
	
	public static boolean setSharedPreferences(Context context, String key, String value) {
		try {
			SharedPreferences.Editor editor = context.getSharedPreferences(AppProperties.PREFERANCE_FILENAME_STRING, Context.MODE_PRIVATE).edit();
		    editor.putString(key, value).commit();
		    editor.apply();	
		    return true;
		} catch (Exception e) {
			return false;
		}
	}
		
	public static String getSharedPreferences(Context context, String key, String returnvalue) {
		try {
			return context.getSharedPreferences(AppProperties.PREFERANCE_FILENAME_STRING, Context.MODE_PRIVATE).getString(key, returnvalue);
		} catch (Exception e) {
			return returnvalue;
		}
	}
	
	public static long getSharedPreferences(Context context, String key, long returnvalue) {
		try {
			return Long.parseLong( context.getSharedPreferences(AppProperties.PREFERANCE_FILENAME_STRING, Context.MODE_PRIVATE).getString(key, String.valueOf(returnvalue)) );
		} catch (Exception e) {
			return returnvalue;
		}
	}
			
	public static String[] getAudiosourceItems() {
		return AUDIOSOURCE_ITEM;
	}
	public static String[] getSyncOptions() {
		return SyncOption_ITEM;
	}
	public static String[] getRecordingOption() {
		return RECORDING_ITEM;
	}
	
}