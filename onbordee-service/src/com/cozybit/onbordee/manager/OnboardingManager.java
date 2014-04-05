package com.cozybit.onbordee.manager;

import java.util.Arrays;

import com.cozybit.onbordee.ble.BleProvisioner;
import com.cozybit.onbordee.manager.ConnectionManager;
import com.cozybit.onbordee.profile.OnboardingProfile;
import com.cozybit.onbordee.profile.OnboardingProfile.Auth;
import com.cozybit.onbordee.profile.OnboardingProfile.Commands;
import com.cozybit.onbordee.profile.OnboardingProfile.Characteristics;
import com.cozybit.onbordee.profile.OnboardingProfile.Events;
import com.cozybit.onbordee.profile.OnboardingProfile.States;
import com.cozybit.onbordee.utils.Log;
import com.cozybit.onbordee.wifi.WifiProvisioner;
import com.cozybit.onbordee.wifi.WifiProvisioner.Authentication;
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
	private States mState = States.OFF;
	private States mPreviusState;
	private Events mEvent = Events.SHUT_DOWN;
	private Events mPreviousEvent;
	
	private WifiProvisioner mWifiProvisioner;
	private BleProvisioner mBleProvisioner;
	private OnboardingCredentials mCredentials;

	private class OnboardingCredentials {

		public String SSID;
		//public int channel = -1;
		public int channel = 5;
		//public int auth;
		public Authentication auth = Authentication.UNKNOWN;
		public String password;

		public boolean validateSSID() {
			return ( SSID != null && !SSID.isEmpty() );
		}

		public boolean validateChannel() {
			return (channel >= 1 && channel <= 13);
		}

		public boolean validatePassword() {
			if (auth != Authentication.OPEN )
				return ( password != null && !password.isEmpty() );
			return true;
		}

		public boolean validateCredentials() {
			return ( validateSSID() && validateChannel() && validatePassword() );
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
				sendMessage(Events.AUTHENTICATING);
				break;
			case ASSOCIATING:
				sendMessage(Events.ASSOCIATING);
				break;
			case DISCONNECTED:
				sendMessage(Events.WIFI_LINK_DISCONNECTED);
				break;
			default:
				//Do nothing
				break;
			}
		}
		
		public void onWifiNetworkStatus(DetailedState status) {
			
			switch (status) {
			case OBTAINING_IPADDR:
				sendMessage(Events.GETTING_IPADDR);
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
		public void onDataReceived(OnboardingProfile.Characteristics type, byte[] data) {
			
			//Log.d(TAG, "Received %s -> Int: %d | String %s", type, data[0], new String(data));
			Log.d(TAG, "Charac. Type %s; Value:%s", type, Arrays.toString(data));
			
			switch (type) {
			case COMMAND:
				
				//Data should be the index of the enums
				int cmd = (int) data[0];
				
				if ( cmd == Commands.CONNECT.ordinal() ) {
					sendMessage(Events.CONNECT_TO_WIFI);
				} else if ( cmd == Commands.DISCONNECT.ordinal() ) {
					sendMessage(Events.FORGET_WIFI);
				} else if ( cmd ==  Commands.RESET.ordinal() ) {
					//TODO
				} else {
					Log.d(TAG, "Unknown onboarding command: %d", cmd);
				}
				break;
			
			case AUTH:
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
	
	public States getCurrentState() {
		return mState;
	}
	
	public Events getCurrentEvent() {
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
		
		Events event = Events.values()[msg.what];
		if(mState != States.FAILED) updateEvent(event);
		Log.d(TAG, "START: Event: %s", event);

		switch(mState) {
		case OFF:
			
			if( event == Events.INIT ) {
				mCredentials = new OnboardingCredentials();
				mWifiProvisioner.init();
		        updateState(States.BOOTING);
		        updateEvent(Events.NONE);
			} 
			break;
		
		case BOOTING:
			
			if( event == Events.WIFI_ON ) {
				mWifiProvisioner.flushAllConfiguredNetworks();
				updateState(States.READY);
				updateEvent(Events.NONE);
			} else if ( event == Events.WIFI_OFF || event == Events.NO_WIFI || event == Events.WIFI_FAILED ) {
				updateState(States.FAILED);
			}
			break;
			
		case READY:
			if( event == Events.PARAMETER_RECEIVED ) {
				OnboardingProfile.Characteristics type = OnboardingProfile.Characteristics.values()[msg.arg1];
				byte[] data = (byte[]) msg.obj;
				
				switch (type) {
				case AUTH:
					int index = (int) data[0];
					Auth auth = (index >= 0 && index < 4) ? Auth.values()[index] : Auth.UNKNOWN;
					//Translate OnboardingProfile.AUTH to WifiProvisioner.Authentication values
					if( auth == Auth.OPEN ) mCredentials.auth = Authentication.OPEN;
					else if( auth == Auth.WEP ) mCredentials.auth = Authentication.WEP;
					else if( auth == Auth.WPA_PSK ) mCredentials.auth = Authentication.WPA_PSK;
					else mCredentials.auth = Authentication.UNKNOWN;
					 
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
				
				updateState(States.READY);
				
			} else if( event == Events.CONNECT_TO_WIFI ) {
				//Testing things
				//mWifiProvisioner.connectTo("cozybit", null, 5, "WPA", "cozy but secure!");
				//mWifiProvisioner.connectTo("MiFi", null, 5, "WPA", "holahola");
				Log.d(TAG, "Oboarding paramterers -> SSID: %s; Ch: %d; Auth: %s; Pswd: %s", 
						mCredentials.SSID, mCredentials.channel, mCredentials.auth, mCredentials.password);
				mWifiProvisioner.connectTo(mCredentials.SSID, null, mCredentials.channel, mCredentials.auth, mCredentials.password);
				updateState(States.CONNECTING);
				if( !mCredentials.validateCredentials() ) {
					Log.d(TAG, "Oboarding paramterers -> SSID: %s; Ch: %d; Auth: %d; Pswd: %s", 
							mCredentials.SSID, mCredentials.channel, mCredentials.auth, mCredentials.password);
					sendMessage(Events.INVALID_PARAMETERS);
				}
			}
			
			break;
			
		case CONNECTING:
			if ( event == Events.INVALID_PARAMETERS ) {
				updateState(States.FAILED);
				
			} else if ( event == Events.WIFI_CONNECTED ) {
				updateState(States.CONNECTED);
				
			} else if ( event == Events.WIFI_LINK_DISCONNECTED ) {
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
		
		if (mBleProvisioner != null) {
			if( !mBleProvisioner.updateCharacteristic(Characteristics.STATUS.uuid, (byte) newState.ordinal(), true) )
				Log.e(TAG, "ERROR: bleProvisioner couldn't update the value of the characteristic %s", Characteristics.STATUS);
			
		} else
			Log.w(TAG, "WARNING: bleProvisioner is being used but is NULL!");
	}
	
	private void updateEvent(Events event) {
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