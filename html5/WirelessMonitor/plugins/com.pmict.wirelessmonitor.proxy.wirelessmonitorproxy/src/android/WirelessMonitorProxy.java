package com.pmict.wirelessmonitor.proxy;

import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WirelessMonitorProxy extends CordovaPlugin {

	public WirelessMonitorProxy() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public boolean execute(String action, JSONArray args,
			final CallbackContext callbackContext) throws JSONException {		
		if (action.equals("scanWifi")) {
			scanWifi(callbackContext);
			return true;
		}
		return false;		
	}
	
	private void scanWifi(final CallbackContext callbackContext) {
		String connectivity_context = Context.WIFI_SERVICE;
		final Context context = this.webView.getContext();
		final WifiManager wifi = (WifiManager)context.getSystemService(connectivity_context);

		this.webView.getContext().registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				List<ScanResult> results = wifi.getScanResults();
				Log.d("Results", results.toString());

				JSONArray array = new JSONArray();

				try {
					for (ScanResult scan : results) {
						JSONObject jsonScan = new JSONObject();
							jsonScan.accumulate("BSSID", scan.BSSID);
						jsonScan.accumulate("Capabilities", scan.capabilities);
						jsonScan.accumulate("Frequency", scan.frequency);
						jsonScan.accumulate("Level", scan.level);
						jsonScan.accumulate("SSID", scan.SSID);
						//jsonScan.accumulate("Timestamp", scan.timestamp);
						array.put(jsonScan);
					}
				} catch (JSONException e) {
					callbackContext.error(e.getMessage());
					return;
				}
				
				callbackContext.success(array);
				return;
			}
		}, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)); // RSSI_CHANGED_ACTION

		wifi.startScan();
	
	}
	
}
