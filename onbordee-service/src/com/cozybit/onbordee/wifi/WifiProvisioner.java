package com.cozybit.onbordee.wifi;

import java.util.List;

import com.cozybit.onbordee.utils.Log;
import com.cozybit.onbordee.wifi.WifiProvisioner.WifiProvisionerCallback.FailureReason;
import com.cozybit.onbordee.wifi.WifiProvisioner.WifiProvisionerCallback.WifiIfaceStatus;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

public class WifiProvisioner {

	private final static String TAG = WifiProvisioner.class.getName();

	public interface WifiProvisionerCallback {

		enum WifiIfaceStatus { OFF, ENABLING, ON, DISABLING, FAILED };
		enum WifiNetworkStatus { ASSOCIATING, AUTHENTICATING, OBTAINING_IPADDR, CONNECTED, DISCONNECTED, FAILED };
		public enum FailureReason { UNKNOWN, NO_CONTEXT, NO_WIFI_AVAILABLE };

		public void onWifiIfaceStatus(WifiIfaceStatus status);
		public void onWifiNetworkStatus(WifiNetworkStatus status);
		public void onFailure(FailureReason error);
	}

	private Context mContext;
	private WifiProvisionerCallback mCallback;
	private WifiBroadcastReceiver mBroadcastReceiver;
	private WifiManager mWifiManager;
	private boolean mWifiInitState;
	private boolean mIsInitiated = false;

	public WifiProvisioner(Context context, WifiProvisionerCallback callback) {
		mContext = context;
		mCallback = callback;
	}
	
	public void init() {
		
		if( !mIsInitiated ) {
			if (mContext == null) {
				Log.e(TAG,"ERROR: context not available.");
				if(mCallback != null) mCallback.onFailure(FailureReason.NO_CONTEXT);
				return;
			} else {
				mBroadcastReceiver = new WifiBroadcastReceiver(mContext, mCallback);
				mBroadcastReceiver.init();
				initIface();
				mIsInitiated = true;
			}
		}
	}
	
	public void tearDown() {
		
		if( mIsInitiated ) {
			if( mWifiManager != null && !mWifiInitState ) {
				WifiIfaceStatus status = mWifiManager.setWifiEnabled(false) ? WifiIfaceStatus.DISABLING:WifiIfaceStatus.FAILED;
				if(mCallback != null) mCallback.onWifiIfaceStatus(status);
			}
			mBroadcastReceiver.stop();
			mIsInitiated = false;
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
		
	    if ( mWifiInitState = mWifiManager.isWifiEnabled() ) {
	    	if(mCallback != null) {
	    		mCallback.onWifiIfaceStatus(WifiIfaceStatus.ENABLING);
	    		mCallback.onWifiIfaceStatus(WifiIfaceStatus.ON);
	    	}
	    } else {
	    	if( mWifiManager.setWifiEnabled(true) )
	    		if(mCallback != null) mCallback.onWifiIfaceStatus(WifiIfaceStatus.ENABLING);
	    	else
	    		if(mCallback != null) mCallback.onWifiIfaceStatus(WifiIfaceStatus.FAILED);
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
