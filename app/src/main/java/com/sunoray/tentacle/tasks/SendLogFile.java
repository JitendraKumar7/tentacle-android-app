package com.sunoray.tentacle.tasks;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sunoray.tentacle.R;
import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.PreferenceUtil;
import com.sunoray.tentacle.helper.PermissionRequest;
import com.sunoray.tentacle.network.HttpServices;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.widget.TextView;

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
        try {
            Thread.currentThread().setName("SendLogFileAsyncTask");
            log.info("Sending log to tentacle server...");
            logRunningApps();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PermissionRequest.logAllPermissions(context);
            logAppSettings();
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            log.info("App Version: " + pInfo.versionName + " | Version Code: " + pInfo.versionCode);
            log.info("Agent: " + System.getProperty("http.agent"));

            JSONObject jobj = new JSONObject();
            jobj.put("pin", PreferenceUtil.getSharedPreferences(context, PreferenceUtil.PINID, ""));
            jobj.put("alert_type", "log");

            try {
                File file = new File(Environment.getExternalStorageDirectory(), AppProperties.DEVICE_LOGFILE_PATH);
                if (file.isFile()) {
                    //log.info("log file available");
                    HttpServices httpServices = new HttpServices();
                    // Execute HTTP Post Request
                    httpServices.postFile(AppProperties.MEDIA_SERVER_URL
                            + AppProperties.SERVER_NAME_STRING
                            + AppProperties.DEVICE_LOG
                            + "?pin=" + PreferenceUtil.getSharedPreferences(context, PreferenceUtil.PINID, "")
                            + "&token=" + PreferenceUtil.getSharedPreferences(context, PreferenceUtil.AUTHTOKEN, "")
                            + "&appversion=" + String.valueOf(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode)
                            + "&json=" + URLEncoder.encode(jobj.toString(), "UTF-8"), file);
                    log.info("log file send successfully");
                    taskStatus = true;
                } else {
                    log.info("log file not found");
                    taskStatus = false;
                }
            } catch (Exception e) {
                taskStatus = false;
                log.debug("Exception: ", e);
            }
        } catch (Exception e) {
            log.debug("Exception: ", e);
            taskStatus = false;
        }
        return taskStatus;
    }

    @Override
    protected void onPostExecute(Boolean taskStatus) {
        if (taskStatus) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                txt.setTextColor(context.getResources().getColor(R.color.tentacle_green, context.getTheme()));
            } else {
                txt.setTextColor(context.getResources().getColor(R.color.tentacle_green));
            }
            txt.setText(txt.getText() + " - Success");
        } else {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                txt.setTextColor(context.getResources().getColor(R.color.orange, context.getTheme()));
            } else {
                txt.setTextColor(context.getResources().getColor(R.color.orange));
            }
            txt.setText(txt.getText() + " - Warning");
        }
    }

    private void logRunningApps() {
        try {
            log.debug("List of Apps Running in Background");
            List<String> runningAppList = new ArrayList<>(new HashSet<String>(getRunningApps()));
            for (int i = 0; i < runningAppList.size(); i++) {
                if (!runningAppList.get(i).contains("com.android") && !runningAppList.get(i).contains("com.google")) {
                    log.info(i + " Package Name: " + runningAppList.get(i));
                }
            }
        } catch (Exception e) {
            log.debug("Exception: " + e);
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
            log.debug("Exception: " + e);
        }
        return appList;
    }

    private void logAppSettings() {
        log.info("Audio Source   : "+ PreferenceUtil.getAudiosourceItems()[Integer.parseInt(PreferenceUtil.getSharedPreferences(context, PreferenceUtil.AudioSource, "0"))]);
        log.info("Call Recording : "+ PreferenceUtil.getRecordingOption()[Integer.parseInt(PreferenceUtil.getSharedPreferences(context, PreferenceUtil.RecordingOption, "0"))]);
        log.info("Incoming Calls : "+ PreferenceUtil.getTrackInboundOption()[Integer.parseInt(PreferenceUtil.getSharedPreferences(context, PreferenceUtil.TrackInboundOption, "1"))]);
        log.info("Sync Options   : "+ PreferenceUtil.getSyncOptions()[Integer.parseInt(PreferenceUtil.getSharedPreferences(context, PreferenceUtil.SyncOption, "0"))].replace("\n"," "));
    }

}