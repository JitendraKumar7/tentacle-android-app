package com.sunoray.tentacle;

import java.io.File;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.sunoray.tentacle.application.ApplicationExtension;
import com.sunoray.tentacle.bean.LocationBean;
import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.CommonField;
import com.sunoray.tentacle.common.PreferenceUtil;
import com.sunoray.tentacle.common.Util;
import com.sunoray.tentacle.db.DatabaseHandler;
import com.sunoray.tentacle.db.Recording;
import com.sunoray.tentacle.helper.BetteryHelper;
import com.sunoray.tentacle.helper.CallHelper;
import com.sunoray.tentacle.helper.LocationTracker;
import com.sunoray.tentacle.network.HttpServices;
import com.sunoray.tentacle.tasks.SendCurrentLocation;
import com.sunoray.tentacle.tasks.SendStatus;
import com.sunoray.tentacle.tasks.SendUserTrack;

public class GCMIntentService extends FirebaseMessagingService {

    private static final Logger log = LoggerFactory.getLogger(GCMIntentService.class);
    static int reqCall = 0; // To Create New Notification Every Time
    static Queue<String> callidqueue = new LinkedList<String>();

    public GCMIntentService() {
        log.info("Msg Receiver Services is Started");
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        try {
            Map<String, String> data = message.getData();
            String serverType = data.get("server_type");
            String alertType = data.get("alert_type") == null ? "call" : data.get("alert_type");

            log.debug("New Push notification Request type: " + alertType);
            if (alertType.equalsIgnoreCase("log")) {
                log.debug("log call");
                sendLogToServer();
            } else if (alertType.equalsIgnoreCase("call")) {

                String number = data.get("number");
                String callId = data.get("call_id");

                log.debug("New Call alert | id: " + callId + " | server:" + serverType);

                if (!(Util.isNull(callId) || Util.isNull(number))) {
                    if (matchHitQueue(number, callId)) {
                        log.debug("Duplicate Call Request");
                        sendNotification(getBaseContext(), number, callId, serverType, data);
                    } else {
                        addHitQueue(number, callId);
                        reqCall++;
                        sendNotification(getBaseContext(), number, callId, serverType, data);
                    }
                } else {
                    log.debug("Got NULL Call Request. Request Ignored");
                }
            } else if (alertType.equalsIgnoreCase("redirect")) {
                reqCall++;
                sendNotification(getBaseContext(), serverType, data);
            } else if (alertType.equalsIgnoreCase("flash")) {
                sendMsg(getBaseContext(), data.get("number"));
            } else if (alertType.equalsIgnoreCase("location")) {
                log.info("location received");
                sendCurrentLocation(getBaseContext());
            } else if (alertType.equalsIgnoreCase("sharedpreference")) {
                String token = data.get("token");
                if (token.equalsIgnoreCase(PreferenceUtil.getSharedPreferences(getBaseContext(), PreferenceUtil.AUTHTOKEN, ""))) {
                    String key = data.get("key");
                    String value = data.get("value");
                    sendSharedPreference(getBaseContext(), key, value, alertType);
                }
            } else if (alertType.equalsIgnoreCase("hangup")) {
                log.info("Hangup Call Request from GSM");
                CallHelper.endCall(getBaseContext());
            } else if (alertType.equalsIgnoreCase("custom_api")) {
                log.info("Custom app invocation from Website (Hybrid calling)");
                sendToKdialer(getBaseContext(), data);
            }
        } catch (Exception e) {
            log.debug("Error at onMessage: ", e);
        }

    }

