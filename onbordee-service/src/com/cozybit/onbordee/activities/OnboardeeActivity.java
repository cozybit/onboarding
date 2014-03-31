package com.cozybit.onbordee.activities;

import com.cozybit.onbordee.R;
import com.cozybit.onbordee.service.OnboardeeService;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class OnboardeeActivity extends Activity {

	private Button mStartServiceButton;
	private Button mStopServiceButton;
	private Intent mServiceIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_onboardee);

		mStartServiceButton = (Button) findViewById(R.id.startServiceButton);
		mStopServiceButton = (Button) findViewById(R.id.stopServiceButton);

		mServiceIntent = new Intent(getApplicationContext(), OnboardeeService.class);

		mStartServiceButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startService(mServiceIntent);
			}
		});

		mStopServiceButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stopService(mServiceIntent);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.onboardee, menu);
		return true;
	}

}
