package com.pmict.wirelessmonitor.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.consumer.ODataConsumers;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;
import com.pmict.wirelessmonitor.ICallback;
import com.pmict.wirelessmonitor.database.WirelessMonitorDb;
import com.pmict.wirelessmonitor.database.entity.SyncEntity;
import com.pmict.wirelessmonitor.service.entity.MonitorEntity;

public class WirelessMonitorService {

	protected ODataConsumer consumer;
	
	private final String host = "http://192.168.43.237:8080";
	
	public WirelessMonitorService() {
		// create consumer instance
		String serviceUrl = host + "/widb";
		this.consumer = ODataConsumers.create(serviceUrl);	
	}
	
	public void getMonitors(final Context context, final LatLng location, final float zoom, 
			final ICallback<List<MonitorEntity>> callback) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				String latitude = String.valueOf(location.latitude);
				String longitude = String.valueOf(location.longitude);
				String radius = "100";// String.valueOf( 20 - zoom );
				
			    BufferedReader bufferedReader = null;
				try {
			        URL url = new URL(host + "/monitor/get/" + latitude 
			        		+ "/" + longitude 
			        		+ "/" + radius);
			        
			        HttpURLConnection conn;
			        conn = (HttpURLConnection) url.openConnection();
			        conn.setRequestMethod("GET");
			        conn.setRequestProperty("Accept", "application/json");
			        conn.setRequestProperty("Content-type", "application/json");
			        conn.setDoOutput(false);
			        conn.connect();
			        
			        int respCode = conn.getResponseCode();

			        if (respCode == 200) {

				        BufferedReader in = new BufferedReader(
				        		new InputStreamReader(conn.getInputStream()));			        

				        JSONArray array = new JSONArray(in.readLine());
				        
				        List<MonitorEntity> result = new LinkedList<MonitorEntity>();
				        
				        for (int i = 0; i < array.length(); i++) {
				        	JSONObject item = array.getJSONObject(i);
				        	MonitorEntity entity = new MonitorEntity();				        	
				        	JSONObject value = item.getJSONObject("value");

				        	entity.SSID = value.getString("SSID");
				        	entity.BSSID = value.getString("BSSID");
				        	entity.Latitude = value.getDouble("Latitude");
				        	entity.Longitude = value.getDouble("Longitude");
				        	entity.Radius = value.has("Radius") && !value.isNull("Radius") ? 
				        			value.getDouble("Radius") : 0;
				        	entity.Level = value.has("Level") && !value.isNull("Level") ?
				        			value.getDouble("Level") : 0;
				        	
				        	result.add(entity);
				        }
				        
				        callback.success(result);
			        	return;
			        }
			        
			        BufferedReader in = new BufferedReader(
			        		new InputStreamReader(conn.getInputStream()));			        
			        callback.error(new Exception(in.readLine()));
			        return;
			        
			    } catch (MalformedURLException e1) {
			    	callback.error(e1);
			        e1.printStackTrace();
			    } catch (IOException e1) {
			    	callback.error(e1);
			        e1.printStackTrace();
			    } catch (JSONException e) {
			    	callback.error(e);
					e.printStackTrace();
			    }
				catch (Exception e) {
			    	callback.error(e);
					e.printStackTrace();
				} finally {
			        if (bufferedReader != null) {
			            try {
			                bufferedReader.close();
			            } catch (IOException e) {
			            	callback.error(e);
			                e.printStackTrace();
			            }
			        }
			    }		
				
			}
			
		}).start();		
	}
	
	public void syncronize(Context context, ICallback<Integer> callback) {
		final Context cntx = context;
		final ICallback<Integer> cb = callback;
		
		new Thread(new Runnable() {
			public void run() {
		        List<SyncEntity> entities = WirelessMonitorDb.getMonitorsToSync(cntx);
		        JSONArray jsonBatch = new JSONArray();        
		        for (int i = 0; i < entities.size(); i++)
		        	jsonBatch.put(entities.get(i).toJson());
				
			    BufferedReader bufferedReader = null;
			    try {    
			        URL url = new URL(host + "/monitor/batch");
			        HttpURLConnection conn;
			        conn = (HttpURLConnection) url.openConnection();
			        conn.setRequestMethod("POST");
			        conn.setRequestProperty("Accept", "application/json");
			        conn.setRequestProperty("Content-type", "application/json");
			        conn.setDoOutput(true);

			        OutputStream os = null;
			        try {
			            os = conn.getOutputStream();
			            os.write(jsonBatch.toString().getBytes());
			        } catch (Exception e) {
			            e.printStackTrace();
			        }
			        os.close();

			        conn.connect();
			        int respCode = conn.getResponseCode();

			        if (respCode == 200) {
			        	WirelessMonitorDb.removeSyncedMonitors(cntx, entities);
			        	cb.success(entities.size());
			        	return;
			        }
			        
			        BufferedReader in = new BufferedReader(
			        		new InputStreamReader(conn.getInputStream()));			        
			        cb.error(new Exception(in.readLine()));
			        return;
			        
			    } catch (MalformedURLException e1) {
			    	cb.error(e1);
			        e1.printStackTrace();
			    } catch (IOException e1) {
			    	cb.error(e1);
			        e1.printStackTrace();
			    } catch(Exception e) {
			    	cb.error(e);
			    	e.printStackTrace();
			    } finally {
			        if (bufferedReader != null) {
			            try {
			                bufferedReader.close();
			            } catch (IOException e) {
			            	cb.error(e);
			                e.printStackTrace();
			            }
			        }
			    }			
			}
		}).start();
	}
	
}
