package com.sunoray.tentacle;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v13.app.ActivityCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import static com.sunoray.tentacle.common.AppProperties.WEB_APP_URL;
import static com.sunoray.tentacle.common.AppProperties.APP_TYPE;
import static com.sunoray.tentacle.common.AppProperties.SIGN_UP_PAGE;
import static com.sunoray.tentacle.common.AppProperties.MEDIA_SERVER_URL;
import static com.sunoray.tentacle.common.AppProperties.CAMERA_IMAGE_DIR;
import static com.sunoray.tentacle.common.AppProperties.COMPRESS_IMAGE_DIR;

import com.sunoray.tentacle.common.PreferenceUtil;
import com.sunoray.tentacle.common.Util;
import com.sunoray.tentacle.helper.ImageProcessor;
import com.sunoray.tentacle.helper.PermissionRequest;
import com.sunoray.tentacle.helper.StorageHandler;
import com.sunoray.tentacle.layout.WebViewSwipeRefreshLayout;
import com.sunoray.tentacle.common.CommonField;
import com.sunoray.tentacle.db.Recording;

public class ViewActivity extends Activity {

    private static final Logger log = LoggerFactory.getLogger(ViewActivity.class);
    WebView tentacleBrowser;
    ProgressBar pageLoadingBar;
    ProgressBar viewLoadingBar;
    WebViewSwipeRefreshLayout swipeRefreshLayout;
    TextView callEndBtn;
    boolean doubleBackToExitPressedOnce = false;
    private String currentURL = WEB_APP_URL + "/campaigns"; //"http://192.168.0.11:8080/LocationHistory/welcome.html"; //AppProperties.webAppURL + "/campaigns";
    private ValueCallback<Uri> uploadMessage = null;
    private ValueCallback<Uri[]> uploadMessageArray = null;
    private final static int OPENCAMERA_RESULTCODE = 1001;
    private final static int FILECHOOSER_RESULTCODE = 1002;
    private final static int GALLERYCHOOSER_RESULTCODE = 1003;
    private Uri fileUri = null;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    // Receiver to send Call Status Back to WebApp
    WakefulBroadcastReceiver viewActivityReceiver = new WakefulBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String msgSender = intent.getExtras().getString("MSG_SENDER");
                String msgType = intent.getExtras().getString("MSG_TYPE");
                String callDuration = intent.getExtras().getString("CALL_DURATION");
                String dialTime = intent.getExtras().getString("DIAL_TIME");

