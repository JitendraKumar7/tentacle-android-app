package com.sunoray.tentacle.service;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.sunoray.tentacle.R;
import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.CommonField;
import com.sunoray.tentacle.common.PreferenceUtil;
import com.sunoray.tentacle.common.Util;
import com.sunoray.tentacle.db.Recording;
import com.sunoray.tentacle.helper.MediaRecording;
import com.sunoray.tentacle.tasks.AddRecording;

import static android.graphics.PixelFormat.TRANSLUCENT;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
import static android.view.WindowManager.LayoutParams.TYPE_PHONE;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;

public class CallService extends Service {

    private static final Logger log = LoggerFactory.getLogger(CallService.class);
    private Context context = null;
    private TelephonyManager telephonyManager = null;
    private PhoneCallListener phoneListener = null;
    private MediaRecording media = null;
    private String recordingOption = null;
    private Recording rec = null;
    private View tDialerView = null;
    private WindowManager wm = null;

    @Override
    public void onCreate() {
        log.info("Service Created");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log.debug("Inside onStartCommand");
        handleStart(intent, startId);
        return START_NOT_STICKY;
    }

    private void handleStart(Intent intent, int startId) {
        try {
            log.info("Service Started. ID: " + startId);
            if (intent != null) {
                AppProperties.isCallServiceRunning = true;    // Flag to Check Call is in Progress or Not
                this.context = this;
                this.rec = intent.getExtras().getSerializable(CommonField.RECORDING) == null ? new Recording() : (Recording) intent.getExtras().getSerializable(CommonField.RECORDING);
                this.rec.setAudioSrc(PreferenceUtil.getSharedPreferences(context, PreferenceUtil.AudioSource, "0"));
                this.recordingOption = PreferenceUtil.getSharedPreferences(context, PreferenceUtil.RecordingOption, "0");
                log.debug("Recorder is ready");
                this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                this.phoneListener = new PhoneCallListener();
                telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
            }
        } catch (Exception e) {
            log.debug("Exception in CallService.onStart: ", e);
            AppProperties.isCallServiceRunning = false;    // Resetting Flag in case of exception
        }
    }

    @SuppressLint("InflateParams")
    private class PhoneCallListener extends PhoneStateListener {

        boolean isPhoneCalling = false;  // To avoid duplicate call recorder

        @SuppressLint({"InlinedApi", "NewApi"})
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            try {
                log.debug("Call Listener new state received: " + state);

                // ------------ Off Hook ----------------
                if (TelephonyManager.CALL_STATE_OFFHOOK == state) {
                    log.info("OFFHOOK");
                    try {
                        if (!isPhoneCalling) {
                            // Hide dialer if hide number is True
                            if (Util.makeNumberHiding(rec.getHideNumber())) {
                                log.debug("Hide number is enabled");
                                openTDialer();
                            }

                            log.info("callid : " + rec.getCallId());
                            media = new MediaRecording(context, rec.getCallId());
                            if (recordingOption.equals("0")) {
                                media.startRecording(rec.getAudioSrc());
                                rec.setStartTime(System.currentTimeMillis());
                            }
                            isPhoneCalling = true;
                        }
                    } catch (Exception e) {
                        log.debug("Exception in call listener state: ", e);
                    }
                }

                // ------------ ringing -----------------
                if (TelephonyManager.CALL_STATE_RINGING == state) {
                    log.info("RINGING number: " + incomingNumber);
                }

                // ------------ disconnected ------------
                if (TelephonyManager.CALL_STATE_IDLE == state) {
                    // run when class initial and phone call ended, need detect flag from CALL_STATE_OFFHOOK
                    log.info("IDLE");

                    if (isPhoneCalling) {
                        try {
                            AppProperties.isCallServiceRunning = false;
                            if (recordingOption.equals("0")) {
                                media.stopRecording();
                                rec.setStopTime(System.currentTimeMillis());
                            }
                            telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_NONE);
                            isPhoneCalling = false;

                            log.info("Recording stopped, adding call log in db");
                            rec.setPath(media.getFilename());
                            AsyncTask<Recording, Void, String> aTask = new AddRecording(context);
                            aTask.execute(rec);
                            log.debug(aTask.get());

                            // Removing HideDialer UI
                            if (Util.makeNumberHiding(rec.getHideNumber())) {
                                closeTDialer();
                            }
                        } catch (Exception e) {
                            log.debug("Exception: ", e);
                        } finally {
                            // Stopping Call Service
                            stopSelf();
                        }
                    }
                }// ------------ disconnected Section Over

            } catch (Exception e) {
                AppProperties.isCallServiceRunning = false;
                isPhoneCalling = false;
                log.debug("Exception (onCallStateChanged): ", e);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        log.info("Service Destroy");
        return null;
    }

