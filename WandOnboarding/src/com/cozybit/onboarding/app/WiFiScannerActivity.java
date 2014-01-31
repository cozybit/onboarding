package com.cozybit.onboarding.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.cozybit.onboarding.R;
import com.cozybit.onboarding.wifi.INetworkProvisioner;
import com.cozybit.onboarding.wifi.WiFiNetwork;
import com.cozybit.onboarding.wifi.WiFiNetworkProvisioner;
import com.cozybit.onboarding.wifi.INetworkProvisioner.Listener;

public class WiFiScannerActivity extends Activity {

	List<Map<String, String>> mWifiNetworksList;
	private ListView mListView;
	private SimpleAdapter mSimpleAdapter;
	private Handler handler = new Handler();
	private INetworkProvisioner mProvisioner;
	private AlertDialog.Builder mBuilder;
	private LayoutInflater mLayoutInflater;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_wifiscan);
		
		mBuilder = new AlertDialog.Builder(this);
		mLayoutInflater = getLayoutInflater();

		mWifiNetworksList = new ArrayList<Map<String, String>>();
		
		mListView = (ListView) findViewById(R.id.list);
		
		String[] from = { "ssid", "encryption" };
		int[] to = { android.R.id.text1, android.R.id.text2 };

		mSimpleAdapter = new SimpleAdapter(this, mWifiNetworksList, android.R.layout.simple_list_item_2, from, to);
		mListView.setAdapter(mSimpleAdapter);
		
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				
				final TextView textView1 = (TextView)view.findViewById(android.R.id.text1);
				final TextView textView2 = (TextView)view.findViewById(android.R.id.text2);
				
				if (textView2.getText().toString().equals("OPEN")) {
					mProvisioner.setWifiConfiguration(textView1.getText().toString(), textView2.getText().toString(), null);
				} else if (textView2.getText().toString().equals("UNKNOWN")) {
					
				} else {
					mBuilder.setMessage(null);
					View dialogView = mLayoutInflater.inflate(R.layout.dialog_wifi, null);
					final EditText editText = (EditText)dialogView.findViewById(R.id.password);
					mBuilder.setView(dialogView);
					
					mBuilder.setTitle(textView1.getText());
					mBuilder.setPositiveButton("OK", new OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mProvisioner.setWifiConfiguration(textView1.getText().toString(),
															  textView2.getText().toString(),
															  editText.getText().toString());
						}
					});
					mBuilder.setNegativeButton("Cancel", null);
					AlertDialog securityDialog = mBuilder.create();
					securityDialog.show();
				}
			}

		});
				
		mBuilder.setMessage("OnBoarding application requires to be connected to the same wireless network where new devices will be onboarded." +
							"\n\n Please, connect to the wireless network you want to use for new devices.").setTitle("OnBoarding Setup");
		
		mBuilder.setPositiveButton("OK", null);

		AlertDialog welcomeDialog = mBuilder.create();
		
		welcomeDialog.show();
		
		mProvisioner = new WiFiNetworkProvisioner(this, this.handler, new Listener() {
			
			@Override
			public void onScanResult(List<ScanResult> wifiList) {
		
				mWifiNetworksList.clear();
				
				for(int i = 0; i < wifiList.size(); i++)
				{
					ScanResult result = wifiList.get(i);
					
					if (result == null || result.SSID.isEmpty() ||
							result.capabilities.contains("[IBSS]") || 
							result.level < -75) {
						continue;
					}
					
					String resultEncryption = getEncryption(result);
					
					for (Map<String, String> entry : mWifiNetworksList) {
						if (entry.containsKey(result.SSID)) {
							// Wifi is known
							String encryption = entry.get("encryption");
							// To avoid collisions of multiple SSIDs
							if (!encryption.equals(getEncryption(result))) {
								resultEncryption = resultEncryption + "," + encryption;
							}
							break;
						}
					}
					mWifiNetworksList.add(putData(result.SSID, resultEncryption));
				}
				mSimpleAdapter.notifyDataSetChanged();
			}
			
			@Override
			public void onFailed() {
				// TODO Auto-generated method stub
				Toast.makeText(getBaseContext(), "Failed", Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void onDisconnected() {
				Toast.makeText(getBaseContext(), "Disconnected", Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void onConnected(WiFiNetwork wifiNetwork) {
				Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_SHORT).show();
				storeWiFiNetwork(wifiNetwork);
				finish();
			}

			@Override
			public void onConnecting() {
				Toast.makeText(getBaseContext(), "Connecting", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onAuthenticating() {
				Toast.makeText(getBaseContext(), "Authenticating", Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void onObtainingIP() {
				Toast.makeText(getBaseContext(), "Obtaining IP", Toast.LENGTH_SHORT).show();
			}
		});
		
		mProvisioner.initWifiNetworkProvisioner();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "Refresh");
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		mProvisioner.requestScan();
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	protected void onDestroy() 
	{
		//mProvisioner.releaseWifiConfiguration();
		mProvisioner.unregisterBroadcastReceiver();
		super.onDestroy();
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		mProvisioner.unregisterBroadcastReceiver();

	}

	@Override
	protected void onResume()
	{
		super.onResume();
		mProvisioner.registerBroadcastReceiver();
		mProvisioner.requestScan();
	}

	private Map<String, String> putData(String ssid, String encryption) {
		Map<String, String> item = new HashMap<String, String>();
		item.put("ssid", ssid);
		item.put("encryption", encryption);
		return item;
	}
	
    private static String getEncryption(ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            return "WEP";
        } else if (result.capabilities.contains("PSK")) {
            return getPskType(result) ;
        } else if (result.capabilities.contains("EAP")) {
            return "EAP";
        }
        return "OPEN";
    }

	private static String getPskType(ScanResult result) {
		 boolean wpa = result.capabilities.contains("WPA-PSK");
	        boolean wpa2 = result.capabilities.contains("WPA2-PSK");
	        if (wpa2 && wpa) {
	            return "WPA/WPA2";
	        } else if (wpa2) {
	            return "WPA2";
	        } else if (wpa) {
	            return "WPA";
	        } else {
	            return "UNKNOWN";
	        }
	}
	
	private void storeWiFiNetwork(WiFiNetwork wifiNetwork) {
		// Here store this on the Application Shared Preferences
		SharedPreferences prefs = getSharedPreferences("wifi_network", MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("SSID", wifiNetwork.SSID);
		if (!wifiNetwork.authentication.equals("OPEN"))
			editor.putString("authentication", "SECURE");
		else
			editor.putString("authentication", wifiNetwork.authentication);
		editor.putString("password", wifiNetwork.password);
		editor.putInt("networkId", wifiNetwork.networkId);
		editor.commit();
	}
	

	
	
}