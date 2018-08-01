package com.sunoray.tentacle.common;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sunoray.tentacle.network.NetworkUtil;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

public class Util {

	private static final Logger log = LoggerFactory.getLogger(Util.class);
	
	public static void send2tentacleReceiver(Context context,String msgSender, String msgType ,String msgBody) {
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
			if(str == null || str.equalsIgnoreCase("") || str.equalsIgnoreCase("null"))
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
			int syncPrefernce = NetworkUtil.getUserSyncPreferece(context);
			// Net Status :: 0 - Not Connected | 1 - WiFi | 2- Mobile
			// Sync Prefernce :: 0 - Anytime | 1 - WiFi only
			if (netStatus != 0) {
				// Connection is ON
				if (syncPrefernce == 0) {
					toUpload = true;
				} else if (syncPrefernce == 1 && netStatus == 2) {
					toUpload = false;
				} else if (syncPrefernce == 1 && netStatus == 1) {
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
			log.info("Exception@ addMinToStringDate :",e);
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
			conn.setDoInput(true); 			// Allow Inputs
			conn.setDoOutput(true); 		// Allow Outputs
			conn.setUseCaches(false);		// Don't use a Cached Copy
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
		if(number.contains("XX"))
			return true;
		else
			return false;
	}
	
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static String getFilePathFromURI(final Context context, final Uri uri) {
        log.info("getRealPathFromURI uri="+ uri.getPath());
        String filePath = null;
        try {
            if (DocumentsContract.isDocumentUri(context, uri)) {            	
            	final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if (isExternalStorageDocument(uri)) {
                	// ExternalStorageProvider
                    if ("primary".equalsIgnoreCase(type)) {
                    	filePath =  Environment.getExternalStorageDirectory() + File.separator + split[1];
                    }
                    // TODO handle non-primary volumes
                } else if (isDownloadsDocument(uri)) {
                    // DownloadsProvider
                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                    filePath =  getDataColumn(context, contentUri, null, null);
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
                    filePath = getDataColumn(context, contentUri, "_id=?", new String[] { split[1] });
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
			log.debug("Exception in getRealPathFromURI: ",e);
		}
        log.debug("Image FilePath: "+filePath);
        return filePath;
    }

	/**
	 * Get the value of the data column for this Uri. This is useful for
	 * MediaStore Uris, and other file-based ContentProviders.
	 *
	 * @param context The context.
	 * @param uri The Uri to query.
	 * @param selection (Optional) Filter used in the query.
	 * @param selectionArgs (Optional) Selection arguments used in the query.
	 * @return The value of the _data column, which is typically a file path.
	 */
	public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
		Cursor cursor = null;
		final String column = "_data";
		final String[] projection = { column };
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
	
	public static String checkPhoneNumber(String number){
		number = number.replaceAll("-", "");											// Removing '-' from number
		number = number.replaceAll(" ", "");											// Removing space from number
		number = number.length() == 12 && number.startsWith("91")? "+"+number : number;	// Adding '+' to numbers which starts with 91 and its 12 digit
		number = number.length() == 10 ? "0"+number : number;							// Adding '0' to numbers if its 10 digit
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
}