                log.info("viewActivityReceiver: New msg from: " + msgSender + " | msgType: " + msgType + " | callDuration: " + callDuration + "| DialTime: " + dialTime);
                if (msgType.equals("CALL_STATUS")) {
                    if (callDuration.equalsIgnoreCase("0"))
                        tentacleBrowser.loadUrl("javascript:dialerCallback('no_answer','" + dialTime + "')");
                    else
                        tentacleBrowser.loadUrl("javascript:dialerCallback('answered','" + dialTime + "')");
                }
            } catch (Exception e) {
                log.info("Exception in viewActivityReceiver:" + e);
            }
        }
    };

    //@SuppressWarnings("deprecation")
    @SuppressLint({"SetJavaScriptEnabled", "NewApi"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // To ignore VM for file URI exposure exception
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        setContentView(R.layout.view_activity);
        this.setTheme(R.style.AppTheme);

        IntentFilter tentacleReceiverFilter = new IntentFilter(CommonField.VIEW_ACTIVITY_RECEIVER);
        registerReceiver(viewActivityReceiver, tentacleReceiverFilter);

        doubleBackToExitPressedOnce = false;
        viewLoadingBar = (ProgressBar) findViewById(R.id.view_fore_progressBar);
        viewLoadingBar.setEnabled(true);

        // WebView debugging mode for chrome for staging only
        tentacleBrowser = (WebView) findViewById(R.id.view_webview_view);

        if (APP_TYPE.equalsIgnoreCase("staging")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }
        // Text Zoom base on Screen Size
        tentacleBrowser.getSettings().setTextZoom(getResources().getInteger(R.integer.view_activity_webview_font));

        // Setting UserAgent
        try {
            tentacleBrowser.getSettings().setUserAgentString(tentacleBrowser.getSettings().getUserAgentString() + " " + "Tentacle/" + String.valueOf(getPackageManager().getPackageInfo(getPackageName(), 0).versionName) + ")");
        } catch (Exception e) {
            tentacleBrowser.getSettings().setUserAgentString(tentacleBrowser.getSettings().getUserAgentString() + " " + "Tentacle/0.0.0");
        }

        tentacleBrowser.addJavascriptInterface(new TentacleJSInterface(this, tentacleBrowser), "tentacle");
        tentacleBrowser.getSettings().setJavaScriptEnabled(true);
        tentacleBrowser.getSettings().setGeolocationEnabled(true);
        tentacleBrowser.getSettings().setDomStorageEnabled(true);
        tentacleBrowser.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        tentacleBrowser.setWebViewClient(new MyBrowser());

        tentacleBrowser.setWebChromeClient(new WebChromeClient() {
            // For progress bar
            public void onProgressChanged(WebView view, int progress) {
                pageLoadingBar.setProgress(progress);
            }

            // FILE BROWSER ==============================================
            // For Android 3.0+
            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
                log.info("openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) Called ");
                openFileChooser(uploadMsg, acceptType, null);
            }

            // for Android 4.1
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                try {
                    uploadMessage = uploadMsg;
                    log.info("acceptType=" + acceptType + "capture=" + capture);
                    if (acceptType.equalsIgnoreCase("Image/*")) {
                        ImageChooserDialog();
                    } else {
                        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType("*/*");
                        ViewActivity.this.startActivityForResult(i, FILECHOOSER_RESULTCODE);
                    }
                } catch (Exception e) {
                    log.debug("Exception in openFileChooser ", e);
                }
            }

            // file upload Android 5.0
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                try {
                    uploadMessageArray = filePathCallback;
                    for (String element : fileChooserParams.getAcceptTypes()) {
                        log.info("acceptType: " + element);
                        if (element.equalsIgnoreCase("Image/*")) {
                            ImageChooserDialog();
                        } else {
                            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                            i.addCategory(Intent.CATEGORY_OPENABLE);
                            i.setType("*/*");
                            ViewActivity.this.startActivityForResult(i, FILECHOOSER_RESULTCODE);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Exception in openFileChooser ", e);
                }
                return true;
            }
        });
        // FILE BROWSER END ========================================

        try {
            String url = currentURL;
            String goTo = Util.isNull(getIntent().getExtras().getString("goTo")) ? "" : getIntent().getExtras().getString("goTo");

            if (getIntent().getData() != null) {
                // if redirected to application form from thirdPartyApp
                Toast.makeText(this, "Redirecting", Toast.LENGTH_SHORT).show();
                url = WEB_APP_URL + getIntent().getData().getPath() + "?" + getIntent().getData().getQuery();
            } else if (goTo.equalsIgnoreCase("signup")) {
                url = WEB_APP_URL + SIGN_UP_PAGE;
            } else if (goTo.equalsIgnoreCase("URL")) {
                url = Util.isNull(getIntent().getExtras().getString("action")) ? currentURL : WEB_APP_URL + getIntent().getExtras().getString("action");
            } else {
                String lastURL = PreferenceUtil.getSharedPreferences(getBaseContext(), "lastURL", currentURL);
                if (!Util.isNull(lastURL)) url = lastURL;
            }

            if (APP_TYPE.equalsIgnoreCase("staging"))
                Toast.makeText(getApplicationContext(), url, Toast.LENGTH_LONG).show();

            tentacleBrowser.loadUrl(url);    //("http://192.168.0.11:8080/LocationHistory/welcome.html");
        } catch (Exception e) {
            log.debug("some error @ url load : ", e);
            tentacleBrowser.loadUrl(WEB_APP_URL);
        }

        swipeRefreshLayout = (WebViewSwipeRefreshLayout) findViewById(R.id.view_swipeprogressBar);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            swipeRefreshLayout.setColorSchemeColors(getColor(R.color.view_progressbar_color),
                    getColor(R.color.view_progressbar_color),
                    getColor(R.color.view_progressbar_color),
                    getColor(R.color.view_progressbar_color));
        } else {
            swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.view_progressbar_color),
                    getResources().getColor(R.color.view_progressbar_color),
                    getResources().getColor(R.color.view_progressbar_color),
                    getResources().getColor(R.color.view_progressbar_color));
        }

        swipeRefreshLayout.setOnRefreshListener(new OnRefreshListener() {

            @Override
            public void onRefresh() {
                Toast.makeText(getBaseContext(), "Refresh", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
                tentacleBrowser.reload();
            }
        });

        pageLoadingBar = (ProgressBar) findViewById(R.id.view_progressBar);
        pageLoadingBar.setMax(100);

    }

    private void ImageChooserDialog() {
        try {
            new AlertDialog.Builder(ViewActivity.this)
                    .setTitle("Choose Image Source")
                    .setCancelable(isFinishing())
                    .setItems(new CharSequence[]{"Gallery", "Camera"},
                            new DialogInterface.OnClickListener() {
                                @TargetApi(Build.VERSION_CODES.KITKAT)
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case 0:
                                            if (ActivityCompat.checkSelfPermission(ViewActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                                                PermissionRequest.checkStoragePermissions(ViewActivity.this);
                                            else
                                                openGallery();
                                            break;
                                        case 1:
                                            if (ActivityCompat.checkSelfPermission(ViewActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                                                PermissionRequest.checkCameraPermissions(ViewActivity.this);
                                            else
                                                openCamera();
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            }
                    ).show();
        } catch (Exception e) {
            log.debug("Exception in ImageChooserDialog ",e);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionRequest.REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                uploadMessageArray.onReceiveValue(null);
                uploadMessageArray = null;
                Toast.makeText(this, "No access to camera", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == PermissionRequest.REQUEST_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                uploadMessageArray.onReceiveValue(null);
                uploadMessageArray = null;
                Toast.makeText(this, "No access to storage", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openGallery() {
        try {
            log.info("Image Source = Gallery ");
            Intent galleryIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);
            galleryIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            galleryIntent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            galleryIntent.setType("image/*");
            startActivityForResult(galleryIntent, GALLERYCHOOSER_RESULTCODE);
        } catch (Exception e) {
            log.debug("Error while opening Gallery: ", e);
        }
    }

    private void openCamera() {
        try {
            log.info("Image Source = Camera ");
            File imageStorageDir = StorageHandler.getFileDirPath(ViewActivity.this, CAMERA_IMAGE_DIR);
            File file = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
            fileUri = Uri.fromFile(file);
            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            startActivityForResult(cameraIntent, OPENCAMERA_RESULTCODE);
        } catch (Exception e) {
            log.debug("Error while opening Camera: ", e);
        }
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce || currentURL.contains("users/sign_in")) {
            super.onBackPressed();
            return;
        }

        doubleBackToExitPressedOnce = true;
        tentacleBrowser.goBack();

        final Toast toast = Toast.makeText(this, "Press again to go Home", Toast.LENGTH_SHORT);
        toast.show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
                toast.cancel();
            }
        }, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(viewActivityReceiver);
        PreferenceUtil.setSharedPreferences(getBaseContext(), "lastURL", currentURL);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(viewActivityReceiver);
        super.onDestroy();
    }

    private class MyBrowser extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            if (APP_TYPE.equalsIgnoreCase("staging"))
                Toast.makeText(getApplicationContext(), url, Toast.LENGTH_LONG).show();

            if (url.contains("tentacle://call.tentaclecrm.com")) {
                try {
                    Recording rec = new Recording();
                    // Extracting all parameters
                    url = url.replace("tentacle://call.tentaclecrm.com/", "");
                    rec.setPhoneNumber(url.substring(0, url.indexOf("/")));
                    url = url.replace(rec.getPhoneNumber() + "/", "");
                    rec.setCallId(url.substring(0, url.indexOf("/")));
                    url = url.replace(rec.getCallId() + "/", "");

                    rec.setAccountId(url.contains("/") ? url.substring(0, url.indexOf("/")) : "");
                    url = url.replaceFirst(rec.getAccountId() + "/", "");
                    rec.setCampaignId(url.contains("/") ? url.substring(0, url.indexOf("/")) : "");
                    url = url.replaceFirst(rec.getCampaignId() + "/", "");
                    rec.setProspectId(url.contains("/") ? url.substring(0, url.indexOf("/")) : "");
                    url = url.replaceFirst(rec.getProspectId() + "/", "");

                    Uri parseUri = Uri.parse(url);
                    String numberFlag = parseUri.getQueryParameter("hide_number");

                    if (numberFlag != null && numberFlag.equalsIgnoreCase("true")) {
                        rec.setHideNumber("XXXXXXXX" + rec.getPhoneNumber().substring(rec.getPhoneNumber().length() - 2, rec.getPhoneNumber().length()));
                    } else {
                        rec.setHideNumber(rec.getPhoneNumber());
                    }

                    rec.setServerType((url.length() > 1 && url.contains("/")) ? url.substring(0, url.indexOf("/")) : "production");
                    log.info("New Call request using MobileApp, ID: " + rec.getCallId() + " | type: " + rec.getServerType() + " | AccountID: " + rec.getAccountId() + " | Campaign: " + rec.getCampaignId() + " | prospectID: " + rec.getProspectId());

                    // Starting Call Activity
                    Intent callActivityIntent = new Intent(getApplicationContext(), com.sunoray.tentacle.extraActivity.CallActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    callActivityIntent.putExtra(CommonField.RECORDING, rec);
                    startActivity(callActivityIntent);

                    return true;
                } catch (Exception e) {
                    log.info("Exception:", e);
                    return true;
                }
            } else if (url.contains("tel:")) {
                try {
                    Intent viewMsgIntent = new Intent(getBaseContext(), com.sunoray.tentacle.extraActivity.MsgViewActivity.class);
                    viewMsgIntent.putExtra("showType", "view_tel_alert");
                    viewMsgIntent.putExtra("finalurl", url);
                    startActivity(viewMsgIntent);
                } catch (Exception e) {
                    log.debug("Exception :" + e);
                }
                return true;
            } else if (url.contains("tentacle.sunoray.com/retry")) {
                url = currentURL;
                view.loadUrl(url);
                return true;
            } else if (url.contains(MEDIA_SERVER_URL)
                    || url.contains(WEB_APP_URL)
                    || url.contains("tentacle.sunoray.net")
                    || url.contains("tentacle.sunoray.com")
                    || url.contains("app.sunoray.com")
                    || url.contains("tentaclecrm.herokuapp.com")) {
                currentURL = url;
                view.loadUrl(url);
                return true;
            } else {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
                return true;
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            pageLoadingBar.setVisibility(View.VISIBLE);
            viewLoadingBar.setEnabled(false);
            viewLoadingBar.setVisibility(View.GONE);
            tentacleBrowser.setVisibility(View.VISIBLE);
            findViewById(R.id.view_txt_pulltorefresh).setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            pageLoadingBar.setVisibility(View.GONE);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            view.loadUrl("file:///android_asset/error.html");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            log.info("requestCode=" + requestCode + " | resultCode=" + resultCode);

            Uri resultUri = null;
            if (requestCode == FILECHOOSER_RESULTCODE && resultCode != 0) {
                resultUri = (data == null || resultCode != RESULT_OK) ? null : data.getData();
            } else if (requestCode == OPENCAMERA_RESULTCODE && resultCode == RESULT_OK && fileUri != null) {
                File imageStorageDir = StorageHandler.getFileDirPath(ViewActivity.this, COMPRESS_IMAGE_DIR);
                File file = new File(imageStorageDir + File.separator + "IMGC_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
                resultUri = Uri.fromFile(file);
                ImageProcessor.Rescale(fileUri, resultUri);
            } else if (requestCode == GALLERYCHOOSER_RESULTCODE && resultCode != 0) {
                Uri result = (data == null || resultCode != RESULT_OK) ? null : data.getData();
                String path = Util.getFilePathFromURI(getApplicationContext(), result);
                result = Uri.fromFile(new File(path));

                File imageStorageDir = StorageHandler.getFileDirPath(ViewActivity.this, COMPRESS_IMAGE_DIR);
                File file = new File(imageStorageDir + File.separator + "IMGG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
                resultUri = Uri.fromFile(file);
                ImageProcessor.Rescale(result, resultUri);
            }

            // returning URI to WebView.
            if (resultUri != null) {
                log.info("result=" + resultUri.getPath());
                if (android.os.Build.VERSION.SDK_INT < 20) {
                    uploadMessage.onReceiveValue(resultUri);
                } else {
                    uploadMessageArray.onReceiveValue(new Uri[]{resultUri});
                }
                log.debug("File added");
                Toast.makeText(getApplicationContext(), "File added", Toast.LENGTH_SHORT).show();
            } else {
                // Setting NULL in case user press back without selecting file
                uploadMessageArray.onReceiveValue(null);
                uploadMessageArray = null;
            }
        } catch (Exception e) {
            log.info("Exception (onActivityResult): ", e);
        }
    }
}