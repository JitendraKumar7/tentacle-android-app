package com.sunoray.tentacle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;
import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.PreferanceUtil;
import com.sunoray.tentacle.common.Util;
import com.sunoray.tentacle.PingTestFragment;
import com.sunoray.tentacle.helper.LocationTracker;
import com.sunoray.tentacle.helper.StorageHandler;
import com.sunoray.tentacle.service.TrackerService;
import com.sunoray.tentacle.R;

@SuppressWarnings("deprecation")
public class StartupActivity extends Activity {
	
	private static final Logger log = LoggerFactory.getLogger(StartupActivity.class);
	TextView alert;
	ProgressBar pb;
	Button btnLogin;
	Button btnViewCall;
	TextView btnSignUp;
	Button retry;
	MenuItem optMenuAudioSettings;

	BroadcastReceiver tentacleReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String msgSender = intent.getExtras().getString("MSG_SENDER");
			String msgType = intent.getExtras().getString("MSG_TYPE");
			String msgBody = intent.getExtras().getString("MSG_BODY");
			log.info("TentacleReceiver: New msg from = " + msgSender);
			if (msgType.equals("ERROR")) {
				if (msgBody.equalsIgnoreCase("AUTHENTICATION_FAILED")) {
					alert.setText("Please sing-in to Play Store");
					alert.setVisibility(View.VISIBLE);
				} else {
					alert.setText(msgBody + " ");
					alert.setVisibility(View.VISIBLE);
				}
			} else if (msgType.equalsIgnoreCase("GCMREG")) {
				checkAppPrerequisite();
			} else if (msgType.equalsIgnoreCase("PINREG")) {
				checkAppPrerequisite();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.startup_activity);
		log.info("Tentacle Strarted...");
		alert = (TextView) findViewById(R.id.startup_txt_alert);
		pb = (ProgressBar) findViewById(R.id.startup_pb_appregistor);
		retry = (Button) findViewById(R.id.startup_btn_retry);
		
		pb.setEnabled(true);		
		
		sendBroadcast(new Intent("com.google.android.intent.action.GTALK_HEARTBEAT"));
		sendBroadcast(new Intent("com.google.android.intent.action.MCS_HEARTBEAT"));
		
		if (checkAppPrerequisite()) {
			if (!PreferanceUtil.getSharedPreferences(this, PreferanceUtil.REGID, "").equals(GCMRegistrar.getRegistrationId(getApplicationContext()))) {
				getSharedPreferences(AppProperties.PREFERANCE_FILENAME_STRING, Context.MODE_PRIVATE).edit().clear().commit();
				log.info("clear regId & pin which is not same" + PreferanceUtil.getSharedPreferences(this, PreferanceUtil.REGID, "") + PreferanceUtil.getSharedPreferences(this, PreferanceUtil.PINID, ""));
				registerClient();
			}
		}
			
