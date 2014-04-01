package com.cozybit.onbordee.wifi;

import com.cozybit.onbordee.wifi.WifiProvisioner.WifiProvisionerCallback;
import com.cozybit.onbordee.wifi.WifiProvisioner.WifiProvisionerCallback.WifiIfaceState;

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
		/*WifiManager.SCAN_RESULTS_AVAILABLE_ACTION,*/
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

	private void handleWifiStateChangedAction(final Intent intent) {

		int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
				WifiManager.WIFI_STATE_UNKNOWN);
		WifiIfaceState ifaceState = WifiIfaceState.values()[state];
		if(mCallback != null) mCallback.onWifiIfaceStatus(ifaceState);
	}
	
	private void handleNetworkStateChangedAction(final Intent intent) {
		
		NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
		DetailedState state = networkInfo.getDetailedState();
		if(mCallback != null) mCallback.onWifiNetworkStatus(state);
	}
	
	protected void handleSupplicantStateChangedAction(final Intent intent) {
		
		SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
		if(mCallback != null) mCallback.onWifiLinkStatus(state);
	}
}
