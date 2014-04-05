package com.cozybit.onboarding.profile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import android.bluetooth.BluetoothGattCharacteristic;

public class OnboardingProfile {

	/* Service UUID */
	/*FIXME: for some reason, android devices can't discover our custom service with this UUID. 
	 *  don'tknow if the reason is that the UUID is malformed and it's using some bits that it's
	 *  not supposed to, or that the Android device is looking for a range of services, etc. 
	 *  It's a mystery. For now, use the HEART RATE PROFILE UUID as a workaround.
	 */
	//public static final UUID SERVICE_UUID = UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000000");
	public static final UUID SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");

	
	private final static int PROP_R = BluetoothGattCharacteristic.PROPERTY_READ;
	private final static int PROP_W = BluetoothGattCharacteristic.PROPERTY_WRITE;
	private final static int PER_R = BluetoothGattCharacteristic.PERMISSION_READ;
	private final static int PER_W = BluetoothGattCharacteristic.PERMISSION_WRITE;
	
	// Definition of the onboarding characteristics: UUIDs, properties and permissions
	public static enum Characteristics {
		STATUS( UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000001"), PROP_R, PER_R),
		LONG_STATUS(UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000002"), PROP_R, PER_R),
		SSID(UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000003"), PROP_R | PROP_W, PER_R | PER_W),
		AUTH(UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000004"), PROP_R | PROP_W, PER_R | PER_W),
		PASS(UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000005"), PROP_R | PROP_W, PER_R | PER_W),
		CHANNEL(UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000006"), PROP_R | PROP_W, PER_R | PER_W),
		COMMAND(UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000007"), PROP_W, PER_W),
		VENDOR_ID(UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000008"), PROP_R, PER_R),
		DEVICE_ID(UUID.fromString("FFFFFFFF-C0C1-FFFF-C0C1-201401000009"), PROP_R, PER_R);

		private final static Map<UUID, Characteristics> map = new HashMap<UUID, Characteristics>();
	    static {
	        for (Characteristics c : Characteristics.values())
	            map.put(c.uuid, c);
	    }
	    
		final public UUID uuid;
		final public int properties;
		final public int permissions;

		private Characteristics(UUID uuid, int properties, int permissions) {
			this.uuid = uuid;
			this.properties = properties;
			this.permissions = permissions;
		}

	    public static Characteristics valueOf(UUID uuid) {
	    	return map.get(uuid);
	    }
	};

	/*
	 * OPTIONAL VALUES FOR THE DIFFERENT CHARACTERISTICS
	 */

	public static enum STATES {
		OFF, BOOTING, READY, CONNECTING, CONNECTED, FAILED
	};

	public enum EVENTS {
		//init & stop events
		NONE(""), INIT("Initializing"),
		SHUT_DOWN("Shutting Down"),
		//Wifi related events
		WIFI_ON("Wifi Iface Enabled"), WIFI_OFF("Wifi Iface Disabled"),
		WIFI_FAILED("Wifi Iface failed"), NO_WIFI("Wifi Iface no available"),
		//Parameter related event
		PARAMETER_RECEIVED("Receiving Oboarding parameters"), INVALID_PARAMETERS("Invalid Parameters"),
		//Onboarding actions
		CONNECT_TO_WIFI("Connecting to Wifi"), FORGET_WIFI("Forget Wifi"), RESET("Reseting"),
		//High Level events related to connection establishment
		WIFI_CONNECTED("Connected to wifi network"), WIFI_DISCONNECTED("Disconnected from network"),
		//Events related to connection establishment
		AUTHENTICATING("Authenticating"), ASSOCIATING("Associating"), HAND_SHAKING("Hand Shaking"),
		GETTING_IPADDR("Getting IP address"), CONN_ESTAB("Connection completed"),
		WIFI_LINK_DISCONNECTED("Wifi Link disconnected");
		public final String msg;

	    private EVENTS(String msg) {
	        this.msg = msg;
	    }
	}
	
	
	// Definition of the different COMMANDS methods
	public static enum COMMANDS {
		CONNECT, DISCONNECT, RESET
	};

	// Definition of the different AUTH methods
	public static enum AUTH { 
		OPEN, WEP, WPA_PSK, UNKNOWN;
	}
}