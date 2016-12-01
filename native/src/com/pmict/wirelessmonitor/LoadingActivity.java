package com.pmict.wirelessmonitor;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;

public class LoadingActivity extends FragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_loading);
		
		new Handler().postDelayed(new Runnable() {
			@Override 
		    public void run() {
	          	Intent mainIntent = new Intent(LoadingActivity.this, MainActivity.class); 
	          	LoadingActivity.this.startActivity(mainIntent); 
			    LoadingActivity.this.finish(); 
			} 
		}, 200);		
	}
}
