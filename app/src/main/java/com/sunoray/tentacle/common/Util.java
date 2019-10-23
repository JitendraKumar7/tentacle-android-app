package com.sunoray.tentacle.common;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sunoray.tentacle.helper.StorageHandler;
import com.sunoray.tentacle.network.NetworkUtil;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

public class Util {

    private static final Logger log = LoggerFactory.getLogger(Util.class);

    public static void send2tentacleReceiver(Context context, String msgSender, String msgType, String msgBody) {
        log.info("Msg Broadcasting to MainActivity$TentacleReceiver by " + msgSender);
        Intent intent = new Intent();
        intent.setAction("TANTACLE_MSG_RECEIVER");
        intent.putExtra("MSG_SENDER", msgSender);
        intent.putExtra("MSG_TYPE", msgType);
        intent.putExtra("MSG_BODY", msgBody);
        context.sendBroadcast(intent);
    }

    public static void send2ViewActivity(Context context, String msgSender, String msgType, String callDuration, String dialTime) {
        log.info("Msg Broadcasting to ViewActivity$viewActivityReceiver by " + msgSender);
        Intent intent = new Intent();
        intent.setAction(CommonField.VIEW_ACTIVITY_RECEIVER);
        intent.putExtra("MSG_SENDER", msgSender);
        intent.putExtra("MSG_TYPE", msgType);
        intent.putExtra("CALL_DURATION", callDuration);
        intent.putExtra("DIAL_TIME", dialTime);
        context.sendBroadcast(intent);
    }

