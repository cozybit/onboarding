package com.cozybit.onbordee.ble;

import java.util.Arrays;
import java.util.UUID;

import com.cozybit.onbordee.utils.Log;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

public class BleProvisioner {

	private String TAG = BleProvisioner.class.getName();
	
	private boolean mBtInitState;
	private BluetoothAdapter mBluetoothAdapter;
	
	private Context mContext;
    private BluetoothGatt mBluetoothGatt;
    private BleBlockingQueue mBleBlockingQueue;
    
    private UUID mServiceUUID = OnboardingGattService.SERVICE_UUID;

	/*private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
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
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
	       	 Log.d(TAG, "onCharacteristicWrite received: " + status);      
        	 mBleBlockingQueue.newResponse(characteristic);
        }
    };*/
    
	private BluetoothGattServer mGattServer;
	
	private BluetoothGattServerCallback mBtGattServerCbk = new BluetoothGattServerCallback() {
    	
    	/* Missing methods to implement. Keep them in mind, because we might need them for something:
    	 *   
    	 *  void 	onDescriptorReadRequest(BluetoothDevice device, int requestId,
    	 *  			int offset, BluetoothGattDescriptor descriptor)
    	 *  		A remote client has requested to read a local descriptor.
    	 *  
    	 *  void 	onDescriptorWriteRequest(BluetoothDevice device, int requestId,
    	 *  			BluetoothGattDescriptor descriptor, boolean preparedWrite,
    	 *  			boolean responseNeeded, int offset, byte[] value)
    	 *  		A remote client has requested to write to a local descriptor.
    	 */
		
		//A remote client has requested to read a local characteristic.
    	@Override
    	public void onCharacteristicReadRequest(BluetoothDevice device, int requestId,
    			 int offset, BluetoothGattCharacteristic characteristic) {
    		 Log.d(TAG, "onCharacteristicReadRequest -> uuid: %s", characteristic.getUuid().toString());
    		 
    		 if (mGattServer != null) {
    			 
    			byte[] value = new byte[1];
    			UUID uuid = characteristic.getUuid();

    			if( uuid.equals(OnboardingGattService.CHARACTERISTIC_STATUS) ) {
    			} else if( uuid.equals(OnboardingGattService.CHARACTERISTIC_LONG_STATUS) ) {
    				
    			} else if( uuid.equals(OnboardingGattService.CHARACTERISTIC_SSID) ) {
    				
    			} else if( uuid.equals(OnboardingGattService.CHARACTERISTIC_AUTH) ) {
    				
    			} else if( uuid.equals(OnboardingGattService.CHARACTERISTIC_PASS) ) {
    				
    			} else if( uuid.equals(OnboardingGattService.CHARACTERISTIC_CHANNEL) ) {
    				
    			} else if( uuid.equals(OnboardingGattService.CHARACTERISTIC_VENDOR_ID) ) {
    				value[0] = 100;
    			} else if( uuid.equals(OnboardingGattService.CHARACTERISTIC_DEVICE_ID) ) {
    				value[0] = 101;
    			} else {
    				Log.d(TAG, "Unknown characteristic! --> uuid: %s", uuid.toString() );
    			}
    			
    			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
    			 
    		    /*mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, new byte[] { bogusValue++ });
    		    if(mWelcomeFragUiUpdater != null)
    		    	mWelcomeFragUiUpdater.updateValue(bogusValue);*/
    		 }
    	}
    	
    	//A remote client has requested to write to a local characteristic.
    	@Override
    	public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, 
    			BluetoothGattCharacteristic characteristic, boolean preparedWrite, 
    			boolean responseNeeded, int offset, byte[] value) {
    		Log.d(TAG, "UUID: %s, Value: %s", characteristic.getUuid(), Arrays.toString(value) );

	   		 if (mGattServer != null) {
	   			UUID uuid = characteristic.getUuid();
    		
	    		if( uuid.equals(OnboardingGattService.CHARACTERISTIC_SSID) ||
	    				uuid.equals(OnboardingGattService.CHARACTERISTIC_AUTH) ||
	    				uuid.equals(OnboardingGattService.CHARACTERISTIC_PASS) ||
	    				uuid.equals(OnboardingGattService.CHARACTERISTIC_CHANNEL) ) {
				} else if( uuid.equals(OnboardingGattService.CHARACTERISTIC_COMMAND) ) {
					
				} else {
					Log.d(TAG, "Unknown characteristic! --> uuid: " + uuid.toString() );
				}
	    		
	    		mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
	   		 }
    	}
    	
		//Callback indicating when a remote device has been connected or disconnected.
		@Override
		public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
			Log.d(TAG, "Device: %s | Status: %d | newSate: %d", device.getAddress(), status, newState);
			
