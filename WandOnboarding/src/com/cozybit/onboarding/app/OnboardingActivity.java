package com.cozybit.onboarding.app;

import android.content.Context;
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
import android.widget.Toast;

import com.cozybit.onboarding.R;
import com.cozybit.onboarding.app.fragments.OnboardingFragment;
import com.cozybit.onboarding.app.fragments.WelcomeFragment;
import com.cozybit.onboarding.ble.BleProvisioner;
import com.cozybit.onboarding.profile.OnboardingProfile;
import com.cozybit.onboarding.profile.OnboardingProfile.COMMANDS;
import com.cozybit.onboarding.profile.OnboardingProfile.Characteristics;
import com.cozybit.onboarding.profile.OnboardingProfile.STATES;
import com.cozybit.onboarding.profile.OnboardingProfile.EVENTS;
import com.cozybit.onboarding.utils.Log;
import com.cozybit.onboarding.wifi.WiFiNetwork;

public class OnboardingActivity extends FragmentActivity {
	
    private final static String TAG = OnboardingActivity.class.getSimpleName();
	private int NUM_ITEMS = 2;

	private final static String PREFERENCES_NAME = "wifi_network";
	
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

	//TODO Increase RSSI threshold just to make development easier
	//private static int RSSI = -35;
	private static int RSSI = -85;
	
	private boolean DEBUG = false;

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
                	mBleProvisioner.startScanLeDevices(OnboardingProfile.SERVICE_UUID, RSSI);
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
                	if(DEBUG) Toast.makeText(getApplicationContext(), "GATT CONNECTED", Toast.LENGTH_SHORT).show();
                    mBleProvisioner.discoverServices();
                	break;
                case GATT_DISCONNECTED:
                	if(DEBUG) Toast.makeText(getApplicationContext(), "GATT DISCONNECTED", Toast.LENGTH_SHORT).show();
                	break;
                case GATT_FAILED:
                	if (mRetryCount < 5) {
                		if(DEBUG) Toast.makeText(getApplicationContext(), "RETRYING GATT connection", Toast.LENGTH_SHORT).show();
                		mBleProvisioner.connectGatt();
                		mRetryCount++;
                	} else {
                		if(DEBUG) Toast.makeText(getApplicationContext(), "Giving up retrying GATT connection", Toast.LENGTH_SHORT).show();
                	}
                	break;
                case GATT_SERVICES_DISCOVERED:
                	if(DEBUG) Toast.makeText(getApplicationContext(), "SERVICES DISCOVERED", Toast.LENGTH_SHORT).show();                	
                	// Once services are discovered read VendorID and DeviceID
                	mBleProvisioner.readCharacteristic(Characteristics.VENDOR_ID.uuid);
                	mBleProvisioner.readCharacteristic(Characteristics.DEVICE_ID.uuid);
                	break;

                case VENDOR_ID_READ:
                	mScanningFragment.setVendorId((String) inputMessage.obj);
                	break;
                	
                case DEVICE_ID_READ:
                	mScanningFragment.setDeviceId((String) inputMessage.obj);
                	mBleProvisioner.setCharacteristicNotification(Characteristics.STATUS.uuid, true);
                	mBleProvisioner.setCharacteristicNotification(Characteristics.LONG_STATUS.uuid, true);
                	mBleProvisioner.readCharacteristic(Characteristics.STATUS.uuid);
                	mBleProvisioner.readCharacteristic(Characteristics.LONG_STATUS.uuid);
                	break;
                	
                case STATUS_NOTIFIED:
                	STATES state = STATES.values()[inputMessage.arg1];
                	Log.d(TAG, "Notified status: %s", state);
                	mScanningFragment.updateStatus(state.toString());
                	
                	if( state == STATES.READY || state == STATES.FAILED) {
	                	// At this point we can execute all necessary writes for onboarding
	                	mBleProvisioner.writeCharacteristic(Characteristics.SSID.uuid, mNetwork.SSID);
	                	// TODO: this is dangerous -> Make sure that WiFiNetwork AUTH values match OnboardingProfile AUTH one.
	                	mBleProvisioner.writeCharacteristic(Characteristics.AUTH.uuid, (byte) mNetwork.authentication.ordinal());
	                	String pssd = mNetwork.authentication.equals("OPEN") ? "" : mNetwork.password;
	                	mBleProvisioner.writeCharacteristic(Characteristics.PASS.uuid, pssd);
	                	mBleProvisioner.writeCharacteristic(Characteristics.COMMAND.uuid, (byte) COMMANDS.CONNECT.ordinal());
                	}
                	break;
                	
                case LONG_STATUS_NOTIFIED:
                	Log.d(TAG, "Notified long status: %s", EVENTS.values()[inputMessage.arg1].msg);
                	mScanningFragment.updateLongStatus(EVENTS.values()[inputMessage.arg1].msg);
                	break;
                	
                case DISCONNECT_TARGET:
                	mBleProvisioner.writeCharacteristic(Characteristics.COMMAND.uuid, (byte) COMMANDS.DISCONNECT.ordinal());
                	break;
                	
                case RESET_TARGET:
                	mBleProvisioner.writeCharacteristic(Characteristics.COMMAND.uuid, (byte) COMMANDS.RESET.ordinal());
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
        	if(DEBUG) Toast.makeText(getApplicationContext(), "Not connected to any wifi", Toast.LENGTH_SHORT).show();
        	finish();
        	return;
        }

        mNetwork = WiFiNetwork.readWiFiNetwork( getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE) );

        if (mNetwork == null) { 
        	// TODO THIS SHOULD BE A DIALOG
        	Log.d(TAG, "READ NETWORK IS NULL!!!!!");
        	if(DEBUG) Toast.makeText(getApplicationContext(), "Not connected through onboarding setup", Toast.LENGTH_SHORT).show();
        	finish();
        	return;
        }

        if (info.getNetworkId() != mNetwork.networkId) {
        	// TODO THIS SHOULD BE A DIALOG
        	if(DEBUG) Toast.makeText(getApplicationContext(), "Connected to wrong wifi, run onboarding setup", Toast.LENGTH_SHORT).show();
        	finish();
        	return;
        }

        mBleProvisioner = new BleProvisioner(getApplicationContext(), mHandler);
        mBleProvisioner.resume();
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        if(mBleProvisioner != null) {
	        // Ensures Bluetooth is enabled on the device
	        if (!mBleProvisioner.isEnabled()) {
	           mBleProvisioner.init();
	        }
	        mBleProvisioner.resume();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mBleProvisioner.pause();
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
