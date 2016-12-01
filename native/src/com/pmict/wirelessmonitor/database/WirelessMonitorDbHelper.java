package com.pmict.wirelessmonitor.database;

import com.pmict.wirelessmonitor.database.WirelessMonitorContract.AccessPoint;
import com.pmict.wirelessmonitor.database.WirelessMonitorContract.AccessPointMonitor;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class WirelessMonitorDbHelper extends SQLiteOpenHelper {

	public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "WirelessMonitor.db";
	
    private static final String SQL_CREATE_ACCESSPOINTS =
    	    "CREATE TABLE " + AccessPoint.TABLE_NAME + " (" +
    	    		AccessPoint._ID + " INTEGER PRIMARY KEY," +
    	    		AccessPoint.COLUMN_NAME_BSSID + " TEXT);";
    
    private static final String SQL_CREATE_ACCESSPOINTMONITORS =
    	    "CREATE TABLE " + AccessPointMonitor.TABLE_NAME + " (" +
    	    		AccessPointMonitor._ID + " INTEGER PRIMARY KEY," +
    	    		AccessPointMonitor.COLUMN_NAME_ACCESSPOINT_ID + " INTEGER, " +
    	    		AccessPointMonitor.COLUMN_NAME_CAPABILITIES + " TEXT, " +
    	    		AccessPointMonitor.COLUMN_NAME_FREQUENCY + " INTEGER, " +
    	    		AccessPointMonitor.COLUMN_NAME_LEVEL + " INTEGER, " +
    	    		AccessPointMonitor.COLUMN_NAME_SSID + " TEXT, " +
    	    		AccessPointMonitor.COLUMN_NAME_SYNCHRONIZED + " TEXT, " +
    	    		AccessPointMonitor.COLUMN_NAME_LATITUDE + " REAL, " +
    	    		AccessPointMonitor.COLUMN_NAME_LONGITUDE + " REAL, " +
    	    		AccessPointMonitor.COLUMN_NAME_TIMESTAMP + " TEXT); ";
    
    private static final String SQL_DELETE_ACCESSPOINT =
    	    "DROP TABLE IF EXISTS " + AccessPoint.TABLE_NAME;
    
    private static final String SQL_DELETE_ACCESSPOINTMONITOR =
    	    "DROP TABLE IF EXISTS " + AccessPointMonitor.TABLE_NAME;
    
    public WirelessMonitorDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SQL_DELETE_ACCESSPOINTMONITOR);
		db.execSQL(SQL_DELETE_ACCESSPOINT);
		db.execSQL(SQL_CREATE_ACCESSPOINTS);
		db.execSQL(SQL_CREATE_ACCESSPOINTMONITORS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(SQL_DELETE_ACCESSPOINTMONITOR);
		db.execSQL(SQL_DELETE_ACCESSPOINT);
        onCreate(db);
	}
	
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
