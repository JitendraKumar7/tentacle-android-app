package com.sunoray.tentacle;

import java.io.File;
import java.lang.reflect.Method;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sunoray.tentacle.bean.LocationBean;
import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.PreferenceUtil;
import com.sunoray.tentacle.common.Util;
import com.sunoray.tentacle.db.DatabaseHandler;
import com.sunoray.tentacle.db.Recording;
import com.sunoray.tentacle.helper.LocationTracker;
import com.sunoray.tentacle.helper.MediaRecording;
import com.sunoray.tentacle.helper.StorageHandler;
import com.sunoray.tentacle.service.BackGroundService;
import com.sunoray.tentacle.service.TrackerService;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

public class TentacleJSInterface {

    private Context context;
    private WebView tentacleBrowser;

    static private final Logger log = LoggerFactory.getLogger(ViewActivity.class);

    public TentacleJSInterface(Context context, WebView tentacleBrowser) {
        this.context = context;
        this.tentacleBrowser = tentacleBrowser;
    }

    public TentacleJSInterface(Context context) {
        this.context = context;
    }


    @JavascriptInterface
    public String getLocation(String role) {
        try {
            PreferenceUtil.setSharedPreferences(context, PreferenceUtil.userRole, role);
            log.debug("getLocation method called ");
            final LocationBean locationbean = new LocationBean();
            final LocationTracker mGPS = new LocationTracker(context, locationbean);

            if (mGPS.getLocation()) {
                if (locationbean.getLatitude() == 0.0 && locationbean.getLongitude() == 0.0) {
                    log.debug("method called getGeoLocation else");
                    new CountDownTimer(15000, 1000) {
                        public void onTick(long millisUntilFinished) {
                            if (locationbean.getLatitude() != 0.0 && locationbean.getLongitude() != 0.0) {
                                postLocation("on", locationbean.getLatitude(), locationbean.getLongitude());
                                mGPS.stopUsingGPS();
                                this.cancel();
                            }
                        }

                        public void onFinish() {
                            if (locationbean.getLatitude() != 0.0 && locationbean.getLongitude() != 0.0) {
                                postLocation("on", locationbean.getLatitude(), locationbean.getLongitude());
                            }
                            mGPS.stopUsingGPS();
                        }
                    }.start();

                } else {
                    mGPS.stopUsingGPS();
                    if (locationbean.getLatitude() != 0.0 && locationbean.getLongitude() != 0.0) {
                        postLocation("on", locationbean.getLatitude(), locationbean.getLongitude());
                    }
                }
            } else {
                postLocation("off", null, null);
            }
        } catch (Exception e) {
            log.debug("exception:" + e);
        }
        return null;
    }


    @JavascriptInterface
    public String userTracker(String uniqueID, String role, String userEmail) {
        try {
            log.info("trackUser called | User Unique ID=" + uniqueID + " | UserRole=" + role + " | User Email Id=" + userEmail);
            Intent locationServiceIntent = new Intent(context, TrackerService.class);
            locationServiceIntent.putExtra(PreferenceUtil.userUniqueID, uniqueID);
            locationServiceIntent.putExtra(PreferenceUtil.userRole, role);
            locationServiceIntent.putExtra(PreferenceUtil.userID, userEmail);

            log.info("Uear Tracker callled. Role: " + role);
            // Tracking user only in case of Role - field_exec
            if (role.equals("field_exec")) {
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == false) {
                    LocationTracker.showLocationSettingsAlert(context);
                }
                context.startService(locationServiceIntent);
            }

        } catch (Exception e) {
            log.info("Exception in userTracker: " + e);
        } finally {
            // Sending User Details to TentacleCall
            JSONObject jsonObj = new JSONObject();
            try {
                jsonObj.put("alert_type", "user_update");
                jsonObj.put(PreferenceUtil.userRole, role);
                jsonObj.put(PreferenceUtil.userID, userEmail);
                jsonObj.put("pin", PreferenceUtil.getSharedPreferences(context, PreferenceUtil.PINID, ""));
                new com.sunoray.tentacle.tasks.SendStatus(context).execute(jsonObj.toString());
            } catch (Exception e) {
                log.info("Exception in ACK@userTracker :" + e);
            }
        }
        return PreferenceUtil.getSharedPreferences(context, PreferenceUtil.PINID, "0");
    }

    @JavascriptInterface
    public void disconnectCall() {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Method m1 = tm.getClass().getDeclaredMethod("getITelephony");
            m1.setAccessible(true);
            Object iTelephony = m1.invoke(tm);
            Method m3 = iTelephony.getClass().getDeclaredMethod("endCall");
            m3.invoke(iTelephony);
        } catch (Exception e) {
            log.info("Exception @ End call", e);
        }
    }

    public void postLocation(final String status, final Double latitude, final Double longitude) {
        try {
            Handler mainHandler = new Handler(context.getMainLooper());
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    tentacleBrowser.loadUrl("javascript:setGeoLocation('" + status + "'," + latitude + "," + longitude + ")");
                }
            });
        } catch (Exception e) {
            log.info("Exception:" + e);
        }
    }

    @JavascriptInterface
    public void postRecording(String log_id, String unique_call_id, String account_id, String campaign_id, String prospect_id, String host) {
        try {
            log.debug("postRecording Call:" + log_id);
            DatabaseHandler dh = new DatabaseHandler(context);
            if (dh.getRecording(Integer.parseInt(log_id)).getStatus().trim().equalsIgnoreCase("INBOUND")) {
                Recording rec = dh.getRecording(Integer.parseInt(log_id));
                rec.setId(Integer.parseInt(log_id));
                rec.setAccountId(account_id);
                rec.setCampaignId(campaign_id);
                rec.setProspectId(prospect_id);
                rec.setServerType(host);
                rec.setCallId(unique_call_id);
                if (dh.updateRecordingInbound(rec) > 0) {
                    String fullPath = StorageHandler.getFileDirPath(context, AppProperties.DEVICE_RECORDING_PATH).getAbsolutePath() + File.separator + unique_call_id + MediaRecording.file_exts[MediaRecording.currentFormat];
                    if (Util.renameFile(context, rec.getPath(), fullPath)) {
                        rec.setStatus("NEW");
                        rec.setPath(fullPath);
                        dh.updateRecordingInbound(rec);
                    }
                    context.startService(new Intent(context, BackGroundService.class));
                }
            }
        } catch (Exception e) {
            log.info("Exception:" + e);
        }
    }

}