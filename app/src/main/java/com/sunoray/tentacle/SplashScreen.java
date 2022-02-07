package com.sunoray.tentacle;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.crashlytics.android.Crashlytics;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.File;

import io.fabric.sdk.android.Fabric;
import rx.functions.Action1;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        Fabric.with(this, new Crashlytics());

        if(isRooted()){
            Thread background = new Thread() {
                public void run() {
                    try {
                        sleep(1 * 1000);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            CheckPermissionsWithAnswerCall();
                        else
                            CheckPermissions();
                    } catch (Exception e) {
                    }
                }
            };
            // start thread
            background.start();
//            AlertDialog.Builder builder = new AlertDialog.Builder(SplashScreen.this);
//            builder.setTitle("Alert");
//            builder.setMessage("This device is Rooted. You can't use this app!");
//
//            builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    SplashScreen.this.finishAffinity();
//                }
//            });
//            builder.show();
        }
        else{
            Thread background = new Thread() {
                public void run() {
                    try {
                        sleep(1 * 1000);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            CheckPermissionsWithAnswerCall();
                        else
                            CheckPermissions();
                    } catch (Exception e) {
                    }
                }
            };
            // start thread
            background.start();
        }

    }

    public static boolean findBinary(String binaryName) {
        boolean found = false;
        if (!found) {
            String[] places = { "/sbin/", "/system/bin/", "/system/xbin/",
                    "/data/local/xbin/", "/data/local/bin/",
                    "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/" };
            for (String where : places) {
                if (new File(where + binaryName).exists()) {
                    found = true;

                    break;
                }
            }
        }
        return found;
    }
    private static boolean isRooted() {
        return findBinary("su");
    }

    public void initialize(boolean isAppInitialized) {
        if (isAppInitialized) {
            Intent i = new Intent(getBaseContext(), StartupActivity.class);
            startActivity(i);
            finish();
        } else {
            /* If one Of above permission not grant show alert (force to grant permission)*/
            AlertDialog.Builder builder = new AlertDialog.Builder(SplashScreen.this);
            builder.setTitle("Alert");
            builder.setMessage("All permissions are required by Tentacle to work");
            builder.setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    CheckPermissions();
                }
            });
            builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            builder.show();
        }
    }

    void CheckPermissions() {
        /*android.Manifest.permission.WRITE_EXTERNAL_STORAGE,*/
        RxPermissions.getInstance(SplashScreen.this)
                .request(
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.WRITE_CALL_LOG,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.GET_ACCOUNTS,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        initialize(aBoolean);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {

                    }
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    void CheckPermissionsWithAnswerCall() {
        /*android.Manifest.permission.WRITE_EXTERNAL_STORAGE,*/
        RxPermissions.getInstance(SplashScreen.this)
                .request(
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.WRITE_CALL_LOG,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.GET_ACCOUNTS,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ANSWER_PHONE_CALLS
                )
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        initialize(aBoolean);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {

                    }
                });
    }

}