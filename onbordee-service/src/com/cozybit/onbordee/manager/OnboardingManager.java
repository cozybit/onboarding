package com.cozybit.onbordee.manager;

import com.cozybit.onbordee.ble.BleProvisioner;
import com.cozybit.onbordee.ble.OnboardingGattService;
import com.cozybit.onbordee.constants.OnboardingCommands;
import com.cozybit.onbordee.constants.OnboardingCommands.LongStatus;
import com.cozybit.onbordee.constants.OnboardingCommands.OnboardingStates;
import com.cozybit.onbordee.manager.ConnectionManager;
import com.cozybit.onbordee.manager.ConnectionManager.DataReceivedCallback.DataTypes;
import com.cozybit.onbordee.utils.Log;
import com.cozybit.onbordee.wifi.WifiProvisioner;
import com.cozybit.onbordee.wifi.WifiProvisioner.WifiProvisionerCallback;

import android.content.Context;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.SupplicantState;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;

public class OnboardingManager {
	
	public final static String TAG = OnboardingManager.class.getName();
	
	enum States {
		OFF, BOOTING, READY,
		CONNECTING,	CONNECTED, FAILED
	}
	
	public enum Events {
		INIT, SHUT_DOWN,
		WIFI_ON, WIFI_OFF, WIFI_FAILED, NO_WIFI,
		PARAMETER_RECEIVED, INVALID_PARAMETERS, CONNECT_TO_WIFI, FORGET_WIFI,
		WIFI_CONNECTED, WIFI_DISCONNECTED, WIFI_CONNECTING_UPDATE, WIFI_LINK_DISCONNECTED
	}

	private Context mContext;
	private HandlerThread mHandlerThread;
	private Handler mHandler;
	
	private States mState;
	private States mPreviusState;
	private Message mLastMessage;
	
	private WifiProvisioner mWifiProvisioner;
	private BleProvisioner mBleProvisioner;
	private OnboardingCredentials mCredentials;

	private class OnboardingCredentials {

		public String SSID;
		//public int channel = -1;
		public int channel = 5;
		//public int auth;
		public int auth = OnboardingCommands.AUTH_WPA_PSK;
		public String password;

		public boolean validateSSID() {
			return ( SSID != null && !SSID.isEmpty() );
		}

		public boolean validateChannel() {
			return (channel >= 1 && channel <= 13);
		}

		public boolean validateAuthentication() {
			return (auth >= 0 && auth <=3);
		}

		public boolean validatePassword() {
			if (auth != OnboardingCommands.AUTH_NONE )
				return ( password != null && !password.isEmpty() );
			return true;
		}

		public boolean validateCredentials() {
			return ( validateSSID() && validateChannel() &&
					validateAuthentication() && validatePassword() );
		}
	}
	
	/*
	 * Start of Callback implementations
	 */
	
	//Callbacks from the Wifi Provisioner reporting Wifi related events 
	private	 WifiProvisionerCallback mWifiProvisionerCallback = new WifiProvisionerCallback() { 
		
		@Override
		public void onWifiIfaceStatus(WifiIfaceState status) {
			
			switch (status) {
			case WIFI_OFF:
				sendMessage(Events.WIFI_OFF);
				break;
			case WIFI_ON:
				sendMessage(Events.WIFI_ON);
				break;
			case WIFI_FAILED:
				sendMessage(Events.WIFI_FAILED);
				break;
			case NO_WIFI:
				sendMessage(Events.NO_WIFI);
				break;
			}
		}
		
		@Override
		public void onWifiLinkStatus(SupplicantState state) {
			switch (state) {
			case AUTHENTICATING:
				sendMessage(Events.WIFI_CONNECTING_UPDATE, SupplicantState.AUTHENTICATING);
				break;
			case ASSOCIATING:
				sendMessage(Events.WIFI_CONNECTING_UPDATE, SupplicantState.ASSOCIATING);
				break;
			case DISCONNECTED:
				sendMessage(Events.WIFI_LINK_DISCONNECTED);
				break;
			default:
				//Do nothing
				break;
			}
		}
		
		@Override
		public void onWifiNetworkStatus(DetailedState status) {
			
			switch (status) {
			case OBTAINING_IPADDR:
				sendMessage(Events.WIFI_CONNECTING_UPDATE, DetailedState.OBTAINING_IPADDR);
				break;
			case CONNECTED:
				sendMessage(Events.WIFI_CONNECTED);
				break;
			case DISCONNECTED:
				sendMessage(Events.WIFI_DISCONNECTED);
				break;
			case FAILED:
				break;
			default:
				//Do nothing
				break;
			}
		}
	};

