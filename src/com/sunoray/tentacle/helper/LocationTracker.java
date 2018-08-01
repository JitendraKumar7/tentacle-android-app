package com.sunoray.tentacle.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sunoray.tentacle.bean.LocationBean;
import com.sunoray.tentacle.common.PreferanceUtil;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;

public final class LocationTracker implements LocationListener {

	private static final Logger log = LoggerFactory.getLogger(LocationTracker.class);
    private final Context context;
    public boolean isGPSEnabled = false;		// flag for GPS status
    boolean isNetworkEnabled = false;			// flag for network status
    boolean canGetLocation = false;				// flag for GPS status
	Location location = null; 					// location
	
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1;  // The minimum distance to change Updates in meters
    private static final long MIN_TIME_BW_UPDATES = 1;  			// The minimum time between updates in milliseconds
    protected LocationManager locationManager;						// Declaring a Location Manager
	private LocationBean locationbean;

    public LocationTracker(Context context, LocationBean locationbean) {
        this.context = context;
        this.locationbean = locationbean;
    }

    public boolean getLocation() {
        try {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);          
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            log.info("isNetworkEnabled", "=" + isNetworkEnabled +" | "+ "isGPSEnabled", "=" + isGPSEnabled);

            if (isGPSEnabled == false && isNetworkEnabled == false) {
            	if(PreferanceUtil.getSharedPreferences(context, PreferanceUtil.userRole, "field_exec").equalsIgnoreCase("field_exec")){
            		showLocationSettingsAlert(context);
            	} else if(PreferanceUtil.getSharedPreferences(context, PreferanceUtil.locationAlert, "show").equalsIgnoreCase("show")){
            		showLocationSettingsAlert(context);
            		PreferanceUtil.setSharedPreferences(context, PreferanceUtil.locationAlert, "seen");
            	}
            	return false;
            } else {
            	this.canGetLocation = true;
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    log.info("Network location is detected");
                    if (locationManager != null) {
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            locationbean.setLatitude(location.getLatitude());
                            locationbean.setLongitude(location.getLongitude());
                        }
                    }
                }
     
                if (location == null) {
                	if (isGPSEnabled) { // if GPS Enabled get lat/long using GPS Services
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        log.info("GPS Enabled ");
                        if (locationManager != null) {
                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                            	locationbean.setLatitude(location.getLatitude());
                                locationbean.setLongitude(location.getLongitude());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Exception : " + e);
        }
        return true;
    }
   
    public void stopUsingGPS() {
        if (locationManager != null) {
            locationManager.removeUpdates(LocationTracker.this);
        }
    }

    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    public static void showLocationSettingsAlert(final Context context) {
    	
    	new AlertDialog.Builder(context)
    	.setTitle("No location access")
    	.setCancelable(false)
    	.setMessage("Turn this Feature ON from Location Services in your phone setting.")
    	.setPositiveButton("Go to Setting",new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
            })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    
                }
            }).show();
    }

    @Override
    public void onLocationChanged(Location location) {
    	log.info("location=" + location.getLatitude() + location.getLongitude());
    	this.stopUsingGPS();
    	locationbean.setLatitude(location.getLatitude());
        locationbean.setLongitude(location.getLongitude());
    	//Toast.makeText(context,"Latitude=" + location.getLatitude() + " | Longitude="+ location.getLongitude(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
    	showLocationSettingsAlert(context);
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
}