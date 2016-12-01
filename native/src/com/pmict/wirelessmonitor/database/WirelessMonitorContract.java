package com.pmict.wirelessmonitor.database;

import android.provider.BaseColumns;

public final class WirelessMonitorContract {

	public static abstract class AccessPoint implements BaseColumns {
        public static final String TABLE_NAME = "AccessPoint";
        public static final String COLUMN_NAME_BSSID = "BSSID";
    }
	
	public static abstract class AccessPointMonitor implements BaseColumns {
        public static final String TABLE_NAME = "AccessPointMonitor";
        public static final String COLUMN_NAME_ACCESSPOINT_ID = "AccessPointId";
        public static final String COLUMN_NAME_SSID = "SSID";
        public static final String COLUMN_NAME_CAPABILITIES = "Capabilities";
        public static final String COLUMN_NAME_LEVEL = "Level";
        public static final String COLUMN_NAME_FREQUENCY = "Frequency";
        public static final String COLUMN_NAME_TIMESTAMP = "Timestamp";
        public static final String COLUMN_NAME_SYNCHRONIZED = "Synchronized";
        public static final String COLUMN_NAME_LATITUDE = "Latitude";
        public static final String COLUMN_NAME_LONGITUDE = "Longitude";
    }
	
}
