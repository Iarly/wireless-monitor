package com.pmict.wirelessmonitor.service.entity;

import org.json.JSONException;
import org.json.JSONObject;

public class MonitorEntity {
	public String BSSID;
	public String SSID;
	public double Latitude;
	public double Longitude;
	public double Radius;
	public double Level;	
	
	@SuppressWarnings("finally")
	public JSONObject toJson() {	
        JSONObject jsonObject = new JSONObject();
        try {
        	jsonObject.accumulate("BSSID", this.BSSID);
			jsonObject.accumulate("SSID", this.SSID);
	        jsonObject.accumulate("Latitude", this.Latitude);
	        jsonObject.accumulate("Longitude", this.Longitude);
	        jsonObject.accumulate("Radius", this.Radius);
	        jsonObject.accumulate("Level", this.Level);
		} catch (JSONException e) {
		}
        finally {        	
        	return jsonObject;
        }		
	}
	
}
