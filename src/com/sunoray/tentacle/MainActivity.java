package com.sunoray.tentacle;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

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
import android.content.res.Resources.NotFoundException;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.sunoray.tentacle.common.PreferanceUtil;
import com.sunoray.tentacle.db.DatabaseHandler;
import com.sunoray.tentacle.db.Recording;
import com.sunoray.tentacle.layout.TableViewSwipeRefreshLayout;
import com.sunoray.tentacle.network.NetworkUtil;
import com.sunoray.tentacle.service.BackGroundService;
import com.sunoray.tentacle.tasks.GetPendingCall;
import com.sunoray.tentacle.R;

@SuppressLint("RtlHardcoded")
@SuppressWarnings("deprecation")
public class MainActivity extends Activity {
	
	private static final Logger log = LoggerFactory.getLogger(MainActivity.class);
	private TableLayout table;
	private static TextView alert;
	
	BroadcastReceiver tentacleReceiver = new BroadcastReceiver() {		
		@Override
		public void onReceive(Context context, Intent intent) {
			String msgSender = intent.getExtras().getString("MSG_SENDER");
			String msgType = intent.getExtras().getString("MSG_TYPE");
			String msgBody = intent.getExtras().getString("MSG_BODY");
			log.info("TentacleReceiver: New msg from = " + msgSender);
			if (msgType.equals("ERROR")) {
				if (msgBody.equalsIgnoreCase("AUTHENTICATION_FAILED")) {
					alert.setText("Please singin google account");
					alert.setVisibility(View.VISIBLE);
				} else {
					alert.setText(msgBody);
					alert.setVisibility(View.VISIBLE);
				}
			} else if (msgType.equalsIgnoreCase("REFRESH")) {
				displayCallRecodedList();
			}
				
		}
	};
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		table = (TableLayout) findViewById(R.id.table1);
		alert = (TextView) findViewById(R.id.alert);
		findViewById(R.id.main_txt_pulltorefresh).setVisibility(View.VISIBLE);		

		// To Check or Register to GCMServer|WebServer|Network|SDCard|BackgroungDataIsEnable.
		checkAppPrerequisite();
		
