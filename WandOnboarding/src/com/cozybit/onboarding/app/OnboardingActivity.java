package com.cozybit.onboarding.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.Toast;

import com.cozybit.onboarding.R;
import com.cozybit.onboarding.app.fragments.OnboardingFragment;
import com.cozybit.onboarding.app.fragments.WelcomeFragment;
import com.cozybit.onboarding.ble.BleProvisioner;
import com.cozybit.onboarding.ble.OnboardingGattService;
import com.cozybit.onboarding.wifi.WiFiNetwork;

public class OnboardingActivity extends FragmentActivity {
	
    private final static String TAG = OnboardingActivity.class.getSimpleName();
	private int NUM_ITEMS = 2;
	
	public static final int START_SCANNING = 1;
	public static final int STOP_SCANNING = 2;
	public static final int DETECTED_DEVICE = 3;
	public static final int CONNECT_DEVICE = 4;
	public static final int GATT_CONNECTED = 5;
	public static final int GATT_DISCONNECTED = 6;
	public static final int GATT_FAILED = 7;
	public static final int GATT_SERVICES_DISCOVERED = 8;
	public static final int STATUS_NOTIFIED = 9;
	public static final int LONG_STATUS_NOTIFIED = 10;
	public static final int VENDOR_ID_READ = 11;
	public static final int DEVICE_ID_READ = 12;
	public static final int DISCONNECT_TARGET = 13;
	public static final int RESET_TARGET = 14;

	private static int RSSI = -35;

	private Handler mHandler;
	private MyAdapter mAdapter;
    private ViewPager mPager;
	
    private WelcomeFragment mWelcomeFragment;
	private OnboardingFragment mScanningFragment;
	
	private BleProvisioner mBleProvisioner;
	private WifiManager mWifiManager;
	
