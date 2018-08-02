
package com.sunoray.tentacle.tasks;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.AsyncTask;

import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.PreferenceUtil;
import com.sunoray.tentacle.network.HttpServices;

import okhttp3.Response;

@SuppressWarnings("deprecation")
public class SendCurrentLocation extends AsyncTask<String, Void, Void> {
	private static final Logger log = LoggerFactory.getLogger(SendCurrentLocation.class);
	Context context;

	public SendCurrentLocation(Context context) {
		this.context = context;
	}

	protected Void doInBackground(String... params) {
		try {
			Thread.currentThread().setName("SendCurrentLocationAsyncTask");
			log.info("Sending Current Location in background...");
			String json = "";
			if (params.length > 0) {
				json = params[0];
			}

			try {
				Map<String, String> nameValuePairs = new HashMap<>();
				nameValuePairs.put("token", PreferenceUtil.getSharedPreferences(context, PreferenceUtil.AUTHTOKEN, ""));
				nameValuePairs.put("json", json);
				nameValuePairs.put("pin", PreferenceUtil.getSharedPreferences(context, PreferenceUtil.PINID, ""));
				nameValuePairs.put("appversion", String.valueOf(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode));

				HttpServices httpClient = new HttpServices();
				Response response = httpClient.post(AppProperties.MEDIA_SERVER_URL
						+ AppProperties.SERVER_NAME_STRING
						+ AppProperties.DEVICE_CALLHIT_STRING,nameValuePairs);
				// add code to remove data from database
				log.info("Execute HTTP Post Request");
				// log.info("Location Sync respose Code=" + response.getStatusLine().getStatusCode());
				log.info("Location Sync respose Code=" + response.code());

			} catch (Exception e) {
				log.debug("Sendcall Status to tentacle E=", e);
			}

		} catch (Exception e) {
			log.debug("Exception: ", e);
		}
		return null;
	}

}