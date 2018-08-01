package com.sunoray.tentacle.tasks;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Response;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.CommonField;
import com.sunoray.tentacle.common.PreferanceUtil;
import com.sunoray.tentacle.common.Util;
import com.sunoray.tentacle.db.Recording;
import com.sunoray.tentacle.network.HttpServices;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.StrictMode;

public class GetPendingCall extends AsyncTask<Void, Void, String> {
	
	private static final Logger log = LoggerFactory.getLogger(GetPendingCall.class);
	private Context context;
	private ProgressDialog loading;
	
	public GetPendingCall(Context context) {
		this.context = context;
	}

	@Override
	protected void onPreExecute() {
		loading = new ProgressDialog(context);
		loading.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		loading.setMessage("Loading. Please wait...");
        loading.setIndeterminate(true);
        loading.setCanceledOnTouchOutside(false);
        loading.show();
	}
	
	@Override
	protected String doInBackground(Void... arg0) {
		try {
			Thread.currentThread().setName(GetPendingCall.class.getName());
			if (!PreferanceUtil.getSharedPreferences(context, PreferanceUtil.PINID, "").isEmpty()) {
				log.info("Geting Pending Call in Background");
				if (android.os.Build.VERSION.SDK_INT > 9) {
					StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
					StrictMode.setThreadPolicy(policy);
				}
				
				// Building Post Parameter
                Map<String, String> keyValuePairs = new HashMap<>();
                keyValuePairs.put("token", PreferanceUtil.getSharedPreferences(context, PreferanceUtil.AUTHTOKEN, ""));
                keyValuePairs.put("pin", PreferanceUtil.getSharedPreferences(context, PreferanceUtil.PINID, ""));
                keyValuePairs.put("appversion", String.valueOf(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode));
                
                // Making HTTP Request
				HttpServices httpService = new HttpServices();
				Response response = httpService.post(AppProperties.MEDIA_SERVER_URL	+ AppProperties.SERVER_NAME_STRING + AppProperties.SYNC_ALERT_STRING, keyValuePairs);
				
				// Returning Response
				if (response.isSuccessful()) {
					return response.body().string();
				} else {
					return "Connection Timeout";
				}
			}
		} catch (UnknownHostException e) {
			return "Unable to make a connection";
		} catch (IOException e) {
			return "No Internet access";
		} catch (Exception e) {
			log.info("Exception in GetPendingCall: ",e);
			return "";
		} 
		return "";
	}

	@Override
	protected void onPostExecute(String result) {
		try {
			
			// Removing Alert 
			if (loading != null && loading.isShowing()) {
	            loading.dismiss();
	        }
			log.info("Get Pending Call Response: "+result);
			if (Util.isJSON(result)) {
				JSONObject jObject = new JSONObject(result);
				Recording rec = new Recording();
				rec.setCallId(jObject.getString("callid"));
				rec.setPhoneNumber(jObject.getString("phone_number"));
				String numberFlag = jObject.getString("hide_number");							
				if (numberFlag != null && numberFlag.equalsIgnoreCase("true")) {
					rec.setHideNumber("XXXXXXXXXX");
				} else {
					rec.setHideNumber(rec.getPhoneNumber());
				}							
				
			    log.info("Pending Call found. Call ID: "+rec.getCallId() + " | Phone Number =" +rec.getPhoneNumber());
			    
				// Sending Delivery ACK to Server in background
				JSONObject jsonObj = new JSONObject();	
				try {
					jsonObj.put("alert_type", "call");
					jsonObj.put("call_no", rec.getPhoneNumber());
					jsonObj.put("call_id", rec.getCallId());
					jsonObj.put("pin", PreferanceUtil.getSharedPreferences(context, PreferanceUtil.PINID,""));
					jsonObj.put("appversion",String.valueOf(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode));
				} catch (Exception e) {
					log.info("Eception:" + e);
				}	
				new SendStatus(context).execute(jsonObj.toString());							
				
				Intent callIntent = new Intent(context, com.sunoray.tentacle.extraActivity.GCMActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				callIntent.putExtra(CommonField.RECORDING, rec);
				context.startActivity(callIntent);
			} else {
				if (Util.isNull(result)) {
					result = "No pending call found.";
				}
				new AlertDialog.Builder(context).setTitle("Pending Call")
				.setMessage(result)
				.setCancelable(true)
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				}).create().show();
			}
		} catch (Exception e) {
			log.info("Error: ",e);
			// Removing Alert 
			if (loading != null && loading.isShowing()) {
	            loading.dismiss();
	        }
		}
	}
	
}