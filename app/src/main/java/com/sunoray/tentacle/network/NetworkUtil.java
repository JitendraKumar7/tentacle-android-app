package com.sunoray.tentacle.network;

import com.sunoray.tentacle.common.PreferenceUtil;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
 
public class NetworkUtil {
     
    public static int TYPE_WIFI = 1;
    public static int TYPE_MOBILE = 2;
    public static int TYPE_NOT_CONNECTED = 0;     
     
    public static int getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if(activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                return TYPE_WIFI; // 1
            if(activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                return TYPE_MOBILE;// 2
        } 
        return TYPE_NOT_CONNECTED;// 0
    }
    
    public static int getUserSyncPreference(Context context) {
    	return Integer.parseInt(PreferenceUtil.getSharedPreferences(context, PreferenceUtil.SyncOption, "0"));    // For '0' Preference "Send AnyTime"
    }
    
    public static boolean isNetworkAvailable(Context context) {
    	try {
    		ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
    		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
		} catch (Exception e) {
			return false;
		}
	}
    
    public static boolean isOnline() {
    	try {
    		String command = "ping -c 1 google.com";
            return (Runtime.getRuntime().exec (command).waitFor() == 0);	
		} catch (Exception e) {
			return false;
		}
    }
    
}