package com.pmict.wirelessmonitor.database.entity;

import org.json.JSONException;
import org.json.JSONObject;

public class SyncEntity {

	public int Id;
	public String BSSID;
	public String SSID;
	public int Frequency;
	public int Level;
	public String Capabilities;
	public double Latitude;
	public double Longitude;
	
	@SuppressWarnings("finally")
	public JSONObject toJson() {	
        JSONObject jsonObject = new JSONObject();
        try {
        	jsonObject.accumulate("Id", this.Id);
			jsonObject.accumulate("BSSID", this.BSSID);
	        jsonObject.accumulate("SSID", this.SSID);
	        jsonObject.accumulate("Frequency", this.Frequency);
	        jsonObject.accumulate("Level", this.Level);
	        jsonObject.accumulate("Capabilities", this.Capabilities);
	        jsonObject.accumulate("Latitude", this.Latitude);
	        jsonObject.accumulate("Longitude", this.Longitude);
		} catch (JSONException e) {
		}
        finally {        	
        	return jsonObject;
        }		
	}
	
}
