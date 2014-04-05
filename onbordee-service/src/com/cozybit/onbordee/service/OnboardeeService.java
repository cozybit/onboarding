package com.cozybit.onbordee.service;

import com.cozybit.onbordee.manager.ConnectionManager;
import com.cozybit.onbordee.manager.OnboardingManager;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class OnboardeeService extends Service{

	private ConnectionManager mConnectionManager;
	private OnboardingManager mOnboardingManager;

	@Override
	public void onCreate() {
		mConnectionManager = new ConnectionManager(this);
		mOnboardingManager = new OnboardingManager(this);
		mConnectionManager.setDataReceivedCallback( mOnboardingManager.getDataReceivedCallback() );
		mConnectionManager.setOnboardingManager( mOnboardingManager );
		mOnboardingManager.setBleProvisioner( mConnectionManager.getBleProvisioner() );
		
		mConnectionManager.init();
		mOnboardingManager.init();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// If we get killed, after returning from here, restart
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// We don't provide binding, so return null
		return null;
	}

	@Override
	public void onDestroy() {
		mConnectionManager.shutDown();
		mOnboardingManager.shutDown();
		
		mConnectionManager = null;
		mOnboardingManager = null;
	}

}