		retry.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				retry.setVisibility(View.GONE);
				if(checkAppPrerequisite())
					if(! PreferanceUtil.getSharedPreferences(getBaseContext(), PreferanceUtil.REGID, "").equals(GCMRegistrar.getRegistrationId(getApplicationContext()))) {
						getSharedPreferences(AppProperties.PREFERANCE_FILENAME_STRING, Context.MODE_PRIVATE).edit().clear().commit();
						 log.info("clear regId & pin which is not same" + PreferanceUtil.getSharedPreferences(getBaseContext(), PreferanceUtil.REGID, "") + PreferanceUtil.getSharedPreferences(getBaseContext(), PreferanceUtil.PINID, ""));
						 registerClient();
					 }
			}
		});
	}
	
	@Override
	protected void onStart() {	
		super.onStart();
		if (PreferanceUtil.getSharedPreferences(this, PreferanceUtil.userRole, "").equals("field_exec")) {
			LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == false) {
				LocationTracker.showLocationSettingsAlert(StartupActivity.this);
			}
			startService(new Intent(this, TrackerService.class));
		} else {
			log.info("User Role: "+ PreferanceUtil.getSharedPreferences(this, PreferanceUtil.userRole, "") + ".");
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter tentacleReciverFilter = new IntentFilter("TANTACLE_MSG_RECEIVER");
		registerReceiver(tentacleReceiver, tentacleReciverFilter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(tentacleReceiver);
	}

	private void renderAfterReg() {
		
		// Remove Retry Button
		retry.setVisibility(View.GONE);
		
		btnLogin = (Button) findViewById(R.id.startup_btn_logintentacle);
		btnViewCall = (Button) findViewById(R.id.startup_btn_viewcall);
		btnSignUp = (TextView) findViewById(R.id.startup_btn_signup);
		pb.setVisibility(View.GONE);
		SpannableString content = new SpannableString("Sign Up for Tentacle");
		content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
		btnSignUp.setText(content);

		btnLogin.setVisibility(View.VISIBLE);
		btnLogin.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent loginIntent = new Intent(getApplicationContext(),ViewActivity.class);
				loginIntent.putExtra("goTo","login");
				startActivity(loginIntent);
				//overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
			}
		});

		btnViewCall.setVisibility(View.VISIBLE);
		btnViewCall.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent callViewIntent = new Intent(getApplicationContext(),	MainActivity.class);
				startActivity(callViewIntent);
			}
		});

		btnSignUp.setVisibility(View.VISIBLE);
		btnSignUp.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent signUpIntent = new Intent(getApplicationContext(), ViewActivity.class);
				signUpIntent.putExtra("goTo", "signup");
				startActivity(signUpIntent);
			}
		});		
	}

	
	private boolean checkAppPrerequisite() {
		log.info("App initiating...");
		alert.setText("Configuring Device...");
		alert.setVisibility(View.VISIBLE);
		pb.setVisibility(View.VISIBLE);		
		ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		AccountManager accMan = AccountManager.get(this);
		Account[] accArray = accMan.getAccountsByType("com.google");
		// check SDcard
		try {
			if((PreferanceUtil.getSharedPreferences(this, PreferanceUtil.storageDrive, "").isEmpty())){
				PreferanceUtil.setSharedPreferences(this, PreferanceUtil.storageDrive,StorageHandler.setStorageAvailable());
			}			
			if (PreferanceUtil.getSharedPreferences(this, PreferanceUtil.storageDrive, StorageHandler.NO_STORAGE).equals(StorageHandler.NO_STORAGE)) {
				
				alert.setText("Your Storage is full");
				retry.setVisibility(View.VISIBLE);
				pb.setVisibility(View.GONE);
				return false;
			}
			// check Internet connection
			if (cm.getActiveNetworkInfo() == null) {
			alert.setText("Check your Internet connection and try again");
			retry.setVisibility(View.VISIBLE);
			pb.setVisibility(View.GONE);
			return false;
			}
			// check Google account
			else if (android.os.Build.VERSION.SDK_INT < 16 && accArray.length < 1) {
				alert.setText("Please sing-in to Play Store");
				retry.setVisibility(View.VISIBLE);
				pb.setVisibility(View.GONE);
				return false;
			}
			// check GCM registration
			else if (PreferanceUtil.getSharedPreferences(this, PreferanceUtil.REGID, "").isEmpty()) {
				alert.setText("Registering Device...");
				registerClient();
				return false;
			}
			// check PIN.
			else if (PreferanceUtil.getSharedPreferences(this, PreferanceUtil.PINID, "").isEmpty()) {
				sendRegistrationToServer();
				return false;
			} else if (!cm.getBackgroundDataSetting()) {
				alert.setText("Please enable background data");
				retry.setVisibility(View.VISIBLE);
				return false;
			}
			alert.setVisibility(View.GONE);
			retry.setVisibility(View.GONE);
			renderAfterReg();
			return true;
		} finally {
			
			
		}

	}

	public void registerClient() {
		String regId;
		log.info("Regestration Check...");
		alert.setVisibility(View.GONE);
		try {
			try {
				// Check that the device supports GCM (should be in a try catch)
				GCMRegistrar.checkDevice(this);
			} catch (Exception e) {
				log.debug("Error in GCMRegistrar check: ",e);
				alert.setText("Service intrupted");
				alert.setVisibility(View.VISIBLE);
				return;
			}

			if (!GCMRegistrar.isRegistered(getApplicationContext())) {
				log.debug("Registering Device...");
				// IF Device is not Register to GCM Server
				alert.setText("Registering Device...");
				alert.setVisibility(View.VISIBLE);
				registerGCM(); // Registering Client to Server
				regId = GCMRegistrar.getRegistrationId(getApplicationContext());
				if (!regId.equals("")) {
					log.info("regID=" + regId);
					PreferanceUtil.setSharedPreferences(this, PreferanceUtil.REGID, regId);
					sendRegistrationToServer();
				}
			} else {
				log.debug("Device is already registed");
				// IF Device is register to GCM Server
				regId = GCMRegistrar.getRegistrationId(getApplicationContext());
				if (!PreferanceUtil.getSharedPreferences(this, PreferanceUtil.REGID, "").equals(regId)) {
					if (!regId.equals("")) {
						PreferanceUtil.setSharedPreferences(this, PreferanceUtil.REGID, regId);
						sendRegistrationToServer();
					}
				}
			}

		} catch (Exception e) {
			log.debug("Error registerClient ", e);
		}
	}

	@SuppressLint("NewApi")
	private void sendRegistrationToServer() {

		log.debug("Generating PIN");
		alert.setText("Generating Device PIN..");
		alert.setVisibility(View.VISIBLE);
		try {
			// Setting Default Audio Source to Voice Call
			if (PreferanceUtil.getSharedPreferences(this, PreferanceUtil.AudioSource, "").isEmpty()) {
				PreferanceUtil.setSharedPreferences(this, PreferanceUtil.AudioSource, "1");
			}
			// IF PIN is not stored on device
			if (PreferanceUtil.getSharedPreferences(this, PreferanceUtil.PINID, "").isEmpty()) {
				if (android.os.Build.VERSION.SDK_INT > 9) {
					StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
					StrictMode.setThreadPolicy(policy);
				}

				HttpClient httpclient = new DefaultHttpClient();
				httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT,System.getProperty("http.agent"));
				HttpPost httppost = new HttpPost(AppProperties.MEDIA_SERVER_URL	+ AppProperties.SERVER_NAME_STRING + AppProperties.DEVICE_RECEIVER_STRING);

				try {

					TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
				
					// Add your data
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
					nameValuePairs.add(new BasicNameValuePair("DeviceId", PreferanceUtil.getSharedPreferences(this, PreferanceUtil.REGID, "")));
					nameValuePairs.add(new BasicNameValuePair("Manufacturer", android.os.Build.MANUFACTURER));
					nameValuePairs.add(new BasicNameValuePair("Model", android.os.Build.MODEL));
					nameValuePairs.add(new BasicNameValuePair("OSVersion", android.os.Build.VERSION.RELEASE));
					nameValuePairs.add(new BasicNameValuePair("IMEI", telephonyManager.getDeviceId()));
					nameValuePairs.add(new BasicNameValuePair("appversion", String.valueOf(getPackageManager().getPackageInfo(getPackageName(), 0).versionCode)));
					nameValuePairs.add(new BasicNameValuePair("appversionname", getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

					// Execute HTTP Post Request
					HttpResponse response = httpclient.execute(httppost);
					// Get Response
					final HttpEntity entity = response.getEntity();
					String resp = EntityUtils.toString(entity).trim();
					log.info("PIN response=" + resp + "  | length of response=" +resp.length());
					JSONObject jObject = new JSONObject(resp);
				    String pinid = jObject.getString("pin");
				    if (resp.length() == 6) {
				    	PreferanceUtil.setSharedPreferences(this, PreferanceUtil.PINID, resp);
				    	PreferanceUtil.setSharedPreferences(getBaseContext(), PreferanceUtil.isServiceStarted, "false");
						alert.setVisibility(View.GONE);
						renderAfterReg();
						return;
				    }else if (pinid.length() == 6) {
				    	PreferanceUtil.setSharedPreferences(this, PreferanceUtil.PINID, pinid);
						PreferanceUtil.setSharedPreferences(this,PreferanceUtil.AUTHTOKEN, jObject.getString("token"));
						if (jObject.has(PreferanceUtil.MIN_DISTANCE_CHANGE_FOR_UPDATES) && jObject.has(PreferanceUtil.MIN_TIME_BW_UPDATES)) {
							PreferanceUtil.setSharedPreferences(this,PreferanceUtil.MIN_DISTANCE_CHANGE_FOR_UPDATES, jObject.getString(PreferanceUtil.MIN_DISTANCE_CHANGE_FOR_UPDATES));
							PreferanceUtil.setSharedPreferences(this,PreferanceUtil.MIN_TIME_BW_UPDATES, jObject.getString(PreferanceUtil.MIN_TIME_BW_UPDATES));
						}
						PreferanceUtil.setSharedPreferences(getBaseContext(), "isServiceStarted", "false");
						alert.setVisibility(View.GONE);
						renderAfterReg();
						return;
					} else {
						pb.setVisibility(View.GONE);
						alert.setText("Try after sometime");
						retry.setVisibility(View.VISIBLE);
						log.info("pin not got = " + resp);
						return;
					}

				} catch (IOException e) {
					pb.setVisibility(View.GONE);
					alert.setText("Try after sometime");
					retry.setVisibility(View.VISIBLE);
					log.debug("Main Activity Error: " + e);
				}
			} else {
				alert.setVisibility(View.GONE);
				retry.setVisibility(View.GONE);
			}
		} catch (Exception e) {
			log.debug(e.toString());
		}
	}

	private void registerGCM() {
		try {
			Handler tempHandler = new Handler(getMainLooper());
			tempHandler.post(new Runnable() {
				@Override
				public void run() {
					GCMRegistrar.register(getApplicationContext(), AppProperties.PROJECT_ID);
				}
			});
		} catch (Exception e) {
			log.debug("Error occurend while GCM Registeration");
			return;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.startup_menu, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		optMenuAudioSettings = menu.findItem(R.id.opt_Menu_Audio_Settings);		
		if(Util.string2Int(PreferanceUtil.getSharedPreferences(getBaseContext(), PreferanceUtil.RecordingOption, "0")) == 0)
			optMenuAudioSettings.setVisible(true);
		else 
			optMenuAudioSettings.setVisible(false);		
		return super.onPrepareOptionsMenu(menu);
	}

	
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
				
		switch (item.getItemId()) {
		
			case R.id.opt_Menu_pin:
				
				new AlertDialog.Builder(this).setTitle("Tentacle PIN")
					.setMessage("PIN : " + PreferanceUtil.getSharedPreferences(this, PreferanceUtil.PINID, ""))
					.setCancelable(true)
					.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
									dialog.cancel();
							}
					}).create().show();
			return true;
			
			case R.id.opt_Menu_Audio_Settings:
				final String[] items = PreferanceUtil.getAudiosourceItems();
				int pos = Integer.parseInt(PreferanceUtil.getSharedPreferences(getBaseContext(), PreferanceUtil.AudioSource, "0"));
			
				new AlertDialog.Builder(this)
					.setTitle("Audio Source")
					.setSingleChoiceItems(items, pos, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int position) {
									PreferanceUtil.setSharedPreferences(getBaseContext(), PreferanceUtil.AudioSource, String.valueOf(position));
								}
							})
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									Toast.makeText(getApplicationContext(),items[Integer.parseInt(PreferanceUtil.getSharedPreferences(getBaseContext(), PreferanceUtil.AudioSource, "0"))],Toast.LENGTH_SHORT).show();
								}
							}).create().show();
			return true;
			
			case R.id.opt_Menu_recording:
				
				int recpos = Integer.parseInt(PreferanceUtil.getSharedPreferences(getBaseContext(), PreferanceUtil.RecordingOption, "0"));
				
				new AlertDialog.Builder(this).setTitle("Call Recording")
					.setSingleChoiceItems(PreferanceUtil.getRecordingOption(), recpos, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int position) {								
								PreferanceUtil.setSharedPreferences(getBaseContext(), PreferanceUtil.RecordingOption, String.valueOf(position));
								if(position==0)
									optMenuAudioSettings.setVisible(true);
								else
									optMenuAudioSettings.setVisible(false);
							}
						})
						.setPositiveButton("OK",new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								Toast.makeText(getBaseContext(),"Recording Turn" + PreferanceUtil.getRecordingOption()[Integer.parseInt(PreferanceUtil.getSharedPreferences(getBaseContext(), PreferanceUtil.RecordingOption, "0"))], Toast.LENGTH_SHORT).show();
							}
						}).create().show();
				return true;
				
			case R.id.opt_Menu_Ping_Test:
				
				android.app.FragmentManager m = getFragmentManager();
				
				PingTestFragment.newInstance().show(m, "ff");
				
				return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}