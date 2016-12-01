package com.pmict.wirelessmonitor;

import java.util.List;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pmict.wirelessmonitor.database.WirelessMonitorDb;
import com.pmict.wirelessmonitor.service.GPSService;
import com.pmict.wirelessmonitor.service.WirelessMonitorService;

import android.support.v4.app.FragmentActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MonitorActivity extends FragmentActivity {

	private SupportMapFragment mMapView;
	private GoogleMap mMap;
	private Marker currPosMarker;
	private ProgressBar spinner;
	private Button btnSend;
	private LatLng lastPosition;
	private GPSService gpsService;
	private TextView lbAccuracy;
	private boolean markertDragged = false;

	private void initializeWiFiListener() {
		String connectivity_context = Context.WIFI_SERVICE;
		final WifiManager wifi = (WifiManager) getSystemService(connectivity_context);
		final MonitorActivity me = this;

		/*
		 * if(!wifi.isWifiEnabled()){ if(wifi.getWifiState() !=
		 * WifiManager.WIFI_STATE_ENABLING){ wifi.setWifiEnabled(true); } }
		 */

		spinner.setVisibility(View.VISIBLE);

		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {

				List<ScanResult> results = wifi.getScanResults();
				Log.d("Results", results.toString());

				for (ScanResult scan : results) {
					WirelessMonitorDb.insertAccessPointMonitor(me,
							scan.BSSID.toString(), scan.SSID.toString(),
							scan.capabilities.toString(), scan.frequency,
							scan.level, scan.timestamp, lastPosition.latitude,
							lastPosition.longitude);
				}

				WirelessMonitorService service = new WirelessMonitorService();
				service.syncronize(me, new ICallback<Integer>() {

					@Override
					public void success(Integer o) {
						Handler handler = new Handler(Looper.getMainLooper());
						handler.post(new Runnable() {
							@Override
							public void run() {
								spinner.setVisibility(View.GONE);
							}
						});
					}

					@Override
					public void error(Exception e) {
						Handler handler = new Handler(Looper.getMainLooper());
						handler.post(new Runnable() {
							@Override
							public void run() {
								spinner.setVisibility(View.GONE);
							}
						});
					}

				});

			}
		}, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)); // RSSI_CHANGED_ACTION

		wifi.startScan();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_monitor);

		gpsService = new GPSService(this);
		gpsService.registerLocationCallback(new ICallback<Location>() {
			@Override
			public void success(final Location location) {

				Handler handler = new Handler(Looper.getMainLooper());
				handler.post(new Runnable() {

					@Override
					public void run() {
						LatLng target = new LatLng(location.getLatitude(),
								location.getLongitude());
						lastPosition = target;

						lbAccuracy.setText(getString(R.string.accuracy_info)
								+ " " + location.getAccuracy() + "m");

						CameraPosition cameraPosition = new CameraPosition.Builder()
								.target(target) // Sets the center of the map to
												// Mountain View
								.zoom(19) // Sets the zoom
								// .bearing(90) // Sets the orientation of the
								// camera to east
								.tilt(45) // Sets the tilt of the camera to 30
											// degrees
								.build(); // Creates a CameraPosition from the
											// builder
						mMap.animateCamera(CameraUpdateFactory
								.newCameraPosition(cameraPosition));

						setUpMarker(target);

						spinner.setVisibility(View.GONE);
					}
				});
			}

			@Override
			public void error(Exception e) {
				// TODO Auto-generated method stub

			}
		});

		lbAccuracy = (TextView) findViewById(R.id.accuracy);

		btnSend = (Button) findViewById(R.id.btnSend);
		btnSend.setOnClickListener((new OnClickListener() {
			@Override
			public void onClick(View v) {
				initializeWiFiListener();
			}
		}));

		spinner = (ProgressBar) findViewById(R.id.progressBar1);

		mMapView = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mMapView.onCreate(savedInstanceState);

		mMap = mMapView.getMap();
		mMap.setMapType(2); // MAP_TYPE_SATELLITE
		// mMap.setBuildingsEnabled(true);

		MapsInitializer.initialize(this);
	}

	private void setUpMarker(LatLng crntLocationLatLng) {
		if (!markertDragged) {
			OnMarkerDragListener listener = new OnMarkerDragListener() {
				@Override
				public void onMarkerDragStart(Marker arg0) {
					markertDragged = true;
				}

				@Override
				public void onMarkerDragEnd(Marker arg0) {
					lastPosition = arg0.getPosition();
					mMap.animateCamera(CameraUpdateFactory.newLatLng(arg0
							.getPosition()));
				}

				@Override
				public void onMarkerDrag(Marker arg0) {
				}
			};

			this.mMap.setOnMarkerDragListener(listener);

			if (this.currPosMarker == null)
			this.currPosMarker = this.mMap.addMarker(new MarkerOptions()
					.position(crntLocationLatLng)
					// .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_my_location))
					.draggable(true));
			else 
				this.currPosMarker.setPosition(crntLocationLatLng);
		}
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
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();
		spinner.setVisibility(View.VISIBLE);
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
