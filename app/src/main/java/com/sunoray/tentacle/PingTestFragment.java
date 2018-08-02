package com.sunoray.tentacle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.PreferenceUtil;
import com.sunoray.tentacle.network.NetworkUtil;
import com.sunoray.tentacle.service.TrackerService;
import com.sunoray.tentacle.tasks.EevaluatingApp;
import com.sunoray.tentacle.tasks.SendLogFile;

public class PingTestFragment extends DialogFragment {


	private static final Logger log = LoggerFactory.getLogger(PingTestFragment.class);

	TextView txtInternetConnectionStatus;
	TextView txtPingTest;
	TextView txtTentacleServerTest;
	TextView txtSentDeviceDetail;
	TextView txtShowErrorDetail;

	public static PingTestFragment newInstance() {
		PingTestFragment f = new PingTestFragment();
		f.setCancelable(false);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

		getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

		View v = inflater.inflate(R.layout.dialog_ping_test, container, false);

		txtInternetConnectionStatus = (TextView) v.findViewById(R.id.dialog_ping_txt_internet);
		txtPingTest = (TextView) v.findViewById(R.id.dialog_ping_txt_ping_test);
		txtTentacleServerTest = (TextView) v.findViewById(R.id.dialog_ping_txt_tentacle_server);
		txtSentDeviceDetail = (TextView) v.findViewById(R.id.dialog_ping_txt_sent_detail_to_server);
		txtShowErrorDetail = (TextView) v.findViewById(R.id.dialog_ping_txt_show_error_detail);

		Button btnCancel = (Button) v.findViewById(R.id.dialog_ping_btn_cancel);
		btnCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getDialog().dismiss();
			}
		});
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if(checkInternetConnection()){
			checkPingTest();
			checkTentacleServer();
			sendLogToServer();
			startLocationService();
		}
		resetFlags();

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	private boolean checkInternetConnection() {
		boolean taskStatus = false;
		try {
			ConnectivityManager cm = (ConnectivityManager) getActivity().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo ab = cm.getActiveNetworkInfo();
			if (ab != null) {
				taskStatus = ab.isConnected() ? true : false;
			} else {
				taskStatus = false;
			}
			if(NetworkUtil.isOnline()){
				if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					txtInternetConnectionStatus.setTextColor(getActivity().getResources().getColor(R.color.tentacle_green, getActivity().getTheme()));
				} else {
					txtInternetConnectionStatus.setTextColor(getActivity().getResources().getColor(R.color.tentacle_green));
				}
				txtInternetConnectionStatus.setText(txtInternetConnectionStatus.getText() + " - Success");
			} else {
				txtInternetConnectionStatus.setTextColor(Color.RED);
				txtInternetConnectionStatus.setText(txtInternetConnectionStatus.getText() + " - Fail");
				txtShowErrorDetail.setText("  Check your Internet Connection \n and try again  ");
			}
		} catch (Exception e) {
			log.info("Exception" + e.toString());
		}
		return taskStatus;
	}

	private void checkTentacleServer() {
		try {
			String url = AppProperties.WEB_APP_URL;
			AsyncTask<String, Void, Boolean> aTask = new EevaluatingApp(getActivity().getApplicationContext(),txtTentacleServerTest,txtShowErrorDetail);
			aTask.execute(url);
		} catch (Exception e) {
			log.info("Exception=" + e.toString());
		}
	}

	private void checkPingTest() {
		try {
			String url = AppProperties.MEDIA_SERVER_URL	+ AppProperties.SERVER_NAME_STRING + "/PingTest";
			AsyncTask<String, Void, Boolean> aTask = new EevaluatingApp(getActivity().getApplicationContext(), txtPingTest,txtShowErrorDetail);
			aTask.execute(url);
		} catch (Exception e) {
			log.info("Exception" + e.toString());
		}
	}

	private void sendLogToServer() {
		try {
			AsyncTask<String, Void, Boolean> aTask = new SendLogFile(getActivity().getApplicationContext(), txtSentDeviceDetail);
			aTask.execute();
		} catch (Exception e) {
			log.debug("Exception: " + e);
		}
	}

	private void startLocationService() {
		try {
			if(PreferenceUtil.getSharedPreferences(getActivity().getApplicationContext(), PreferenceUtil.userRole, "").equals("field_exec")) {
	            getActivity().getApplicationContext().startService(new Intent(getActivity().getApplicationContext(), TrackerService.class));
			}
		} catch (Exception e) {
			log.info("Exception" + e.toString());
		}
	}

	private void resetFlags() {
		try {
			AppProperties.activeAlertDialog = false;
			AppProperties.isCallServiceRunning = false;
		} catch (Exception e) {
		}

	}

}