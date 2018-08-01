package com.sunoray.tentacle.db;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.lang.reflect.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sunoray.tentacle.bean.Tracker_updates;
import com.sunoray.tentacle.common.PreferanceUtil;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHandler extends SQLiteOpenHelper {
	
	private static final Logger log =  LoggerFactory.getLogger(DatabaseHandler.class);
	private static final String DATABASE_NAME = "crdb.db";
	private static final int DATABASE_VERSION = 8;

	private static final String TABLE_RECORDINGS = "recordings";
	private static final String TABLE_LOCATION = "location";
	
	private static final String KEY_ID = "id";
	private static final String KEY_CALLID = "callid";
	private static final String KEY_PATH = "record_path";
	private static final String KEY_PH_NO = "phone_number";
	private static final String KEY_HIDE_NO = "hide_number";
	private static final String KEY_CREATED_AT = "created_at";
	private static final String KEY_UPDATED_AT = "updated_at";
	private static final String KEY_DURATION = "duration";
	private static final String KEY_NO_OF_TRIES = "no_of_tries";
	private static final String KEY_STATUS = "status";
	private static final String KEY_AUDIO_SOURCE = "audio_source";
	private static final String KEY_DATA_SENT = "data_sent";
	private static final String KEY_SERVER_TYPE = "server_type";
	private static final String KEY_ACCOUNT_ID = "account_id";
	private static final String KEY_CAMPAIGN_ID = "campaign_id";
	private static final String KEY_PROSPECT_ID = "prospect_id";
	
	private static final String KEY_LATITUDE = "lat";
	private static final String KEY_LONGITUDE = "lng";
	//private static final String KEY_DATE = "date";
	public static final String KEY_PIN = "pin";
	public static final String KEY_USERUNIQUEID = "useruniqueid";
	private static final String KEY_DEVICE_INFO = "device_info";
	private static final String KEY_CAPTURED_AT = "captured_at";
		
	private static SQLiteDatabase db = null;
	private static Cursor cursor = null;
	private Context context;
	
	public DatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context=context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		try{
		
		String CREATE_RECORDINGS_TABLE = "CREATE TABLE " + TABLE_RECORDINGS + "("
				+ KEY_ID + " INTEGER PRIMARY KEY," 
				+ KEY_CALLID + " TEXT UNIQUE,"
				+ KEY_PATH + " TEXT,"
				+ KEY_PH_NO + " TEXT,"
				+ KEY_HIDE_NO + " TEXT,"
				+ KEY_CREATED_AT + " DATETIME,"
				+ KEY_UPDATED_AT + " DATETIME,"
				+ KEY_DURATION + " INTEGER,"
				+ KEY_NO_OF_TRIES + " INTEGER,"
				+ KEY_STATUS + " TEXT," 
				+ KEY_AUDIO_SOURCE + " TEXT,"
				+ KEY_DATA_SENT + " INT,"
				+ KEY_SERVER_TYPE + " TEXT," 
				+ KEY_ACCOUNT_ID + " TEXT,"
				+ KEY_CAMPAIGN_ID + " TEXT,"
				+ KEY_PROSPECT_ID + " TEXT);";
		db.execSQL(CREATE_RECORDINGS_TABLE);
		
		String CREATE_LOCATION_TABLE = "CREATE TABLE " + TABLE_LOCATION + "("
				+ KEY_ID + " INTEGER PRIMARY KEY,"
				+ KEY_USERUNIQUEID + " TEXT,"
				+ KEY_LATITUDE + " TEXT,"
				+ KEY_LONGITUDE + " TEXT,"
				+ KEY_CAPTURED_AT + " DATETIME UNIQUE,"
				+ KEY_DEVICE_INFO + " TEXT );";
		db.execSQL(CREATE_LOCATION_TABLE);
		
		}catch (Exception e) {
				
		}
				
		// Inserting Row
		//Dummy data
		/*for (int i = 0; i < 15; i++) {
			ContentValues recvalues = new ContentValues();
			recvalues.put(KEY_CALLID, "de9d133528a8414581410ced8c81201"+i);
			recvalues.put(KEY_PATH,"/mnt/sdcard/AudioRecorder/de9d133528a8414581410ced8c81201"+i+".3gp");
			recvalues.put(KEY_PH_NO,"976217501"+i);
			recvalues.put(KEY_CREATED_AT,"2014-02-10 18:54:25");
			recvalues.put(KEY_UPDATED_AT," 2014-02-10 18:54:25");
			recvalues.put(KEY_DURATION,i);
			recvalues.put(KEY_NO_OF_TRIES,"1");
			recvalues.put(KEY_STATUS,"SENT");

			db.insert(TABLE_RECORDINGS, null, recvalues);
		}*/
		
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORDINGS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATION);
		// Create tables again
		onCreate(db);
	}		
			
	/**
	 * All CRUD(Create, Read, Update, Delete) Operations for RECORDINGS
	 */

	// Adding new contact
	public void addRecording(Recording recording) {		
		try {
			db = this.getWritableDatabase();

			ContentValues values = new ContentValues();
			values.put(KEY_CALLID, recording.getCallId());
			values.put(KEY_PATH, recording.getPath());
			values.put(KEY_PH_NO, recording.getPhoneNumber());
			values.put(KEY_HIDE_NO, recording.getHideNumber());
			values.put(KEY_NO_OF_TRIES, recording.getNumberOfTries());
			values.put(KEY_STATUS, recording.getStatus());
			values.put(KEY_CREATED_AT, getDateTime());
			values.put(KEY_UPDATED_AT, getDateTime());
			values.put(KEY_DURATION, recording.getDuration());
			values.put(KEY_AUDIO_SOURCE, recording.getAudioSrc());
			values.put(KEY_DATA_SENT, recording.getDataSent());
			values.put(KEY_SERVER_TYPE, recording.getServerType());
			values.put(KEY_ACCOUNT_ID, recording.getAccountId());
			values.put(KEY_CAMPAIGN_ID, recording.getCampaignId());
			values.put(KEY_PROSPECT_ID, recording.getProspectId());
			
			// Inserting Row
			db.insert(TABLE_RECORDINGS, null, values);
		} catch (Exception e) {
			log.debug("Exception: "+e);
		} finally {
			attemptClose(db);
		}
	}
	
	public void updateRecording(Recording recording) {
		try {
			db = this.getWritableDatabase();

			ContentValues values = new ContentValues();
			values.put(KEY_PATH, recording.getPath());
			values.put(KEY_PH_NO, recording.getPhoneNumber());
			values.put(KEY_HIDE_NO, recording.getHideNumber());
			values.put(KEY_NO_OF_TRIES, recording.getNumberOfTries());
			values.put(KEY_STATUS, recording.getStatus());
			values.put(KEY_CREATED_AT, getDateTime());
			values.put(KEY_UPDATED_AT, getDateTime());
			values.put(KEY_DURATION, recording.getDuration());
			values.put(KEY_AUDIO_SOURCE, recording.getAudioSrc());
			values.put(KEY_DATA_SENT, recording.getDataSent());
			values.put(KEY_SERVER_TYPE, recording.getServerType());
			values.put(KEY_ACCOUNT_ID, recording.getAccountId());
			values.put(KEY_CAMPAIGN_ID, recording.getCampaignId());
			values.put(KEY_PROSPECT_ID, recording.getProspectId());
			// Update Row
			db.update(TABLE_RECORDINGS, values,KEY_CALLID + " = ?", new String[]{String.valueOf(recording.getCallId())}); 
		} catch (Exception e) {
			log.debug("Exception @ updateRecording:"+e);
		} finally {
			attemptClose(db);
		}
	}
	
	public boolean checkRecording(String callid) {
		Log.i("DatabaseHandler", "Checking callid");
		boolean recording = false;
		try {
			String[] columns = { KEY_ID };
			db = this.getWritableDatabase();
			cursor = db.query(TABLE_RECORDINGS, columns, KEY_CALLID + " = '" + callid + "'", null, null, null, null);
			// looping through all rows and adding to list
			if (cursor.moveToFirst()) {
				recording = true;
				Log.i("DatabaseHandler", "callid exist: " + recording);
			}
		} catch (Exception e) {
			log.debug("Exception @ checkRecording:",e);
			recording = false;
		} finally {
			attemptClose(cursor);
			attemptClose(db);
		}
		return recording;
	}
	
	// Getting All Contacts
	public Recording getRecording(String callid) {
		
		Recording recording = new Recording();
		try {
			String[] columns = {KEY_ID, KEY_CALLID, KEY_PATH, KEY_PH_NO, KEY_HIDE_NO, KEY_CREATED_AT, 
					KEY_UPDATED_AT, KEY_DURATION, KEY_NO_OF_TRIES, KEY_STATUS, KEY_AUDIO_SOURCE,
					KEY_DATA_SENT, KEY_SERVER_TYPE, KEY_ACCOUNT_ID, KEY_CAMPAIGN_ID ,KEY_PROSPECT_ID };
			db = this.getWritableDatabase();
			cursor = db.query(TABLE_RECORDINGS, columns, KEY_CALLID + " = "+callid, null, null, null, null);
			// looping through all rows and adding to list
			if (cursor.moveToFirst()) {
				do {
					recording.setId(cursor.getInt(0));
					recording.setCallId(cursor.getString(1));
					recording.setPath(cursor.getString(2));
					recording.setPhoneNumber(cursor.getString(3));
					recording.setHideNumber(cursor.getString(4));
					recording.setCreatedAt(cursor.getString(5));
					recording.setUpdatedAt(cursor.getString(6));
					recording.setDuration(cursor.getInt(7));
					recording.setNumberOfTries(cursor.getInt(8));
					recording.setStatus(cursor.getString(9));
					recording.setAudioSrc(cursor.getString(10));
					recording.setDataSent(cursor.getInt(11));
					recording.setServerType(cursor.getString(12));
					recording.setAccountId(cursor.getString(13));
					recording.setCampaignId(cursor.getString(14));
					recording.setProspectId(cursor.getString(15));
					// Adding contact to list
				} while (cursor.moveToNext());
			}	
		} catch (Exception e) {
			log.debug("Exception @ getRecording: " , e);
		} finally {
			attemptClose(cursor);
			attemptClose(db);
		}
		return recording;
	}
	
	// Getting All Contacts
	public List<Recording> getRecordings(String limit) {
		List<Recording> recordingsList = new ArrayList<Recording>();
		try {
			String[] columns = {KEY_ID, KEY_CALLID, KEY_PATH, KEY_PH_NO, KEY_HIDE_NO, KEY_CREATED_AT, KEY_UPDATED_AT, KEY_DURATION, KEY_NO_OF_TRIES, KEY_STATUS, KEY_AUDIO_SOURCE, KEY_DATA_SENT ,KEY_SERVER_TYPE};
			db = this.getWritableDatabase();
			cursor = db.query(TABLE_RECORDINGS, columns, null, null, null, null, KEY_CREATED_AT + " DESC", limit);
			if (cursor.moveToFirst()) {
				do {
					Recording recording = new Recording();
					recording.setId(cursor.getInt(0));
					recording.setCallId(cursor.getString(1));
					recording.setPath(cursor.getString(2));
					recording.setPhoneNumber(cursor.getString(3));
					recording.setHideNumber(cursor.getString(4));
					recording.setCreatedAt(cursor.getString(5));
					recording.setUpdatedAt(cursor.getString(6));
					recording.setDuration(cursor.getInt(7));
					recording.setNumberOfTries(cursor.getInt(8));
					recording.setStatus(cursor.getString(9));
					recording.setAudioSrc(cursor.getString(10));
					recording.setDataSent(cursor.getInt(11));
					recording.setServerType(cursor.getString(12));
					
					
					// Adding contact to list
					recordingsList.add(recording);
				} while (cursor.moveToNext());
			}	
		} catch (Exception e) {
			log.debug("Exception @ getRecordings:",e);
		} finally {
			attemptClose(cursor);
			attemptClose(db);
		}
		return recordingsList;
	}
	
	// Getting All Contacts
	public List<Recording> getAllRecordingsToUpload(String[] status) {
		
		List<Recording> recordingsList = new ArrayList<Recording>();
		try {
			// Select All Query
			String selectQuery = "SELECT  "+ KEY_ID + "," + KEY_CALLID +","+ KEY_PATH +"," + KEY_PH_NO + "," + KEY_HIDE_NO + "," 
					+ KEY_CREATED_AT + "," +
					KEY_UPDATED_AT + "," + KEY_DURATION+ "," + KEY_NO_OF_TRIES + "," + KEY_STATUS + "," + KEY_AUDIO_SOURCE + "," +
					KEY_DATA_SENT+ "," + KEY_SERVER_TYPE + "," + KEY_ACCOUNT_ID +"," + KEY_CAMPAIGN_ID + "," + KEY_PROSPECT_ID
					+ " FROM " + TABLE_RECORDINGS +
					" WHERE " + KEY_STATUS + " IN (?,?,?) ORDER BY "+ KEY_CREATED_AT +" DESC LIMIT 10";
			db = this.getWritableDatabase();
			cursor = db.rawQuery(selectQuery, status);
			// looping through all rows and adding to list
			if (cursor.moveToFirst()) {
				do {
					Recording recordings = new Recording();
					recordings.setId(cursor.getInt(0));
					recordings.setCallId(cursor.getString(1));
					recordings.setPath(cursor.getString(2));
					recordings.setPhoneNumber(cursor.getString(3));
					recordings.setHideNumber(cursor.getString(4));
					recordings.setCreatedAt(cursor.getString(5));
					recordings.setUpdatedAt(cursor.getString(6));
					recordings.setDuration(cursor.getInt(7));
					recordings.setNumberOfTries(cursor.getInt(8));
					recordings.setStatus(cursor.getString(9));
					recordings.setAudioSrc(cursor.getString(10));
					recordings.setDataSent(cursor.getInt(11));
					recordings.setServerType(cursor.getString(12));
					recordings.setAccountId(cursor.getString(13));
					recordings.setCampaignId(cursor.getString(14));
					recordings.setProspectId(cursor.getString(15));
					// Adding contact to list
					recordingsList.add(recordings);				    	 
				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			log.debug("Exception in getAllRecordingsToUpload: ",e);
		} finally {
			attemptClose(cursor);
			attemptClose(db);
		}
		return recordingsList;
	}
	
	// Updating single contact
	public int updateRecStatusNNoOfTries(Recording recording) {
		
		try {
			db = this.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(KEY_STATUS, recording.getStatus());
			values.put(KEY_NO_OF_TRIES, recording.getNumberOfTries());
			values.put(KEY_UPDATED_AT, getDateTime());
			values.put(KEY_DATA_SENT, recording.getDataSent());
			// updating row
			return db.update(TABLE_RECORDINGS, values, KEY_CALLID + " = ?", new String[] {String.valueOf(recording.getCallId()) });	
		} catch (Exception e) {
			log.debug("Exception @ updateRecStatusNNoOfTries: " ,e);
			return 0;
		} finally {
			attemptClose(db);
		}
	}
	
	public int updateRecDuration(Recording recording){
		
		try {
			db = this.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(KEY_DURATION, recording.getDuration());
			values.put(KEY_UPDATED_AT, getDateTime());
			// updating row
			return db.update(TABLE_RECORDINGS, values, KEY_CALLID + " = ?", new String[] { String.valueOf(recording.getCallId()) });	
		} catch (Exception e) {
			log.debug("Exception @ updateRecDuration:" , e);
			return 0;
		} finally {
			attemptClose(db);
		}
	}	
	
	public void deleteRecordings(String offset) {
		
		try {
			String sql = "DELETE FROM recordings WHERE id IN (SELECT id FROM recordings WHERE status IN ('SENT','NO REC') ORDER BY created_at DESC LIMIT "+offset+",50)";
			db = this.getWritableDatabase();
			db.execSQL(sql);
		} catch (Exception e) {
			log.debug("Exception in deleteRecordings: ",e);
		} finally {
			attemptClose(db);
		}
	}	
	
	//location table
	

	// Adding New Location
	public void addLocation(String lat,String lng,String device_info) {		
		try {
			String userUniqueId=PreferanceUtil.getSharedPreferences(context, PreferanceUtil.userUniqueID, "");
			log.info("userUniqueId="+ userUniqueId + " | lat="+ lat +" | lng="+ lng +" | " + KEY_DEVICE_INFO + "=" + device_info);
			db = this.getWritableDatabase();

			ContentValues values = new ContentValues();
			values.put(KEY_USERUNIQUEID, userUniqueId);
			values.put(KEY_LATITUDE, lat);
			values.put(KEY_LONGITUDE, lng);
			values.put(KEY_CAPTURED_AT, getDateTime());			
			values.put(KEY_DEVICE_INFO,device_info);			
			
			// Inserting Row
			long rowId =db.insert(TABLE_LOCATION, null, values);
			log.info(rowId + " Rows inserted in "+ TABLE_LOCATION);
		} catch (Exception e) {
			log.debug("Exceptions @ addLocation :", e);
		} finally {
			attemptClose(db);
		}
	}
				
	// Getting All Locations
	@SuppressWarnings("unchecked")
	public List<Tracker_updates> getAllLocation() {
		
		List<Tracker_updates> trackerList = new ArrayList<Tracker_updates>();
		try {
			String[] columns = {KEY_ID, KEY_USERUNIQUEID, KEY_LONGITUDE, KEY_LATITUDE, KEY_CAPTURED_AT, KEY_DEVICE_INFO};
			db = this.getWritableDatabase();
			cursor = db.query(TABLE_LOCATION, columns, null, null, null, null, KEY_CAPTURED_AT, "50");
			Gson g = new GsonBuilder().create();
			
			Type type =new TypeToken<Map<String,String>>(){}.getType();
			if (cursor.moveToFirst()) {
				do {
					Tracker_updates tu=new Tracker_updates();
						tu.setLng(cursor.getString(2));					
						tu.setLat(cursor.getString(3));				
						tu.setCaptured_at(cursor.getString(4));
						tu.setDevice_info( (Map<String,String>) g.fromJson (cursor.getString(5).trim(),type ) );	
					trackerList.add(tu);
				} while (cursor.moveToNext());
			}	
		} catch (Exception e) {
			log.debug("Exception in getAllLocation: ", e);
			removeLocation(KEY_ID, cursor.getString(0));
		} finally {
			attemptClose(cursor);
			attemptClose(db);
		}
		return trackerList;
	}
	
	public void removeLocation(List<Tracker_updates> trackerList) {
		log.info("Start Deleteing " + trackerList.size() + " rows");
		for (Tracker_updates map : trackerList) {
			try {
				String sql = "DELETE FROM "+TABLE_LOCATION + " WHERE "+ KEY_CAPTURED_AT + "='" + map.getCaptured_at()  + "';";
				db = this.getWritableDatabase();
				db.execSQL(sql);
			} catch (Exception e) {
				log.debug("Exception in deleteRecordings: ",e);
			} finally {
				attemptClose(db);
			}			
		}
		log.info("Delete Successfully");			
	}
	
	public void removeLocation(String clounmName, String value) {
		log.info("Start Deleteing " + clounmName +"="+ value + " rows");
		try {
				String sql = "DELETE FROM "+TABLE_LOCATION + " WHERE "+ clounmName + "='" + value  + "';";
				db = this.getWritableDatabase();
				db.execSQL(sql);
			} catch (Exception e) {
				log.debug("Exception while deleteRecordings: ",e);
			} finally {
				attemptClose(db);
			}
		log.info("Delete Successfully");	
	}
	
	private String getDateTime() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
		Date date = new Date();
		return dateFormat.format(date);
	}
	
	private void attemptClose(SQLiteDatabase db) {
		try {
			if (db != null && db.isOpen())
				db.close();
		} catch (Exception e) {
			log.debug("Error while cloing DB " , e);
		}
	}

	private void attemptClose(Cursor cursor) {
		try {
			if (!(cursor == null || cursor.isClosed()))
				cursor.close();
		} catch (Exception e) {
			log.debug("Error while cloing cursor "+e);
		}
	}
}