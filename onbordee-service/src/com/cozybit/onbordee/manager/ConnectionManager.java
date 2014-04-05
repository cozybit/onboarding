package com.cozybit.onbordee.manager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;

import com.cozybit.onbordee.ble.BleProvisioner;
import com.cozybit.onbordee.ble.BtBroadcastReceiver;
import com.cozybit.onbordee.profile.OnboardingProfile;
import com.cozybit.onbordee.profile.OnboardingProfile.Characteristics;
import com.cozybit.onbordee.utils.Log;

public class ConnectionManager implements IManager {

	private static String TAG = ConnectionManager.class.getName();

	enum States {
		OFF, FAILED, BOOTING, 
		WAITING_4_CLIENT, VALIDATING_CLIENT, CLIENT_CONNECTED 
	}
	
	public enum Events {
		INIT, SHUT_DOWN,
		BT_ON, BT_OFF, BT_BROKEN, NO_BT, NO_BLE, 
		GATT_SERVER_DEPLOYED, GATT_SERVER_FAILED,
		BOOT_ERROR,
		CLIENT_CONNECTED, CLIENT_VALID,	CLIENT_INVALID,	CLIENT_DISCONNECTED,
		RECEIVED_DATA
	}
	
	private Context mContext;
	private IntentFilter mIntentFilter;
	
	private States mState;
	private States mPreviusState;
	private HandlerThread mHandlerThread;
	private Handler mHandler;
	private Message mLastMessage; 

	private final int RSSI_TH = 55;
	private BleProvisioner mBleProvisioner;
	private BtBroadcastReceiver mBroadcastReciver;
	private BluetoothDevice mBtClient;
	
	private DataReceivedCallback mDataCallback;
	private OnboardingManager mOnboardingManager;
	
	public interface DataReceivedCallback {
		public void onDataReceived(OnboardingProfile.Characteristics type, byte[] data);
	};
	
	public ConnectionManager(Context context) {
		mContext = context;
		mBleProvisioner = new BleProvisioner(mContext, this);
		mState = States.OFF;

		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		mBroadcastReciver = new BtBroadcastReceiver(this);

		//TODO: do you have to STOP the Handler
		mHandlerThread = new HandlerThread ("BtConnectionManagerHandlerThread", Process.THREAD_PRIORITY_BACKGROUND);
		mHandlerThread.start();                                                                                                                                                                           		
		mHandler = new Handler(mHandlerThread.getLooper(), new Handler.Callback() {

			@Override
			public boolean handleMessage(Message msg) {
				processMessage(msg);
				return false;
			}
		});
	}
	
	public void setOnboardingManager(OnboardingManager onboardingManager) {
		mOnboardingManager = onboardingManager;
	}
	
	public void setDataReceivedCallback(DataReceivedCallback callback) {
		mDataCallback = callback;
	}
	
	public BleProvisioner getBleProvisioner() {
		return mBleProvisioner;
	}

	public void init() {
		if( mState == States.OFF ) {
			mContext.registerReceiver(mBroadcastReciver, mIntentFilter);
			mBleProvisioner.initBtIface();
		}
	}
	
	/*TODO: shutDown is a non blocking call that will turn off the state machine and
	 * will send the right signals to Android in order to tear down the gattserver & BT
	 * iface. The problem here is that, Android will night some extra time to perform 
	 * those actions. So if init() & shutDown() are invoked several times in a row, it's 
	 * probable that something fails. */ 
	public void shutDown() {
		processMessage(Message.obtain(null, Events.SHUT_DOWN.ordinal()));
	}

	@Override
	public void sendMessage(Message msg) {
		if (mHandler != null)
			mHandler.dispatchMessage(msg);
	}

	@Override
	public void sendMessage(Enum event) {
		if( mHandler != null && !mHandler.sendEmptyMessage(event.ordinal()) )
			Log.e(TAG, "Cant not send message (%s) to handler", event);
	}

	@Override
	public void sendMessage(Enum event, Enum reason) {
		if(mHandler != null) {
			Message msg = mHandler.obtainMessage();
			msg.what = event.ordinal();
			msg.arg1 = reason.ordinal();
			msg.sendToTarget();
		}
	}

    /*------------------------
     * Start of State Machine
     *------------------------*/

