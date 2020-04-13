package com.sunoray.tentacle.helper;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sunoray.tentacle.common.AppProperties;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;

public class CallHelper {
	
	private static final Logger log = LoggerFactory.getLogger(CallHelper.class);

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static boolean isCallInProgress(Context context) {
		try {
			if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
				TelecomManager tm = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
				if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
					// TODO: Consider calling
					//    ActivityCompat#requestPermissions
					// here to request the missing permissions, and then overriding
					//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
					//                                          int[] grantResults)
					// to handle the case where the user grants the permission. See the documentation
					// for ActivityCompat#requestPermissions for more details.
				}
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
