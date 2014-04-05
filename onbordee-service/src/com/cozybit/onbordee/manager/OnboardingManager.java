package com.cozybit.onbordee.manager;

import java.util.Arrays;

import com.cozybit.onbordee.ble.BleProvisioner;
import com.cozybit.onbordee.manager.ConnectionManager;
import com.cozybit.onbordee.profile.OnboardingProfile;
import com.cozybit.onbordee.profile.OnboardingProfile.COMMANDS;
import com.cozybit.onbordee.profile.OnboardingProfile.Characteristics;
import com.cozybit.onbordee.profile.OnboardingProfile.EVENTS;
import com.cozybit.onbordee.profile.OnboardingProfile.STATES;
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

	/*
	 * The STATES and EVENTS handled in this state machine are defined in the OnboardingProfiled.class
	 * */

	private Context mContext;
	private HandlerThread mHandlerThread;
	private Handler mHandler;
	
	/* The states of this state machine are defined in OnboardingProfile */
	private STATES mState = STATES.OFF;
	private STATES mPreviusState;
	private EVENTS mEvent = EVENTS.SHUT_DOWN;
	private EVENTS mPreviousEvent;
	
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
				sendMessage(EVENTS.WIFI_OFF);
				break;
			case WIFI_ON:
				sendMessage(EVENTS.WIFI_ON);
				break;
			case WIFI_FAILED:
				sendMessage(EVENTS.WIFI_FAILED);
				break;
			case NO_WIFI:
				sendMessage(EVENTS.NO_WIFI);
				break;
			}
		}
		
		@Override
		public void onWifiLinkStatus(SupplicantState state) {
			switch (state) {
			case AUTHENTICATING:
				sendMessage(EVENTS.AUTHENTICATING);
				break;
			case ASSOCIATING:
				sendMessage(EVENTS.ASSOCIATING);
				break;
			case DISCONNECTED:
				sendMessage(EVENTS.WIFI_LINK_DISCONNECTED);
				break;
			default:
				//Do nothing
				break;
			}
		}
		
		public void onWifiNetworkStatus(DetailedState status) {
			
			switch (status) {
			case OBTAINING_IPADDR:
				sendMessage(EVENTS.GETTING_IPADDR);
				break;
			case CONNECTED:
				sendMessage(EVENTS.WIFI_CONNECTED);
				break;
			case DISCONNECTED:
				sendMessage(EVENTS.WIFI_DISCONNECTED);
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
		public void onDataReceived(OnboardingProfile.Characteristics type, byte[] data) {
			
			//Log.d(TAG, "Received %s -> Int: %d | String %s", type, data[0], new String(data));
			Log.d(TAG, "Charac. Type %s; Value:%s", type, Arrays.toString(data));
			
			switch (type) {
			case COMMAND:
				
				//Data should be the index of the enums
				int cmd = (int) data[0];
				
				if ( cmd == COMMANDS.CONNECT.ordinal() ) {
					sendMessage(EVENTS.CONNECT_TO_WIFI);
				} else if ( cmd == COMMANDS.DISCONNECT.ordinal() ) {
					sendMessage(EVENTS.FORGET_WIFI);
				} else if ( cmd ==  COMMANDS.RESET.ordinal() ) {
					//TODO
				} else {
					Log.d(TAG, "Unknown onboarding command: %d", cmd);
				}
				break;
			
			//TODO unignore THIS!!!
			//case AUTH:
			case CHANNEL:
			case PASS:
			case SSID:
				Message msg = Message.obtain();
				msg.what = EVENTS.PARAMETER_RECEIVED.ordinal();
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
		mState = STATES.OFF;
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
	
	public STATES getCurrentState() {
		return mState;
	}
	
	public EVENTS getCurrentEvent() {
		return mEvent;
	}
	
	public void setBleProvisioner(BleProvisioner bleProvisioner) {
		mBleProvisioner = bleProvisioner;
	}

	public void init() {
		if( mBleProvisioner == null) {
			Log.e(TAG, "Bluetooth Provisioner never set. Don't init.");
			return;
		}
		
		if( mState == STATES.OFF )
			sendMessage(EVENTS.INIT);
	}
	
	public boolean isRunning() {
		return (mState != STATES.OFF);
	}

	public void shutDown() {
		processMessage(Message.obtain(null, EVENTS.SHUT_DOWN.ordinal()));
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
		
		STATES beforeEventState = mState;
		
		EVENTS event = EVENTS.values()[msg.what];
		if(mState != STATES.FAILED) updateEvent(event);
		Log.d(TAG, "START: Event: %s", event);

		switch(mState) {
		case OFF:
			
			if( event == EVENTS.INIT ) {
				mCredentials = new OnboardingCredentials();
				mWifiProvisioner.init();
		        updateState(STATES.BOOTING);
		        updateEvent(EVENTS.NONE);
			} 
			break;
		
		case BOOTING:
			
			if( event == EVENTS.WIFI_ON ) {
				mWifiProvisioner.flushAllConfiguredNetworks();
				updateState(STATES.READY);
				updateEvent(EVENTS.NONE);
			} else if ( event == EVENTS.WIFI_OFF || event == EVENTS.NO_WIFI || event == EVENTS.WIFI_FAILED ) {
				updateState(STATES.FAILED);
			}
			break;
			
		case READY:
			if( event == EVENTS.PARAMETER_RECEIVED ) {
				OnboardingProfile.Characteristics type = OnboardingProfile.Characteristics.values()[msg.arg1];
				byte[] data = (byte[]) msg.obj;
				
				switch (type) {
				case AUTH:
					mCredentials.auth = data[0];
					break;
				case CHANNEL:
					mCredentials.channel = data[0];
					break;
				case PASS:
					mCredentials.password = new String(data);
					break;
				case SSID:
					mCredentials.SSID = new String(data);
					break;
				}
				
				updateState(STATES.READY);
				
			} else if( event == EVENTS.CONNECT_TO_WIFI ) {
				//Testing things
				//mWifiProvisioner.connectTo("cozybit", null, 5, "WPA", "cozy but secure!");
				//mWifiProvisioner.connectTo("MiFi", null, 5, "WPA", "holahola");
				mWifiProvisioner.connectTo(mCredentials.SSID, null, mCredentials.channel, "WPA", mCredentials.password);
				updateState(STATES.CONNECTING);
				if( !mCredentials.validateCredentials() ) {
					Log.d(TAG, "Oboarding paramterers -> SSID: %s; Ch: %d; Auth: %d; Pswd: %s", 
							mCredentials.SSID, mCredentials.channel, mCredentials.auth, mCredentials.password);
					sendMessage(EVENTS.INVALID_PARAMETERS);
				}
			}
			
			break;
			
		case CONNECTING:
			if ( event == EVENTS.INVALID_PARAMETERS ) {
				updateState(STATES.FAILED);
				
			} else if ( event == EVENTS.WIFI_CONNECTED ) {
				updateState(STATES.CONNECTED);
				
			} else if ( event == EVENTS.WIFI_LINK_DISCONNECTED ) {
				updateState(STATES.FAILED);
				
			} 
			break;
			
		case CONNECTED:
			if ( event == EVENTS.FORGET_WIFI ) {
				mWifiProvisioner.flushAllConfiguredNetworks();
				updateState(STATES.READY);
			} else if( event == EVENTS.WIFI_DISCONNECTED ) {
				updateState(STATES.CONNECTING);
			} 
			
			break;
			
		case FAILED:
			//TODO: we need a way to restart from here.
			break;
		}
		
		if(mState != STATES.OFF) {
			if( event == EVENTS.SHUT_DOWN ) {
	        	updateState(STATES.OFF);
	        	mWifiProvisioner.stop();
	        	resetStateMachineVars();
	        } else if ( event == EVENTS.WIFI_OFF || event == EVENTS.WIFI_FAILED ) {
	        	updateState(STATES.FAILED);
	        }
		}
		
		Log.d(TAG, "END: Event: %s; Transition: %s -> %s", event, beforeEventState, mState );
	}
	
    /*------------------------
     * End of State Machine
     *------------------------*/
	
	private void updateState(STATES newState) {
		mPreviusState = mState;
		mState = newState;
		
		if (mBleProvisioner != null) {
			if( !mBleProvisioner.updateCharacteristic(Characteristics.STATUS.uuid, (byte) newState.ordinal(), true) )
				Log.e(TAG, "ERROR: bleProvisioner couldn't update the value of the characteristic %s", Characteristics.STATUS);
			
		} else
			Log.w(TAG, "WARNING: bleProvisioner is being used but is NULL!");
	}
	
	private void updateEvent(EVENTS event) {
		mPreviousEvent = mEvent;
		mEvent = event;
		
		if (mBleProvisioner != null) {
			if( !mBleProvisioner.updateCharacteristic(Characteristics.LONG_STATUS.uuid, (byte) event.ordinal(), true) )
				Log.e(TAG, "ERROR: bleProvisioner couldn't update the value of the characteristic %s", Characteristics.STATUS);
		} else
			Log.w(TAG, "WARNING: bleProvisioner is being used but is NULL!");
	}

	private void resetStateMachineVars() {
		mCredentials = null;
	}

}