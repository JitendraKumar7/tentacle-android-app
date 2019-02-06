package com.sunoray.tentacle;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.PreferenceUtil;
import com.sunoray.tentacle.common.Util;
import com.sunoray.tentacle.helper.LocationTracker;
import com.sunoray.tentacle.helper.PermissionRequest;
import com.sunoray.tentacle.helper.StorageHandler;
import com.sunoray.tentacle.network.HttpServices;
import com.sunoray.tentacle.service.CallBarring;
import com.sunoray.tentacle.service.CheckKeepAliveStatus;
import com.sunoray.tentacle.service.KeepAliveService;
import com.sunoray.tentacle.service.TrackerService;

import android.Manifest;

import okhttp3.Response;


public class StartupActivity extends Activity {

    private static Logger log;
    TextView alert;
    ProgressBar pb;
    Button btnLogin;
    Button btnViewCall;
    TextView btnSignUp;
    Button retry;
    MenuItem optMenuAudioSettings;
    public static final int CHECKER_JOB_ID = 12;

    BroadcastReceiver incomingCallReceiver = new CallBarring();

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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            PermissionRequest.checkStoragePermissions(this);
        }

        StorageHandler.createLogFile(this);
        log = LoggerFactory.getLogger(StartupActivity.class);
        log.info("Tentacle Started...");
        alert = (TextView) findViewById(R.id.startup_txt_alert);
        pb = (ProgressBar) findViewById(R.id.startup_pb_appregistor);
        retry = (Button) findViewById(R.id.startup_btn_retry);

        pb.setEnabled(true);

        checkAppPrerequisite();

        retry.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                retry.setVisibility(View.GONE);
                checkAppPrerequisite();
            }
        });

        IntentFilter filter = new IntentFilter(TelephonyManager.EXTRA_STATE);
        registerReceiver(incomingCallReceiver, filter);


    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setCheckerJob() {
        try {
            JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.schedule(new JobInfo.Builder(CHECKER_JOB_ID,
                    new ComponentName(this, CheckKeepAliveStatus.class))
                    .setRequiresDeviceIdle(true)    // device should be idle
                    .setPeriodic(10 * 60 * 1000)     // 10 min
                    .build());
        } catch (Exception e) {
            log.debug("Exception in setCheckerJob", e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (PreferenceUtil.getSharedPreferences(this, PreferenceUtil.userRole, "").equals("field_exec")) {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == false) {
                LocationTracker.showLocationSettingsAlert(StartupActivity.this);
            }
            if (PermissionRequest.checkLocationPermissions(this))
                startService(new Intent(this, TrackerService.class));
        } else {
            log.info("User Role: " + PreferenceUtil.getSharedPreferences(this, PreferenceUtil.userRole, "") + ".");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(incomingCallReceiver);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        /*if (!PreferenceUtil.getSharedPreferences(this, PreferenceUtil.PINID, "").trim().isEmpty()) {
            checkOverlayPermission(this);
        }*/
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

    public void checkOverlayPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(activity)) {
            new AlertDialog.Builder(this, Util.getAlertTheame()).setTitle("Screen overlay detected")
                    .setMessage("To track incoming calls please enable screen overlay from settings.")
                    .setCancelable(true)
                    .setPositiveButton("Open settings", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.getPackageName()));
                            startActivityForResult(intent, 268435456);
                        }
                    }).create().show();
        } else
            Toast.makeText(getBaseContext(), "Track Incoming Calls Turned " + PreferenceUtil.getTrackInboundOption()[Integer.parseInt(PreferenceUtil.getSharedPreferences(getBaseContext(), PreferenceUtil.TrackInboundOption, "1"))], Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 268435456) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(StartupActivity.this)) {
                //PreferenceUtil.setSharedPreferences(getBaseContext(), PreferenceUtil.TrackInboundOption, String.valueOf(0));
                log.debug("Screen Overlay activated");
            } else {
                PreferenceUtil.setSharedPreferences(getBaseContext(), PreferenceUtil.TrackInboundOption, String.valueOf(1));
                log.debug("Screen Overlay deactivated");
            }
            Toast.makeText(getBaseContext(), "Track Incoming Calls Turned " + PreferenceUtil.getTrackInboundOption()[Integer.parseInt(PreferenceUtil.getSharedPreferences(getBaseContext(), PreferenceUtil.TrackInboundOption, "1"))], Toast.LENGTH_SHORT).show();
        }
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
                Intent loginIntent = new Intent(getApplicationContext(), ViewActivity.class);
                loginIntent.putExtra("goTo", "login");
                startActivity(loginIntent);
                //overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }
        });

        btnViewCall.setVisibility(View.VISIBLE);
        btnViewCall.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent callViewIntent = new Intent(getApplicationContext(), MainActivity.class);
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
        // check SDCard
        try {
            if ((PreferenceUtil.getSharedPreferences(this, PreferenceUtil.storageDrive, "").isEmpty())) {
                PreferenceUtil.setSharedPreferences(this, PreferenceUtil.storageDrive, StorageHandler.setStorageAvailable());
            }
            if (PreferenceUtil.getSharedPreferences(this, PreferenceUtil.storageDrive, StorageHandler.NO_STORAGE).equals(StorageHandler.NO_STORAGE)) {
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
            /*// check Google account
            else if (android.os.Build.VERSION.SDK_INT < 16 && accArray.length < 1) {
                alert.setText("Please sing-in to Play Store");
                retry.setVisibility(View.VISIBLE);
                pb.setVisibility(View.GONE);
                return false;
            }*/
            // check GCM registration
            else if (!PreferenceUtil.getSharedPreferences(this, PreferenceUtil.REGID, "").isEmpty()
                    && PreferenceUtil.getSharedPreferences(this,PreferenceUtil.FCM_IS_UPDATED,"false").equalsIgnoreCase("true")) {
                alert.setText("Registering Device...");
                sendRegistrationToServer();
                return false;
            }
            // check PIN.
            else if (PreferenceUtil.getSharedPreferences(this, PreferenceUtil.PINID, "").isEmpty()
                    && !PreferenceUtil.getSharedPreferences(this, PreferenceUtil.REGID, "").isEmpty()) {
                sendRegistrationToServer();
                return false;
            } else if (cm.getActiveNetworkInfo() == null) {
                alert.setText("Please enable Internet");
                retry.setVisibility(View.VISIBLE);
                return false;
            }
            // Check permission for screen overlay / Inbound call recording
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && !Settings.canDrawOverlays(getBaseContext())
                    && PreferenceUtil.getSharedPreferences(getBaseContext(), PreferenceUtil.TrackInboundOption, "1").equalsIgnoreCase("0")) {
                checkOverlayPermission(StartupActivity.this);
            }

            // starting notification service here
            if (PreferenceUtil.getSharedPreferences(getBaseContext(),
                    PreferenceUtil.TrackInboundOption, "1").equalsIgnoreCase("0")) {
                Intent intentService = new Intent(StartupActivity.this, KeepAliveService.class);
                ContextCompat.startForegroundService(StartupActivity.this, intentService);
            } else {
                Intent serviceIntent = new Intent(StartupActivity.this, KeepAliveService.class);
                stopService(serviceIntent);
            }

            //setting notification Job scheduler checker
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                setCheckerJob();

            alert.setVisibility(View.GONE);
            retry.setVisibility(View.GONE);
            renderAfterReg();
            return true;
        } finally {

        }
    }

    /*public void registerClient() {
        String regId;
        log.info("Registration Check...");
        alert.setVisibility(View.GONE);
        try {
            try {
                // Check that the device supports GCM (should be in a try catch)
                GCMRegistrar.checkDevice(this);
            } catch (Exception e) {
                log.debug("Error in GCMRegistrar check: ", e);
                alert.setText("Service Interrupted");
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
                    PreferenceUtil.setSharedPreferences(this, PreferenceUtil.REGID, regId);
                    sendRegistrationToServer();
                }
            } else {
                log.debug("Device is already registered");
                // IF Device is register to GCM Server
                regId = GCMRegistrar.getRegistrationId(getApplicationContext());
                if (!PreferenceUtil.getSharedPreferences(this, PreferenceUtil.REGID, "").equals(regId)) {
                    if (!regId.equals("")) {
                        PreferenceUtil.setSharedPreferences(this, PreferenceUtil.REGID, regId);
                        sendRegistrationToServer();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error registerClient ", e);
        }
    }*/

    @SuppressLint("NewApi")
    private void sendRegistrationToServer() {
        log.debug("Generating PIN");
        alert.setText("Generating Device PIN..");
        alert.setVisibility(View.VISIBLE);
        try {
            // Setting Default Audio Source to Voice Call
            if (PreferenceUtil.getSharedPreferences(this, PreferenceUtil.AudioSource, "").isEmpty()) {
                if (Build.MANUFACTURER.toLowerCase(Locale.ENGLISH).contains("samsung")
                        || Build.MANUFACTURER.toLowerCase(Locale.ENGLISH).contains("lge")
                        || Build.MANUFACTURER.toLowerCase(Locale.ENGLISH).contains("xiaomi"))
                    PreferenceUtil.setSharedPreferences(this, PreferenceUtil.AudioSource, "2");
                else
                    PreferenceUtil.setSharedPreferences(this, PreferenceUtil.AudioSource, "1");
            }
            // IF PIN is not stored on device
            if (PreferenceUtil.getSharedPreferences(this,PreferenceUtil.FCM_IS_UPDATED,"false").equalsIgnoreCase("true")) {
                if (android.os.Build.VERSION.SDK_INT > 9) {
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                    StrictMode.setThreadPolicy(policy);
                }

                try {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                        PermissionRequest.checkPhonePermissions(this);
                    }
                    TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

                    // Building Post Parameter
                    Map<String, String> keyValuePairs = new HashMap<>();
                    keyValuePairs.put("DeviceId", PreferenceUtil.getSharedPreferences(this, PreferenceUtil.REGID, ""));
                    keyValuePairs.put("Manufacturer", android.os.Build.MANUFACTURER);
                    keyValuePairs.put("Model", android.os.Build.MODEL);
                    keyValuePairs.put("OSVersion", android.os.Build.VERSION.RELEASE);
                    keyValuePairs.put("IMEI", telephonyManager.getDeviceId());
                    keyValuePairs.put("appversion", String.valueOf(getPackageManager().getPackageInfo(getPackageName(), 0).versionCode));
                    keyValuePairs.put("appversionname", getPackageManager().getPackageInfo(getPackageName(), 0).versionName);

                    // Making HTTP Request
                    HttpServices httpService = new HttpServices();
                    Response response = httpService.post(AppProperties.MEDIA_SERVER_URL + AppProperties.SERVER_NAME_STRING + AppProperties.DEVICE_RECEIVER_STRING, keyValuePairs);

                    // Get Response
                    String resp = response.body().string();
                    log.info("PIN response=" + resp + "  | length of response=" + resp.length());
                    JSONObject jObject = new JSONObject(resp);
                    String pinid = jObject.getString("pin");
                    if (resp.length() == 6) {
                        PreferenceUtil.setSharedPreferences(this, PreferenceUtil.PINID, resp);
                        PreferenceUtil.setSharedPreferences(getBaseContext(), PreferenceUtil.isServiceStarted, "false");
                        PreferenceUtil.setSharedPreferences(this, PreferenceUtil.FCM_IS_UPDATED, "false");
                        alert.setVisibility(View.GONE);
                        renderAfterReg();
                        return;
                    } else if (pinid.length() == 6) {
                        PreferenceUtil.setSharedPreferences(this, PreferenceUtil.PINID, pinid);
                        PreferenceUtil.setSharedPreferences(this, PreferenceUtil.AUTHTOKEN, jObject.getString("token"));
                        if (jObject.has(PreferenceUtil.MIN_DISTANCE_CHANGE_FOR_UPDATES) && jObject.has(PreferenceUtil.MIN_TIME_BW_UPDATES)) {
                            PreferenceUtil.setSharedPreferences(this, PreferenceUtil.MIN_DISTANCE_CHANGE_FOR_UPDATES, jObject.getString(PreferenceUtil.MIN_DISTANCE_CHANGE_FOR_UPDATES));
                            PreferenceUtil.setSharedPreferences(this, PreferenceUtil.MIN_TIME_BW_UPDATES, jObject.getString(PreferenceUtil.MIN_TIME_BW_UPDATES));
                        }
                        PreferenceUtil.setSharedPreferences(getBaseContext(), "isServiceStarted", "false");
                        PreferenceUtil.setSharedPreferences(this, PreferenceUtil.FCM_IS_UPDATED, "false");
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
        } finally {

        }
    }

    /*private void registerGCM() {
        try {
            Handler tempHandler = new Handler(getMainLooper());
            tempHandler.post(new Runnable() {
                @Override
                public void run() {
                    GCMRegistrar.register(getApplicationContext(), AppProperties.PROJECT_ID);
                }
            });
        } catch (Exception e) {
            log.debug("Error occurred while GCM Registration");
            return;
        }
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.startup_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        optMenuAudioSettings = menu.findItem(R.id.opt_Menu_Audio_Settings);
        if (Util.string2Int(PreferenceUtil.getSharedPreferences(getBaseContext(), PreferenceUtil.RecordingOption, "0")) == 0)
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
                        .setMessage("PIN : " + PreferenceUtil.getSharedPreferences(this, PreferenceUtil.PINID, ""))
                        .setCancelable(true)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).create().show();
                return true;

            case R.id.opt_Menu_Audio_Settings:
                final String[] items = PreferenceUtil.getAudiosourceItems();
                int pos = Integer.parseInt(PreferenceUtil.getSharedPreferences(getBaseContext(), PreferenceUtil.AudioSource, "0"));
                new AlertDialog.Builder(this)
                        .setTitle("Audio Source")
                        .setSingleChoiceItems(items, pos, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int position) {
                                PreferenceUtil.setSharedPreferences(getBaseContext(), PreferenceUtil.AudioSource, String.valueOf(position));
                            }
                        })
                        .setPositiveButton("Ok",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        Toast.makeText(getApplicationContext(), items[Integer.parseInt(PreferenceUtil.getSharedPreferences(getBaseContext(), PreferenceUtil.AudioSource, "0"))], Toast.LENGTH_SHORT).show();
                                    }
                                }).create().show();
                return true;

            case R.id.opt_Menu_recording:
                int recpos = Integer.parseInt(PreferenceUtil.getSharedPreferences(getBaseContext(), PreferenceUtil.RecordingOption, "0"));
                new AlertDialog.Builder(this).setTitle("Call Recording")
                        .setSingleChoiceItems(PreferenceUtil.getRecordingOption(), recpos, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int position) {
                                PreferenceUtil.setSharedPreferences(getBaseContext(), PreferenceUtil.RecordingOption, String.valueOf(position));
                                if (position == 0)
                                    optMenuAudioSettings.setVisible(true);
                                else
                                    optMenuAudioSettings.setVisible(false);
                            }
                        })
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Toast.makeText(getBaseContext(), "Recording Turn" + PreferenceUtil.getRecordingOption()[Integer.parseInt(PreferenceUtil.getSharedPreferences(getBaseContext(), PreferenceUtil.RecordingOption, "0"))], Toast.LENGTH_SHORT).show();
                            }
                        }).create().show();
                return true;

            case R.id.opt_Menu_Track_Inbound_Call:
                if (PreferenceUtil.getSharedPreferences(getBaseContext(), PreferenceUtil.TrackInboundOption, "").isEmpty()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(getBaseContext()))
                        PreferenceUtil.setSharedPreferences(getBaseContext(), PreferenceUtil.TrackInboundOption, String.valueOf(0));
                    else
                        PreferenceUtil.setSharedPreferences(getBaseContext(), PreferenceUtil.TrackInboundOption, String.valueOf(1));
                }
                int trackOpt = Integer.parseInt(PreferenceUtil.getSharedPreferences(getBaseContext(), PreferenceUtil.TrackInboundOption, "1"));
                new AlertDialog.Builder(this).setTitle("Track Incoming Calls")
                        .setSingleChoiceItems(PreferenceUtil.getTrackInboundOption(), trackOpt, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int position) {
                                PreferenceUtil.setSharedPreferences(getBaseContext(), PreferenceUtil.TrackInboundOption, String.valueOf(position));
                            }
                        })
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (PreferenceUtil.getSharedPreferences(getBaseContext(), PreferenceUtil.TrackInboundOption, "1").equalsIgnoreCase("0")) {
                                    Intent intentService = new Intent(StartupActivity.this, KeepAliveService.class);
                                    ContextCompat.startForegroundService(StartupActivity.this, intentService);
                                    checkOverlayPermission(StartupActivity.this);
                                } else {
                                    Toast.makeText(getBaseContext(), "Track Incoming Calls Turned " + PreferenceUtil.getTrackInboundOption()[Integer.parseInt(PreferenceUtil.getSharedPreferences(getBaseContext(), PreferenceUtil.TrackInboundOption, "1"))], Toast.LENGTH_SHORT).show();
                                    Intent intentService = new Intent(StartupActivity.this, KeepAliveService.class);
                                    stopService(intentService);
                                }
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