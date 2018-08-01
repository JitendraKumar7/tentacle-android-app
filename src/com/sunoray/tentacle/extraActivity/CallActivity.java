package com.sunoray.tentacle.extraActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sunoray.tentacle.R;
import com.sunoray.tentacle.db.DatabaseHandler;
import com.sunoray.tentacle.db.Recording;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.widget.Toast;

import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.CommonField;
import com.sunoray.tentacle.common.Util;

public class CallActivity extends Activity {
	
	static private final Logger log =  LoggerFactory.getLogger(CallActivity.class);
	private Recording rec = null; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log.info("Call Activity Created");

		try {
			Bundle extraParams = this.getIntent().getExtras();
			if (extraParams != null) {
				
				rec = (Recording) extraParams.getSerializable(CommonField.RECORDING);
				rec.setPhoneNumber(Util.checkPhoneNumber(rec.getPhoneNumber()));
				
				log.info("Call activity started for callid: " + rec.getCallId());
				
				if(new DatabaseHandler(this).checkRecording(rec.getCallId())) {
					// --------- IF CALLID is already in DB
					this.setTheme(R.style.AppTheme);
					AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
					alertDialogBuilder.setTitle("Tentacle");
					// set dialog message
					alertDialogBuilder
						.setMessage("There is unsaved call recording with this number." +
								" If you call again you will lose your recording." +
								"\n\nAre you sure you want to call again?")
						.setCancelable(false)
						.setPositiveButton("Yes",new DialogInterface.OnClickListener(){
							public void onClick(DialogInterface dialog,int id) {
								log.info("Select Recalling");
								callNumber();
							}
						})
						.setNegativeButton("No",new DialogInterface.OnClickListener(){
							public void onClick(DialogInterface dialog,int id) {
								log.info("Rejected Recalling.");
								dialog.cancel();
								CallActivity.this.finish();
							}
						});
					// create alert dialog
					AlertDialog alertDialog = alertDialogBuilder.create();
					alertDialog.show();
				} else {
					callNumber();
				}
			} else {
				log.debug("Paramater Missing");
			}			
		} catch (Exception e) {
			log.debug("Error Occured: ",e);
		}
	}
	
	protected void callNumber() {
		try {
			if (!isCallInProgress()) {

				// Setting Starting Dial Time in rec 
				rec.setDialTime(System.currentTimeMillis());
				
				// Starting Call Listener Service
				Intent callService = new Intent(this, com.sunoray.tentacle.service.CallService.class);
				callService.putExtra(CommonField.RECORDING, rec);
				startService(callService);
				
				// Making Call
				Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + rec.getPhoneNumber()));
				callIntent.setData(Uri.parse("tel:" + rec.getPhoneNumber()));
				startActivity(callIntent);
				//startActivityForResult(callIntent, 20);
				
			} else {
				log.debug("ERROR: got new call request. This call can't be processed as last call not completed yet");
				Toast.makeText(getApplicationContext(), "Call is already in progress", Toast.LENGTH_SHORT).show();	
			}
		} catch (Exception e) {
			log.debug("Exception: ", e);
		}
		finish();
	}
	
	@SuppressLint("NewApi")
	private boolean isCallInProgress() {
		try {
			if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
				TelecomManager tm = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
				return tm.isInCall();
			} else  {
				return AppProperties.isCallServiceRunning;
			} 
		} catch (Exception e) {
			log.debug("Exception: ", e);
		}
		return false;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		log.debug("closing call activity");
		finish();
	}
	
}