	private WiFiNetwork mNetwork;
	protected int mRetryCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_onboarding);
        
        mHandler = new Handler(Looper.getMainLooper()) {

			@Override
            public void handleMessage(Message inputMessage) {
                switch (inputMessage.what) {
                
                case START_SCANNING:
                	mRetryCount = 0;
                	//mBleProvisioner.startScanLeDevices(OnboardingGattService.SERVICE_UUID, RSSI);
                	mBleProvisioner.openGattServer();
                	break;
                case STOP_SCANNING:
                	mBleProvisioner.stopScanLeDevices();
                	break;               
                case DETECTED_DEVICE:
                	mScanningFragment.detectedDevice();
                	break;
                case CONNECT_DEVICE:
                	mBleProvisioner.connectGatt();
                	break;
                case GATT_CONNECTED:
                	// TODO Maybe discoverServices should be called from here instead of doing it
                	// directly on the Ble Provisioner
                	Toast.makeText(getApplicationContext(), "GATT CONNECTED", Toast.LENGTH_SHORT).show();
                    mBleProvisioner.discoverServices();
                	break;
                case GATT_DISCONNECTED:
                	Toast.makeText(getApplicationContext(), "GATT DISCONNECTED", Toast.LENGTH_SHORT).show();
                	break;
                case GATT_FAILED:
                	if (mRetryCount < 5) {
                		Toast.makeText(getApplicationContext(), "RETRYING GATT connection", Toast.LENGTH_SHORT).show();
                		mBleProvisioner.connectGatt();
                		mRetryCount++;
                	} else {
                		Toast.makeText(getApplicationContext(), "Giving up retrying GATT connection", Toast.LENGTH_SHORT).show();
                	}
                	break;
                case GATT_SERVICES_DISCOVERED:
                	Toast.makeText(getApplicationContext(), "SERVICES DISCOVERED", Toast.LENGTH_SHORT).show();
                	mBleProvisioner.setCharacteristicNotification(OnboardingGattService.CHARACTERISTIC_STATUS, true);
                	mBleProvisioner.setCharacteristicNotification(OnboardingGattService.CHARACTERISTIC_LONG_STATUS, true);
                	
                	// Once services are discovered read VendorID and DeviceID
                	mBleProvisioner.readCharacteristic(OnboardingGattService.CHARACTERISTIC_VENDOR_ID);
                	mBleProvisioner.readCharacteristic(OnboardingGattService.CHARACTERISTIC_DEVICE_ID);

                	break;
                case VENDOR_ID_READ:
                	mScanningFragment.setVendorId((String) inputMessage.obj);
                	break;
                case DEVICE_ID_READ:
                	mScanningFragment.setDeviceId((String) inputMessage.obj);
                	
                	// At this point we can execute all necessary writes for onboarding
                	mBleProvisioner.writeCharacteristic(OnboardingGattService.CHARACTERISTIC_SSID, mNetwork.SSID);
                	mBleProvisioner.writeCharacteristic(OnboardingGattService.CHARACTERISTIC_AUTH, mNetwork.authentication);
                	if (!mNetwork.authentication.equals("OPEN"))
                		mBleProvisioner.writeCharacteristic(OnboardingGattService.CHARACTERISTIC_PASS, mNetwork.password);
                	else 
                		mBleProvisioner.writeCharacteristic(OnboardingGattService.CHARACTERISTIC_PASS, "");
                	byte[] ba = new byte[1];
                	ba[0] = 0x01;
                	mBleProvisioner.writeCharacteristic(OnboardingGattService.CHARACTERISTIC_COMMAND, ba);
                	break;
                case STATUS_NOTIFIED:
                	String status = (String) inputMessage.obj;
                	mScanningFragment.updateStatus(status);
                	break;
                case LONG_STATUS_NOTIFIED:
                	String longStatus = (String) inputMessage.obj;
                	mScanningFragment.updateLongStatus(longStatus);
                	break;
                case DISCONNECT_TARGET:
                	mBleProvisioner.writeCharacteristic(OnboardingGattService.CHARACTERISTIC_COMMAND, new byte[] { 0x02 } );
                	break;
                case RESET_TARGET:
                	mBleProvisioner.writeCharacteristic(OnboardingGattService.CHARACTERISTIC_COMMAND, new byte[] { 0x03 } );
                	break;
                default:
                	super.handleMessage(inputMessage);

                }
            }
        };
        
        mAdapter = new MyAdapter(getSupportFragmentManager());
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

    	mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    	WifiInfo info = mWifiManager.getConnectionInfo();
    	
        if (info == null) {
        	Toast.makeText(getApplicationContext(), "Not connected to any wifi", Toast.LENGTH_SHORT).show();
        	finish();
        	return;
        }
        
        mNetwork = readWiFiNetwork();
        
        if (mNetwork == null) { 
        	// TODO THIS SHOULD BE A DIALOG
        	Toast.makeText(getApplicationContext(), "Not connected through onboarding setup", Toast.LENGTH_SHORT).show();
        	finish();
        	return;
        }
        
        if (info.getNetworkId() != mNetwork.networkId) {
        	// TODO THIS SHOULD BE A DIALOG
        	Toast.makeText(getApplicationContext(), "Connected to wrong wifi, run onboarding setup", Toast.LENGTH_SHORT).show();
        	finish();
        	return;
        }
        
        mBleProvisioner = new BleProvisioner(getApplicationContext(), mHandler);
        mBleProvisioner.resume();
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device
        if (!mBleProvisioner.isEnabled()) {
           mBleProvisioner.init();
        }
        mBleProvisioner.resume();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mBleProvisioner.pause();
    }
    
	private WiFiNetwork readWiFiNetwork() {
		 SharedPreferences prefs = getSharedPreferences("wifi_network",
				  MODE_PRIVATE); 
		 String SSID = prefs.getString("SSID", null);
		 String authentication = prefs.getString("authentication", null);
		 String password = prefs.getString("password", null);
		 int networkId = prefs.getInt("networkId", -1);
		 
		 if (SSID == null || authentication == null || networkId == -1)
			 return null;
		 
		 if (!authentication.equals("OPEN") && password == null)
			 return null;
		 
		 WiFiNetwork w = new WiFiNetwork(SSID, authentication, password);
		 w.networkId = networkId;
		 return w;
	}

    public class MyAdapter extends FragmentPagerAdapter {

    	public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public Fragment getItem(int position) {
        	
        	Log.d(TAG, "getItem " + position);
        	
        	if (position == 0) {
        		if (mWelcomeFragment == null) 
        			mWelcomeFragment = new WelcomeFragment();
        		return mWelcomeFragment;
        	} else if (position == 1) {
        		if (mScanningFragment == null)
        			mScanningFragment = new OnboardingFragment();
        		mScanningFragment.setHandler(mHandler);
        		return mScanningFragment;
        	} 
        	/* This should NOT happen */
        	return null;
        }
    }
}