    public static boolean isNull(String str) {
        try {
            if (str == null || str.equalsIgnoreCase("") || str.equalsIgnoreCase("null"))
                return true;
            else
                return false;
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean canUpload(Context context) {
        boolean toUpload = false;
        try {
            int netStatus = NetworkUtil.getConnectivityStatus(context);
            int syncPreference = NetworkUtil.getUserSyncPreference(context);
            // Net Status :: 0 - Not Connected | 1 - WiFi | 2- Mobile
            // Sync Preference :: 0 - Anytime | 1 - WiFi only
            if (netStatus != 0) {
                // Connection is ON
                if (syncPreference == 0) {
                    toUpload = true;
                } else if (syncPreference == 1 && netStatus == 2) {
                    toUpload = false;
                } else if (syncPreference == 1 && netStatus == 1) {
                    toUpload = true;
                } else
                    toUpload = false;
            }
            return toUpload;
        } catch (Exception e) {
            return toUpload;
        }
    }

    public static Date addMinToStringDate(String date, int minute) {
        Calendar newDate = Calendar.getInstance();
        try {
            newDate.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(date));
            newDate.add(Calendar.MINUTE, minute);
        } catch (Exception e) {
            log.info("Exception@ addMinToStringDate :", e);
        }
        return newDate.getTime();
    }

    public static int string2Int(String str) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return 0;
        }
    }

    public static HttpURLConnection getConnection(String uri) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(uri);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);            // Allow Inputs
            conn.setDoOutput(true);        // Allow Outputs
            conn.setUseCaches(false);        // Don't use a Cached Copy
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
        } catch (Exception e) {
            log.debug("Exception in getConnection : ", e);
        }
        return conn;
    }

    public static boolean isAfterInterval(String date, int min) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Calendar c = Calendar.getInstance();
            c.setTime(dateFormat.parse(date));
            c.add(Calendar.MINUTE, min);
            if (c.compareTo(Calendar.getInstance()) >= 0)
                return false;
            else
                return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isNumber(String val) {
        try {
            Integer.parseInt(val);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    // this method use for convert number into XXXXXXX123 format
    public static boolean makeNumberHiding(String number) {
        if (number.contains("XX"))
            return true;
        else
            return false;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getFilePathFromURI(final Context context, final Uri uri) {
        log.info("getRealPathFromURI uri=" + uri.getPath());
        String filePath = null;
        try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if (isExternalStorageDocument(uri)) {
                    // ExternalStorageProvider
                    if ("primary".equalsIgnoreCase(type)) {
                        filePath = Environment.getExternalStorageDirectory() + File.separator + split[1];
                    }
                    // TODO handle non-primary volumes
                } else if (isDownloadsDocument(uri)) {
                    // DownloadsProvider
                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                    filePath = getDataColumn(context, contentUri, null, null);
                } else if (isMediaDocument(uri)) {
                    // MediaProvider
                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }
                    filePath = getDataColumn(context, contentUri, "_id=?", new String[]{split[1]});
                }
            } else if ("content".equalsIgnoreCase(uri.getScheme())) {
                // MediaStore (and general)
                filePath = getDataColumn(context, uri, null, null);
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                // File
                filePath = uri.getPath();
            } else {
                filePath = uri.getPath();
            }
        } catch (Exception e) {
            log.debug("Exception in getRealPathFromURI: ", e);
        }
        log.debug("Image FilePath: " + filePath);
        return filePath;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int columnIndex = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(columnIndex);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isJSON(String value) {
        try {
            new JSONObject(value);
        } catch (Exception e) {
            try {
                new JSONArray(value);
            } catch (Exception e2) {
                return false;
            }
        }
        return true;
    }

    public static String checkPhoneNumber(String number) {
        number = number.replaceAll("-", "");                                            // Removing '-' from number
        number = number.replaceAll(" ", "");                                            // Removing space from number
        number = number.length() == 12 && number.startsWith("91") ? "+" + number : number;    // Adding '+' to numbers which starts with 91 and its 12 digit
        number = number.length() == 10 ? "0" + number : number;                            // Adding '0' to numbers if its 10 digit
        return number;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean renameFile(Context context, String oldName, String newName) {
        try {
            File file = StorageHandler.getFileDirPath(context, AppProperties.DEVICE_RECORDING_PATH);
            if (file.exists()) {
                File from = new File(oldName);
                log.info("Renamed Rec FullPath: " + newName);
                File to = new File(newName);
                if (from.exists())
                    return from.renameTo(to);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static int getAlertTheame() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            return android.R.style.Theme_DeviceDefault_Light_Dialog_Alert;
        } else {
            return AlertDialog.THEME_DEVICE_DEFAULT_LIGHT;
        }
    }

    public static boolean externalMemoryAvailable() {
        return android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
    }

    public static long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = 0;
        long availableBlocks = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            blockSize = stat.getBlockSizeLong();
            availableBlocks = stat.getAvailableBlocksLong();
            return availableBlocks * blockSize;
        } else {
            blockSize = stat.getBlockSize();
            availableBlocks = stat.getAvailableBlocks();
            return availableBlocks * blockSize;
        }
    }

    public static long getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = 0;
        long totalBlocks = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            blockSize = stat.getBlockSizeLong();
            totalBlocks = stat.getBlockCountLong();
            return totalBlocks * blockSize;
        } else {
            blockSize = stat.getBlockSize();
            totalBlocks = stat.getBlockCount();
            return totalBlocks * blockSize;
        }
    }

    public static long getAvailableExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = 0;
            long availableBlocks = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                blockSize = stat.getBlockSizeLong();
                availableBlocks = stat.getAvailableBlocksLong();
                return availableBlocks * blockSize;
            } else {
                blockSize = stat.getBlockSize();
                availableBlocks = stat.getAvailableBlocks();
                return availableBlocks * blockSize;
            }
        } else {
            return 0;
        }
    }

    public static long getTotalExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = 0;
            long totalBlocks = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                blockSize = stat.getBlockSizeLong();
                totalBlocks = stat.getBlockCountLong();
                return totalBlocks * blockSize;
            } else {
                blockSize = stat.getBlockSize();
                totalBlocks = stat.getBlockCount();
                return totalBlocks * blockSize;
            }
        } else {
            return 0;
        }
    }

    public static String formatSize(long size) {
        String suffix = null;

        if (size >= 1024) {
            suffix = " KB";
            size /= 1024;
            if (size >= 1024) {
                suffix = " MB";
                size /= 1024;
            }
        }

        StringBuilder resultBuffer = new StringBuilder(Long.toString(size));

        int commaOffset = resultBuffer.length() - 3;
        while (commaOffset > 0) {
            resultBuffer.insert(commaOffset, ',');
            commaOffset -= 3;
        }

        if (suffix != null) resultBuffer.append(suffix);
        return resultBuffer.toString();
    }

    public static long getAvilableRamSize(Context context) {
        ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        Objects.requireNonNull(actManager).getMemoryInfo(memInfo);
        return memInfo.availMem;
    }

    public static long getTotalRamSize(Context context) {
        ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        Objects.requireNonNull(actManager).getMemoryInfo(memInfo);
        return memInfo.totalMem;
    }

    //schedule the start of the service every 10-30seconds
    @TargetApi(Build.VERSION_CODES.M)
    public static void scheduleJob(Context context, int jobId, Class serviceClass, long minDelay, long maxDelay) {
        try {
            ComponentName serviceComponent = new ComponentName(context, serviceClass);
            JobInfo.Builder builder = new JobInfo.Builder(jobId, serviceComponent);

            // wait at least
            builder.setMinimumLatency(minDelay);

            // maximum delay
            builder.setOverrideDeadline(maxDelay);

            // require unmetered network
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);

            // device should be idle
            builder.setRequiresDeviceIdle(true);

            // we don't care if the device is charging or not
            builder.setRequiresCharging(false);

            JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
            Objects.requireNonNull(jobScheduler).schedule(builder.build());
        } catch (Exception e) {
            log.info("Exception in scheduleJob", e);
        }
    }
}