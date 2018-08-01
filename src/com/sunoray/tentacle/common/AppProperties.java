package com.sunoray.tentacle.common;

public class AppProperties {
	
	public static final String APP_TYPE = "production"; 									// staging | production
	public static final String PREFERANCE_FILENAME_STRING = "sunoray.tentacle.appfile";
	public static final String PROJECT_ID = APP_TYPE.equalsIgnoreCase("production") ? "375051463289" : "49812070842";
	public static final String QUEUE_FILENAME_STRING = "tentacle.queue";

	public static final String WEB_APP_URL 		= APP_TYPE.equalsIgnoreCase("production") ? "https://tentacle.sunoray.net" : "https://tentaclecrm.herokuapp.com";				//  http://tentacle.sunoray.net | http://tentaclecrm.herokuapp.com
	public static final String MEDIA_SERVER_URL = APP_TYPE.equalsIgnoreCase("production") ? "http://tentaclecall.sunoray.net" : "http://tentaclecall.sunoray.net";			//	http://drive.sunoray.net 	| http://192.168.0.21:8080
	public static final String SIGN_UP_PAGE 	= "/sales/sign_up?source=Android";
	
	public static final String SERVER_NAME_STRING 	  = "/TentacleCall";
	public static final String DEVICE_RECEIVER_STRING = "/Pin";
	public static final String FILE_RECEIVER_STRING   = "/Receiver";
	public static final String SYNC_ALERT_STRING 	  = "/SyncAlert";
	public static final String DEVICE_CALLHIT_STRING  = "/ack";
	public static final String DEVICE_LOG 			  = "/ack";
	
	// External Media File Path
	public static final String CAMERA_IMAGE_DIR 	 = "/DCIM/Tentacle/";
	public static final String COMPRESS_IMAGE_DIR 	 = "/Tentacle/Media/Tentacle Image/";
	public static final String DEVICE_RECORDING_PATH = "/Tentacle/Media/Tentacle Rec/";
	public static final String DEVICE_LOGFILE_PATH   = "/Tentacle/Log/TentacleLog.log";
	
	// Location Setting
	public static final long DEFAULT_MIN_DISTANCE_BW_UPDATES = 250;							// distance in meter
	public static final long DEFAULT_MIN_TIME_BW_UPDATES 	 = 30000;							// Time in milliseconds
	
	// View activity broadcast trying limit 
	public static final int MAX_TRY_FOR_BROADCAST = 15;
	
	public static boolean activeAlertDialog = false;
	public static boolean dialerCallback 	= false;
	
	// required for SDK < 22
	public static boolean isCallServiceRunning = false;      								// To avoid duplicate call 

}