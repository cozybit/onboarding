package com.cozybit.onbordee.manager;

import com.cozybit.onbordee.manager.ConnectionManager;
import com.cozybit.onbordee.utils.Log;
import com.cozybit.onbordee.wifi.WifiProvisioner;
import com.cozybit.onbordee.wifi.WifiProvisioner.WifiProvisionerCallback;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;

public class OnboardingManager {
	
	public final static String TAG = OnboardingManager.class.getName();
	
	enum States {
		OFF, BOOTING, IDLE,
		CONNECTING,	CONNECTED, FAILED
	}
	
	public enum Events {
		INIT, INIT_ERROR, SHUT_DOWN,
		WIFI_ON, WIFI_OFF, WIFI_FAILED,
		CONNECT_TO_WIFI, FORGET_WIFI,
		INVALID_PARAMETERS,
		WIFI_CONNECTED, WIFI_DISCONNECTED, WIFI_CONNECTING_UPDATE, WIFI_FAILED_CONNECTING, WIFI_CONNECTION_REJECTED,
	}
	
	public enum SubEvents {
		DEFAULT, NO_WIFI_AVAILABLE,	WIFI_BROKEN,
	}

	private Context mContext;
	private HandlerThread mHandlerThread;
	private Handler mHandler;
	
	private States mState;
	private States mPreviusState;
	private Message mLastMessage;
	
	private WifiProvisioner mWifiProvisioner;

	/*
	 * Start of Callback implementations
	 */
	
	//Callbacks from the Wifi Provisioner reporting Wifi related events 
	private	 WifiProvisionerCallback mWifiProvisionerCallback = new WifiProvisionerCallback() { 
		
		@Override
		public void onWifiIfaceStatus(WifiIfaceStatus status) {
			
			switch (status) {
			case OFF:
				sendMessage(Events.WIFI_OFF);
				break;
			case ENABLING:
				break;
			case ON:
				sendMessage(Events.WIFI_ON);
				break;
			case DISABLING:
				break;
			case FAILED:
				sendMessage(Events.WIFI_FAILED);
				break;
			}
		}
		@Override
		public void onWifiNetworkStatus(WifiNetworkStatus status) {
			
			switch (status) {
			case ASSOCIATING:
				break;
			case AUTHENTICATING:
				break;
			case OBTAINING_IPADDR:
				break;
			case CONNECTED:
				sendMessage(Events.WIFI_CONNECTED);
				break;
			case DISCONNECTED:
				sendMessage(Events.WIFI_DISCONNECTED);
				break;
			case FAILED:
				break;
			}
		}
		
		@Override
		public void onFailure(FailureReason error) {
			
			switch (error) {
			case UNKNOWN:
				break;
			case NO_CONTEXT:
				break;
			case NO_WIFI_AVAILABLE:
	        	sendMessage(Events.INIT_ERROR, SubEvents.NO_WIFI_AVAILABLE);
				break;
			}
			
		}
	};

	//Callback from the ConnectionManager reporting the received data that needs to be process here
	private ConnectionManager.DataReceivedCallback mDataReceivedCallback = new ConnectionManager.DataReceivedCallback() {

		@Override
		public void onDataReceived(DataTypes type, byte[] data) {
			
			switch (type) {
			case AUTH:
				break;
			case CHANNEL:
				break;
			case CMD:
				break;
			case PASS:
				break;
			case SSID:
				break;
			}
		}

	};
	
	/*
	 * End of Callback implementations
	 */

	public OnboardingManager(Context context) {
		mContext = context;
		mState = States.OFF;
		mWifiProvisioner = new WifiProvisioner(mContext, mWifiProvisionerCallback);

		//TODO: do you have to STOP the Handler
		mHandlerThread = new HandlerThread ("WifiConnectionManagerHandlerThread", Process.THREAD_PRIORITY_BACKGROUND);
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
		if( mState == States.OFF )
			sendMessage(Events.INIT);
	}

	public void shutDown() {
		processMessage(Message.obtain(null, Events.SHUT_DOWN.ordinal()));
	}

	private void sendMessage(Message msg) {
		if( mHandler != null )
			mHandler.dispatchMessage(msg);
	}

	private void sendMessage(Enum event) {
		if( mHandler != null && !mHandler.sendEmptyMessage(event.ordinal()) )
			Log.e(TAG, "Cant not send message (%s) to handler", event);
	}
	
	private void sendMessage(Enum event, Enum reason) { 
		if(mHandler != null) {
			Message msg = mHandler.obtainMessage();
			msg.what = event.ordinal();
			msg.arg1 = reason.ordinal();
			msg.sendToTarget();
		}
	}

	private void processMessage(Message msg) {
		
		States beforeEventState = mState;
		
		//received message
		mLastMessage = msg;
		Events event = Events.values()[msg.what];
		SubEvents subEvent = SubEvents.values()[msg.arg1];
		Log.d(TAG, "START: Event: %s; Subevent: %s;", event, subEvent);

		switch(mState) {
		case OFF:
			
			if( event == Events.INIT ) {
				mWifiProvisioner.init();
		        updateState(States.BOOTING);
			} 
			break;
		
		case BOOTING:
			
			if( event == Events.WIFI_ON ) {
				mWifiProvisioner.flushAllConfiguredNetworks();
				//Testing things
				mWifiProvisioner.connectTo("cozybit", null, 5, "WPA", "cozy but insecure!");
				//mWifiProvisioner.connectTo("MiFi", null, 5, "none", "holahola");
				updateState(States.IDLE);
			} else if ( event == Events.WIFI_OFF ) {
				updateState(States.FAILED);
			} else if( event == Events.INIT_ERROR ) {
				updateState(States.FAILED);
			}
			break;
			
		case IDLE:
			
			if( event == Events.CONNECT_TO_WIFI ) {
				//Get stuff from OnboardeeSharedInfo
				//mWifiProvisioner.connectTo("cozybit", null, 5, "WPA", "cozy but insecure!");
			} 
			
			break;
			
		case CONNECTING:
			// OPEN:
			//   If everything works: ASSOCIATING/DE, OBTAINING_IPADDR, CONNECTED
			
			// WEP: doesn't work
			// WPA PSK: doesn't work
			//
			// WPA2 PSK:
			//   If everything works: ASSOCIATING/DE, OBTAINING_IPADDR, CONNECTED
			//   IF wrong password: ASSOCIATING/DE, FOR_WAY_HANDSHAKE, DISCONNECTED
			if ( event == Events.INVALID_PARAMETERS ) {
				updateState(States.IDLE);
			} else if ( event == Events.WIFI_CONNECTED ) {
				updateState(States.CONNECTED);
			}
			//TODO: missing the connecting updates
			break;
			
		case CONNECTED:
			if ( event == Events.FORGET_WIFI ) {
				mWifiProvisioner.flushAllConfiguredNetworks();
				updateState(States.IDLE);
			} else if( event == Events.WIFI_DISCONNECTED ) {
				updateState(States.CONNECTING);
			} 
			
			break;
			
		case FAILED:
			//TODO: we need a way to restart from here.
			break;
		}
		
		if(mState != States.OFF) {
			if( event == Events.SHUT_DOWN ) {
	        	updateState(States.OFF);
	        	mWifiProvisioner.tearDown();
	        	resetStateMachineVars();
	        } else if ( event == Events.WIFI_OFF || event == Events.WIFI_FAILED ) {
	        	updateState(States.FAILED);
	        }
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
		//TODO nothing to do yet
	}

}