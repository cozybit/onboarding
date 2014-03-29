package com.cozybit.onbordee.ble;

import java.util.Arrays;
import java.util.UUID;

import com.cozybit.onbordee.ble.BtConnectionManager.Events;
import com.cozybit.onbordee.ble.BtConnectionManager.SubEvents;
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
import android.content.pm.PackageManager;
import android.os.Message;

public class BleProvisioner {

	private String TAG = BleProvisioner.class.getName();
	
	private Context mContext;
	private IBtConnectionManager mBtConnMngr;
	
	private boolean mBtInitState;
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	
	private UUID mServiceUUID = OnboardingGattService.SERVICE_UUID;
	
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattServer mGattServer;
    private BleBlockingQueue mBleBlockingQueue;
    
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
			
			Message msg = Message.obtain();
			
			switch(newState) {
			case BluetoothProfile.STATE_CONNECTED:
				msg.what = Events.CLIENT_CONNECTED.ordinal();
				msg.obj = device;
				break;
			case BluetoothProfile.STATE_DISCONNECTED:
				msg.what = Events.CLIENT_DISCONNECTED.ordinal();
				msg.obj = device;
				break;
			default:
				break;
				
			}
			
			mBtConnMngr.sendMessage(msg);
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
   		 	mBtConnMngr.sendMessage(Events.GATT_SERVER_DEPLOYED);
    	}
    	
	};
	

	// Constructor
	public BleProvisioner(Context context, IBtConnectionManager conMngr) {
		mContext = context;
		mBtConnMngr = conMngr;
	}
	
	public void initBtIface() {
		
		if (mContext == null) {
			Log.e(TAG,"ERROR: context not available.");
			mBtConnMngr.sendMessage(Events.INIT_ERROR);
			return;
		}
		
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // check for Bluetooth support
        if ( mBluetoothAdapter == null) {
        	Log.e(TAG, "ERROR: this android device has no Bluetooth support.");
        	mBtConnMngr.sendMessage(Events.INIT_ERROR, SubEvents.NO_BLE_AVAILABLE);
        	return;
        } /*else if ( mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) ) {
        	// N5, MK908 seems to not have BLE enabled as featurecheck for BLE support.
        	Log.e(TAG, "ERROR: this android device has no Bluetooth LE support.");
        	mBtConMngr.sendMessage(Events.INIT_ERROR, SubEvents.NO_BLE_AVAILABLE);
        	return;
        }*/ else {
            if( mBluetoothAdapter.isEnabled() ) {
            	mBtConnMngr.sendMessage(Events.INIT);
            	mBtConnMngr.sendMessage(Events.BLUETOOTH_ON);
            } else {
            	if( mBluetoothAdapter.enable() )
            		mBtConnMngr.sendMessage(Events.INIT);
            	else
        			mBtConnMngr.sendMessage(Events.INIT_ERROR, SubEvents.BT_BROKEN);
            }
        }
	}

	public void tearDown() {
		stopGattServer();
		//leave BT as in its initial state
		if( !mBtInitState )
			mBluetoothAdapter.disable();
	}
	
	public void deployGattServer() {
		Log.d(TAG, "Deploying Gatt Server...");
		
		/*TODO: many times, BluetoothManager fails opening the GattServer 
		 * and it returns null. Probably it's because we still have to wait for
		 * things to get initialized */
		try { Thread.sleep(500); } catch (InterruptedException e) {}
		
        mGattServer = mBluetoothManager.openGattServer(mContext, mBtGattServerCbk);
        if(mGattServer == null) {
        	Log.e(TAG, "ERROR: BluetoothManager couldn't open a gatt server.");
        	mBtConnMngr.sendMessage(Events.GATT_SERVER_FAILED);
        	return;
        }

        BluetoothGattService service = new BluetoothGattService(OnboardingGattService.SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);        
        for (BluetoothGattCharacteristic c : generateCharacteristics() ) {
        	if( !service.addCharacteristic(c) ) {
        		Log.e(TAG, "ERROR: characteristic (%s) couln't be added to the service.", c.getUuid());
        		mBtConnMngr.sendMessage(Events.GATT_SERVER_FAILED);
        		return;
        	}
        }

        if ( !mGattServer.addService(service) ) {
        	Log.e(TAG, "ERROR: service was not added succesfully.");
        	mBtConnMngr.sendMessage(Events.GATT_SERVER_FAILED);
        }
	}
	
	public void stopGattServer() {
		if(mGattServer != null) {
			mGattServer.close();
			mGattServer = null;
		}
	}
	
	public void disconnectBtClient(BluetoothDevice device) {
		if(mGattServer != null) {
			mGattServer.cancelConnection(device);
		}
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
