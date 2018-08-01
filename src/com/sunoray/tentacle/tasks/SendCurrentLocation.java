
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

import com.sunoray.tentacle.common.PreferanceUtil;

@SuppressWarnings("deprecation")
public class SendCurrentLocation extends AsyncTask<String, Void, Void> {
	private static final Logger log = LoggerFactory.getLogger(SendCurrentLocation.class);
	Context context;
	
	public SendCurrentLocation(Context context) {
		this.context=context;
	}

	protected Void doInBackground(String... params) {
		try {
			Thread.currentThread().setName("SendCurrentLocationAsyncTask");							
				log.info("Sending Current Location in background...");				
				String json="";
				if (params.length > 0) {
					json = params[0];
					
				}
				HttpClient httpclient = new DefaultHttpClient();
				httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT,System.getProperty("http.agent"));
				//HttpPost httppost = new HttpPost(AppProperties.SERVER_IP_STRING	+ AppProperties.SERVER_NAME_STRING + AppProperties.DEVICE_CALLHIT_STRING);
				HttpPost httppost = new HttpPost("http://192.168.0.13:8080/LocationHistory/CurrentLocation");
				try {
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
					nameValuePairs.add(new BasicNameValuePair("token", PreferanceUtil.getSharedPreferences(context, PreferanceUtil.AUTHTOKEN, "")));					
					nameValuePairs.add(new BasicNameValuePair("json",json));
					nameValuePairs.add(new BasicNameValuePair("pin", PreferanceUtil.getSharedPreferences(context,PreferanceUtil.PINID,"")));
					nameValuePairs.add(new BasicNameValuePair("appversion", String.valueOf(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode)));
					//log.info("json = " + gson.toJson(Locationlist));
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
					// add code to remove data from database
					log.info("Execute HTTP Post Request");
					HttpResponse response = httpclient.execute(httppost);
					log.info("Location Sync respose Code=" + response.getStatusLine().getStatusCode());					
					
				} catch (Exception e) {
					log.debug("Sendcall Status to tentacle E=", e);
				}
					
		} catch (Exception e) {
			log.debug("Exception: " ,e);
		} 
		return null;
	}

	
}