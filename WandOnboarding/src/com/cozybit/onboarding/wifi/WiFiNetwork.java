package com.cozybit.onboarding.wifi;

import com.cozybit.onboarding.utils.Log;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiConfiguration;

/* Small inner class to Store and transfer WiFiNetwork without all other clutter */
public class WiFiNetwork {

	private final static String TAG = WiFiNetwork.class.getName();
	
	private final static String SSID_KEY = "SSID";
	private final static String AUTHENTICATION_KEY = "AUTHENTICATION";
	private final static String PASSWORD_KEY = "PASSWORD";
	private final static String NETWORKID_KEY = "NETWORK_ID";
	
	public static enum AUTH { OPEN, WEP, WPA_PSK, UNKNOWN; }
	
	public int networkId = -1;
	public String SSID;
	public AUTH authentication;
	public String password;
	
	public WiFiNetwork(String SSID, AUTH authentication, String password) {
		this.SSID = SSID;
		this.authentication = authentication;
		this.password = password;
	}

	public boolean isSameConfig(String SSID, String authentication,
			String password) {

		 if (SSID == null || authentication == null)
			 return false;

		 if (!this.SSID.equals(SSID))
			 return false;

		 if (!this.authentication.equals(authentication))
			 return false;
 
		 // If authentication is NOT open
		 if (!this.authentication.equals("OPEN")) {
			 // If the autentication is NOT open but password is null
			 if (password == null)
				 return false;
			 // If password is the same we
			 if (!this.password.equals(password))
				 return false;
		 }
		 // Data matches!
		 return true;
	}
	
	public WifiConfiguration toWifiConfiguration() {
		
		WifiConfiguration config = new WifiConfiguration();
		config.SSID = SSID;
		
		switch(authentication) {
		
		case OPEN:
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			break;
		
		case WEP:
			//TODO: I don't think this is needed
			//wc.hiddenSSID = true;
			//config.status = WifiConfiguration.Status.DISABLED;     
			//config.priority = 40;
			config.wepKeys[0] = String.format("\"%s\"", password);
			config.wepTxKeyIndex = 0;
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			config.allowedProtocols.set(WifiConfiguration.Protocol.RSN); 
			config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
			config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
			config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
			break;
		
		case WPA_PSK:
			config.preSharedKey = String.format("\"%s\"", password);
			//config.status = WifiConfiguration.Status.ENABLED;
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
			config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			break;
		
		default:
			config = null;
			break;
		}
		
		return config;
	}
	
	public boolean storeWiFiNetwork(Editor editor) {
		if(editor != null) {
			editor.putString(SSID_KEY, SSID);
			editor.putInt(AUTHENTICATION_KEY, authentication.ordinal());
			editor.putString(PASSWORD_KEY, password);
			editor.putInt(NETWORKID_KEY, networkId);
			return editor.commit();
		}
		
		return false;
	}
	
	public static WiFiNetwork readWiFiNetwork(SharedPreferences prefs) {
 
		 String SSID = prefs.getString(SSID_KEY, null);
		 int authIndex = prefs.getInt(AUTHENTICATION_KEY, -1);
		 String password = prefs.getString(PASSWORD_KEY, null);
		 int networkId = prefs.getInt(NETWORKID_KEY, -1);
		 
		 if (SSID == null || networkId == -1 || authIndex < 0 || authIndex > 3)
			 return null;
		 
		AUTH auth = AUTH.values()[prefs.getInt(AUTHENTICATION_KEY, 3)];

		if ( auth != AUTH.OPEN && password == null)
			return null;

		 WiFiNetwork wn = new WiFiNetwork(SSID, auth, password);
		 wn.networkId = networkId;
		 return wn;
	}
	
	public static AUTH stringToAuth( String authType ) {
		if( authType.equals("NONE") || authType.equals("OPEN") )
			return AUTH.OPEN;
		else if ( authType.equals("WEP") )
			return AUTH.WEP;
		else if ( authType.contains("WPA") ) 
			return AUTH.WPA_PSK;
		
		return AUTH.UNKNOWN;
	}
};
