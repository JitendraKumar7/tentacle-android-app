package com.sunoray.tentacle.extraActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;

import android.net.Uri;
import android.os.Bundle;
import com.sunoray.tentacle.ViewActivity;
import com.sunoray.tentacle.common.AppProperties;
import com.sunoray.tentacle.common.CommonField;
import com.sunoray.tentacle.common.Util;
import com.sunoray.tentacle.db.Recording;

public class GCMActivity extends Activity {

    static private final Logger log = LoggerFactory.getLogger(GCMActivity.class);
    private String action = null;
    private Recording rec = null;
    private int alertTheme = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {

            final Bundle extras = this.getIntent().getExtras();
            if (extras != null) {
                action = Util.isNull(extras.getString("action")) ? null : extras.getString("action");

                // --------- Tentacle Call Alert
//                this.setTheme(R.style.AppTheme);


                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    alertTheme = android.R.style.Theme_DeviceDefault_Light_Dialog_Alert;
                } else {
                    alertTheme = AlertDialog.THEME_DEVICE_DEFAULT_LIGHT;
                }

                if (action == null) {
                    rec = (Recording) extras.getSerializable(CommonField.RECORDING);
                    log.info("New Call request (Hybrid) ID: " + rec.getCallId() + " | at:" + rec.getServerType());
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this, alertTheme);
                    // Set alert title
                    alertDialogBuilder.setTitle("Tentacle Call Alert");
                    // Set alert message
                    alertDialogBuilder
                            .setMessage("Call to: " + rec.getHideNumber())
                            .setCancelable(false)
                            .setNegativeButton("Decline", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    log.info("Call declined by uesr");
                                    removeHitQueue(rec.getPhoneNumber());
                                    dialog.cancel();
                                    AppProperties.activeAlertDialog = false;
                                    // Closing this activity
                                    finish();
                                }
                            })
                            .setPositiveButton("Call", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    log.info("Call button pressed");
                                    removeHitQueue(rec.getPhoneNumber());
                                    dialog.cancel();
                                    AppProperties.activeAlertDialog = false;
                                    //startActivity(new Intent(getBaseContext(),com.sunoray.tentacle.MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));

                                    // Starting Call Activity
                                    Intent callActivityIntent = new Intent(getApplicationContext(), com.sunoray.tentacle.extraActivity.CallActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    callActivityIntent.putExtra(CommonField.RECORDING, rec);
                                    startActivity(callActivityIntent);

                                    // Closing this activity
                                    finish();
                                }
                            });
                    // create alert dialog
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                    AppProperties.activeAlertDialog = true;
                } else {
                    if (action.equalsIgnoreCase("kdialer")) {
                        log.info("Custom app invocation from Website (Hybrid calling)");

                        String phone_number = extras.getString("phone_number");
                        String unique_call_id = extras.getString("unique_call_id");
                        String sr_number = extras.getString("sr_number");
                        String api_key = extras.getString("api_key");

                        String url = action + "://" + phone_number
                                + "/" + unique_call_id
                                + "/" + sr_number
                                + "/" + api_key;
                        try {

                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            if (browserIntent.resolveActivity(getPackageManager()) != null)
                                startActivity(browserIntent);
                            else
                                activityNotFoundAlert(this, alertTheme);
                        } catch (ActivityNotFoundException e) {
                            log.info("Exception in sendToKdialer(): ", e);
                            activityNotFoundAlert(this, alertTheme);
                        } catch (Exception e) {
                            log.info("Exception in sendToKdialer(): ", e);
                        }
                    } else {
                        log.info("alert action:" + action);
                        String alertTitle = Util.isNull(extras.getString("title")) ? null : extras.getString("title");
                        String alertContent = Util.isNull(extras.getString("content")) ? null : extras.getString("content");
                        if (alertTitle != null && alertContent != null) {
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this, alertTheme);
                            // set title
                            alertDialogBuilder.setTitle(alertTitle);
                            // set dialog message
                            alertDialogBuilder
                                    .setMessage(alertContent)
                                    .setCancelable(false)
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            log.info("Alert declined");
                                            AppProperties.activeAlertDialog = false;
                                            dialog.cancel();
                                            // Closing this activity
                                            finish();
                                        }
                                    })
                                    .setPositiveButton("Open", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            log.info("Alert Opened");
                                            AppProperties.activeAlertDialog = false;
                                            dialog.cancel();

                                            // Calling View Activity with URL
                                            Intent webViweIntent = new Intent(getApplicationContext(), ViewActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            webViweIntent.putExtra("goTo", "URL");
                                            webViweIntent.putExtra("action", action);
                                            startActivity(webViweIntent);

                                            // Closing this activity
                                            finish();
                                        }
                                    });
                            // create alert dialog
                            AlertDialog alertDialog = alertDialogBuilder.create();
                            alertDialog.show();
                            AppProperties.activeAlertDialog = true;
                        } else {
                            log.debug("title and content can't be null");
                        }
                    }
                }
            } else {
                log.debug("No parameter found to process");
            }
        } catch (Exception e) {
            log.debug("Some Error in alert display: " + e);
            AppProperties.activeAlertDialog = false;
        }
    }

    public static void activityNotFoundAlert(Context context, int alertTheme) {
        try {
            new AlertDialog.Builder(context, alertTheme).setTitle("Telephony Service Not Found")
                    .setMessage("The application configured in custom telephony service for making calls is not found. Make sure application is installed before making the call.")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    }).create().show();
        } catch (Exception e) {
            log.info("Exception in activityNotFoundAlert", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            log.info("onActivityResult", "closing activity");
            AppProperties.activeAlertDialog = false;
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeHitQueue(String call_no) {
        SharedPreferences.Editor editor = getSharedPreferences(AppProperties.QUEUE_FILENAME_STRING, Context.MODE_PRIVATE).edit();
        editor.remove(call_no);
        editor.commit();
    }

}