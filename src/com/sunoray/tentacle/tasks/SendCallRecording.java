package com.sunoray.tentacle.tasks;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.AsyncTask;

import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.PreferanceUtil;
import com.sunoray.tentacle.common.Util;
import com.sunoray.tentacle.db.DatabaseHandler;
import com.sunoray.tentacle.db.Recording;

public class SendCallRecording extends AsyncTask<Recording, Void, Boolean> {

	private static final Logger log = LoggerFactory.getLogger(SendCallRecording.class);
	private Recording recording;
	private Context context;
	
	public SendCallRecording(Context context) {
		this.context = context;
	}
	
	@Override
	protected Boolean doInBackground(Recording... params) {
		try {
			Thread.currentThread().setName("SendCalRecrding");
			if (params.length > 0) {
				this.recording = params[0];
				return sendCallRecording();
			}
		} catch (Exception e) {
			log.debug("Exception: " ,e);
		} 
		return false;
	}
	
	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		Util.send2tentacleReceiver(context, this.getClass().getName(), "REFRESH", "TABLE");
	}
	
	public boolean sendCallRecording() {
		
		log.debug("sending call recording to server by "+ Thread.currentThread().getName());
		
		String fileName = recording.getPath();
		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int resCode = 0;
		String resMsg = "";
		DatabaseHandler db = new DatabaseHandler(context);
		File sourceFile = new File(fileName);
		int maxBufferSize = (int) sourceFile.length(); // Max buffer Size is equal to Source file size. 

		HttpURLConnection conn = null;
		DataOutputStream dos = null;
		FileInputStream fileInputStream = null;
		try {
			conn = Util.getConnection(AppProperties.MEDIA_SERVER_URL + AppProperties.SERVER_NAME_STRING + AppProperties.FILE_RECEIVER_STRING);
			recording.setNumberOfTries(recording.getNumberOfTries() + 1);

			if (recording.getNumberOfTries() > 99) {
				recording.setStatus("EXPIRE");
				db.updateRecStatusNNoOfTries(recording);
				return true;
			} else if(sourceFile.isFile()) {
				if (sourceFile.length() > 1) {
					conn.setRequestProperty("Content-Type", "audio/3gpp");
					conn.setRequestProperty("uploaded_file", fileName.substring(fileName.lastIndexOf("/") + 1));
					conn.setRequestProperty("callid", recording.getCallId());
					conn.setRequestProperty("token", PreferanceUtil.getSharedPreferences(context, PreferanceUtil.AUTHTOKEN, ""));
					conn.setRequestProperty("pin", PreferanceUtil.getSharedPreferences(context, PreferanceUtil.PINID, ""));
					conn.setRequestProperty("appversion", String.valueOf(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode));
					conn.setRequestProperty("req_type", "Recoding_file");
					conn.setRequestProperty("duration", recording.getDuration()	+ "");
					conn.setRequestProperty("server_type", recording.getServerType());
					conn.setRequestProperty("status", recording.getDuration() == 0 ? "NOANSWER" : "ANSWER");
					//log.info("account_id"+ recording.getAccountId() + "campaign_id"+ recording.getCampaignId() +"prospect_id"+ recording.getProspectId());
					conn.setRequestProperty("account_id", recording.getAccountId());
					conn.setRequestProperty("campaign_id", recording.getCampaignId());
					conn.setRequestProperty("prospect_id", recording.getProspectId());
					fileInputStream = new FileInputStream(sourceFile);
	
					bytesAvailable = fileInputStream.available();
					bufferSize = Math.min(bytesAvailable, maxBufferSize);
					buffer = new byte[bufferSize];
					// read file and write it into form...
					conn.setRequestProperty("Content-Length", bufferSize + "");
					bytesRead = fileInputStream.read(buffer, 0, bufferSize);
					dos = new DataOutputStream(conn.getOutputStream());
	
					while (bytesRead > 0) {
						dos.write(buffer, 0, bufferSize);
						bytesAvailable = fileInputStream.available();
						bufferSize = Math.min(bytesAvailable, maxBufferSize);
						bytesRead = fileInputStream.read(buffer, 0, bufferSize);
					}
					
					// Responses from the server (code and message)
					resCode = conn.getResponseCode();
					resMsg = conn.getResponseMessage();
					log.debug("sendcallRecording response: " + resMsg + " | code: " + resCode);
					
					if (resCode == 200) {
						recording.setStatus("SENT");
						db.updateRecStatusNNoOfTries(recording);
						log.debug("recoding file deleting from the device");
						sourceFile.delete();									// delete file
						return true;
					} else {
						log.debug("fail to sent recording");
						recording.setStatus("PENDING");
						db.updateRecStatusNNoOfTries(recording);
						return false;
					}
				} else {
					log.debug("recording file size is less so deleting file");
					sourceFile.delete();										// delete file
					recording.setStatus("NO REC");
					db.updateRecStatusNNoOfTries(recording);
					return true;
				}
			} else {
				log.debug("recording file not exist");
				recording.setStatus("NO REC");
				db.updateRecStatusNNoOfTries(recording);
				return true;	
			}
		} catch (java.net.ConnectException e) {
			log.debug("Error - Server is not reachable");
			recording.setStatus("PENDING");
			db.updateRecStatusNNoOfTries(recording);
			return false;
		} catch (Exception e) {
			log.debug("Exception in sendcallRecording : " ,e);
			recording.setStatus("PENDING");
			db.updateRecStatusNNoOfTries(recording);
			return false;
		} finally {
			conn.disconnect();
			try {
				if (dos != null)
					dos.flush();
			} catch (IOException e) {
				log.debug("Exception : " + e.getMessage(), e);
			}
			try {
				if (dos != null)
					dos.close();
			} catch (IOException e) {
				log.debug("Exception : " + e.getMessage(), e);
			}
			if (fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
					log.debug("Exception : " + e.getMessage(), e);
				}
			}
			db.close();
		}
	}
}