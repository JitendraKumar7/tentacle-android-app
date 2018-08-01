package com.sunoray.tentacle.tasks;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.CallLog;

import com.sunoray.tentacle.common.Util;
import com.sunoray.tentacle.db.DatabaseHandler;
import com.sunoray.tentacle.db.Recording;

public class AddRecording extends AsyncTask<Recording, Void, String> {

	private static final Logger log = LoggerFactory.getLogger(AddRecording.class);	
	private Context context = null;	
				
	public AddRecording(Context context) {
		this.context = context;
	}	
	
	@Override
	protected String doInBackground(Recording... params) {
		try {
			Thread.currentThread().setName("AdRecrdgAsyn");
			if (params.length > 0) {
				Recording rec = params[0];
				
				// Adding Record in db.
				addRecordToDB(rec);
								
				// Delete OLD Records
				removeOldRecords("100");
			}
		} catch (Exception e) {
			log.debug("Exception: " ,e);
		} finally {
			//context.stopService(new Intent(context, com.sunoray.tentacle.service.CallService.class));
		}
		return "done";
	}

	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		context.stopService(new Intent(context, com.sunoray.tentacle.service.CallService.class));
	}
	
	private Recording addRecordToDB(Recording rec) {
		try {
			log.info(rec.getCallId() + " | adding Record To DB " + rec.getPhoneNumber());
			rec.setStatus("NEW");
			rec.setNumberOfTries(0);
			rec.setDataSent(0);			
			
			HashMap<String, String> phoneCallLogDetails = getLastOutCall(rec.getPhoneNumber());
			if (phoneCallLogDetails != null) {
				log.debug(rec.getCallId() +" | Number " + phoneCallLogDetails.get(CallLog.Calls.NUMBER) + " | Type " + phoneCallLogDetails.get(CallLog.Calls.TYPE) + " | Duration " + phoneCallLogDetails.get(CallLog.Calls.DURATION));
				if (phoneCallLogDetails.get(CallLog.Calls.DURATION) == null	|| phoneCallLogDetails.get(CallLog.Calls.DURATION).equals(""))
					rec.setDuration(0);
				else
					rec.setDuration(Integer.parseInt(phoneCallLogDetails.get(CallLog.Calls.DURATION)));
				
				// Removing Number for Phone Log If Its there
				if(Util.makeNumberHiding(rec.getHideNumber()))
					removeCallBaseOnId(phoneCallLogDetails.get(CallLog.Calls._ID));
			} else {
				rec.setDuration(0);
			}
			
			if (rec.getDialTime() > 0) {
				rec.setDialTime(System.currentTimeMillis() - rec.getDialTime());
			} else {
				log.info("ERROR - Dial Time is : "+rec.getDialTime());
				rec.setDialTime(0);
			}

			// Send call duration to View activity. 			
			new BroadcastCallDuration(context, rec.getDuration(), rec.getDialTime()).start();

			DatabaseHandler dh = new DatabaseHandler(context);
			if (dh.checkRecording(rec.getCallId()))
				dh.updateRecording(rec);
			else
				dh.addRecording(rec);

		} catch (Exception e) {
			log.debug(rec.getCallId() + " | Exception here : ", e);
		}
		return rec;
	}
	
	private void removeCallBaseOnId(String callId) {		
		 context.getContentResolver().delete(
                 CallLog.Calls.CONTENT_URI,
                 CallLog.Calls._ID + "= ? ",
                 new String[] { String.valueOf(callId) });		
	}

	private HashMap<String, String> getLastOutCall(String number) {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			log.debug("getLastOutCall", "Exception: ", e);
		}
		
		try {
			HashMap<String, String> callData = new HashMap<String, String>();
			String strSelection = CallLog.Calls.NUMBER + " LIKE '%" + number + "'";
			String strOrder = CallLog.Calls.DATE + " DESC LIMIT 1";
			Cursor mCallCursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, null , strSelection, null, strOrder);
			
			if (mCallCursor.moveToFirst()) {
				callData.put(CallLog.Calls.TYPE, mCallCursor.getString(mCallCursor.getColumnIndex(CallLog.Calls.TYPE)));
				callData.put(CallLog.Calls.NUMBER, mCallCursor.getString(mCallCursor.getColumnIndex(CallLog.Calls.NUMBER) ));				
				callData.put(CallLog.Calls.DURATION,mCallCursor.getString(mCallCursor.getColumnIndex(CallLog.Calls.DURATION)));
				callData.put(CallLog.Calls._ID, String.valueOf( mCallCursor.getInt(mCallCursor.getColumnIndex(CallLog.Calls._ID))));
				mCallCursor.close();
				return callData;
			}			
		} catch (Exception e) {
			log.debug("getLastOutCall", "Exception here : ", e);
		}
		return null;
	}
	
	private void removeOldRecords(String recordAfter) {
		try {
			DatabaseHandler dh = new DatabaseHandler(context);
			dh.deleteRecordings(recordAfter);
		} catch (Exception e) {
			log.debug("some error: "+e);
		}
	}
}