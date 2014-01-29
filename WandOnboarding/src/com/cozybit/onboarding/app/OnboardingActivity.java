package com.cozybit.onboarding.app;

import android.content.SharedPreferences;
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
	public static final int GATT_DISCONNECTED = 5;
	public static final int GATT_SERVICES_DISCOVERED = 6;
	public static final int STATUS_NOTIFIED = 7;
	public static final int LONG_STATUS_NOTIFIED = 8;


	private static int RSSI = -90;
	
	private Handler mHandler;
	private MyAdapter mAdapter;
    private ViewPager mPager;
	
    private WelcomeFragment mWelcomeFragment;
	private OnboardingFragment mScanningFragment;
	
	private BleProvisioner mBleProvisioner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_onboarding);
        
        mHandler = new Handler(Looper.getMainLooper()) {

			@Override
            public void handleMessage(Message inputMessage) {
                switch (inputMessage.what) {
                
                case START_SCANNING:
                	mBleProvisioner.startScanLeDevices(OnboardingGattService.SERVICE_UUID, RSSI);
                	break;
                case STOP_SCANNING:
                	mBleProvisioner.stopScanLeDevices();
                	break;               
                case DETECTED_DEVICE:
                	mScanningFragment.detectedDevice();
                	break;
                case CONNECT_DEVICE:
                	mBleProvisioner.connectToDevice();
                	break;
                case GATT_CONNECTED:
                	// TODO Maybe discoverServices should be called from here instead of doing it
                	// directly on the Ble Provisioner
                	Toast.makeText(getApplicationContext(), "GATT CONNECTED", Toast.LENGTH_SHORT).show();
                	break;
                case GATT_SERVICES_DISCOVERED:
                	Toast.makeText(getApplicationContext(), "SERVICES DISCOVERED", Toast.LENGTH_SHORT).show();
                	mBleProvisioner.setCharacteristicNotification(OnboardingGattService.CHARACTERISTIC_STATUS, true);
                	mBleProvisioner.setCharacteristicNotification(OnboardingGattService.CHARACTERISTIC_LONG_STATUS, true);
                	mBleProvisioner.writeCharacteristic(OnboardingGattService.CHARACTERISTIC_SSID, "cozyguest");
                	mBleProvisioner.writeCharacteristic(OnboardingGattService.CHARACTERISTIC_AUTH, "OPEN");
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
                default:
                	super.handleMessage(inputMessage);

                }
            }
        };
        
        mAdapter = new MyAdapter(getSupportFragmentManager());
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        WiFiNetwork wifi = readWiFiNetwork();
        
        if (wifi == null)
        	Toast.makeText(getApplicationContext(), "WIFI IS NOT SET CORRECTLY", Toast.LENGTH_SHORT).show();
        
        mBleProvisioner = new BleProvisioner(getApplicationContext(), mHandler);
        mBleProvisioner.init();
    }
    
	private WiFiNetwork readWiFiNetwork() {
		 SharedPreferences prefs = getSharedPreferences("wifi_network",
				  MODE_PRIVATE); 
		 String SSID = prefs.getString("SSID", null);
		 String authentication = prefs.getString("SSID", null);
		 String password = prefs.getString("SSID", null);
		 
		 if (SSID == null || authentication == null)
			 return null;
		 
		 if (!authentication.equals("OPEN") && password == null)
			 return null;
		 
		 return new WiFiNetwork(SSID, authentication, password);
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
