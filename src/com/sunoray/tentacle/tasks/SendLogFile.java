package com.sunoray.tentacle.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sunoray.tentacle.R;
import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.PreferanceUtil;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public class SendLogFile extends AsyncTask<String, Void, Boolean> {
			
	private static final Logger log = LoggerFactory.getLogger(SendLogFile.class);
	private Context context;
	private TextView txt;

	public SendLogFile(Context context, TextView txt) {
		this.context = context;
		this.txt = txt;
	}

	@Override
	protected Boolean doInBackground(String... params) {

		boolean taskStatus = false;
		HttpClient httpclient = new DefaultHttpClient();
		try {
			Thread.currentThread().setName("SendLogFileAsyncTask");
			log.info("Sending log to tentacle server...");
			logRunningApps();
			PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			log.info("App Version: " + pInfo.versionName + " | Version Code: " + pInfo.versionCode);
			log.info("agent: " + System.getProperty("http.agent"));
			
			JSONObject jobj = new JSONObject();
			jobj.put("pin", PreferanceUtil.getSharedPreferences(context, PreferanceUtil.PINID, ""));
			jobj.put("alert_type", "log");

			File file = new File(Environment.getExternalStorageDirectory(),	AppProperties.DEVICE_LOGFILE_PATH);	

			
			httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, System.getProperty("http.agent"));
			HttpPost httppost = new HttpPost(AppProperties.MEDIA_SERVER_URL + AppProperties.SERVER_NAME_STRING + AppProperties.DEVICE_LOG + "?pin=" + PreferanceUtil.getSharedPreferences(context, PreferanceUtil.PINID, "")
							+ "&token=" + PreferanceUtil.getSharedPreferences(context, PreferanceUtil.AUTHTOKEN, "") + "&appversion=" + String.valueOf(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode)
							+ "&json=" + URLEncoder.encode(jobj.toString(), "UTF-8"));
			try {
				if (file.isFile()) {
					log.info("log file available");
					InputStreamEntity reqEntity = new InputStreamEntity(new FileInputStream(file), -1);
					reqEntity.setContentType("text/plain");
					reqEntity.setChunked(true); 	// Send in multiple parts if needed
					httppost.setEntity(reqEntity);	// Execute HTTP Post Request
					httpclient.execute(httppost);	
					// Get Response
					/*
					 * final HttpEntity entity = response.getEntity(); String
					 * tempResp=EntityUtils.toString(entity).trim();
					 */
					log.info("log file send successfully");
					taskStatus = true;
				} else {
					log.info("log file not found");
					taskStatus = false;
				}
			} catch (IOException e) {
				taskStatus = false;
				log.debug("Main Activity Error IOE=", e);
			}
		} catch (Exception e) {
			log.debug("Exceptiopn:", e);
			taskStatus = false;
		}
		return taskStatus;
	}
		
	@Override
	protected void onPostExecute(Boolean taskStatus) {
		if (taskStatus) {
			txt.setTextColor(context.getResources().getColor(R.color.tentacle_green));
			txt.setText(txt.getText() + " - Success");
		} else {
			txt.setTextColor(context.getResources().getColor(R.color.orange));
			txt.setText(txt.getText() + " - Warning");
		}
	}

	private void logRunningApps() {
		try {
			log.debug("List of Apps Running in Background");
			List<String> runningAppList = new ArrayList<>(new HashSet<String>(getRunningApps()));		    
			for (int i = 0; i < runningAppList.size(); i++) {
				if (!runningAppList.get(i).contains("com.android") && !runningAppList.get(i).contains("com.google")) {
					log.info( i + " Package Name: " + runningAppList.get(i));
				}
			}
		} catch (Exception e) {
			log.debug("Exception: "+e);
		}
		return;
	}
	
	private List<String> getRunningApps() {
		List<String> appList = new ArrayList<String>();
		try {
	        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
	        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);
	        for (int i = 0; i < services.size(); i++) {
	        	appList.add(services.get(i).service.getPackageName());
	        }	
		} catch (Exception e) {
			log.debug("Exception: "+e);
		}
        return appList;
    }
	
}