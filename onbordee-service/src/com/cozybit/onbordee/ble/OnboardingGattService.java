package com.cozybit.onbordee.ble;

import java.util.UUID;

public class OnboardingGattService {
	
	/* Service UUID */
	/*FIXME: for some reason, android devices can't discover our custom service with this UUID. 
	 *  don'tknow if the reason is that the UUID is malformed and it's using some bits that it's
	 *  not supposed to, or that the Android device is looking for a range of services, etc. 
	 *  It's a mystery. For now, use the HEART RATE PROFILE UUID as a workaround.
	 */
	//public static final UUID SERVICE_UUID = UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000000");
	public static final UUID SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");

	/* Characteristics UUIDs */
	public static final UUID CHARACTERISTIC_STATUS = UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000001");
	public static final UUID CHARACTERISTIC_LONG_STATUS = UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000002");
	public static final UUID CHARACTERISTIC_SSID = UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000003");
	public static final UUID CHARACTERISTIC_AUTH = UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000004");
	public static final UUID CHARACTERISTIC_PASS = UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000005");
	public static final UUID CHARACTERISTIC_CHANNEL = UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000006");
	public static final UUID CHARACTERISTIC_COMMAND = UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000007");
	public static final UUID CHARACTERISTIC_VENDOR_ID = UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000008");
	public static final UUID CHARACTERISTIC_DEVICE_ID = UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000009");

}