		// The swipe refresh facility is working in only android 4+ / API 14+
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			final TableViewSwipeRefreshLayout swipeRefreshLayout = (TableViewSwipeRefreshLayout) findViewById(R.id.main_screen_refreshview);
			swipeRefreshLayout.setColorScheme(R.color.view_progressbar_color, R.color.view_progressbar_color,R.color.view_progressbar_color, R.color.view_progressbar_color);
			swipeRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
				@Override
				public void onRefresh() {
					Toast.makeText(getBaseContext(), "Refresh", Toast.LENGTH_SHORT).show();
					displayCallRecodedList();
					swipeRefreshLayout.setRefreshing(false);
				}
			});
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		IntentFilter tentacleReciverFilter = new IntentFilter("TANTACLE_MSG_RECEIVER");
		registerReceiver(tentacleReceiver, tentacleReciverFilter);
		checkAppPrerequisite();
		// Display available Record in DB.
		displayCallRecodedList();
		// Start background Service.
		startService(new Intent(getBaseContext(), BackGroundService.class));
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(tentacleReceiver);
	}

	private void checkAppPrerequisite() {
		alert.setText("Configuring Device...");
		alert.setVisibility(View.VISIBLE);
		//StorageHelper sh = new StorageHelper();
		ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		AccountManager accMan = AccountManager.get(this);
		Account[] accArray = accMan.getAccountsByType("com.google");
		
		// check Internet connection
		 if (cm.getActiveNetworkInfo() == null) {
			alert.setText("Please Connect to Internet");
			return;
		}
		// check Google account
		else if (android.os.Build.VERSION.SDK_INT < 16 && accArray.length < 1) {
			alert.setText("Please SingIn to Goolgle Account");
			return;
		} else if (!cm.getBackgroundDataSetting()) {
			alert.setText("Please Enable Background Data");
			return;
		} 
		alert.setVisibility(View.GONE);
		return;
	}

	@SuppressLint("NewApi")
	private void displayCallRecodedList() {
		try {

			table.removeAllViews();
			table.setShrinkAllColumns(true);

			final DatabaseHandler db = new DatabaseHandler(this);
			List<Recording> listRecordings = db.getRecordings("50");
			
			if (listRecordings.isEmpty()) {
				TableRow row = new TableRow(this);
				row.setBackgroundColor(Color.rgb(213, 213, 213));

				TextView highlight = new TextView(this);
				highlight.setText(" ");
				highlight.setLayoutParams(new LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT,android.view.ViewGroup.LayoutParams.MATCH_PARENT));
				highlight.setBackgroundColor(Color.rgb(190, 96, 253));
				TextView txt = new TextView(this);
				txt.setGravity(Gravity.CENTER_HORIZONTAL);
				row.addView(highlight);

				txt.setText(" No record found !");
				txt.setTextColor(getResources().getColor(R.color.table_content_text_color));
				row.addView(txt);

				TableLayout.LayoutParams tableRowParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT, 2.0f);
				tableRowParams.setMargins(2, 2, 2, 0);
				row.setLayoutParams(tableRowParams);
				table.addView(row);
			}
			
			for (final Recording recording : listRecordings) {
				TableRow row = new TableRow(this);
				row.setBackgroundColor(Color.rgb(213, 213, 213));

				TextView highlight = new TextView(this);
				highlight.setText(" ");
				highlight.setLayoutParams(new LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT));

				TextView phNo = new TextView(this);
				ImageView statusImage = new ImageView(this);
				phNo.setGravity(Gravity.LEFT);
				phNo.setGravity(Gravity.START);
				phNo.setPadding(0, 5, 0, 5);

				String date = recording.getCreatedAt();
				String formattedDate = new SimpleDateFormat("hh:mma, EEE, MMM dd, yyyy",Locale.getDefault()).format(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault()).parse(date));
				
				SpannableStringBuilder ssb=new SpannableStringBuilder(Html.fromHtml("<b>&nbsp;&nbsp;&nbsp;" + recording.getHideNumber() + "</b><br/>&nbsp;&nbsp;&nbsp;<small>" + formattedDate + "</small>"));
				
				if (recording.getStatus().equalsIgnoreCase("NEW")) {
					highlight.setBackgroundColor(Color.rgb(0, 134, 88));
					statusImage.setImageResource(R.drawable.ic_new);
				} else if (recording.getStatus().equalsIgnoreCase("PENDING")) {
					highlight.setBackgroundColor(Color.rgb(0, 134, 88));
					statusImage.setImageResource(R.drawable.check1);
				} else if (recording.getStatus().equalsIgnoreCase("SENT")) {
					highlight.setBackgroundColor(Color.rgb(0, 134, 88));
					statusImage.setImageResource(R.drawable.check2);
				} else if (recording.getStatus().equalsIgnoreCase("FAIL")) {
					highlight.setBackgroundColor(Color.rgb(0, 134, 88));
					statusImage.setImageResource(R.drawable.ic_new);
				} else if (recording.getStatus().equalsIgnoreCase("NO REC")) {
					highlight.setBackgroundColor(Color.rgb(255, 138, 0));
					statusImage.setImageResource(R.drawable.check1);
				} else {
					highlight.setBackgroundColor(Color.rgb(0, 134, 88));
					statusImage.setImageResource(R.drawable.expired);
				}

				row.addView(highlight);

				phNo.setText(ssb, BufferType.SPANNABLE);
				phNo.setTextColor(getResources().getColor(R.color.table_content_text_color));
				phNo.setLayoutParams(new LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
				row.addView(phNo);

				TextView duration = new TextView(this);
				duration.setGravity(Gravity.RIGHT);
				duration.setGravity(Gravity.END);
				duration.setPadding(0, 0, 15, 0);
				
				duration.setText(Html.fromHtml("<br/><small>" + recording.getDuration() + "secs &nbsp;</small>"));

				duration.setTextColor(getResources().getColor(R.color.table_content_text_color));
				row.addView(duration);

				//statusImage.setLayoutParams(new LayoutParams(R.dimen.staus_icon_width,android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
				row.addView(statusImage);
				
				TableLayout.LayoutParams tableRowParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT, 2.0f);

				tableRowParams.setMargins(2, 2, 2, 0);
				row.setLayoutParams(tableRowParams);
				
				
				table.addView(row);
			}
		} catch (NotFoundException e) {
		} catch (ParseException e) {
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	
	@SuppressLint("NewApi")
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
				
		switch (item.getItemId()) {
		case R.id.opt_Menu_Refresh:
			checkAppPrerequisite();
			displayCallRecodedList();
			startService(new Intent(getBaseContext(), BackGroundService.class));
			return true;

		case R.id.opt_Menu_SyncWiFi:
			final String[] syncOptions = PreferanceUtil.getSyncOptions();
			AlertDialog.Builder syncOptionsBuilder = new AlertDialog.Builder(this);
			syncOptionsBuilder.setTitle("Sync Options");
			int opt = Integer.parseInt(PreferanceUtil.getSharedPreferences(getBaseContext(), PreferanceUtil.SyncOption, "0"));
			syncOptionsBuilder.setSingleChoiceItems(syncOptions, opt, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int position) {
							PreferanceUtil.setSharedPreferences(getBaseContext(), PreferanceUtil.SyncOption, String.valueOf(position));
						}
					});
			syncOptionsBuilder.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Toast.makeText(getBaseContext(),syncOptions[Integer.parseInt(PreferanceUtil.getSharedPreferences(getBaseContext(), PreferanceUtil.SyncOption, "0"))],Toast.LENGTH_SHORT).show();
						}
					});
			AlertDialog syncOptionsAlert = syncOptionsBuilder.create();
			syncOptionsAlert.show();
			return true;
			
		case R.id.opt_Menu_PingToCallStatus:
			try {
				log.info("Get Pending Call Selected");
				if (NetworkUtil.isNetworkAvailable(this)) {
					new GetPendingCall(this).execute();	
				} else {
					Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT).show();
				}
			} catch (Exception e) {
				log.debug("Exception @ opt_Menu_PingToCallStatus: " + e);
			}			
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}
}