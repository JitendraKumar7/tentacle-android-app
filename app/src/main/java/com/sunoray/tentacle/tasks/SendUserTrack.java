package com.sunoray.tentacle.tasks;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sunoray.tentacle.bean.Tracker_updates;
import com.sunoray.tentacle.bean.UserTrackJson;
import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.PreferenceUtil;
import com.sunoray.tentacle.db.DatabaseHandler;
import com.sunoray.tentacle.network.HttpServices;

import okhttp3.Response;

@SuppressWarnings("deprecation")
public class SendUserTrack extends AsyncTask<Void, Void, Void> {
	private static final Logger log = LoggerFactory.getLogger(SendUserTrack.class);
	Context context;

	public SendUserTrack(Context context) {
		this.context = context;
	}

	protected Void doInBackground(Void... params) {
		try {
			Thread.currentThread().setName("SendUserTrackAsyncTask");
			log.info("Sending Location in background...");
			List<Tracker_updates> Locationlist = new DatabaseHandler(context).getAllLocation();
			if (Locationlist.size() > 0) {
				log.info("Number of Location to push =" + Locationlist.size());


				try {
					if (!PreferenceUtil.getSharedPreferences(context, PreferenceUtil.userUniqueID, "").equals("")) {
						Gson gson = new GsonBuilder().create();
						UserTrackJson utJson = new UserTrackJson();
						utJson.setTracker_updates(Locationlist);
						utJson.setUnique_session_id(PreferenceUtil.getSharedPreferences(context, PreferenceUtil.userUniqueID, ""));
						log.info("Final JSON to POSTt:" + gson.toJson(utJson));

						HttpServices httpServices = new HttpServices();
						Response response = httpServices.postJson(AppProperties.WEB_APP_URL + "/tracker_updates", gson.toJson(utJson));
						log.info("Response : " + response);

						// add code to remove data from database base on response.
						int respCode = response.code();
						log.info("Location Sync respose Code=" + respCode);
						if (respCode == 201) {
							log.info("User tracking sent.");
							new DatabaseHandler(context).removeLocation(Locationlist);
						} else if (respCode == 500) {
							log.info("User tracking not sent, Response code=" + 500);
						} else if (respCode == 404) {
							String respMsg = response.message();
							log.info("User tracking not sent, Got Response code=" + 404 + " | if Response then=" + respMsg + "  | length of response=" + respMsg.length());
							if (respMsg.length() > 5)
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
			log.debug("Exception: ", e);
		}
		return null;
	}
}