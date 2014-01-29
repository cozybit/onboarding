package com.cozybit.onboarding.ble;

import java.util.UUID;

public class OnboardingGattService {
	
	/* Service UUID */
	public static final UUID SERVICE_UUID = UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000000");

	/* Characteristics UUIDs */
	public static final UUID CHARACTERISTIC_STATUS = UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000001");
	public static final UUID CHARACTERISTIC_LONG_STATUS = UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000002");
	public static final UUID CHARACTERISTIC_SSID = UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000003");
	public static final UUID CHARACTERISTIC_AUTH = UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000004");
	public static final UUID CHARACTERISTIC_PASS = UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000005");
	public static final UUID CHARACTERISTIC_CHANNEL = UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000006");
	public static final UUID CHARACTERISTIC_COMMAND = UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000007");

}