			switch(newState) {
			case BluetoothProfile.STATE_CONNECTED:
				break;
			case BluetoothProfile.STATE_DISCONNECTED:
				break;
			default:
				break;
			}
		}
    	
    	//Execute all pending write operations for this device.
    	@Override
    	public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
    		Log.d(TAG, "Device: %s | requestId: %d | execure: %b", device.getAddress(), requestId, execute );
    	}
    	
    	//Indicates whether a local service has been added successfully.
    	@Override
    	public void onServiceAdded(int status, BluetoothGattService service) {
   		 	Log.d(TAG, "Status: %d Service (UUID): %s", status, service.getUuid() );
    	}
    	
	};
	

	// Constructor
	public BleProvisioner(Context context) {
		mContext = context;
	}
	
	/*TODO If Bluetooth is not enabled when the service is started, there will be a race 
	 * condition (BT needs some ms to be loaded) and there will be NPE when starting the service*/
	public boolean setup() {
		
		if (mContext == null) {
			Log.e(TAG,"ERROR: context not available.");
			return false;
		}
		
		// Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        
        // check for Bluetooths upport
        if (mBluetoothAdapter == null) {
        	Log.e(TAG, "ERROR: this android device has no Bluetooth support.");
            return false;
        }

        // Comment this out, because MK908 seems to not have BLE enabled as feature
        // check for BLE support
        /*if ( getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) ) {
			Log.e(TAG, "ERROR: this android device has no Bluetooth LE support. Aborting service");
            return false;
        }*/
        
        mBtInitState = isBtEnabled(); 
        
        //TODO: we might need to use intents in order to track the real state of BT. Use this for now
        if ( !mBtInitState )
        	if( ! mBluetoothAdapter.enable() )
        		return false;
		
        Log.d(TAG , "Bluetooth initialized.");
        return true;
	}
	
	public boolean isBtEnabled() {
		if (mBluetoothAdapter == null)
			return false;
		
		return mBluetoothAdapter.isEnabled();
	}
	
	public void tearDown() {
		//leave BT as in its initial state
		if( !mBtInitState )
			mBluetoothAdapter.disable();
	}
	
	public void startGattServer() {
		
        final BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mGattServer = bluetoothManager.openGattServer(mContext, mBtGattServerCbk);
        mGattServer.clearServices();
        BluetoothGattService service = new BluetoothGattService(OnboardingGattService.SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        for (BluetoothGattCharacteristic c : generateCharacteristics() )
        	service.addCharacteristic(c);
        boolean serviceAdded = mGattServer.addService(service);
        Log.d(TAG, "was mGattServer.addService(service) successful? %b", serviceAdded);
	}

	private BluetoothGattCharacteristic[] generateCharacteristics() {
		
        /*BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(characteristicUUID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        characteristic.setValue(77, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(descriptorUUID, BluetoothGattDescriptor.PERMISSION_READ);
        characteristic.addDescriptor(descriptor);*/

		/*TODO Jlo
		 * - Different between property & permission?
		 * - How to set a characteristic READ & WRITE
		 * - Do we need descriptor?
		*/ 
		BluetoothGattCharacteristic[] characs = new BluetoothGattCharacteristic[9];
		characs[0] = new BluetoothGattCharacteristic(OnboardingGattService.CHARACTERISTIC_STATUS, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
		characs[0].setValue(1, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		
		characs[1] = new BluetoothGattCharacteristic(OnboardingGattService.CHARACTERISTIC_LONG_STATUS, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
		characs[1].setValue(2, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		
		characs[2] = new BluetoothGattCharacteristic(OnboardingGattService.CHARACTERISTIC_SSID, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
				BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
		characs[2].setValue(3, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		
		characs[3] = new BluetoothGattCharacteristic(OnboardingGattService.CHARACTERISTIC_AUTH, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
				BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
		characs[3].setValue(4, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		
		characs[4] = new BluetoothGattCharacteristic(OnboardingGattService.CHARACTERISTIC_PASS, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
				BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
		characs[4].setValue(5, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		
		characs[5] = new BluetoothGattCharacteristic(OnboardingGattService.CHARACTERISTIC_CHANNEL, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
				BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
		characs[5].setValue(6, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		
		characs[6] = new BluetoothGattCharacteristic(OnboardingGattService.CHARACTERISTIC_COMMAND, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
		characs[6].setValue(7, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		
		characs[7] = new BluetoothGattCharacteristic(OnboardingGattService.CHARACTERISTIC_VENDOR_ID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
		characs[7].setValue(8, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		
		characs[8] = new BluetoothGattCharacteristic(OnboardingGattService.CHARACTERISTIC_DEVICE_ID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
		characs[8].setValue(9, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		
		return characs;
	}

	public void stopGattServer() {
		mGattServer.clearServices();
		mGattServer.close();
		mGattServer = null;
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
  			Log.w(TAG, "GATT Service not found for UUID: %s", mServiceUUID.toString());
  			return null;
  		}
  		// Get Characteristic
  		return gattService.getCharacteristic(characteristicUUID);
	}
	
	public void writeCharacteristic(final UUID characteristicUUID, final String value) {
		
		final BluetoothGattCharacteristic characteristic = getCharacteristicFromUUID(characteristicUUID);
  		
  		if (characteristic == null) {
  			Log.w(TAG, "Problem getting characteristic from UUID %s", characteristicUUID.toString());
  			return;
  		}
  		
  		mBleBlockingQueue.newRequest(characteristic, new Runnable() {
  			@Override
  			public void run() {
  				
  				characteristic.setValue(value);
  				
  		        if (!mBluetoothGatt.writeCharacteristic(characteristic))
  					Log.w(TAG, "writeCharacteristic not possible for UUID %s", characteristicUUID.toString());
  			}
  		}, true);
        
	}
	
	public void writeCharacteristic(final UUID characteristicUUID, final byte[] value) {
        
		final BluetoothGattCharacteristic characteristic = getCharacteristicFromUUID(characteristicUUID);
  		
  		if (characteristic == null) {
  			Log.w(TAG, "Problem getting characteristic from UUID %s", characteristicUUID.toString());
  			return;
  		}
  		
  		mBleBlockingQueue.newRequest(characteristic, new Runnable() {
  			@Override
  			public void run() {
  				
  				characteristic.setValue(value);
  				
  		        if (!mBluetoothGatt.writeCharacteristic(characteristic))
  					Log.w(TAG, "writeCharacteristic not possible for UUID %s", characteristicUUID.toString());
  			}
  		}, true);
        
	}
	
	/*public void readCharacteristic(final UUID characteristicUUID) {
    	
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
	}*/
}
