package com.sunoray.tentacle.helper;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sunoray.tentacle.common.AppProperties;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;

public class CallHelper {
	
	private static final Logger log = LoggerFactory.getLogger(CallHelper.class);

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static boolean isCallInProgress(Context context) {
		try {
			if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
				TelecomManager tm = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
				return tm.isInCall();
			} else  {
				return AppProperties.isCallServiceRunning;
			} 
		} catch (Exception e) {
			log.debug("Exception: ", e);
			return false;
		}
	}
	
	public static void endCall(Context context) {
		try {
			if(isCallInProgress(context)) {
				TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
				Method m1 = tm.getClass().getDeclaredMethod("getITelephony");
				m1.setAccessible(true);
				Object iTelephony = m1.invoke(tm);
				Method m2 = iTelephony.getClass().getDeclaredMethod("endCall");
				m2.invoke(iTelephony);
				log.info("Call Disconnected");	
			}
		} catch (Exception e) {
			log.info("Exception @ End call",e);
		}
	}

}