	//Callback from the ConnectionManager reporting the received data that needs to be process here
	private ConnectionManager.DataReceivedCallback mDataReceivedCallback = new ConnectionManager.DataReceivedCallback() {

		@Override
		public void onDataReceived(DataTypes type, byte[] data) {
			
			Log.d(TAG, "Received %s -> Int: %d | String %s", type, data[0], new String(data));
			
			switch (type) {
			case CMD:
				byte cmd = data[0];
				if ( cmd == OnboardingCommands.CMD_CONNECT) {
					sendMessage(Events.CONNECT_TO_WIFI);
				} else if ( cmd == OnboardingCommands.CMD_DISCONNECT) {
					sendMessage(Events.FORGET_WIFI);
				} else if ( cmd == OnboardingCommands.CMD_RESET ) {
					//TODO
				} else {
					Log.d(TAG, "Unknown onboarding command:%d", cmd);
				}
				break;
			//case AUTH:
			case CHANNEL:
			case PASS:
			case SSID:
				Message msg = Message.obtain();
				msg.what = Events.PARAMETER_RECEIVED.ordinal();
				msg.arg1 = type.ordinal();
				msg.obj = data;
				sendMessage(msg);
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
		mHandlerThread = new HandlerThread ("OnboardingManagerHandlerThread", Process.THREAD_PRIORITY_BACKGROUND);
		mHandlerThread.start();                                                                                                                                                                           		
		mHandler = new Handler(mHandlerThread.getLooper(), new Handler.Callback() {

			@Override
			public boolean handleMessage(Message msg) {
				processMessage(msg);
				return false;
			}
		});
	}
	
	public ConnectionManager.DataReceivedCallback getDataReceivedCallback() {
		return mDataReceivedCallback;
	}
	
	public void setBleProvisioner(BleProvisioner bleProvisioner) {
		mBleProvisioner = bleProvisioner;
	}

	public void init() {
		if( mBleProvisioner == null) {
			Log.e(TAG, "Bluetooth Provisioner never set. Don't init.");
			return;
		}
		
		if( mState == States.OFF )
			sendMessage(Events.INIT);
	}
	
	public boolean isRunning() {
		return (mState != States.OFF);
	}

	public void shutDown() {
		processMessage(Message.obtain(null, Events.SHUT_DOWN.ordinal()));
	}
	
	private void sendMessage(Message msg) {
		if( mHandler != null) mHandler.sendMessage(msg);
	}

	private void sendMessage(Enum event) {
		if( mHandler != null && !mHandler.sendEmptyMessage(event.ordinal()) )
			Log.e(TAG, "Cant not send message (%s) to handler", event);
	}
	
	private void sendMessage(Enum event, Object obj) { 
		if(mHandler != null) {
			Message msg = mHandler.obtainMessage();
			msg.what = event.ordinal();
			msg.obj = obj;
			msg.sendToTarget();
		}
	}

	private void processMessage(Message msg) {
		
		States beforeEventState = mState;
		
		//received message
		mLastMessage = msg;
		Events event = Events.values()[msg.what];
		Log.d(TAG, "START: Event: %s", event);

		switch(mState) {
		case OFF:
			
			if( event == Events.INIT ) {
				mCredentials = new OnboardingCredentials();
				mWifiProvisioner.init();
		        updateState(States.BOOTING);
			} 
			break;
		
		case BOOTING:
			
			if( event == Events.WIFI_ON ) {
				mWifiProvisioner.flushAllConfiguredNetworks();
				updateState(States.READY);
			} else if ( event == Events.WIFI_OFF || event == Events.NO_WIFI ) {
				updateState(States.FAILED);
			}
			break;
			
		case READY:
			if( event == Events.PARAMETER_RECEIVED ) {
				DataTypes type = DataTypes.values()[msg.arg1];
				byte[] data = (byte[]) msg.obj;
				
				if( type == DataTypes.AUTH )
					mCredentials.auth = data[0];
				else if( type == DataTypes.CHANNEL )
					mCredentials.channel = data[0];
				else if( type == DataTypes.PASS )
					mCredentials.password = new String(data);
				else if( type == DataTypes.SSID )
					mCredentials.SSID = new String(data);
				
				reportOnboardeeState(OnboardingStates.INITIALIZING, LongStatus.RECEVING_PARAMS);
				
			} else if( event == Events.CONNECT_TO_WIFI ) {
				//Testing things
				//mWifiProvisioner.connectTo("cozybit", null, 5, "WPA", "cozy but secure!");
				//mWifiProvisioner.connectTo("MiFi", null, 5, "WPA", "holahola");
				mWifiProvisioner.connectTo(mCredentials.SSID, null, mCredentials.channel, "WPA", mCredentials.password);
				updateState(States.CONNECTING);
				reportOnboardeeState(OnboardingStates.CONNECTING, LongStatus.NONE);
				if( !mCredentials.validateCredentials() ) {
					Log.d(TAG, "Oboarding paramterers -> SSID: %s; Ch: %d; Auth: %d; Pswd: %s", 
							mCredentials.SSID, mCredentials.channel, mCredentials.auth, mCredentials.password);
					sendMessage(Events.INVALID_PARAMETERS);
				}
			}
			
			break;
			
		case CONNECTING:
			if ( event == Events.INVALID_PARAMETERS ) {
				reportOnboardeeState(OnboardingStates.FAILED, LongStatus.INVALID_PARAMS);
				updateState(States.FAILED);
				
			} else if ( event == Events.WIFI_CONNECTING_UPDATE ) {
				Enum update = (Enum) msg.obj;
				if( update == SupplicantState.AUTHENTICATING ) {
					reportOnboardeeState(OnboardingStates.CONNECTING, LongStatus.AUTHING);
				} else if( update == SupplicantState.ASSOCIATING ) {
					reportOnboardeeState(OnboardingStates.CONNECTING, LongStatus.ASSOCIATING);
				} else if( update == DetailedState.OBTAINING_IPADDR ) {
					reportOnboardeeState(OnboardingStates.CONNECTING, LongStatus.GETTING_IP);
				}
				
			} else if ( event == Events.WIFI_CONNECTED ) {
				reportOnboardeeState(OnboardingStates.CONNECTED, LongStatus.NONE);
				updateState(States.CONNECTED);
				
			} else if ( event == Events.WIFI_LINK_DISCONNECTED ) {
				reportOnboardeeState(OnboardingStates.FAILED, LongStatus.WIFI_LINK_DISCONN);
				updateState(States.FAILED);
				
			} 
			break;
			
		case CONNECTED:
			if ( event == Events.FORGET_WIFI ) {
				mWifiProvisioner.flushAllConfiguredNetworks();
				updateState(States.READY);
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
	        	mWifiProvisioner.stop();
	        	resetStateMachineVars();
	        } else if ( event == Events.WIFI_OFF || event == Events.WIFI_FAILED ) {
	        	updateState(States.FAILED);
	        }
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
	
	private void reportOnboardeeState(OnboardingStates states, LongStatus longStatus) {
		mBleProvisioner.updateCharacteristic(OnboardingGattService.CHARACTERISTIC_STATUS, states.name().toString());
		mBleProvisioner.updateCharacteristic(OnboardingGattService.CHARACTERISTIC_LONG_STATUS, longStatus.name().toString());
	}
	
	private void resetStateMachineVars() {
		mCredentials = null;
	}

}