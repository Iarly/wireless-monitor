package com.pmict.wirelessmonitor.service;

import java.util.LinkedList;
import java.util.List;

import com.pmict.wirelessmonitor.ICallback;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

public class GPSService {

    private LocationManager locationManager = null; 
	
    private Location lastLocation = null;
    
	private List<ICallback<Location>> locationCallbacks = new LinkedList<ICallback<Location>>();
	
	/**
	* Time difference threshold set for one minute.
	*/
	static final int TIME_DIFFERENCE_THRESHOLD = 1 * 60 * 1000;
	
	boolean isBetterLocation(Location oldLocation, Location newLocation) {
	    // If there is no old location, of course the new location is better.
	    if(oldLocation == null) 
	        return true;
	 
	    // Check if new location is newer in time.
	    boolean isNewer = newLocation.getTime() > oldLocation.getTime();	 
	    // Check if new location more accurate. Accuracy is radius in meters, so less is better.
	    boolean isMoreAccurate = newLocation.getAccuracy() < oldLocation.getAccuracy();       
	    
	    if(isMoreAccurate && isNewer) {         
	        // More accurate and newer is always better.         
	        return true;     
	    } else if(isMoreAccurate && !isNewer) {         
	        // More accurate but not newer can lead to bad fix because of user movement.         
	        // Let us set a threshold for the maximum tolerance of time difference.         
	        long timeDifference = newLocation.getTime() - oldLocation.getTime(); 
	 
	        // If time difference is not greater then allowed threshold we accept it.         
	        if(timeDifference > -TIME_DIFFERENCE_THRESHOLD) {
	            return true;
	        }
	    }
	 
	    return false;
	}
	
	public GPSService(Context context) {
		locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);		
	}
	
	private final LocationListener gpsLocationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
            case LocationProvider.AVAILABLE:
            	Log.w("LocationProvider.AVAILABLE", "GPS available again");
                break;
            case LocationProvider.OUT_OF_SERVICE:
            	Log.w("LocationProvider.OUT_OF_SERVICE", "GPS out of service");
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
            	Log.w("LocationProvider.TEMPORARILY_UNAVAILABLE", "GPS temporarily unavailable");
                break;
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
        	Log.w("LocationProvider.onProviderEnabled", "GPS Provider Enabled");
        }

        @Override
        public void onProviderDisabled(String provider) {
        	Log.w("LocationProvider.onProviderDisabled", "GPS Provider Disabled");
        }

        @Override
        public void onLocationChanged(Location location) {
        	if (isBetterLocation(lastLocation, location)) {
	            locationManager.removeUpdates(networkLocationListener);            
	            for (ICallback<Location> cb : locationCallbacks)
	            	cb.success(location);
	            lastLocation = location;
        	}
        }
    };

    private final LocationListener networkLocationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
            case LocationProvider.AVAILABLE:
            	Log.w("LocationProvider.AVAILABLE", "Network location available again");
                break;
            case LocationProvider.OUT_OF_SERVICE:
            	Log.w("LocationProvider.OUT_OF_SERVICE", "Network location out of service");
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
            	Log.w("LocationProvider.TEMPORARILY_UNAVAILABLE", "Network location temporarily unavailable");
                break;
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
        	Log.w("LocationProvider.onProviderEnabled", "Network location Provider Enabled");
        }

        @Override
        public void onProviderDisabled(String provider) {
        	Log.w("LocationProvider.onProviderDisabled", "Network location Provider Disabled");
        }

        @Override
        public void onLocationChanged(Location location) {
        	if (isBetterLocation(lastLocation, location)) {           
	            for (ICallback<Location> cb : locationCallbacks)
	            	cb.success(location);
	            lastLocation = location;
        	}
        }
    };
	
    public void registerLocationCallback(ICallback<Location> cb) {
    	this.locationCallbacks.add(cb);
    }
    
    public void onResume() {    	
    	locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5, networkLocationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 5, gpsLocationListener);    	
    }
    
}
