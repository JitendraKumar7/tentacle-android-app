package com.sunoray.tentacle.service;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.view.WindowManager;

import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.PreferenceUtil;
import com.sunoray.tentacle.db.DatabaseHandler;
import com.sunoray.tentacle.db.Recording;
import com.sunoray.tentacle.helper.MediaRecording;
import com.sunoray.tentacle.layout.InboundDialog;
import com.sunoray.tentacle.tasks.AddRecording;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class CallBarring extends PhoneCallReceiver {

    private static final Logger log = LoggerFactory.getLogger(CallBarring.class);
    private static Recording rec = null;
    private static MediaRecording media = null;
    private static String recordingOption = null;
    private static boolean isRecording = false;  // To avoid duplicate call recorder

    @Override
    protected void onIncomingCallReceived(Context context, String number, Date start) {

    }

    @Override
    protected void onIncomingCallAnswered(Context context, String number, Date start) {
        log.debug("Incoming call answered");
        if (PreferenceUtil.getSharedPreferences(context, PreferenceUtil.TrackInboundOption, "1").equals("0"))
            recordCall(context, number);
    }

    @Override
    protected void onIncomingCallEnded(Context context, String number, Date start, Date end) {
        stopCallRec(context, number);
    }

    @Override
    protected void onOutgoingCallStarted(Context context, String number, Date start) {

    }

    @Override
    protected void onOutgoingCallEnded(Context context, String number, Date start, Date end) {

    }

    @Override
    protected void onMissedCall(Context context, String number, Date start) {

    }

    private void recordCall(Context context, String number) {
        try {
            if (number != null) {
                if (!isRecording && !AppProperties.isCallServiceRunning) {
                    log.info("Incoming Call Record Start");
                    isRecording = true;
                    rec = new Recording();
                    rec.setPhoneNumber(number);
                    rec.setDirection("Inbound");
                    rec.setStatus("INBOUND");
                    rec.setHideNumber(rec.getPhoneNumber());
                    rec.setServerType("production");
                    rec.setDialTime(System.currentTimeMillis());
                    rec.setAudioSrc(PreferenceUtil.getSharedPreferences(context, PreferenceUtil.AudioSource, "0"));
                    recordingOption = PreferenceUtil.getSharedPreferences(context, PreferenceUtil.RecordingOption, "0");
                    media = new MediaRecording(context, rec.getPhoneNumber() + "_" + System.currentTimeMillis());
                    if (recordingOption.equals("0")) {
                        media.startRecording(rec.getAudioSrc());
                        rec.setStartTime(System.currentTimeMillis());
                    }
                } else
                    log.debug("Incoming Call Recorder: Call already in Progress. Cant Take new recording request");
            } else
                log.debug("Incoming Call Recorder: Number Missing");
        } catch (Exception e) {
            log.debug("Error Occurred: ",e);
            AppProperties.isCallServiceRunning = false;
            isRecording = false;
            if (media != null && media.recState.equalsIgnoreCase("ON")) {
                media.stopRecording();
                rec.setStopTime(System.currentTimeMillis());
            }
        }
    }

    private void stopCallRec(Context context, String number) {
        try {
            if (isRecording && rec.getPhoneNumber().contains(number)) {
                if (recordingOption.equals("0")) {
                    media.stopRecording();
                    rec.setPath(media.getFilename());
                    rec.setStopTime(System.currentTimeMillis());
                }
                DatabaseHandler dh = new DatabaseHandler(context);
                rec.setId(dh.addRecording(rec));
                AppProperties.isCallServiceRunning = false;
                isRecording = false;
                log.info("Inbound recording stopped, asking for save ?");
                InboundDialog dialog = new InboundDialog(context, rec);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                } else {
                    dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_PHONE);
                }
                dialog.show();
                // Saving Call duration in Rec
                AsyncTask<Recording, Void, String> aTask = new AddRecording(context);
                aTask.execute(rec);
            }
        } catch (Exception e) {
            log.debug("Exception: ", e);
        } finally {
            AppProperties.isCallServiceRunning = false;
            isRecording = false;
            if (media != null && media.recState.equalsIgnoreCase("ON")) {
                media.stopRecording();
                rec.setStopTime(System.currentTimeMillis());
            }
        }
    }

}