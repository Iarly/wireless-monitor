package com.pmict.wirelessmonitor.database;

import java.util.LinkedList;
import java.util.List;

import com.pmict.wirelessmonitor.database.WirelessMonitorContract.AccessPoint;
import com.pmict.wirelessmonitor.database.WirelessMonitorContract.AccessPointMonitor;
import com.pmict.wirelessmonitor.database.entity.SyncEntity;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class WirelessMonitorDb {

	public static void insertAccessPointMonitor(Context context, String bSSID, String sSID, String capabilities, 
			int frequency, int level, double timeStamp, double latitude, double longitude) {
				
		WirelessMonitorDbHelper helper = new WirelessMonitorDbHelper(context);
		SQLiteDatabase db = helper.getWritableDatabase();
		
		String[] columns = {AccessPoint._ID, 
				AccessPoint.COLUMN_NAME_BSSID};
		
		Cursor existingBSSIDCursor = db.query(AccessPoint.TABLE_NAME,
				columns, 
				"BSSID = ?", 
				new String[] { bSSID.toString() }, 
				(String)null, 
				(String)null,
				(String)null);
		
		int accessPointId;
		
		if (!existingBSSIDCursor.moveToFirst()) {
			ContentValues values = new ContentValues();
			values.put(AccessPoint.COLUMN_NAME_BSSID, bSSID.toString());			
			accessPointId = (int)db.insert(AccessPoint.TABLE_NAME, null, values);			
		}		
		else {
			accessPointId = existingBSSIDCursor.getInt(0);			
		}
		
		ContentValues values = new ContentValues();
		values.put(AccessPointMonitor.COLUMN_NAME_ACCESSPOINT_ID, accessPointId);
		values.put(AccessPointMonitor.COLUMN_NAME_CAPABILITIES, capabilities.toString());
		values.put(AccessPointMonitor.COLUMN_NAME_FREQUENCY, frequency);
		values.put(AccessPointMonitor.COLUMN_NAME_LEVEL, level);
		values.put(AccessPointMonitor.COLUMN_NAME_SSID, sSID.toString());
		values.put(AccessPointMonitor.COLUMN_NAME_TIMESTAMP, timeStamp);
		values.put(AccessPointMonitor.COLUMN_NAME_SYNCHRONIZED, "N");
		values.put(AccessPointMonitor.COLUMN_NAME_LATITUDE, latitude);
		values.put(AccessPointMonitor.COLUMN_NAME_LONGITUDE, longitude);
		
		db.insert(AccessPointMonitor.TABLE_NAME, null, values);
	}
	
	public static boolean removeSyncedMonitors(Context context, List<SyncEntity> entities) {	
		String where = "";		
		for (int i = 0; i < entities.size(); i++) {
			SyncEntity entity = entities.get(i);
			if (where.length() > 0)
				where += " OR ";
			where += " _id = " + entity.Id;
		}
		
		if (where.length() > 0) {
			WirelessMonitorDbHelper helper = new WirelessMonitorDbHelper(context);
			SQLiteDatabase db = helper.getWritableDatabase();
			db.execSQL("DELETE "
					+ "FROM AccessPointMonitor "
					+ "WHERE " + where);		
			db.close();
			return true;
		}
		return false;
	}
	
	public static List<SyncEntity> getMonitorsToSync(Context context) {

		WirelessMonitorDbHelper helper = new WirelessMonitorDbHelper(context);
		SQLiteDatabase db = helper.getWritableDatabase();
		
		Cursor monitors = db.rawQuery("SELECT m._id, ap.BSSID, m.SSID, m.Frequency, m.Level, m.Capabilities,"
				+ "m.Latitude, m.Longitude "
				+ "FROM AccessPoint ap, AccessPointMonitor m "
				+ "WHERE ap._id = m.AccessPointId AND "
				+ "		m.Synchronized = 'N' "
				+ "ORDER BY m._id "
				+ "LIMIT 50", null);

		List<SyncEntity> entities = new LinkedList<SyncEntity>();
		if (monitors.moveToFirst()) {
			do {
				SyncEntity entity = new SyncEntity();
				entity.Id = monitors.getInt(0);
				entity.BSSID = monitors.getString(1);
				entity.SSID = monitors.getString(2);
				entity.Frequency = monitors.getInt(3);
				entity.Level = monitors.getInt(4);
				entity.Capabilities = monitors.getString(5);				
				entity.Latitude = monitors.getDouble(6);
				entity.Longitude = monitors.getDouble(7);
				entities.add(entity);
			} while(monitors.moveToNext());
			db.close();
			return entities;
		}
		return entities;
	}
	
}
