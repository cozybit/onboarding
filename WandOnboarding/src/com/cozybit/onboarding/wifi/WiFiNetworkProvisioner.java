package com.cozybit.onboarding.wifi;

import java.lang.reflect.Method;
import java.util.List;

import com.cozybit.onboarding.utils.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;

public class WiFiNetworkProvisioner implements INetworkProvisioner {

	private final static String TAG = WiFiNetworkProvisioner.class.getName();

	
	private static final int MSEC_WAIT_FOR_SCAN_RESULT = 5000;
	private static final int MAX_AUTH_ATTEMPTS = 2;

	private Listener mProvisionerListener;
	private Handler mHandler;
	private Context mContext;
	private WifiManager mWifiManager;
	//private int mNetworkId = -1;
	private int mOldNetworkId;

	private int mAuthCounter = 0;
	
	private WiFiNetwork mWiFiNetwork;
	private volatile WifiConfiguration mConfiguration;

	private enum State {
		DISABLED, 		// Provisioner not started or released
		INITIALIZED, 	// Provisioner started and configured
		SCANNING, 		// Wifi interface is ON and scanning
		CONNECTING, 	// SSID found trying to connect
		CONNECTED, 		// Connected as client
		FAILED;			// Provisioner has failed, we need to restart wifi provisioner
	}

	private State mState = State.DISABLED;

