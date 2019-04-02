package com.sunoray.tentacle.network;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sunoray.tentacle.common.PreferenceUtil;
import com.sunoray.tentacle.common.Util;
import com.sunoray.tentacle.db.DatabaseHandler;
import com.sunoray.tentacle.db.Recording;
import com.sunoray.tentacle.tasks.SendCallData;
import com.sunoray.tentacle.tasks.SendCallRecording;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

public class BulkUploadLoop extends AsyncTask<Void, Void, Boolean> {
	
	private Context context = null;
	private static final Logger log =  LoggerFactory.getLogger(BulkUploadLoop.class);
	private Boolean backgroundStatus = true; 
	
	public BulkUploadLoop(Context context) {
		this.context = context;
	}
	
	@Override
	protected Boolean doInBackground(Void... params) {
		Thread.currentThread().setName("BulkUploadLoopAsyncTask");
		return upload();
	}

    @SuppressLint("NewApi")
	public boolean upload() {
    	backgroundStatus = true;   
		try {
			while(backgroundStatus) {
				// checking network status
				if (Util.canUpload(context)) {
					//SendFile sf = new SendFile();
					Boolean sendStatus = false;
					String[] status = { "NEW", "FAIL", "PENDING" };
					List<Recording> listRecordings = new DatabaseHandler(context).getAllRecordingsToUpload(status);
					log.info("Number of entries to push =" + listRecordings.size());
					if (listRecordings.size() > 0) {
						for (Recording recording : listRecordings) {
							if (!(recording.getPath() == null || recording.equals(""))) {
								if (recording.getStatus().equals("PENDING") || recording.getDataSent() == 1) {
									//sendStatus = sf.sendCallRecording(recording, context);
									log.info("call recording sending");
									AsyncTask<Recording, Void, Boolean> aTask = new SendCallRecording(context);
									if(android.os.Build.VERSION.SDK_INT > 10) 
										aTask.executeOnExecutor(THREAD_POOL_EXECUTOR, recording);
									else
										aTask.execute(recording);
									sendStatus = aTask.get();
								} else {
									log.info("call recording detail sending");
									AsyncTask<Recording, Void, Boolean> aTask = new SendCallData(context);
									if(android.os.Build.VERSION.SDK_INT > 10) 
										aTask.executeOnExecutor(THREAD_POOL_EXECUTOR, recording); 
									else
										aTask.execute(recording);
									sendStatus = aTask.get();
									if (sendStatus) {
										log.info("call recording sending");
										AsyncTask<Recording, Void, Boolean> bTask = new SendCallRecording(context);
										if(android.os.Build.VERSION.SDK_INT > 10) 
											bTask.executeOnExecutor(THREAD_POOL_EXECUTOR, recording);
										else 
											bTask.execute(recording);
										sendStatus = bTask.get();
									} 
								}
							}
							if (!sendStatus) backgroundStatus = false; //Some problem in sending data so stopping upload service 
						} // End for loop
					} else {
						log.info("Thread is stopping because no data found for upload");
						backgroundStatus = false;
					}
				} else {
					backgroundStatus = false;
				}
				// log.info(LOG_TAG, "Going to sleep for 30 minutes");
				// Thread.sleep(1 * 1000 * 60 * 10);
				// log.info(LOG_TAG, "Just woke up after 30 minutes");
			}
		} catch (Exception e) {
			log.info("Thread interrupted - some error ",e);
			backgroundStatus = false;
		} finally {
			PreferenceUtil.setSharedPreferences(context, PreferenceUtil.isServiceStarted,"false");
		}
    	return false; // to stop background service
    }
    
    @Override
    protected void onPostExecute(Boolean result) {
    	log.debug("Seting service status to false and Stoping BackgroundService");
    	PreferenceUtil.setSharedPreferences(context, PreferenceUtil.isServiceStarted,"false");
    	context.stopService(new Intent(context, com.sunoray.tentacle.service.BackGroundService.class));
    	super.onPostExecute(result);
    }

}