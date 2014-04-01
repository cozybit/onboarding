package com.cozybit.onbordee.wifi;

import java.util.List;

import com.cozybit.onbordee.utils.Log;
import com.cozybit.onbordee.wifi.WifiProvisioner.WifiProvisionerCallback.WifiIfaceState;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

public class WifiProvisioner {

	private final static String TAG = WifiProvisioner.class.getName();

	public interface WifiProvisionerCallback {
		
		public enum WifiIfaceState { WIFI_DISABLING, WIFI_OFF, WIFI_ENABLING, WIFI_ON, WIFI_FAILED, NO_WIFI };
		
		public void onWifiIfaceStatus(WifiIfaceState state);
		public void onWifiLinkStatus(SupplicantState state);
		public void onWifiNetworkStatus(DetailedState state);
	}

	private Context mContext;
	private WifiProvisionerCallback mCallback;
	private WifiBroadcastReceiver mBroadcastReceiver;
	private WifiManager mWifiManager;
	private boolean mProvisionerInitiated = false;

	public WifiProvisioner(Context context, WifiProvisionerCallback callback) {
		mContext = context;
		mCallback = callback;
	}
	
	public void init() {
		
		if( !mProvisionerInitiated ) {
			mBroadcastReceiver = new WifiBroadcastReceiver(mContext, mCallback);
			mBroadcastReceiver.init();
			initIface();
			mProvisionerInitiated = true;
		}
	}
	
	public void stop() {
		
		if( mProvisionerInitiated ) {
			tearDownIface();
			mBroadcastReceiver.stop();
			mProvisionerInitiated = false;
		}	
	}

	private void initIface() {

		// N5, MK908 seems to not have Wifi feature enabled.
		/*if( mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI) ) {
			Log.e(TAG, "ERROR: this android device has no WiFi support.");
			if(mCallback != null) mCallback.onFailure(FailureReason.NO_WIFI_AVAILABLE);
			return;
		}*/

		mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

	    if ( mWifiManager.isWifiEnabled() ) {
	    	if(mCallback != null) {
	    		mCallback.onWifiIfaceStatus(WifiIfaceState.WIFI_ENABLING);
	    		mCallback.onWifiIfaceStatus(WifiIfaceState.WIFI_ON);
	    	}
	    } else {
	    	/*if( mWifiManager.setWifiEnabled(true) )
	    		if(mCallback != null) mCallback.onWifiIfaceStatus(WifiIfaceState.ENABLING);
	    	else
	    		if(mCallback != null) mCallback.onWifiIfaceStatus(WifiIfaceState.FAILED);*/
	    	if( !mWifiManager.setWifiEnabled(true) )
    			if(mCallback != null) mCallback.onWifiIfaceStatus(WifiIfaceState.WIFI_FAILED);
	    }
	}
	
	private void tearDownIface() {
		if( mWifiManager != null && mWifiManager.isWifiEnabled() ) { 
			if( !mWifiManager.setWifiEnabled(false) )
				if(mCallback != null) mCallback.onWifiIfaceStatus(WifiIfaceState.WIFI_FAILED);
		}
	}
	
    public void flushAllConfiguredNetworks() {
    	
    	if (mWifiManager != null) {
    		List<WifiConfiguration> list = mWifiManager.getConfiguredNetworks();
    		for (WifiConfiguration wifiConfiguration : list) {
    			mWifiManager.disableNetwork(wifiConfiguration.networkId);
    			mWifiManager.removeNetwork(wifiConfiguration.networkId);
			}
    	} 
    }

    //Connect to a network. BSSID can be null if unknown
    public boolean connectTo(String SSID, String BSSID, int channel, String auth, String password) {
    	
    	if(mWifiManager != null) {
	    	WifiConfiguration config = generateWifiConfiguration(SSID, BSSID, auth, password);
			int networkId = mWifiManager.addNetwork(config);
			config.networkId = networkId;
			if ( networkId != -1 &&	mWifiManager.enableNetwork(networkId, true) &&
				mWifiManager.reconnect() ) {
				Log.d("Connecting to Wifi Network: %s", SSID);
				return true;
			}
    	}
    	return false;
    }

    //TODO: WEP and WPA PSK not supporte. Open and WPA2 PSK supported
	private WifiConfiguration generateWifiConfiguration(String SSID, String BSSID, String auth, String password) {
		
		WifiConfiguration config = new WifiConfiguration();
		config.SSID = String.format("\"%s\"", SSID);
		config.BSSID = BSSID;
		config.status = WifiConfiguration.Status.ENABLED;
		if ( auth.equals("none") ) { // PLAIN
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		} else if ( auth.equals("WPA") ) { // WPA
			config.preSharedKey = String.format("\"%s\"", password);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
			config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
		} else {
			Log.e(TAG, "Unkown authentiation type: %s",auth);
			return null;
		}
		return config;
	}
	
	public boolean reconnectToNetwork() {
		if(mWifiManager != null)
			return mWifiManager.reconnect();
		return false;
	}
   
    public WifiConfiguration getCurrentWifiConfig() {
    	if( mWifiManager != null ) {
	    	String ssid = mWifiManager.getConnectionInfo().getSSID();
	    	ssid = String.format("\"s%\"", ssid);
	    	List<WifiConfiguration> configurations = mWifiManager.getConfiguredNetworks();
	    	for (WifiConfiguration wifiConf : configurations) {
				if( wifiConf.SSID.equals(ssid) )
					return wifiConf;
			}
    	}
    	return null;
    }
}