    @SuppressLint("InflateParams")
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void openTDialer() {

        // Open Tentacle Dialer
        context.setTheme(R.style.AppTheme);
        wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        // create layout for Dialer
        android.view.WindowManager.LayoutParams wmParams;

        // create layout for Dialer
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            wmParams = new WindowManager.LayoutParams(
                    MATCH_PARENT, MATCH_PARENT, TYPE_APPLICATION_OVERLAY, FLAG_NOT_FOCUSABLE
                    | FLAG_LAYOUT_IN_SCREEN
                    | FLAG_LAYOUT_NO_LIMITS
                    | FLAG_NOT_TOUCH_MODAL
                    | FLAG_LAYOUT_INSET_DECOR, TRANSLUCENT);
        } else {
            wmParams = new WindowManager.LayoutParams(
                    MATCH_PARENT, MATCH_PARENT, TYPE_PHONE, FLAG_NOT_FOCUSABLE
                    | FLAG_LAYOUT_IN_SCREEN
                    | FLAG_LAYOUT_NO_LIMITS
                    | FLAG_NOT_TOUCH_MODAL
                    | FLAG_LAYOUT_INSET_DECOR, TRANSLUCENT);
        }

        // set Dialer
        LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        tDialerView = li.inflate(R.layout.dialer_layout, null);

        // Set Dialer Windows special properties
        tDialerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        // Speaker Button.
        tDialerView.findViewById(R.id.dialer_ibtn_speaker).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AudioManager mAudioMgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        if (mAudioMgr.isSpeakerphoneOn()) {
                            mAudioMgr.setMode(AudioManager.MODE_IN_CALL);
                            mAudioMgr.setSpeakerphoneOn(false);
                            tDialerView.findViewById(R.id.dialer_ibtn_speaker).setBackgroundColor(Color.parseColor("#F5F5F5"));
                        } else {
                            mAudioMgr.setMode(AudioManager.MODE_IN_CALL);
                            mAudioMgr.setSpeakerphoneOn(true);
                            tDialerView.findViewById(R.id.dialer_ibtn_speaker).setBackgroundColor(Color.parseColor("#00c17f"));
                        }
                    }
                }
        );

        // End Call Button.
        tDialerView.findViewById(R.id.dialer_btn_endcall).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((TextView) tDialerView.findViewById(R.id.dialer_txt_status)).setText("Call Ending..");
                        try {
                            log.info("Call disconnect using TDialler");
                            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                            Method m1 = tm.getClass().getDeclaredMethod("getITelephony");
                            m1.setAccessible(true);
                            Object iTelephony = m1.invoke(tm);
                            Method m3 = iTelephony.getClass().getDeclaredMethod("endCall");
                            m3.invoke(iTelephony);
                            Thread.sleep(1000);
                            closeTDialer();
                        } catch (Exception e) {
                            wm.removeView(tDialerView);
                            tDialerView = null;
                            log.info("Exception @ End call", e);
                        }
                    }
                }
        );

        wmParams.gravity = Gravity.TOP;
        // add Dialer UI To ViewActivity  
        wm.addView(tDialerView, wmParams);
        log.debug("tDialer Started");
    }

    public void closeTDialer() {
        try {
            if (tDialerView != null) {
                wm.removeView(tDialerView);
                tDialerView = null;
                log.debug("tDialer Closed");
            }
        } catch (Exception e) {
            log.debug("Exception in closeTDialer", e);
        }
    }

    @Override
    public void onDestroy() {
        log.debug("Call Service is Stopped");
        closeTDialer();
        AppProperties.isCallServiceRunning = false;
        Intent intentService = new Intent(getBaseContext(), BackGroundService.class);
        ContextCompat.startForegroundService(getBaseContext(), intentService);
        if (media != null && media.recState.equalsIgnoreCase("ON")) {
            media.stopRecording();
            rec.setStopTime(System.currentTimeMillis());
        }
        super.onDestroy();
    }

}