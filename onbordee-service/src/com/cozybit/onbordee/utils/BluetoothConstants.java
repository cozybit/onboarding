package com.cozybit.onbordee.utils;

import android.util.SparseArray;

//Helper class to translate from status codes to something more human friendly
public class BluetoothConstants {

	public static enum GATT_STATUS {
		FAILURE(257),
		INSUFFICIENT_AUTHENTICATION(5),
		INSUFFICIENT_ENCRYPTION(15),
		INVALID_ATTRIBUTE_LENGTH(13),
		INVALID_OFFSET(7),
		READ_NOT_PERMITTED(2),
		REQUEST_NOT_SUPPORTED(6),
		SUCCESS(0),
		WRITE_NOT_PERMITTED(3);

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
		CONNECTED(2),
		CONNECTING(1),
		DISCONNECTED(0),
		DISCONNECTING(3);

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
