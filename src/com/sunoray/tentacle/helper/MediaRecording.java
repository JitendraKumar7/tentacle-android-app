package com.sunoray.tentacle.helper;

import java.io.File;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.PreferanceUtil;
import com.sunoray.tentacle.helper.StorageHandler;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.widget.Toast;


public class MediaRecording {
	
	private MediaRecorder recorder = null;
	private int output_formats[] = { MediaRecorder.OutputFormat.MPEG_4, MediaRecorder.OutputFormat.THREE_GPP };
	
	private static final String AUDIO_RECORDER_FILE_EXT_3GP = ".3gp";
	private static final String AUDIO_RECORDER_FILE_EXT_MP4 = ".mp4";
	
	private String file_exts[] = { AUDIO_RECORDER_FILE_EXT_MP4, AUDIO_RECORDER_FILE_EXT_3GP };
	
	private int currentFormat = 1;
	private String callid = null;
	private static final Logger log =  LoggerFactory.getLogger(MediaRecording.class);
	
	public String recState = "IDLE";  // To monitor recording state.
	Context context;
	
	public MediaRecording(Context context,String callid) {
		this.context = context;
		this.callid = callid;
		this.recorder = new MediaRecorder();
		this.recState = "IDLE";
	}
	
	public void startRecording(String val) {
		try {
			if (recState.equalsIgnoreCase("ON")) {
				try {recorder.stop();} catch (Exception e){	}
			}
			try {
				initRecorder(val);
				recorder.prepare();
				log.info("STARTING RECORDING");
				recorder.start();
			} catch (RuntimeException e) {
				if (PreferanceUtil.getAudiosourceItems()[Integer.parseInt(val)].equalsIgnoreCase("VOICE CALL")) {
					log.debug("Voice Call not supported on this device. Switching to MIC");
					Toast.makeText(context, "Voice Call not supported on this device", Toast.LENGTH_SHORT).show();
					PreferanceUtil.setSharedPreferences(context, PreferanceUtil.AudioSource, "0");
					recorder.reset();
					initRecorder("0");
					recorder.prepare();
					recorder.start();
				}
			}
			recState = "ON";
			return;
		} catch (IllegalStateException e) {
			log.debug("StartRecording","Exception : ",e);
		} catch (RuntimeException e) {
			log.debug("RuntimeException",e);
		} catch (Exception e) {
			log.debug("Exception Occured: ",e);			
			return;
		}
	}
	
	private void initRecorder(String val) {
		try {
			if (PreferanceUtil.getAudiosourceItems()[Integer.parseInt(val)].equalsIgnoreCase("MIC")) {
				log.info("Audio source selected : MIC");
				recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			} else if(PreferanceUtil.getAudiosourceItems()[Integer.parseInt(val)].equalsIgnoreCase("VOICE CALL")) {
				log.info("Audio source selected : VOICE_CALL");
				if(Build.MANUFACTURER.toLowerCase(Locale.ENGLISH).contains("samsung") || Build.MANUFACTURER.toLowerCase(Locale.ENGLISH).contains("lg")) 
					recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
				else
					recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
			}
			recorder.setOutputFormat(output_formats[currentFormat]);
			recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			recorder.setOutputFile(getFilename());
			recorder.setOnErrorListener(errorListener);
			recorder.setOnInfoListener(infoListener);	
		} catch (Exception e) {
			log.debug("ERROR: ",e);
		}
	}

	public void stopRecording() {
		try {
			if (null != recorder) {
				log.info("STOPPING RECORDING");
				recorder.stop();
				recorder.reset();
				recorder.release();
				recorder = null;
			}
			recState = "IDLE";
			return;
		} catch (IllegalStateException e) {
			log.debug("stopRecording","Exception : " ,e);
			return;
		} finally {
			AppProperties.isCallServiceRunning = false;
		}
	}
	
	public String getFilename(){
		
		File file = StorageHandler.getFileDirPath(context , AppProperties.DEVICE_RECORDING_PATH);
		if (!file.exists()) {
			log.info("="+file.mkdirs());
		}
		
		String fullPath = file.getAbsolutePath() + "/" + callid + file_exts[currentFormat]; 
		log.info("Rec FullPath: "+fullPath);
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