package com.cozybit.onboarding.wifi;

import java.util.List;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;

public interface INetworkProvisioner {
	
	public boolean initWifiNetworkProvisioner();
	
	public boolean isAvailable();

	public boolean isProvisioned();
	
	public boolean setWifiConfiguration(String SSID, String encryption, String password);
	
	public boolean setWifiConfiguration(WifiConfiguration configuration);

	public WifiConfiguration getWifiConfiguration();
	
	public void releaseWifiConfiguration();
	
	public void requestScan();
	
	public interface Listener {
		
		public void onScanResult(List<ScanResult> wifiList);
		
		public void onConnecting();
		
		public void onAuthenticating();
		
		public void onObtainingIP();
		
		public void onConnected(WiFiNetwork wifiNetwork);
		
		public void onDisconnected();

		public void onFailed();

	}






}
