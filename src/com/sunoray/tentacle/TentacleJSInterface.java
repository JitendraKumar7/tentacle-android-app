package com.sunoray.tentacle;

import java.lang.reflect.Method;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sunoray.tentacle.bean.LocationBean;
import com.sunoray.tentacle.common.PreferanceUtil;
import com.sunoray.tentacle.helper.LocationTracker;
import com.sunoray.tentacle.service.TrackerService;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

public class TentacleJSInterface {

	private Context context;
	private WebView tentacleBrowser;
		
	static private final Logger log = LoggerFactory.getLogger(ViewActivity.class);
	
	public TentacleJSInterface(Context context, WebView tentacleBrowser) {
		this.context = context;
		this.tentacleBrowser = tentacleBrowser;
	}
	
	public TentacleJSInterface(Context context) {
		this.context = context;
	}
	
	
	@JavascriptInterface
	public String getLocation(String role) {
		try {
			PreferanceUtil.setSharedPreferences(context, PreferanceUtil.userRole, role);
			log.debug("getLocation method called ");
			final LocationBean locationbean = new LocationBean();
			final LocationTracker mGPS = new LocationTracker(context, locationbean);
		    
	    	if(mGPS.getLocation()) {
	    		if(locationbean.getLatitude() == 0.0 && locationbean.getLongitude() == 0.0 ) { 		   
	    			log.debug("method called getGeoLocation else");
			 	    new CountDownTimer(15000, 1000) {
			 	    	public void onTick(long millisUntilFinished) {
			        	  if(locationbean.getLatitude() != 0.0 && locationbean.getLongitude() != 0.0) {
			        		  postLocation("on", locationbean.getLatitude(), locationbean.getLongitude());
			        		  mGPS.stopUsingGPS();
			        		  this.cancel();
			        	  }
			 	    	}
			 	    	public void onFinish() {
			 	    		if(locationbean.getLatitude() != 0.0 && locationbean.getLongitude() != 0.0) {
			 	    			postLocation("on", locationbean.getLatitude(), locationbean.getLongitude());
			  		   		}
			 	    		mGPS.stopUsingGPS();
			 	    	}
			 	    }.start();
			 	    
			   } else {
				    mGPS.stopUsingGPS();
				    if(locationbean.getLatitude() != 0.0 && locationbean.getLongitude() != 0.0) {
				    	postLocation("on", locationbean.getLatitude(), locationbean.getLongitude());
				    }
			   }
		    } else {
		    	postLocation("off", null ,null );
		    }	
		} catch (Exception e) {
			log.debug("exception:"+e);
		}
		return null;
	}
	

	@JavascriptInterface
	public String userTracker(String uniqueID, String role, String userEmail) {
		try {
			log.info("trackUser called | User Unique ID=" + uniqueID + " | UserRole=" + role + " | User Email Id=" + userEmail);
			Intent locationServiceIntent = new Intent(context, TrackerService.class);
			locationServiceIntent.putExtra(PreferanceUtil.userUniqueID,	uniqueID);
			locationServiceIntent.putExtra(PreferanceUtil.userRole, role);
			locationServiceIntent.putExtra(PreferanceUtil.userID, userEmail);
			
			log.info("Uear Tracker callled. Role: " + role);
			// Tracking user only in case of Role - field_exec  
			if (role.equals("field_exec")) {
				LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
				if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == false) {
					LocationTracker.showLocationSettingsAlert(context);
				}
				context.startService(locationServiceIntent);
			} 

		} catch (Exception e) {
			log.info("Exception in userTracker: "+ e);
		} finally {
			// Sending User Details to TentacleCall
			JSONObject jsonObj = new JSONObject();
			try {			
				jsonObj.put("alert_type", "user_update");
				jsonObj.put(PreferanceUtil.userRole, role);
				jsonObj.put(PreferanceUtil.userID, userEmail);
				jsonObj.put("pin", PreferanceUtil.getSharedPreferences(context, PreferanceUtil.PINID, ""));
				new com.sunoray.tentacle.tasks.SendStatus(context).execute(jsonObj.toString());
			} catch (Exception e) {
				log.info("Exception in ACK@userTracker :" + e);
			}	
		}
		return PreferanceUtil.getSharedPreferences(context, PreferanceUtil.PINID, "0");
	}	

	@JavascriptInterface
	public void disconnectCall() {
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		try {
			Method m1 = tm.getClass().getDeclaredMethod("getITelephony");
			m1.setAccessible(true);
			Object iTelephony = m1.invoke(tm);
			Method m3 = iTelephony.getClass().getDeclaredMethod("endCall");
			m3.invoke(iTelephony);
		} catch (Exception e) {
			log.info("Exception @ End call",e);
		}
	}
	
	public void postLocation(final String status, final Double latitude, final Double longitude) {
		try {
			Handler mainHandler = new Handler(context.getMainLooper());
			mainHandler.post(new Runnable() {
				@Override
				public void run() {
					tentacleBrowser.loadUrl("javascript:setGeoLocation('" + status + "'," + latitude + "," + longitude + ")");
				}
			});
		} catch (Exception e) {
			log.info("Exception:" + e);
		}
	}

}