    private void sendSharedPreference(Context context, String key, String value, String alertType) {

        if (!PreferenceUtil.getSharedPreferences(context, key, "").equalsIgnoreCase("")) {
            PreferenceUtil.setSharedPreferences(context, key, value);
            log.info("Request for change shared preferance | " + key + " = " + value + " IS UPDATED SUCCESSFULLY");
        } else {
            log.info("Request for change shared preferance | " + key + " = " + value + " IS NOT UPDATED");
        }
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("alert_type", alertType);
            jsonObj.put("Key", key);
            jsonObj.put("Value", value);
            jsonObj.put("pin", PreferenceUtil.getSharedPreferences(this, PreferenceUtil.PINID, ""));
            jsonObj.put("appversion", String.valueOf(getPackageManager().getPackageInfo(getPackageName(), 0).versionCode));
            // Sending Delivery ACK to Server
            new SendStatus(this).execute(jsonObj.toString());
        } catch (Exception e) {
            log.info("Exception in ACK:" + e);
        }
    }

    private void sendCurrentLocation(final Context context) {
        //new SendCurrentLocation(context).execute();

        final LocationBean locationbean = new LocationBean();
        final LocationTracker mGPS = new LocationTracker(context, locationbean);
        try {
            if (mGPS.getLocation()) {
                if (locationbean.getLatitude() == 0.0 && locationbean.getLongitude() == 0.0) {
                    log.debug("method called getGeoLocation else");
                    new CountDownTimer(15000, 1000) {
                        public void onTick(long millisUntilFinished) {
                            if (locationbean.getLatitude() != 0.0 && locationbean.getLongitude() != 0.0) {
                                new SendCurrentLocation(context).execute("on | " + locationbean.getLatitude() + " | " + locationbean.getLongitude());
                                mGPS.stopUsingGPS();
                                this.cancel();
                            }
                        }

                        public void onFinish() {
                            if (locationbean.getLatitude() != 0.0 && locationbean.getLongitude() != 0.0) {
                                new SendCurrentLocation(context).execute("on | " + locationbean.getLatitude() + " | " + locationbean.getLongitude());
                            }
                            mGPS.stopUsingGPS();
                        }
                    }.start();

                } else {
                    mGPS.stopUsingGPS();
                    if (locationbean.getLatitude() != 0.0 && locationbean.getLongitude() != 0.0) {
                        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                        try {
                            log.info("Capture Location | Lagtitude=" + locationbean.getLatitude() + " | Longitude=" + locationbean.getLongitude());
                            DatabaseHandler db = new DatabaseHandler(context);
                            Map<String, String> deviceInfo = new HashMap<String, String>();
                            deviceInfo.put("battery_level", BetteryHelper.getBatteryLevel(context));
                            String deviceInfoString = deviceInfo.toString();
                            db.addLocation(String.valueOf(locationbean.getLatitude()), String.valueOf(locationbean.getLongitude()), deviceInfoString);

                            if (cm.getActiveNetworkInfo().isConnected()) {

                                AsyncTask<Void, Void, Void> bTask = new SendUserTrack(context);
                                bTask.execute();
                            }
                        } catch (Exception e) {
                            log.info(" Exception @ onLocationChanged :", e);
                        }
                    }
                }
            } else {
                log.info("Network & GPS Location Capture is Disable");
                // Set dynamic value in table.
                Map<String, String> deviceInfo = new HashMap<String, String>();
                deviceInfo.put("battery_level", BetteryHelper.getBatteryLevel(context));
                deviceInfo.put("location_service", "off");

                DatabaseHandler db = new DatabaseHandler(context);
                db.addLocation("0.0", "0.0", deviceInfo.toString());
            }
        } catch (Exception e) {
            log.debug("exception:" + e);
        }

    }

    protected void sendNotification(Context context, String number, String callId, String serverType, Map<String, String> data) {

        //Creating Recording Object.
        Recording rec = new Recording();
        rec.setCallId(callId);
        rec.setPhoneNumber(number);

        String numberFlag = data.get("hide_number");

        log.info("numberFlag=" + numberFlag);
        if (numberFlag != null && numberFlag.equalsIgnoreCase("true")) {
            rec.setHideNumber("XXXXXXXXXX");
        } else {
            rec.setHideNumber(number);
        }

        rec.setServerType(serverType);
        rec.setAccountId(data.get("account_id"));
        rec.setCampaignId(data.get("campaign_id"));
        rec.setProspectId(data.get("prospect_id"));

        // Sending delivery ACK to TC
        JSONObject jsonObj = new JSONObject();
        try {
            String alertType = data.get("alert_type");
            jsonObj.put("alert_type", alertType);
            jsonObj.put("call_no", number);
            jsonObj.put("call_id", callId);
            jsonObj.put("pin", PreferenceUtil.getSharedPreferences(this, PreferenceUtil.PINID, ""));
            jsonObj.put("appversion", String.valueOf(getPackageManager().getPackageInfo(getPackageName(), 0).versionCode));
            // Sending Delivery ACK to Server
            new SendStatus(this).execute(jsonObj.toString());
        } catch (Exception e) {
            log.info("Exception in ACK:" + e);
        }

        try {

            ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);

            if (taskInfo.get(0).topActivity.getPackageName().equalsIgnoreCase("com.sunoray.tentacle") && !AppProperties.activeAlertDialog) {
                playNotification();

                Intent gsmIntent = new Intent(getBaseContext(), com.sunoray.tentacle.extraActivity.GCMActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                gsmIntent.putExtra(CommonField.RECORDING, rec);
                startActivity(gsmIntent);

            } else {
                Intent nextIntent = new Intent(getApplicationContext(), com.sunoray.tentacle.extraActivity.GCMActivity.class);
                nextIntent.putExtra(CommonField.RECORDING, rec);
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        getApplicationContext(),
                        0,
                        nextIntent,
                        0);

                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Notification notification = new NotificationCompat.Builder(getApplicationContext(),
                        ApplicationExtension.ALERT_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Pending calls")
                        .setLights(Color.GREEN, 1000, 2000)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setAutoCancel(true)
                        .setContentText("call to:" + rec.getHideNumber())
                        .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                        .build();
                mNotificationManager.notify(reqCall, notification);
            }
        } catch (Exception e) {
            log.debug("Exception in SendNotification()", e.toString());
        }
    }

    protected void sendNotification(Context context, String serverType, Map<String, String> data) {

        String action = data.get("action");
        String alertType = data.get("alert_type");
        String ackData = data.get("ack_data");
        String title = data.get("title");
        String content = data.get("content");

        // Sending Delivery ACK to Server
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("alert_type", alertType);
            jsonObj.put("ack_data", ackData);
            jsonObj.put("pin", PreferenceUtil.getSharedPreferences(this, PreferenceUtil.PINID, ""));
            jsonObj.put("appversion", String.valueOf(getPackageManager().getPackageInfo(getPackageName(), 0).versionCode));
            new SendStatus(this).execute(jsonObj.toString());
        } catch (Exception e) {
            log.info("Exception in ACK:" + e);
        }

        try {
            Intent nextIntent = new Intent(getApplicationContext(), com.sunoray.tentacle.extraActivity.GCMActivity.class);
            nextIntent.putExtra("action", action);
            nextIntent.putExtra("server_type", serverType);
            nextIntent.putExtra("title", title);
            nextIntent.putExtra("content", content);

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = new NotificationCompat.Builder(getApplicationContext(),
                    ApplicationExtension.ALERT_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(title)
                    .setLights(Color.GREEN, 1000, 2000)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setAutoCancel(true)
                    .setContentText(content)
                    .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                    .build();
            //notification.flags = Notification.FLAG_NO_CLEAR;
            mNotificationManager.notify(reqCall, notification);
        } catch (Exception e) {
            log.debug("Exception in SendNotification()" + e);
        }

    }

    protected void sendMsg(Context ctx, String message) {

        // Sending Delivery ACK to Server
        // sendcallStatusTentacle(message, callID, Util.getPinId(this), "Alert Msg Received");

        try {
            ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            if (taskInfo.get(0).topActivity.getPackageName().equalsIgnoreCase("com.sunoray.tentacle") && !AppProperties.activeAlertDialog) {
                playNotification();
                Intent msgIntent = new Intent(getBaseContext(), com.sunoray.tentacle.extraActivity.MsgViewActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                msgIntent.putExtra("message", message);
                startActivity(msgIntent);
            } else {
                Intent nextIntent = new Intent(getApplicationContext(), com.sunoray.tentacle.extraActivity.MsgViewActivity.class);
                nextIntent.putExtra("message", message);

                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Notification notification = new NotificationCompat.Builder(getApplicationContext(),
                        ApplicationExtension.BACK_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Tentacle Alert")
                        .setLights(Color.GREEN, 1000, 2000)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setAutoCancel(true)
                        .setContentText(message)
                        .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                        .build();
                //notification.flags = Notification.FLAG_NO_CLEAR;
                mNotificationManager.notify(reqCall, notification);
            }
        } catch (Exception e) {
            log.debug("Exception in SendNotification()", e.toString());
        }
    }

    private void playNotification() {
        try {
            Handler tempHandler = new Handler(getMainLooper());
            tempHandler.post(new Runnable() {
                @Override
                public void run() {
                    RingtoneManager.getRingtone(getBaseContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).play();
                }
            });
        } catch (Exception e) {
            log.debug("Error while playing Notification");
            return;
        }
    }

    private void sendLogToServer() {
        try {
            log.info("Sending log to tentacle server...");

            JSONObject jobj = new JSONObject();
            jobj.put("pin", PreferenceUtil.getSharedPreferences(this, PreferenceUtil.PINID, ""));
            jobj.put("alert_type", "log");

            File file = new File(Environment.getExternalStorageDirectory(), AppProperties.DEVICE_LOGFILE_PATH);

            try {
                if (file.isFile()) {
                    log.info("log file available");
                    HttpServices httpServices = new HttpServices();

                    // Execute HTTP Post Request
                    httpServices.postFile(AppProperties.MEDIA_SERVER_URL
                            + AppProperties.SERVER_NAME_STRING
                            + AppProperties.DEVICE_LOG
                            + "?pin=" + PreferenceUtil.getSharedPreferences(this, PreferenceUtil.PINID, "")
                            + "&token=" + PreferenceUtil.getSharedPreferences(this, PreferenceUtil.AUTHTOKEN, "")
                            + "&appversion=" + String.valueOf(getPackageManager().getPackageInfo(getPackageName(), 0).versionCode)
                            + "&json=" + URLEncoder.encode(jobj.toString(), "UTF-8"), file);
                    log.info("log file send successfully");
                } else
                    log.info("log file not found");
            } catch (Exception e) {
                log.debug("Main Activity Error IOE=", e);
            }
        } catch (Exception e) {
            Log.i("Exception:", e.toString());
        }
    }

    protected void onExtraNotification(Context arg0, String arg1, Intent intent) {

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext(),
                ApplicationExtension.BACK_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Tentacle").setLights(Color.RED, 1000, 2000)
                .setAutoCancel(true).setContentText(arg1);

        Intent nextIntent = new Intent(getApplicationContext(), com.sunoray.tentacle.MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getBaseContext(), 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setContentIntent(contentIntent);
        mNotificationManager.notify(100, notification.build());
    }

    protected void onError(Context context, String msg) {
        log.info("Received error: " + msg);
        Util.send2tentacleReceiver(context, this.getClass().getName(), "ERROR", msg);
        onExtraNotification(context, msg, null);
    }

    protected void onRegistered(Context context, String regId) {
        log.info("*************register called");
        PreferenceUtil.setSharedPreferences(this, PreferenceUtil.REGID, "");
        Util.send2tentacleReceiver(context, this.getClass().getName(), "GCMREG", null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void addHitQueue(String call_no, String call_id) {
        SharedPreferences.Editor editor = getSharedPreferences(AppProperties.QUEUE_FILENAME_STRING, Context.MODE_PRIVATE).edit();
        editor.putString(call_no, call_id);
        editor.commit();

    }

    private boolean matchHitQueue(String call_no, String call_id) {
        String temp_id;
        try {
            SharedPreferences prefs = this.getSharedPreferences(AppProperties.QUEUE_FILENAME_STRING, Context.MODE_PRIVATE);
            temp_id = prefs.getString(call_no, "");
        } catch (ClassCastException e) {
            return false;
        }
        if (!temp_id.equals(call_id))
            return false;
        return true;
    }

    private void sendToKdialer(Context context, Map<String, String> data) {
        String custom_api_name = data.get("custom_api_name");
        String phone_number = data.get("phone_number");
        String unique_call_id = data.get("unique_call_id");
        String sr_number = data.get("sr_number");
        String api_key = data.get("api_key");

        try {
            String url = custom_api_name + "://" + phone_number
                    + "/" + unique_call_id
                    + "/" + sr_number
                    + "/" + api_key;
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            if (browserIntent.resolveActivity(getPackageManager()) != null)
                startActivity(browserIntent);
            else
                Util.activityNotFoundAlert(context);
        } catch (ActivityNotFoundException e) {
            log.info("Exception in sendToKdialer(): ", e);
            Util.activityNotFoundAlert(context);
        } catch (Exception e) {
            log.info("Exception in sendToKdialer(): ", e);
        }
    }
}