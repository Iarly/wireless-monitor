package com.pmict.wirelessmonitor;

import java.util.List;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.pmict.wirelessmonitor.service.GPSService;
import com.pmict.wirelessmonitor.service.WirelessMonitorService;
import com.pmict.wirelessmonitor.service.entity.MonitorEntity;

import android.support.v4.app.FragmentActivity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;

public class MainActivity extends FragmentActivity {

	private boolean mMapIsTouched;
	private SupportMapFragment mMapView;
    private GoogleMap mMap;
    private ProgressBar spinner;
    private GPSService gpsService;
	       
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mMapIsTouched = true;
                break;
            case MotionEvent.ACTION_UP:
                mMapIsTouched = false;
                break;
        }

        return super.dispatchTouchEvent(ev);

    }
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        this.gpsService = new GPSService(this);
        this.gpsService.registerLocationCallback(new ICallback<Location>() {
			
			@Override
			public void success(Location location) {
				LatLng target = new LatLng(location.getLatitude(), location.getLongitude());
				
				CameraPosition cameraPosition = new CameraPosition.Builder()
				    .target(target)      			// Sets the center of the map to Mountain View
				    .zoom(19)                   	// Sets the zoom
				    //.bearing(90)                	// Sets the orientation of the camera to east
				    .tilt(45)                   	// Sets the tilt of the camera to 30 degrees
				    .build();                   	// Creates a CameraPosition from the builder
				mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
								
				spinner.setVisibility(View.GONE);
			}
			
			@Override
			public void error(Exception e) {
				spinner.setVisibility(View.GONE);				
			}
		});
             
        spinner = (ProgressBar)findViewById(R.id.progressBar1);

        mMapView = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);        
        mMapView.onCreate(savedInstanceState);
        
        mMap = mMapView.getMap();         
        mMap.setMapType(2); // MAP_TYPE_SATELLITE
        //mMap.setBuildingsEnabled(true);
        
        mMap.setOnCameraChangeListener((new OnCameraChangeListener() {			
			@Override
			public void onCameraChange(CameraPosition cameraPosition) {
				if (!mMapIsTouched && !isUpdating) {
					updateMonitors(cameraPosition.target, cameraPosition.zoom);
				}
			}
		}));
        
        MapsInitializer.initialize(this);
    }
	
	protected boolean isUpdating = false;
	
	protected int colorFromDecibeis(double dBm) {
        // dBm to Quality:
        if (dBm <= -70)
            return 0x3FFF0000;
        if (dBm >= -50)
            return 0x3F00FF00;
        return 0x3F7F7F00;
        //quality = 2 * (dBm + 100);
    }
	
	protected void updateMonitors(LatLng position, float zoom) {		
		isUpdating = true;
		WirelessMonitorService service = new WirelessMonitorService();
		service.getMonitors(this, position, zoom, new ICallback<List<MonitorEntity>>() {
			
			@Override
			public void success(final List<MonitorEntity> o) {
				Handler handler = new Handler(Looper.getMainLooper());			
				handler.post(new Runnable() {					
					@Override
					public void run() {	
						mMap.clear();
						
						for (MonitorEntity m : o) {				
							CircleOptions opts = new CircleOptions();
							opts.fillColor(colorFromDecibeis(m.Level));
							opts.center(new LatLng(m.Latitude, m.Longitude));
							opts.radius(m.Radius * 100);					
							mMap.addCircle(opts);
						}
					}
				});
				isUpdating = false;
			}
			
			@Override
			public void error(Exception e) {
				isUpdating = false;
			}
		});		
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_monitor) {
    		startActivity(new Intent(this, MonitorActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
   
    @Override
    protected void onResume() {
    	super.onResume();      
    	mMapView.onResume();  
    	gpsService.onResume();
    }
    
    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

}
