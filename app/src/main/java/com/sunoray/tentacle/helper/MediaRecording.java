package com.sunoray.tentacle.helper;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.PreferenceUtil;

import android.content.Context;
import android.media.MediaRecorder;
import android.widget.Toast;


public class MediaRecording {

    private static final Logger log = LoggerFactory.getLogger(MediaRecording.class);

    private int output_formats[] = {MediaRecorder.OutputFormat.MPEG_4, MediaRecorder.OutputFormat.THREE_GPP};
    private static final String AUDIO_RECORDER_FILE_EXT_3GP = ".3gp";
    private static final String AUDIO_RECORDER_FILE_EXT_MP4 = ".mp4";

    public static String file_exts[] = {AUDIO_RECORDER_FILE_EXT_MP4, AUDIO_RECORDER_FILE_EXT_3GP};
    public static int currentFormat = 1;

    private MediaRecorder recorder = null;
    private String callId = null;

    public String recState = "IDLE";  // To monitor recording state.
    Context context;

    //private AudioManager audioManager;

    public MediaRecording(Context context, String callId) {
        this.context = context;
        this.callId = callId;
        this.recorder = new MediaRecorder();
        this.recState = "IDLE";
        //this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    /*public MediaRecording(Context context) {
        this.context = context;
        this.recorder = new MediaRecorder();
        this.recState = "IDLE";
    }*/

    public void startRecording(String val) {
        try {
            if (recState.equalsIgnoreCase("ON")) {
                try {
                    recorder.stop();
                } catch (Exception e) {
                }
            }
            try {
                initRecorder(val);
                recorder.prepare();
                log.info("STARTING RECORDING");
                recorder.start();
            } catch (RuntimeException e) {
                if (PreferenceUtil.getAudiosourceItems()[Integer.parseInt(val)].equalsIgnoreCase("VOICE CALL")) {
                    try {
                        log.debug("Voice Call not supported on this device. Switching to Voice Communication");
                        Toast.makeText(context, "Voice Call not supported on this device", Toast.LENGTH_SHORT).show();
                        PreferenceUtil.setSharedPreferences(context, PreferenceUtil.AudioSource, "2");
                        recorder.reset();
                        initRecorder("2");
                        recorder.prepare();
                        recorder.start();
                    } catch (RuntimeException ex) {
                        log.debug("Voice Communication not supported on this device. Switching to MIC");
                        Toast.makeText(context, "Voice Communication not supported on this device", Toast.LENGTH_SHORT).show();
                        PreferenceUtil.setSharedPreferences(context, PreferenceUtil.AudioSource, "0");
                        recorder.reset();
                        initRecorder("0");
                        recorder.prepare();
                        recorder.start();
                    }
                } else if (PreferenceUtil.getAudiosourceItems()[Integer.parseInt(val)].equalsIgnoreCase("VOICE COMMUNICATION")){
                    log.debug("Voice Communication not supported on this device. Switching to MIC");
                    Toast.makeText(context, "Voice Communication not supported on this device", Toast.LENGTH_SHORT).show();
                    PreferenceUtil.setSharedPreferences(context, PreferenceUtil.AudioSource, "0");
                    recorder.reset();
                    initRecorder("0");
                    recorder.prepare();
                    recorder.start();
                } else {
                    log.debug("Not able to start media recorder. May be in use by another app");
                    Toast.makeText(context, "Not able to record call. Recorder is being used by another app", Toast.LENGTH_SHORT).show();
                }
            }
            recState = "ON";
            return;
        } catch (IllegalStateException e) {
            log.debug("StartRecording", "Exception : ", e);
        } catch (RuntimeException e) {
            log.debug("Not able to start media recorder. May be in use by another app");
            Toast.makeText(context, "Not able to record call. Recorder is being used by another app", Toast.LENGTH_SHORT).show();
            log.debug("RuntimeException", e);
        } catch (Exception e) {
            log.debug("Exception Occurred: ", e);
            return;
        }
    }

    private void initRecorder(String audioSource) {
        try {
            // audioManager.setMode(AudioManager.MODE_IN_CALL);
            // audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0);
            if (PreferenceUtil.getAudiosourceItems()[Integer.parseInt(audioSource)].equalsIgnoreCase("MIC")) {
                log.info("Audio source selected : MIC");
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            } else if (PreferenceUtil.getAudiosourceItems()[Integer.parseInt(audioSource)].equalsIgnoreCase("VOICE CALL")) {
                log.info("Audio source selected : VOICE_CALL");
                recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            } else if (PreferenceUtil.getAudiosourceItems()[Integer.parseInt(audioSource)].equalsIgnoreCase("VOICE COMMUNICATION")) {
                log.info("Audio source selected : VOICE_COMMUNICATION");
                recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
            }
            recorder.setOutputFormat(output_formats[currentFormat]);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(getFilename());
            recorder.setOnErrorListener(errorListener);
            recorder.setOnInfoListener(infoListener);
        } catch (Exception e) {
            log.debug("ERROR: ", e);
        }
    }

    public void stopRecording() {
        try {
            if (null != recorder) {
                log.info("STOPPING RECORDING");
                // audioManager.setMode(AudioManager.MODE_NORMAL);
                recorder.stop();
                recorder.reset();
                recorder.release();
                recorder = null;
            }
            recState = "IDLE";
            return;
        } catch (IllegalStateException e) {
            log.debug("stopRecording", "Exception : ", e);
            return;
        } finally {
            recState = "IDLE";
            AppProperties.isCallServiceRunning = false;
        }
    }

    public String getFilename() {
        File file = StorageHandler.getFileDirPath(context, AppProperties.DEVICE_RECORDING_PATH);
        if (!file.exists()) {
            log.info("=" + file.mkdirs());
        }

        String fullPath = file.getAbsolutePath() + "/" + callId + file_exts[currentFormat];
        log.info("Rec FullPath: " + fullPath);
        return (fullPath);
    }

    private MediaRecorder.OnErrorListener errorListener = new MediaRecorder.OnErrorListener() {
        @Override
        public void onError(MediaRecorder mr, int what, int extra) {
            log.debug("");
            log.debug("onError | Error: " + what + " | " + extra);
            if (recState.equalsIgnoreCase("ON")) {
                stopRecording();
            }
        }
    };

    private MediaRecorder.OnInfoListener infoListener = new MediaRecorder.OnInfoListener() {
        @Override
        public void onInfo(MediaRecorder mr, int what, int extra) {
            //log.debug("Warning: " + what + ", " + extra);
        }
    };
}