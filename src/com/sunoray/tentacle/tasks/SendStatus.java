package com.sunoray.tentacle.tasks;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.AsyncTask;

import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.PreferanceUtil;

@SuppressWarnings("deprecation")
public class SendStatus extends AsyncTask<String, Void, String> {
	private static final Logger log = LoggerFactory.getLogger(SendStatus.class);
	Context context;
	
	public SendStatus(Context context) {
		this.context=context;
	}

	@Override
	protected String doInBackground(String... params) {
		try {
			Thread.currentThread().setName("SendStatusAsyncTask");
			if (params.length > 0) {				
				String json = params[0];
								
				log.info("Sending call Status in background...");
				HttpClient httpclient = new DefaultHttpClient();
				httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT,System.getProperty("http.agent"));
				HttpPost httppost = new HttpPost(AppProperties.MEDIA_SERVER_URL	+ AppProperties.SERVER_NAME_STRING + AppProperties.DEVICE_CALLHIT_STRING);

				try {
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
					nameValuePairs.add(new BasicNameValuePair("token", PreferanceUtil.getSharedPreferences(context, PreferanceUtil.AUTHTOKEN, "")));					
					nameValuePairs.add(new BasicNameValuePair("json",json));
					nameValuePairs.add(new BasicNameValuePair("pin", PreferanceUtil.getSharedPreferences(context,PreferanceUtil.PINID,"")));
					nameValuePairs.add(new BasicNameValuePair("appversion", String.valueOf(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode)));
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

					log.info("Execute HTTP Post Request");
					HttpResponse response = httpclient.execute(httppost);
					log.info("HTTP Post Request respose=" + response.getStatusLine().getStatusCode());
				} catch (Exception e) {
					log.debug("Sendcall Status to tentacle E=", e);
				}
			}
		} catch (Exception e) {
			log.debug("Exception: " ,e);
		} 
		return "done";
	}
}