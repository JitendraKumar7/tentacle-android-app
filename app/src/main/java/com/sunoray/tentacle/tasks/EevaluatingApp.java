package com.sunoray.tentacle.tasks;

import java.net.HttpURLConnection;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sunoray.tentacle.R;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.widget.TextView;

public class EevaluatingApp extends AsyncTask<String, Void, Boolean> {
		private static final Logger log = LoggerFactory.getLogger(EevaluatingApp.class);
		private Context context;
		private TextView txt;
		private TextView txtError;				
		
		public EevaluatingApp(Context context,TextView txt,TextView txtError) {
			this.context=context;
			this.txt=txt;
			this.txtError=txtError;			
		}

		@Override
		protected Boolean doInBackground(String... params) {
			boolean taskStatus=false;				
			Thread.currentThread().setName("EevaluatingAppAsyncTask");
			
				if (params.length > 0) {				
					String pingUrl = params[0];
					if(pingUrl != null){						
						try {
							HttpURLConnection connection;
							connection = (HttpURLConnection) new URL(pingUrl).openConnection();
							connection.setRequestMethod("HEAD");							
							if(connection.getResponseCode()>190 && connection.getResponseCode() <300){
								taskStatus=true;
							}else {
								taskStatus=false;
							}
							
						} catch (Exception e) {
							taskStatus=false;
							log.info(e.toString());
						}
					}else {
						log.info("It is Wrong URL");
					}						
				}
			return taskStatus;
		}
		
		@Override
		protected void onPostExecute(Boolean taskStatus) {
			if(taskStatus){
				txt.setTextColor(context.getResources().getColor(R.color.tentacle_green));
				txt.setText(txt.getText() + " - Success");
			}else {
				txt.setTextColor(Color.RED);
				txt.setText(txt.getText() + " - Fail");
				txtError.setText("Please contact us : tech.support@sunoray.com");
			}
		}
}