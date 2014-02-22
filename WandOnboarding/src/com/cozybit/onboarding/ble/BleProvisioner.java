package com.cozybit.onboarding.ble;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.cozybit.onboarding.R;
import com.cozybit.onboarding.app.OnboardingActivity;

public class BleProvisioner {

	private Context mContext;
	private Handler mHandler;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mDevice;
    private BluetoothGatt mBluetoothGatt;
	private BleBlockingQueue mBleBlockingQueue;

	private enum State {
		DISABLED, 		// Provisioner not started or released
		INITIALIZED, 	// Provisioner started
		SCANNING, 		// BLE interface is ON and scanning
		CONNECTING, 	// Device found trying to connect
		CONNECTED, 		// Connected as central device
		DISCONNECTING,  // Disconnecting
		DISCONNECTED,   // Disconnected 
		FAILED,			// Provisioner has failed
		PAUSED;			// Provisioner is paused
	}
	
	private State mState = State.DISABLED;
	private State mSavedState;
	
	private String TAG = "BleProvisioner";

	private UUID mServiceUUID;	
	private int mRssiThreshold;
	
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mState = State.CONNECTED;
                Log.i(TAG, "Connected to GATT server.");
                mHandler.sendEmptyMessage(OnboardingActivity.GATT_CONNECTED);
          
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            	if (mState == State.DISCONNECTING) {
            		mState = State.DISCONNECTED;
            		Log.i(TAG, "Correctly disconnected from GATT server.");
            		mHandler.sendEmptyMessage(OnboardingActivity.GATT_DISCONNECTED);
            	} else {
            		Log.i(TAG, "Something wrong happened and we're disconnected from GATT server.");
            		mHandler.sendEmptyMessage(OnboardingActivity.GATT_FAILED);
            	}
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "onServicesDiscovered!!!: " + status);
                mHandler.sendEmptyMessage(OnboardingActivity.GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	 Log.d(TAG, "onCharacteristicRead received: " + status);
            	 mBleBlockingQueue.newResponse(characteristic);
            	 
            	 final byte[] data = characteristic.getValue();
                 if (data != null && data.length > 0) {

                 	String value = new String(data);
            	 	
     	       	 	if (characteristic.getUuid().equals(OnboardingGattService.CHARACTERISTIC_VENDOR_ID)) {
     	       	 		mHandler.sendMessage(Message.obtain(mHandler, OnboardingActivity.VENDOR_ID_READ, value));
     	       	 	} else if (characteristic.getUuid().equals(OnboardingGattService.CHARACTERISTIC_DEVICE_ID)) {
     	       	 		mHandler.sendMessage(Message.obtain(mHandler, OnboardingActivity.DEVICE_ID_READ, value));
     	       	 	}
                 }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
       	 	Log.d(TAG, "onCharacteristicChanged received: " + new String(characteristic.getValue()));
       	 	// TODO notifications are not passed to the blocking queue
       	 	// mBleBlockingQueue.newResponse(characteristic);
       	 	
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {

            	String value = new String(data);
       	 	
	       	 	if (characteristic.getUuid().equals(OnboardingGattService.CHARACTERISTIC_STATUS)) {
	       	 		mHandler.sendMessage(Message.obtain(mHandler, OnboardingActivity.STATUS_NOTIFIED, value));
	       	 	} else if (characteristic.getUuid().equals(OnboardingGattService.CHARACTERISTIC_LONG_STATUS)) {
	       	 		mHandler.sendMessage(Message.obtain(mHandler, OnboardingActivity.LONG_STATUS_NOTIFIED, value));
	       	 	}
            }
        }
        
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        }
        
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
	       	 Log.d(TAG, "onCharacteristicWrite received: " + status);      
        	 mBleBlockingQueue.newResponse(characteristic);
        }
    };


	// Constructor
	public BleProvisioner(Context context, Handler handler) {
		mContext = context;
		mHandler = handler;
	}
	
	public boolean init() {
		
		if (mContext == null)
			return false;
		
		if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(mContext, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            return false;
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(mContext, R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (!isEnabled())
        	mBluetoothAdapter.enable();
        
        // Create the Blocking Queue with it's own thread and start it.
        mBleBlockingQueue = new BleBlockingQueue();
        mBleBlockingQueue.start();
        
        mState = State.INITIALIZED;
        Log.d(TAG , "Bluetooth initialized.");
        return true;
	}
	
	public boolean isEnabled() {
		
		if (mBluetoothAdapter == null)
			return false;
		
		return mBluetoothAdapter.isEnabled();
	}
	
/*	private boolean deviceIsNexus4() {
		if (Build.MODEL.equals("Nexus 4"))
			return true;
		return false;
	}
*/
	
	public void openGattServer() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        
        BluetoothGattServer gattServer = bluetoothManager.openGattServer(mContext, new BluetoothGattServerCallback() {
        	
        	/* TODO Here we should @Override any method we want to handle
        	 * 
        	 * from Android API:
        	 * 
        	 *  void 	onCharacteristicReadRequest(BluetoothDevice device, int requestId,
        	 *  			int offset, BluetoothGattCharacteristic characteristic)
        	 *  		A remote client has requested to read a local characteristic.
        	 *  
        	 *  void 	onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
        	 *  			BluetoothGattCharacteristic characteristic, boolean preparedWrite,
        	 *  			boolean responseNeeded, int offset, byte[] value)
        	 *  		A remote client has requested to write to a local characteristic.
        	 *  
        	 *  void 	onConnectionStateChange(BluetoothDevice device, int status, int newState)
        	 * 			Callback indicating when a remote device has been connected or disconnected.
        	 *  
        	 *  void 	onDescriptorReadRequest(BluetoothDevice device, int requestId,
        	 *  			int offset, BluetoothGattDescriptor descriptor)
        	 *  		A remote client has requested to read a local descriptor.
        	 *  
        	 *  void 	onDescriptorWriteRequest(BluetoothDevice device, int requestId,
        	 *  			BluetoothGattDescriptor descriptor, boolean preparedWrite,
        	 *  			boolean responseNeeded, int offset, byte[] value)
        	 *  		A remote client has requested to write to a local descriptor.
        	 *  
        	 *  void 	onExecuteWrite(BluetoothDevice device, int requestId, boolean execute)
        	 *  		Execute all pending write operations for this device.
        	 *  
        	 *  void 	onServiceAdded(int status, BluetoothGattService service)
        	 *  		Indicates whether a local service has been added successfully.
        	 */
        	@Override
        	public void onCharacteristicReadRequest(BluetoothDevice device, int requestId,
        			 int offset, BluetoothGattCharacteristic characteristic) {
        		 Log.d(TAG, "onCharacteristicReadRequest" );
        	}
        	
        	@Override
        	public void onServiceAdded(int status, BluetoothGattService service) {
       		 	Log.d(TAG, "onServiceAdded" );
        	}
        	
		});
                
        gattServer.addService(new BluetoothGattService(UUID.randomUUID(), BluetoothGattService.SERVICE_TYPE_PRIMARY));
        //gattServer.addService(new BluetoothGattService(UUID.randomUUID(), BluetoothGattService.SERVICE_TYPE_SECONDARY));

	}
	
	
	public void startScanLeDevices(UUID uuid, int rssiThreshold) {
		mServiceUUID = uuid;
		mRssiThreshold = rssiThreshold;
		
		startScanLeDevices();
	}
	
	public void startScanLeDevices() {
		
/*		if (deviceIsNexus4()) {
			mHandler.postDelayed(new Runnable() {
	            @Override
	            public void run() {
	        		startScanLeDevices();
	            }
	        }, SCAN_PERIOD);
		}
*/		
        mState = State.SCANNING;
        mBluetoothAdapter.startLeScan(mLeScanCallback);
	}
	

	public void stopScanLeDevices() {
		if (mState == State.SCANNING) { 
			mState = State.INITIALIZED;
	        mBluetoothAdapter.stopLeScan(mLeScanCallback);
		} else if (mState == State.CONNECTING) {
			
		} else if (mState == State.CONNECTED) {

		}
	}
	
    private static void reverse(byte[] array) {
        if (array == null) {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        byte tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }
    
	private boolean hasServiceUUID(byte[] scanRecord) {
		
		int current_entry_idx = 0;
		int data_type_idx = 0;
		int data_start_idx = 0;
		
		while (current_entry_idx < scanRecord.length) {
			
			data_type_idx = current_entry_idx + 1;
			data_start_idx = current_entry_idx + 2;
			
			if (scanRecord[current_entry_idx] > 16 && scanRecord[data_type_idx] == 6) {
				// We found a 128 bits service UUID
				byte [] possibleService = Arrays.copyOfRange(scanRecord, data_start_idx, data_start_idx + 16);
				// We need to reverse the UUID
				BleProvisioner.reverse(possibleService);
				// Build the UUID to compare
				ByteBuffer bb = ByteBuffer.wrap(possibleService); 
				UUID posible = new UUID(bb.getLong(), bb.getLong());
				
				if (posible.equals(mServiceUUID))
					return true;
				
			}
			current_entry_idx = current_entry_idx + scanRecord[current_entry_idx] + 1;
		}
		return false;
	}
	
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
        	if (rssi >= mRssiThreshold) { // If device is close enough 
        		if (hasServiceUUID(scanRecord)) { // If device advertise searched UUID
        			Log.d(TAG,"Found an onboarding device!");
        			stopScanLeDevices();
        			mDevice = device;
        			mHandler.sendEmptyMessage(OnboardingActivity.DETECTED_DEVICE);
        		}
        	}
        }  		          
    };

	public boolean connectGatt() {
		if (mBluetoothAdapter == null || mDevice == null) {
			Log.w(TAG, "BluetoothAdapter not initialized or undetected device.");
			return false;
		}
		
		if (mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
            	mState = State.CONNECTING;
            	return true;
            } else
            	return false;
		}
		
		mBluetoothGatt = mDevice.connectGatt(mContext, false, mGattCallback);
		Log.d(TAG, "Trying to create a new connection.");
		mState = State.CONNECTING;
		return true;
	}
	
	public boolean disconnectGatt() {
		
		if (mBluetoothAdapter == null || mDevice == null) {
			Log.w(TAG, "BluetoothAdapter not initialized or undetected device.");
			return false;
		}
		
		if (mBluetoothGatt == null) {
            Log.d(TAG, "BluetoothGatt is null.");
            return false;
		}
        mBluetoothGatt.disconnect();
		Log.d(TAG, "Disconnecting from gatt server.");
		mState = State.DISCONNECTING;
        return true;
	}
	
	protected BluetoothGattCharacteristic getCharacteristicFromUUID(
			final UUID characteristicUUID) {
		
		if (mBluetoothAdapter == null || mBluetoothGatt == null ||
				mServiceUUID == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return null;
        }
        
        BluetoothGattService gattService = mBluetoothGatt.getService(mServiceUUID);
		
  		if (gattService == null) {
  			Log.w(TAG, "GATT Service not found for UUID " + mServiceUUID.toString());
  			return null;
  		}
  		// Get Characteristic
  		return gattService.getCharacteristic(characteristicUUID);
	}
	
    public void readCharacteristic(final UUID characteristicUUID) {
    	
		final BluetoothGattCharacteristic characteristic = getCharacteristicFromUUID(characteristicUUID);
  		
  		if (characteristic == null) {
  			Log.w(TAG, "Problem getting characteristic from UUID " + characteristicUUID.toString());
  			return;
  		}
  		
		mBleBlockingQueue.newRequest(characteristic, new Runnable() {
			@Override
			public void run() {
		        if (!mBluetoothGatt.readCharacteristic(characteristic))
					Log.w(TAG, "readCharacteristic not possible for UUID " + characteristicUUID.toString());
			}
		}, true);
        
        
    }
    
	public void setCharacteristicNotification(final UUID characteristicUUID,
			final boolean enabled) {
		
		final BluetoothGattCharacteristic characteristic = getCharacteristicFromUUID(characteristicUUID);
			
		mBleBlockingQueue.newRequest(characteristic, new Runnable() {
			@Override
			public void run() {
				// Get Service
				
		        if (!mBluetoothGatt.setCharacteristicNotification(characteristic, enabled))
					Log.w(TAG, "setCharacteristicNotification not possible for UUID " + characteristicUUID.toString());
		        
		        List <BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
		        for (BluetoothGattDescriptor bluetoothGattDescriptor : descriptors) {
		        	if (enabled) {
		        		if (!bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE))
		        			Log.w(TAG, "setValue not possible for UUID " + characteristicUUID.toString());
		        	} else {
		        		if (!bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE))
		        			Log.w(TAG, "setValue not possible for UUID " + characteristicUUID.toString());
		        	}
		    		if (!mBluetoothGatt.writeDescriptor(bluetoothGattDescriptor))
		    			Log.w(TAG, "writeDescriptor not possible for UUID " + characteristicUUID.toString());
				}
			}
		}, false);
	}
    
	public void writeCharacteristic(final UUID characteristicUUID, final String value) {
		
		final BluetoothGattCharacteristic characteristic = getCharacteristicFromUUID(characteristicUUID);
  		
  		if (characteristic == null) {
  			Log.w(TAG, "Problem getting characteristic from UUID " + characteristicUUID.toString());
  			return;
  		}
  		
  		mBleBlockingQueue.newRequest(characteristic, new Runnable() {
  			@Override
  			public void run() {
  				
  				characteristic.setValue(value);
  				
  		        if (!mBluetoothGatt.writeCharacteristic(characteristic))
  					Log.w(TAG, "writeCharacteristic not possible for UUID " + characteristicUUID.toString());
  			}
  		}, true);
        
	}
	
	public void writeCharacteristic(final UUID characteristicUUID, final byte[] value) {
        
		final BluetoothGattCharacteristic characteristic = getCharacteristicFromUUID(characteristicUUID);
  		
  		if (characteristic == null) {
  			Log.w(TAG, "Problem getting characteristic from UUID " + characteristicUUID.toString());
  			return;
  		}
  		
  		mBleBlockingQueue.newRequest(characteristic, new Runnable() {
  			@Override
  			public void run() {
  				
  				characteristic.setValue(value);
  				
  		        if (!mBluetoothGatt.writeCharacteristic(characteristic))
  					Log.w(TAG, "writeCharacteristic not possible for UUID " + characteristicUUID.toString());
  			}
  		}, true);
        
	}

	public void pause() {
		mSavedState = mState;
		mState = State.PAUSED;
		// Here depending on the state where we're we should do different things...
		// Stop Scanning
		// Disconnect from Gatt Server
		if (mSavedState == State.SCANNING) {
			stopScanLeDevices();
		} else if (mSavedState == State.CONNECTED ||
				   mSavedState ==State.CONNECTING) {
			disconnectGatt();
		}

	}

	public void resume() {
		if (mState == State.PAUSED) {
			
			// Then here we need to recover and get to the state were we were before
			// Start Scan again if scanning
			// Connect to GATT Server if connected
			
			if (mSavedState == State.SCANNING) {
				startScanLeDevices();
			} else if (mSavedState == State.CONNECTED  ||
					   mSavedState ==  State.CONNECTING) {
				connectGatt();
			}
			mSavedState = null;
		}
	}

	public boolean discoverServices() {
		if (mBluetoothAdapter == null || mDevice == null) {
			Log.w(TAG, "BluetoothAdapter not initialized or undetected device.");
			return false;
		}
		
		if (mBluetoothGatt != null) {
			mBluetoothGatt.discoverServices();
			return true;
		}
		return false;
	}
}
