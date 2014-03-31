package com.cozybit.onbordee.ble;

import java.util.HashMap;
import java.util.Map;

import com.cozybit.onbordee.manager.IManager;
import com.cozybit.onbordee.manager.ConnectionManager.Events;
import com.cozybit.onbordee.utils.Log;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BtBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = BtBroadcastReceiver.class.getName();
	private IManager mBtConnMngr;
	
	public BtBroadcastReceiver(IManager manager) {
		mBtConnMngr = manager;
	}
	
	//Translate real BT States code to something more human friendly. 
	private enum BtStates {
		UNKNOWN_STATE(-1),
		STATE_OFF(10),
		STATE_TURNING_ON(11),
		STATE_ON(12),
		STATE_TURNING_OFF(13);
		
		public final int code;
		private static Map<Integer, BtStates> map = new HashMap<Integer, BtStates>();
		
	    static {
	        for (BtStates state : BtStates.values())
	            map.put(state.code, state);
	    }

	    private BtStates(int code) { this.code = code; }
	    public static BtStates valueOf(int code) { return map.get(code); }
	} 
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		String action = intent.getAction();
		Log.d(TAG, "Intent received -> action: %s", action);
				
		if( action.equals(BluetoothAdapter.ACTION_STATE_CHANGED) ) {
						
			BtStates state = BtStates.valueOf( intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) );
			BtStates previousState = BtStates.valueOf( intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1) );
			Log.d(TAG, "BT State transition: %s -> %s", previousState, state);
			switch(state) {
			case STATE_OFF:
				mBtConnMngr.sendMessage(Events.BLUETOOTH_OFF);
				break;
			case STATE_TURNING_OFF:
				break;
			case STATE_ON:
				mBtConnMngr.sendMessage(Events.BLUETOOTH_ON);
				break;
			case STATE_TURNING_ON:
				break; 
			}
		}
	}
	
}