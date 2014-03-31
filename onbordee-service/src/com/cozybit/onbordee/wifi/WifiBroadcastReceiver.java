package com.cozybit.onbordee.wifi;

import com.cozybit.onbordee.utils.Log;
import com.cozybit.onbordee.wifi.WifiProvisioner.WifiProvisionerCallback;
import com.cozybit.onbordee.wifi.WifiProvisioner.WifiProvisionerCallback.WifiIfaceStatus;
import com.cozybit.onbordee.wifi.WifiProvisioner.WifiProvisionerCallback.WifiNetworkStatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;

public class WifiBroadcastReceiver extends BroadcastReceiver {

	public final static String TAG = WifiBroadcastReceiver.class.getName();
	
	public Context mContext;
	private IntentFilter mIntentFilter;
	private static String[] actions = { 
		WifiManager.NETWORK_STATE_CHANGED_ACTION,
		WifiManager.SUPPLICANT_STATE_CHANGED_ACTION,
		WifiManager.SCAN_RESULTS_AVAILABLE_ACTION,
		WifiManager.WIFI_STATE_CHANGED_ACTION 
	};
	//private IManager mConnMngr;
	private WifiProvisionerCallback mCallback;
	
	public WifiBroadcastReceiver(Context context, WifiProvisionerCallback callback) {
		mContext = context;
		mCallback = callback;
		mIntentFilter = new IntentFilter();
		for (String action : actions)
			mIntentFilter.addAction(action);
	}
	
	public void init() {
		mContext.registerReceiver(this, mIntentFilter);
	}
	
	public void stop() {
		mContext.unregisterReceiver(this);
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		//TODO not 100% sure about skipping old intents
		if( !isInitialStickyBroadcast() ) { 
			String action = intent.getAction();
			Log.d(TAG, "Intent received -> action: %s", action);
			
			// Handle all events
			if(intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
				handleNetworkStateChangedAction(intent);
			} else if (intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
				handleSupplicantStateChangedAction(intent);
			} else if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
				//handleScanResultsAvailableAction();
			} else if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				handleWifiStateChangedAction(intent);
			}
		}
	}
	
	private boolean arrayContains(int[] arrayStates, int state) {
		for (Integer i : arrayStates) {
			if (i.equals(state))
				return true;
		}
		return false;
	}

	private void handleWifiStateChangedAction(final Intent intent) {

		int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
				WifiManager.WIFI_STATE_UNKNOWN);

		if (state == WifiManager.WIFI_STATE_ENABLED) {
			if(mCallback != null) mCallback.onWifiIfaceStatus(WifiIfaceStatus.ON);
			Log.d(TAG, "Client Wifi has been enabled");
		} else if (state == WifiManager.WIFI_STATE_DISABLED) {
			if(mCallback != null) mCallback.onWifiIfaceStatus(WifiIfaceStatus.OFF);
			Log.d(TAG, "Client Wifi has been disabled");
		}
	}
	
	private void handleNetworkStateChangedAction(final Intent intent) {
		
		NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
		DetailedState state = networkInfo.getDetailedState();
		Log.d(TAG, "The current state of the network is: %s", state.name() );
		switch (state) {
		case OBTAINING_IPADDR:
			if(mCallback != null) mCallback.onWifiNetworkStatus(WifiNetworkStatus.OBTAINING_IPADDR);
			break;
		
		case CONNECTED:
			if(mCallback != null) mCallback.onWifiNetworkStatus(WifiNetworkStatus.CONNECTED);
			break;

		case DISCONNECTED:
			if(mCallback != null) mCallback.onWifiNetworkStatus(WifiNetworkStatus.DISCONNECTED);
			break;
			
		case FAILED:
			if(mCallback != null) mCallback.onWifiNetworkStatus(WifiNetworkStatus.FAILED);
			break;
		}
	}
	
	protected void handleSupplicantStateChangedAction(final Intent intent) {
		
		SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
		Log.d(TAG, "Supplicant state: %s", state);
		switch (state) {
		case ASSOCIATING:
			if(mCallback != null) mCallback.onWifiNetworkStatus(WifiNetworkStatus.ASSOCIATING);
			break;
			
		case AUTHENTICATING:
			//this one is never executed
			if(mCallback != null) mCallback.onWifiNetworkStatus(WifiNetworkStatus.AUTHENTICATING);
			break;
		}
		
	}

	/*protected void handleScanResultsAvailableAction() {

		if (mState != State.SCANNING &&
				mState != State.CONNECTING) {
			Log.w(TAG, "Ignoring handleScanResultsAvailableAction() on " + mState + " state");
			return;
		}

		WifiConfiguration wifiConf = generateWifiConfiguration(mConfiguration);
		mPendingScan = false;

		if (isWifiNetworkAvailable(wifiConf)) {
			mState = State.CONNECTING;
			int networkId = mWifiManager.addNetwork(wifiConf);
			if (networkId != -1) {
				mState = State.CONNECTING;
				mOldNetworkId = mWifiManager.getConnectionInfo().getNetworkId();
				if (mWifiManager.enableNetwork(networkId, true)) {
					mNetworkId = networkId;
					Log.d(TAG, "Enabled client ProximityNetwork configuration");
				} else {
					mState = State.FAILED;
					Log.e(TAG, "Provisioner failed enabling the client ProximityNetwork configuration");
				}
			}
		} else {
				Log.d(TAG, "Requesting scan to find valid Proximity network");
				requestScan();
		}
	}

	protected void requestScan() {
		mScanCounter++;
		mPendingScan = true;
		mWifiManager.startScan();

		// Check after 3 seconds for the scan result
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (mPendingScan == true)
					requestScan();
			}
		}, MSEC_WAIT_FOR_SCAN_RESULT);
	}

	protected void handleSupplicantStateChangedAction(final Intent intent) {

		if (mState != State.CONNECTING &&
				mState != State.CONNECTED) {
			Log.w(TAG, "Ignoring handleSupplicantStateChangedAction() on " + mState + " state");
			return;
		}

		DetailedState detailedStateOf = WifiInfo.getDetailedStateOf((SupplicantState)
				intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE));

		if (mWifiManager != null) {
			if (detailedStateOf.equals(DetailedState.DISCONNECTED) ||
					detailedStateOf.equals(DetailedState.FAILED)) {
				if (mState == State.CONNECTING &&
						mWifiManager.getConnectionInfo().getSSID().equals(PROXIMITY_NETWORK_SSID)) {
					if (mAuthCounter >= MAX_AUTH_ATTEMPTS) {
						mAuthCounter = 0;
						Log.d(TAG, "Could not auth with AP, enabling AP mode");
						enableApMode(generateWifiConfiguration(mConfiguration));
					} else {

						 //Android Wireless system will retry to connect
						 //automatically, just keep a counter of attempts
						Log.d(TAG, "Retrying auth with AP");
						mAuthCounter++;
					}
				} else if (mState == State.CONNECTED) {
					Log.d(TAG, "ProximityNetwork has been lost, scanning medium");
					mProvisionerListener.onDisconnected(getProximityInterface());
					releaseConfiguration();
					mState = State.SCANNING;
					requestScan();
				}
			}
		}
	}*/
}