	// BroadcastReceiver
	private BroadcastReceiver mClientReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			// Handle all events
			if(intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
				handleNetworkStateChangedAction(intent);
			} else if (intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
				handleSupplicantStateChangedAction(intent);
			} else if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
				handleScanResultsAvailableAction();
			} else if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				handleWifiStateChangedAction(intent);
			}
		}

		protected void handleWifiStateChangedAction(final Intent intent) {

			if (mState == State.DISABLED ||
					mState == State.FAILED) {
				Log.w(TAG, "Ignoring handleWifiStateChangedAction() on " + mState + " state");
				return;
			}

			int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
					WifiManager.WIFI_STATE_UNKNOWN);

			if (state == WifiManager.WIFI_STATE_ENABLED) {
				Log.d(TAG, "Client Wifi is enabled, requesting scan");
				mState = State.SCANNING;
				requestScan();
			} else if (state == WifiManager.WIFI_STATE_DISABLED) {
				Log.d(TAG, "Client Wifi has been disabled");

				/* 
				 * If the current state is not ENABLING_AP 
				 * and is already initialized 
				 */
				if (mState.compareTo(State.INITIALIZED) > 0) {
					Log.e(TAG, "Client Wifi has been disabled by the user.");
					mState = State.FAILED;
					mProvisionerListener.onFailed();
				}
			}
		}

		protected void handleScanResultsAvailableAction() {
			
			if (mState != State.SCANNING &&
					mState != State.CONNECTING) {
				Log.w(TAG, "Ignoring handleScanResultsAvailableAction() on " + mState + " state");
				return;
			}
			
			
			
			// Here we should return the list of stations to the UI
			List<ScanResult> wifiList = mWifiManager.getScanResults();
			
			if (wifiList == null)
				return;
			
			mProvisionerListener.onScanResult(wifiList);
			
			if (isWifiNetworkAvailable(mConfiguration)) {
				int networkId = mWifiManager.addNetwork(mConfiguration);
				if (networkId != -1) {
					mState = State.CONNECTING;
					mProvisionerListener.onConnecting();
					mOldNetworkId = mWifiManager.getConnectionInfo().getNetworkId();
					if (mWifiManager.enableNetwork(networkId, true)) {
						//mNetworkId = networkId;
						mWiFiNetwork.networkId = networkId;
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
							mWifiManager.getConnectionInfo().getSSID().equals(mConfiguration.SSID)) {
						if (mAuthCounter >= MAX_AUTH_ATTEMPTS) {
							mAuthCounter = 0;
							Log.d(TAG, "Could not auth with AP, failing...");
							mProvisionerListener.onFailed();
						} else {
							/* 
							 * Android Wireless system will retry to connect
							 * automatically, just keep a counter of attempts
							 */
							Log.d(TAG, "Retrying auth with AP");
							mAuthCounter++;
						}
					} else if (mState == State.CONNECTED) {
						Log.d(TAG, "ProximityNetwork has been lost, scanning medium");
						mProvisionerListener.onDisconnected();
						releaseConfiguration();
						mState = State.SCANNING;
						requestScan();
					}
				} else if (detailedStateOf.equals(DetailedState.AUTHENTICATING)) {
					mProvisionerListener.onAuthenticating();
				} else if (detailedStateOf.equals(DetailedState.OBTAINING_IPADDR)) {
					mProvisionerListener.onObtainingIP();
				}
			}
		}

		protected void handleNetworkStateChangedAction(final Intent intent) {

			if (mState != State.CONNECTING &&
					mState != State.CONNECTED) {
				Log.w(TAG, "Ignoring handleNetworkStateChangedAction() on " + mState + " state");
				return;
			}

			NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			String extraInfo = networkInfo.getExtraInfo();
			if (extraInfo != null && extraInfo.equals(mConfiguration.SSID)) {
				if(networkInfo.isConnected()) {
					// Wifi is connected
					Log.d(TAG, "ProximityNetwork is connected: " + String.valueOf(networkInfo));
					mState = State.CONNECTED;
					mProvisionerListener.onConnected(mWiFiNetwork);
				} else {
					Log.d(TAG, "ProximityNetwork is disconnected: " + String.valueOf(networkInfo));
				}
			}
		}
	};



	// Constructor
	public WiFiNetworkProvisioner(Context context, Handler handler, Listener provisionerListener) {
		mContext = context;
		mProvisionerListener = provisionerListener;
		mHandler = handler;
	}

	@Override 
	public boolean initWifiNetworkProvisioner() {
		if (!isAvailable()) {
			Log.e(TAG, "Provisioner is not available");
			return false;
		}

		if (isProvisioned()) {
			Log.e(TAG, "Provisioner has already provisioned this network");
			return false;
		}
		
		// If Wifi AP mode is ON disable it
		if (isWifiApEnabled()) {
			setWifiApEnabled(null, false);
		}
		// If Wifi STA is OFF enable it
		if (!mWifiManager.isWifiEnabled()) {
			mWifiManager.setWifiEnabled(true);
		}
		
		mWifiManager.disconnect();
		
		registerBroadcastReceiver();
		mState = State.INITIALIZED;
		
		return true;
	}
	
	
	@Override
	public boolean setWifiConfiguration( WifiConfiguration configuration) {

		// Store configuration as applied configuration
		if (isProvisioned())
			releaseConfiguration();
		
		mConfiguration = configuration;
		return true;
	}

	@Override
	public WifiConfiguration getWifiConfiguration() {
		return mConfiguration;
	}

	@Override
	public boolean isProvisioned() {
		return (mConfiguration != null ? true : false);
	}

	@Override
	public boolean isAvailable() {

		if (mWifiManager == null) {
			mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
			if (mWifiManager == null)
				return false;
		}
		return true;
	}


	@Override
	public void releaseWifiConfiguration() {
		disableWifi();
		mConfiguration = null;
	}

	private void disableWifi() {

		unregisterBroadcastReceiver();

		releaseConfiguration();

		mState = State.DISABLED;
		mAuthCounter = 0;
	}

	private void releaseConfiguration() {
		if (mWiFiNetwork != null && mWiFiNetwork.networkId != -1) {
			mWifiManager.disableNetwork(mWiFiNetwork.networkId);
			mWifiManager.removeNetwork(mWiFiNetwork.networkId);
			mWifiManager.enableNetwork(mOldNetworkId, true);
			mWiFiNetwork = null;
			mOldNetworkId = -1;
		}
	}

	private boolean isWifiNetworkAvailable(WifiConfiguration wificonfiguration) {

		if (wificonfiguration == null)
			return false;
		
		String SSID = "\"" + wificonfiguration.SSID + "\"";
		
		// Check latest scan report and look for our proximimityNetwork
		for (ScanResult scan : mWifiManager.getScanResults()) {

			if (scan.SSID.equals(wificonfiguration.SSID)) {
				// Add the quotes...
				wificonfiguration.SSID = SSID;
				if (scan.capabilities.equals("[ESS]") &&
						wificonfiguration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) {
					return true;
				} else if (scan.capabilities.contains("WEP") &&
						wificonfiguration.wepKeys[0] != null) {
					return true;
				} else if (scan.capabilities.contains("WPA") &&
						wificonfiguration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)) {
					return true;
				}
				Log.w(TAG, "Capabilities does not match expected values");
				break;
			}
		}
		return false;
	}
	
	private boolean setWifiApEnabled(WifiConfiguration config, boolean enable) {
		try {
			Method methods = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
			return (Boolean) methods.invoke(mWifiManager, config, enable);
		} catch (Exception e) {
			Log.e(TAG, "Error invoking setWifiApEnabled");
			return false;
		}
	}

	private boolean isWifiApEnabled() {
		try {
			Method methods = mWifiManager.getClass().getMethod("isWifiApEnabled");
			return (Boolean) methods.invoke(mWifiManager);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void requestScan() {

		if (mWifiManager == null)
			return;
		
		mWifiManager.startScan();
		// Check after 3 seconds for the scan result
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (mState == State.SCANNING)
					requestScan();
			}
		}, MSEC_WAIT_FOR_SCAN_RESULT);
	}
	
	@Override
	public boolean setWifiConfiguration(String SSID, String encryption, String password) {
		
		if ( mState == State.CONNECTED && mWiFiNetwork != null && 
			mWiFiNetwork.isSameConfig(SSID, encryption, password) ) {
			// Then ignore this as we're already connected to that network
			mProvisionerListener.onConnected(mWiFiNetwork);
			return true;
		}
		
		WiFiNetwork.AUTH auth = WiFiNetwork.stringToAuth(encryption);
		Log.d(TAG, "XXXXXXXX --> encryption is: %s; AUTH is %s", encryption, auth);
		mWiFiNetwork = new WiFiNetwork(SSID, auth, password);
		return setWifiConfiguration( mWiFiNetwork.toWifiConfiguration() );
	}
	
	public WiFiNetwork getConnectedWifiNetwork() {
		if (mState == State.CONNECTED) {
			return mWiFiNetwork;
		}
		return null;
	}

	@Override
	public void unregisterBroadcastReceiver() {
		if (mState != State.DISABLED) {
			mContext.unregisterReceiver(mClientReceiver);
			mState = State.DISABLED;
		}
	}

	@Override
	public void registerBroadcastReceiver() {
		
		if (mState == State.DISABLED) {
			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
			intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
			intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
			intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
			mContext.registerReceiver(mClientReceiver, intentFilter, null, mHandler);
			mState = State.INITIALIZED;
		}
	}
	
}
