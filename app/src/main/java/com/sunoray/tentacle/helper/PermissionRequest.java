package com.sunoray.tentacle.helper;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ANSWER_PHONE_CALLS;
import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.GET_ACCOUNTS;
import static android.Manifest.permission.READ_CALL_LOG;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_CONTACTS;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

// android.permission-group.STORAGE
// LocationField
// android.permission-group.PHONE
// android.permission-group.MICROPHONE


public class PermissionRequest {
    private static final Logger log = LoggerFactory.getLogger(PermissionRequest.class);

    public static final int REQUEST_CAMERA = 0;
    public static final int REQUEST_CONTACTS = 1;
    public static final int REQUEST_LOCATION = 2;
    public static final int REQUEST_MICROPHONE = 3;
    public static final int REQUEST_PHONE = 4;
    public static final int REQUEST_STORAGE = 5;
    public static final int REQUEST_ANSWER_CALL = 6;

    private static String[] PERMISSIONS_CONTACT = {READ_CONTACTS,
            WRITE_CONTACTS,
            GET_ACCOUNTS};

    private static String[] PERMISSIONS_LOCATION = {ACCESS_FINE_LOCATION,
            ACCESS_COARSE_LOCATION};

    private static String[] PERMISSIONS_PHONE = {READ_CALL_LOG,
            CALL_PHONE,
            READ_PHONE_STATE,
            ANSWER_PHONE_CALLS};

    private static String[] PERMISSIONS_STORAGE = {READ_EXTERNAL_STORAGE,
            WRITE_EXTERNAL_STORAGE};

    private static String[] permissionList = new String[]{CALL_PHONE, READ_CALL_LOG,
            READ_PHONE_STATE, ANSWER_PHONE_CALLS, RECORD_AUDIO, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION,
            GET_ACCOUNTS, READ_CONTACTS, CAMERA};

    // ask particular (single) permission and also track it on activity
    // by overriding onRequestPermissionsResult it.
    public static boolean take(Activity context, String permission, final int REQUEST_READ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            log.info("permission " + permission + " is PERMISSION_GRANTED");
            return true;
        }
        if (context.shouldShowRequestPermissionRationale(permission)) {
            log.info("permission" + permission + " is Not granted");
            ActivityCompat.requestPermissions(context, new String[]{permission}, REQUEST_READ);
        } else {
            log.info("permission" + permission + " else");
            ActivityCompat.requestPermissions(context, new String[]{permission}, REQUEST_READ);
        }
        return false;
    }


    // its ask only those permission which not granted.
    public static boolean takeAll(Activity context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        log.info("Taking all permission");
        ActivityCompat.requestPermissions(context, permissionList, 0);
        return false;
    }


    // Call it when permission not granted.
    public static boolean revoked(Activity context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        log.info("Taking all permission");
        ActivityCompat.requestPermissions(context, permissionList, 0);
        return false;
    }


    public static boolean checkCameraPermissions(Activity context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (ActivityCompat.checkSelfPermission(context, CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            return requestCameraPermission(context);
        } else {
            return true;
        }
    }

    private static boolean requestCameraPermission(Activity context) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(context, CAMERA)) {
            ActivityCompat.requestPermissions(context,
                    new String[]{CAMERA},
                    REQUEST_CAMERA);
        } else {
            ActivityCompat.requestPermissions(context, new String[]{CAMERA},
                    REQUEST_CAMERA);
        }
        return true;
    }

    public static boolean checkStoragePermissions(Activity context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (ActivityCompat.checkSelfPermission(context, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return requestStoragePermission(context);
        } else {
            return true;
        }
    }

    private static boolean requestStoragePermission(Activity context) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(context, READ_EXTERNAL_STORAGE)
                && ActivityCompat.shouldShowRequestPermissionRationale(context, WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(context,
                    PERMISSIONS_STORAGE,
                    REQUEST_STORAGE);
        } else {
            ActivityCompat.requestPermissions(context, PERMISSIONS_STORAGE,
                    REQUEST_STORAGE);
        }
        return true;
    }

    public static boolean checkPhonePermissions(Activity context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (ActivityCompat.checkSelfPermission(context, READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, CALL_PHONE) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
            return requestPhonePermission(context);
        } else {
            return true;
        }
    }

    private static boolean requestPhonePermission(Activity context) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(context, READ_CALL_LOG)
                && ActivityCompat.shouldShowRequestPermissionRationale(context, CALL_PHONE)
                && ActivityCompat.shouldShowRequestPermissionRationale(context, READ_PHONE_STATE)
                && ActivityCompat.shouldShowRequestPermissionRationale(context, ANSWER_PHONE_CALLS)) {
            ActivityCompat.requestPermissions(context,
                    PERMISSIONS_PHONE,
                    REQUEST_PHONE);
        } else {
            ActivityCompat.requestPermissions(context, PERMISSIONS_PHONE,
                    REQUEST_PHONE);
        }
        return true;
    }

    public static boolean checkLocationPermissions(Activity context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (ActivityCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return requestLocationPermission(context);
        } else {
            return true;
        }
    }

    private static boolean requestLocationPermission(Activity context) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(context, ACCESS_FINE_LOCATION)
                && ActivityCompat.shouldShowRequestPermissionRationale(context, ACCESS_COARSE_LOCATION)) {
            ActivityCompat.requestPermissions(context,
                    PERMISSIONS_LOCATION,
                    REQUEST_LOCATION);
        } else {
            ActivityCompat.requestPermissions(context, PERMISSIONS_LOCATION,
                    REQUEST_LOCATION);
        }
        return true;
    }

    public static boolean checkRecordPermissions(Activity context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (ActivityCompat.checkSelfPermission(context, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return requestRecordPermission(context);
        } else {
            return true;
        }
    }

    private static boolean requestRecordPermission(Activity context) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(context, RECORD_AUDIO)) {
            ActivityCompat.requestPermissions(context, new String[]{RECORD_AUDIO}, REQUEST_MICROPHONE);
        } else {
            ActivityCompat.requestPermissions(context, new String[]{RECORD_AUDIO}, REQUEST_MICROPHONE);
        }
        return true;
    }

    public static void logAllPermissions(Context context) {
        for (String permission : permissionList)
            log.info(permission.toUpperCase() + " : " + (ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED ? "YES" : "NO"));
    }

}
