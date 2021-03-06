package com.sunoray.tentacle.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import androidx.core.app.ActivityCompat;

import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.PreferenceUtil;
import com.sunoray.tentacle.db.DatabaseHandler;
import com.sunoray.tentacle.helper.BetteryHelper;
import com.sunoray.tentacle.tasks.SendUserTrack;

public final class TrackerService extends Service {

    private static final Logger log = LoggerFactory.getLogger(TrackerService.class);
    private Context context = null;
    boolean isNetworkLocationEnabled = false;    // flag for network status
    boolean canGetLocation = false;                // flag for GPS status
    protected LocationManager locationManager;    // Declaring a Location Manager


    @Override
    public void onCreate() {
        super.onCreate();
        if (PreferenceUtil.getSharedPreferences(getBaseContext(), PreferenceUtil.MIN_TIME_BW_UPDATES, "").equalsIgnoreCase("")) {
            PreferenceUtil.setSharedPreferences(getBaseContext(), PreferenceUtil.MIN_TIME_BW_UPDATES, String.valueOf(AppProperties.DEFAULT_MIN_TIME_BW_UPDATES));
        }
        if (PreferenceUtil.getSharedPreferences(getBaseContext(), PreferenceUtil.MIN_DISTANCE_CHANGE_FOR_UPDATES, "").equalsIgnoreCase("")) {
            PreferenceUtil.setSharedPreferences(getBaseContext(), PreferenceUtil.MIN_DISTANCE_CHANGE_FOR_UPDATES, String.valueOf(AppProperties.DEFAULT_MIN_DISTANCE_BW_UPDATES));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startLocationTracker(intent, startId);
        return START_STICKY;
    }

    private void startLocationTracker(Intent intent, int startId) {

        this.context = getBaseContext();
        try {
            log.info("LocationTracker Service Starting");
            // check do you get new values by sign in
            if (intent.getExtras() != null && intent.getExtras().getString(PreferenceUtil.userUniqueID) != null && intent.getExtras().getString(PreferenceUtil.userRole) != null && intent.getExtras().getString(PreferenceUtil.userID) != null) {
                String uniqueID = intent.getExtras().getString(PreferenceUtil.userUniqueID);
                String role = intent.getExtras().getString(PreferenceUtil.userRole);
                String userEmail = intent.getExtras().getString(PreferenceUtil.userID);

                if (!PreferenceUtil.getSharedPreferences(context, PreferenceUtil.userUniqueID, "").equalsIgnoreCase(uniqueID)) {
                    // remove old record if you got different UserID compare to current UserId.
                    DatabaseHandler db = new DatabaseHandler(context);
                    db.removeLocation(DatabaseHandler.KEY_USERUNIQUEID, PreferenceUtil.getSharedPreferences(context, PreferenceUtil.userUniqueID, ""));
                    log.info("Set to shared preferance. uniqueID = " + uniqueID + " | role = " + role + " | userEmail = " + userEmail);
                    // save new values into shared preference
                    PreferenceUtil.setSharedPreferences(context, PreferenceUtil.userUniqueID, uniqueID);
                    PreferenceUtil.setSharedPreferences(context, PreferenceUtil.userRole, role);
                    PreferenceUtil.setSharedPreferences(context, PreferenceUtil.userID, userEmail);
                }
            }
        } catch (Exception e) {
            log.info("Exception :", e);
        }

        try {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            isNetworkLocationEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (isNetworkLocationEnabled == false) {

                log.info("Network Location Capture is Disable");
                DatabaseHandler db = new DatabaseHandler(context);
                // Set dynamic value in table.
                Map<String, String> deviceInfo = new HashMap<String, String>();
                deviceInfo.put("battery_level", BetteryHelper.getBatteryLevel(context));
                deviceInfo.put("location_service", "off");
                db.addLocation("0.0", "0.0", deviceInfo.toString());

                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm.getActiveNetworkInfo().isConnected()) {
                    AsyncTask<Void, Void, Void> bTask = new SendUserTrack(context);
                    bTask.execute();
                }

            } else {

                this.canGetLocation = true;
                if (isNetworkLocationEnabled) {
                    long MIN_DISTANCE_CHANGE_FOR_UPDATES = PreferenceUtil.getSharedPreferences(getBaseContext(), PreferenceUtil.MIN_DISTANCE_CHANGE_FOR_UPDATES, AppProperties.DEFAULT_MIN_DISTANCE_BW_UPDATES);
                    long MIN_TIME_BW_UPDATES = PreferenceUtil.getSharedPreferences(getBaseContext(), PreferenceUtil.MIN_TIME_BW_UPDATES, AppProperties.DEFAULT_MIN_TIME_BW_UPDATES);
                    log.info("MIN_DISTANCE_CHANGE_FOR_UPDATES=" + MIN_DISTANCE_CHANGE_FOR_UPDATES + " | MIN_TIME_BW_UPDATES=" + MIN_TIME_BW_UPDATES);
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, new LocationTrack());
                }

            }
        } catch (Exception e) {
            log.info("Exception: " + e);
        }
    }

    private class LocationTrack implements LocationListener {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        @Override
        public void onLocationChanged(Location location) {
            try {
                log.info("Capture Location | Lagtitude= " + location.getLatitude() + " | Longitude= " + location.getLongitude());
                DatabaseHandler db = new DatabaseHandler(context);
                Map<String, String> deviceInfo = new HashMap<String, String>();
                deviceInfo.put("battery_level", BetteryHelper.getBatteryLevel(context));
                String deviceInfoString = deviceInfo.toString();
                db.addLocation(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()), deviceInfoString);

                if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected()) {
                    AsyncTask<Void, Void, Void> sendLocationTask = new SendUserTrack(context);
                    sendLocationTask.execute();
                } else {
                    log.info("No Connection to send Location. Location will be sent Later");
                }
            } catch (Exception e) {
                log.info("Exception @ onLocationChanged :", e);
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            log.info("Called onProviderDisabled=" + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            log.info("Called onProviderEnabled=" + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            log.info("Called onStatusChanged= " + provider + " | status= " + status);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}