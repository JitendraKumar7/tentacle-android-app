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

public class SendStatus extends AsyncTask<String, Void, String> {
	private static final Logger log = LoggerFactory.getLogger(SendStatus.class);
	Context context;

	public SendStatus(Context context) {
		this.context = context;
	}

	@SuppressWarnings("deprecation")
	@Override
	protected String doInBackground(String... params) {
		try {
			Thread.currentThread().setName("SendStatusAsyncTask");
			if (params.length > 0) {
				String json = params[0];

				log.info("Sending call Status in background...");

				try {
					Map<String, String> nameValuePairs = new HashMap<>();
					nameValuePairs.put("token", PreferenceUtil.getSharedPreferences(context, PreferenceUtil.AUTHTOKEN, ""));
					nameValuePairs.put("json", json);
					nameValuePairs.put("pin", PreferenceUtil.getSharedPreferences(context, PreferenceUtil.PINID, ""));
					nameValuePairs.put("appversion", String.valueOf(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode));
					log.info("json = " + json);

					log.info("Execute HTTP Post Request");
					HttpServices httpclient = new HttpServices();
					Response response = httpclient.post(AppProperties.MEDIA_SERVER_URL
							+ AppProperties.SERVER_NAME_STRING
							+ AppProperties.DEVICE_CALLHIT_STRING, nameValuePairs);
//                    HttpResponse response = httpclient.execute(httppost);
					log.info("HTTP Post Request respose=" + response.message());
//                    log.info("HTTP Post Request respose=" + response.getStatusLine().getStatusCode());
				} catch (Exception e) {
					log.debug("Sendcall Status to tentacle E=", e);
				}
			}
		} catch (Exception e) {
			log.debug("Exception: ", e);
		}
		return "done";
	}
}