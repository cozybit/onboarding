package com.cozybit.onboarding.utils;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.util.SparseArray;

//Helper class to translate from status codes to something more human friendly
public class BluetoothConstants {

	public static enum GATT_STATUS {
		FAILURE(BluetoothGatt.GATT_FAILURE),
		INSUFFICIENT_AUTHENTICATION(BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION),
		INSUFFICIENT_ENCRYPTION(BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION),
		INVALID_ATTRIBUTE_LENGTH(BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH),
		INVALID_OFFSET(BluetoothGatt.GATT_INVALID_OFFSET),
		READ_NOT_PERMITTED(BluetoothGatt.GATT_READ_NOT_PERMITTED),
		REQUEST_NOT_SUPPORTED(BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED),
		SUCCESS(BluetoothGatt.GATT_SUCCESS),
		WRITE_NOT_PERMITTED(BluetoothGatt.GATT_WRITE_NOT_PERMITTED);

		public final int code;

		private final static SparseArray<GATT_STATUS> map = new SparseArray<GATT_STATUS>();
	    static {
	        for (GATT_STATUS status : GATT_STATUS.values())
	        	map.append(status.code, status);
	    }

		private GATT_STATUS(int code) {
			this.code = code;
		}

		public static GATT_STATUS valueOf(int code) {
			return map.get(code);
		}
	}
	
	public static enum BT_PROFILE_STATE  {
		CONNECTED(BluetoothProfile.STATE_CONNECTED),
		CONNECTING(BluetoothProfile.STATE_CONNECTING),
		DISCONNECTED(BluetoothProfile.STATE_DISCONNECTED),
		DISCONNECTING(BluetoothProfile.STATE_DISCONNECTING);

		public final int code;

		private final static SparseArray<BT_PROFILE_STATE> map = new SparseArray<BT_PROFILE_STATE>();
	    static {
	        for (BT_PROFILE_STATE status : BT_PROFILE_STATE.values())
	        	map.append(status.code, status);
	    }

		private BT_PROFILE_STATE(int code) {
			this.code = code;
		}

		public static BT_PROFILE_STATE valueOf(int code) {
			return map.get(code);
		}
	}
}
