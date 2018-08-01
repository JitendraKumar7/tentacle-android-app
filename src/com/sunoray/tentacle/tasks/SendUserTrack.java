package com.sunoray.tentacle.tasks;

import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sunoray.tentacle.bean.Tracker_updates;
import com.sunoray.tentacle.bean.UserTrackJson;
import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.PreferanceUtil;
import com.sunoray.tentacle.db.DatabaseHandler;

@SuppressWarnings("deprecation")
public class SendUserTrack extends AsyncTask<Void, Void, Void> {
	private static final Logger log = LoggerFactory.getLogger(SendUserTrack.class);
	Context context;
	
	public SendUserTrack(Context context) {
		this.context=context;
	}

	protected Void doInBackground(Void... params) {
		try {
			Thread.currentThread().setName("SendUserTrackAsyncTask");							
				log.info("Sending Location in background...");
				List<Tracker_updates> Locationlist = new DatabaseHandler(context).getAllLocation();
				if(Locationlist.size() > 0){
					log.info("Number of Location to push =" + Locationlist.size());					
						
					HttpClient httpclient = new DefaultHttpClient();
					httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT,System.getProperty("http.agent"));
					HttpPost httppost = new HttpPost(AppProperties.WEB_APP_URL+"/tracker_updates");
					try {
						if(!PreferanceUtil.getSharedPreferences(context,PreferanceUtil.userUniqueID,"").equals("")){
							Gson gson =new GsonBuilder().create();
							UserTrackJson utJson=new UserTrackJson();
							utJson.setTracker_updates(Locationlist);
							utJson.setUnique_session_id(PreferanceUtil.getSharedPreferences(context,PreferanceUtil.userUniqueID,""));
							log.info("Final JSON to POSTt: " + gson.toJson(utJson));						
							
							httppost.setEntity(new StringEntity(gson.toJson(utJson)));
							httppost.setHeader("Accept", "application/json");
						    httppost.setHeader("Content-type", "application/json");
	
							// add code to remove data from database base on response.
							HttpResponse response = httpclient.execute(httppost);
							log.info("Location Sync respose Code=" + response.getStatusLine().getStatusCode());					
							if(response.getStatusLine().getStatusCode() == 201){
								log.info("User tracking sent.");
								 new DatabaseHandler(context).removeLocation(Locationlist);
							}else if (response.getStatusLine().getStatusCode() == 500) {
								log.info("User tracking not sent, Response code=" + 500);
							}else if (response.getStatusLine().getStatusCode() == 404) {
								HttpEntity entity = response.getEntity();
								String resp = EntityUtils.toString(entity).trim();
								log.info("User tracking not sent, Got Response code=" + 404 +" | if Response then=" + resp + "  | length of response=" +resp.length());
							    if (resp.length() > 5) 
							    	 new DatabaseHandler(context).removeLocation(Locationlist);   								
							}
						} else {
							log.info("No user Unique id found");
						}

					} catch (Exception e) {
						log.debug("Exception in SendUserTrack: ", e);
					}
				}
		} catch (Exception e) {
			log.debug("Exception: " ,e);
		} 
		return null;
	}	
}