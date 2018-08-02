package com.sunoray.tentacle.tasks;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.StrictMode;

import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.PreferenceUtil;
import com.sunoray.tentacle.common.Util;
import com.sunoray.tentacle.db.DatabaseHandler;
import com.sunoray.tentacle.db.Recording;

public class SendCallData extends AsyncTask<Recording, Void, Boolean> {

	private static final Logger log = LoggerFactory.getLogger(SendCallData.class);
	private Recording recording;
	private Context context;
	
	public SendCallData(Context context) {
		this.context = context;
	}

	@Override
	protected Boolean doInBackground(Recording... params) {
		try {
			Thread.currentThread().setName("SendCallDataAsyncTask");
			if (params.length > 0) {
				this.recording = params[0];
				return sendCallData();
			}
		} catch (Exception e) {
			log.debug("Exception in SendCallData: " ,e);
		} 
		return false;
	}
	
	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		Util.send2tentacleReceiver(context, this.getClass().getName(), "REFRESH", "TABLE");
	}
	
	@SuppressLint("NewApi")
	public boolean sendCallData() {
		
		log.debug("sending call data to server by " + Thread.currentThread().getName());
		
		HttpURLConnection conn = null;
		DatabaseHandler db = new DatabaseHandler(context);
		DataOutputStream dos = null;
		try {
			if (android.os.Build.VERSION.SDK_INT > 9) {
				StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
				StrictMode.setThreadPolicy(policy);
			}
			
			conn = Util.getConnection(AppProperties.MEDIA_SERVER_URL + AppProperties.SERVER_NAME_STRING + AppProperties.FILE_RECEIVER_STRING);
			conn.setRequestProperty("Content-Type", "text/html");
			conn.setRequestProperty("req_type", "Recoding_detail");
			conn.setRequestProperty("token", PreferenceUtil.getSharedPreferences(context, PreferenceUtil.AUTHTOKEN, ""));
			conn.setRequestProperty("phone_number", recording.getPhoneNumber());
			conn.setRequestProperty("callid", recording.getCallId());
			conn.setRequestProperty("duration", recording.getDuration() + "");
			conn.setRequestProperty("audio_source", recording.getAudioSrc());
			conn.setRequestProperty("server_type", recording.getServerType());
			conn.setRequestProperty("pin", PreferenceUtil.getSharedPreferences(context, PreferenceUtil.PINID, ""));
			conn.setRequestProperty("appversion", String.valueOf(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode));
			conn.setRequestProperty("status", recording.getDuration() == 0 ? "NOANSWER" : "ANSWER");

			dos = new DataOutputStream(conn.getOutputStream());
			// sending.. Recording detail and set buffer size base on content type
			int bufferSize = Math.min(1024, 1024);
			byte[] buffer = new byte[bufferSize];

			dos.write(buffer, 0, bufferSize);

			int resCode = conn.getResponseCode();
			String resMsg = conn.getResponseMessage();
			// Responses from the server (code and message)
			log.debug("sendcall response : " + resMsg + " | code: " + resCode);
			if (resCode == 200) {
				recording.setDataSent(1);
				recording.setStatus("PENDING");
				db.updateRecStatusNNoOfTries(recording);
				return true;
			} else {
				recording.setDataSent(0);
				recording.setStatus("FAIL");
				db.updateRecStatusNNoOfTries(recording);
				return false;
			}
		} catch (java.net.ConnectException e) {
			log.debug("Error - Server is not reachable");
			recording.setDataSent(0);
			recording.setStatus("FAIL");
			db.updateRecStatusNNoOfTries(recording);
			return false;
		} catch (Exception e) {
			log.debug("Exception Occured: ",e);
			recording.setDataSent(0);
			recording.setStatus("FAIL");
			db.updateRecStatusNNoOfTries(recording);
			return false;
		} finally {
			conn.disconnect();
			try {
				if(dos!=null)
				dos.flush();
			} catch (IOException e) {
				log.debug("Exception : " +e);
			}
			try {
				if(dos!=null)
				dos.close();
			} catch (IOException e) {
				log.debug("Exception : " +e);
			}
			db.close();
		}

	}

}