	private void processMessage(Message msg) {

		States beforeEventState = mState;
		
		//received message
		mLastMessage = msg;
		Events event = Events.values()[msg.what];
		Log.d(TAG, "START: Event: %s", event);

		switch(mState) {
		case OFF:
			
			if( event == Events.INIT )
		        updateState(States.BOOTING);
			else if( event == Events.NO_BT || event == Events.NO_BLE || event == Events.BT_BROKEN )
				updateState(States.FAILED);
			break;
		
		case BOOTING:
			
			if( event == Events.BT_ON ) { 
				mBleProvisioner.deployGattServer();
			} else if ( event == Events.GATT_SERVER_DEPLOYED ) {
				// Update the value of the Characteristics
				if( mOnboardingManager != null ) {
					if( !mBleProvisioner.updateCharacteristic(Characteristics.DEVICE_ID.uuid, Build.PRODUCT, false) )
						Log.e(TAG, "ERROR: BleProvisioner couldn't update %s", Characteristics.DEVICE_ID);
					if( !mBleProvisioner.updateCharacteristic(Characteristics.VENDOR_ID.uuid, Build.MANUFACTURER, false) )
						Log.e(TAG, "ERROR: BleProvisioner couldn't update %s", Characteristics.VENDOR_ID);
					if( !mBleProvisioner.updateCharacteristic(Characteristics.STATUS.uuid, (byte) mOnboardingManager.getCurrentState().ordinal(), false) )
						Log.e(TAG, "ERROR: BleProvisioner couldn't update %s", Characteristics.STATUS);
					if( !mBleProvisioner.updateCharacteristic(Characteristics.LONG_STATUS.uuid, (byte) mOnboardingManager.getCurrentEvent().ordinal(), false) )
						Log.e(TAG, "ERROR: BleProvisioner couldn't update %s", Characteristics.LONG_STATUS);
				}
				updateState(States.WAITING_4_CLIENT);
				
			} else if ( event == Events.GATT_SERVER_FAILED || event == Events.BT_OFF) {
				updateState(States.FAILED);
			}
			break;
			
		case WAITING_4_CLIENT:
			
			if( event == Events.CLIENT_CONNECTED ) {
				mBtClient = (BluetoothDevice) msg.obj;
				updateState(States.VALIDATING_CLIENT);
				//it is impossible for the GattServer to get any information about the
				//rssi of the client. As consequence of this, validation can't be done. Let's
				//fake it.
				sendMessage(Events.CLIENT_VALID);
			}
			break;
			
		case VALIDATING_CLIENT:
			
			if( event == Events.CLIENT_VALID) {
				
				updateState(States.CLIENT_CONNECTED);
			} else if ( event == Events.CLIENT_INVALID) {
				
				//disconnect the invalid client & wait for confirmation
				mBleProvisioner.disconnectBtClient(mBtClient);
			} else if ( event == Events.CLIENT_DISCONNECTED) {
				mBtClient = null;
				updateState(States.WAITING_4_CLIENT);
			}
			break;
			
		case CLIENT_CONNECTED:
			
			if( event == Events.RECEIVED_DATA ) {
				OnboardingProfile.Characteristics type = OnboardingProfile.Characteristics.values()[msg.arg1];
				byte[] value = (byte[]) msg.obj;
				mDataCallback.onDataReceived(type, value);
			} else if ( event == Events.CLIENT_DISCONNECTED) {
				mBtClient = null;
				updateState(States.WAITING_4_CLIENT);
			}
			break;
			
		case FAILED:
			break;
		}
		
        if( event == Events.SHUT_DOWN && mState != States.OFF ) {
        	updateState(States.OFF);
        	mBleProvisioner.tearDown();
        	resetStateMachineVars();
        	mContext.unregisterReceiver(mBroadcastReciver);
        } else if ( event == Events.BT_OFF && mState != States.OFF ) {
        	updateState(States.FAILED);
        }
		
		Log.d(TAG, "END: Event: %s; Transition: %s -> %s", event, beforeEventState, mState );
	}
	
    /*------------------------
     * End of State Machine
     *------------------------*/
	
	private void updateState(States newState) {
		mPreviusState = mState;
		mState = newState;
		Log.d(TAG, "state transition: %s -> %s", mPreviusState, mState);
	}
	
	private void resetStateMachineVars() {
		mBtClient = null;
	}

}
