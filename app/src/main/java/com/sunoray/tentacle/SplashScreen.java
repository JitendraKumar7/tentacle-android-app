package com.sunoray.tentacle;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.tbruyelle.rxpermissions.RxPermissions;

import rx.functions.Action1;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        Thread background = new Thread() {
            public void run() {
                try {
                    sleep(1 * 1000);
                    CheckPermissions();
                } catch (Exception e) {
                }
            }
        };
        // start thread
        background.start();
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
                        Manifest.permission.PROCESS_OUTGOING_CALLS,
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

}