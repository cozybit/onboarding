package com.cozybit.onbordee.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import com.cozybit.onbordee.utils.Log;

public class BtConnectionManager implements IBtConnectionManager {

	private static String TAG = BtConnectionManager.class.getName();

	enum States {
		OFF,
		BOOTING,
		WAITING_4_CLIENT,
		VALIDATING_CLIENT,
		CLIENT_CONNECTED,
		FAILED
	}
	
	enum Events {
		INIT,
		INIT_ERROR,
		BLUETOOTH_ON,
		BLUETOOTH_OFF,
		GATT_SERVER_DEPLOYED,
		GATT_SERVER_FAILED,
		BOOT_ERROR,
		CLIENT_CONNECTED,
		CLIENT_VALID,
		CLIENT_INVALID,
		CLIENT_DISCONNECTED,
		RECEIVED_DATA,
		SHUT_DOWN
	}
	
	enum SubEvents {
		DEFAULT,
		NO_BT_AVAILABLE,
		NO_BLE_AVAILABLE,
		BT_BROKEN,
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
	
	public BtConnectionManager(Context context) {
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
	
	// Implement the IBtConnectionManager interface
	
	@Override
	public void sendMessage(Message msg) {
		mHandler.dispatchMessage(msg);
	}
	
	@Override
	public void sendMessage(Events event) {
		if( mHandler != null && !mHandler.sendEmptyMessage(event.ordinal()) )
			Log.e(TAG, "Cant not send message (%s) to handler", event);
	}
	
	@Override
	public void sendMessage(Events event, SubEvents reason) {
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
		SubEvents subEvent = SubEvents.values()[msg.arg1];
		Log.d(TAG, "START: Event: %s; Subevent: %s;", event, subEvent);

		switch(mState) {
		case OFF:
			
			if( event == Events.INIT )
		        updateState(States.BOOTING);
			else if( event == Events.INIT_ERROR )
				updateState(States.FAILED);
			
			break;
		
		case BOOTING:
			
			if( event == Events.BLUETOOTH_ON ) { 
				mBleProvisioner.deployGattServer();
			} else if ( event == Events.GATT_SERVER_DEPLOYED ) {
				updateState(States.WAITING_4_CLIENT);
			} else if ( event == Events.GATT_SERVER_FAILED || event == Events.BLUETOOTH_OFF) {
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
        }
		
		Log.d(TAG, "END: Event: %s; Subevent: %s; Transition: %s -> %s", event, subEvent, beforeEventState, mState );
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
