package com.cozybit.onbordee.ble;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.cozybit.onbordee.manager.ConnectionManager.Events;
import com.cozybit.onbordee.manager.IManager;
import com.cozybit.onbordee.profile.OnboardingProfile;

import com.cozybit.onbordee.utils.BluetoothConstants.BT_PROFILE_STATE;
import com.cozybit.onbordee.utils.BluetoothConstants.GATT_STATUS;
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

	private final static String TAG = BleProvisioner.class.getName();

	public static int GATT_CLIENT = 1;
	public static int GATT_SERVER = 2;

	private Context mContext;
	private IManager mManager;

	private boolean mBtInitState;
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;

    private BluetoothGattServer mGattServer;
    private BluetoothGattService mGattService;
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
    		 
    		 OnboardingProfile.Characteristics type = OnboardingProfile.Characteristics.valueOf( characteristic.getUuid() );
    		 Log.d(TAG, "Type: %s; Value: %s", type, Arrays.toString( characteristic.getValue() ) );
    		 
    		 if (mGattServer != null) {
    			boolean ret = mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
    			if(!ret) Log.e(TAG, "ERROR: sending a ReadRequest response for %s to %s", type, device.getAddress());
    		 }
    	}
    	
    	//A remote client has requested to write to a local characteristic.
    	@Override
    	public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, 
    			BluetoothGattCharacteristic characteristic, boolean preparedWrite, 
    			boolean responseNeeded, int offset, byte[] value) {
    		
    		OnboardingProfile.Characteristics type = OnboardingProfile.Characteristics.valueOf( characteristic.getUuid() );
    		Log.d(TAG, "Type: %s; Value: %s", type, Arrays.toString(value) );

    		Message msg = Message.obtain();
    		msg.what = Events.RECEIVED_DATA.ordinal();
    		msg.arg1 = type.ordinal();
    		msg.obj = value;
    		mManager.sendMessage(msg);
    		mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
    	}
    	
		//Callback indicating when a remote device has been connected or disconnected.
		@Override
		public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
			GATT_STATUS gStatus = GATT_STATUS.valueOf(status);
			BT_PROFILE_STATE pState = BT_PROFILE_STATE.valueOf(newState);
			Log.d(TAG, "Device: %s | Status: %s | newSate: %s", device.getAddress(), gStatus, pState);

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

			mManager.sendMessage(msg);
		}
    	
    	//Execute all pending write operations for this device.
    	@Override
    	public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
    		Log.d(TAG, "Device: %s | requestId: %d | execure: %b", device.getAddress(), requestId, execute );
    	}
    	
    	//Indicates whether a local service has been added successfully.
    	@Override
    	public void onServiceAdded(int status, BluetoothGattService service) {
    		GATT_STATUS gStatus = GATT_STATUS.valueOf(status);
   		 	Log.d(TAG, "Status: %s; Service (UUID): %s", gStatus, service.getUuid() );
   		 	if( gStatus == GATT_STATUS.SUCCESS )
   		 		mManager.sendMessage(Events.GATT_SERVER_DEPLOYED);
   		 	else
   		 		mManager.sendMessage(Events.GATT_SERVER_FAILED);
    	}
    	
	};
	

	// Constructor
	public BleProvisioner(Context context, IManager conMngr) {
		mContext = context;
		mManager = conMngr;
	}
	
	public void initBtIface() {
		
		if (mContext == null) {
			Log.e(TAG,"ERROR: context not available.");
			//mManager.sendMessage(Events.INIT_ERROR);
			return;
		}
		
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // check for Bluetooth support
        if ( mBluetoothAdapter == null) {
        	Log.e(TAG, "ERROR: this android device has no Bluetooth support.");
        	mManager.sendMessage(Events.NO_BT);
        	return;
        } /*else if ( mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) ) {
        	// N5, MK908 seems to not have BLE enabled as featurecheck for BLE support.
        	Log.e(TAG, "ERROR: this android device has no Bluetooth LE support.");
        	mBtConMngr.sendMessage(Events.NO_BLE);
        	return;
        }*/ else {
            if( mBtInitState = mBluetoothAdapter.isEnabled() ) {
            	mManager.sendMessage(Events.INIT);
            	mManager.sendMessage(Events.BT_ON);
            } else {
            	if( mBluetoothAdapter.enable() )
            		mManager.sendMessage(Events.INIT);
            	else
        			mManager.sendMessage(Events.BT_BROKEN);
            }
        }
        
        // Create the Blocking Queue with it's own thread and start it.
        mBleBlockingQueue = new BleBlockingQueue();
        mBleBlockingQueue.start();
	}

	public void tearDown() {
		stopGattServer();
		//leave BT as in its initial state
		if( mBluetoothAdapter!= null && !mBtInitState )
			mBluetoothAdapter.disable();
	}
	
	public void deployGattServer() {
		Log.d(TAG, "Deploying Gatt Server...");
		
		/*TODO: many times, BluetoothManager fails opening the GattServer 
		 * and it returns null. Probably it's because we still have to wait for
		 * things to get initialized. This seems to work. */
		try { Thread.sleep(500); } catch (InterruptedException e) {}
		
        mGattServer = mBluetoothManager.openGattServer(mContext, mBtGattServerCbk);
        if(mGattServer == null) {
        	Log.e(TAG, "ERROR: BluetoothManager couldn't open a gatt server.");
        	mManager.sendMessage(Events.GATT_SERVER_FAILED);
        	return;
        }

        mGattService = new BluetoothGattService(OnboardingProfile.SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);        
        for (BluetoothGattCharacteristic c : generateCharacteristics() ) {
        	if( !mGattService.addCharacteristic(c) ) {
        		Log.e(TAG, "ERROR: characteristic (%s) couln't be added to the service.", c.getUuid());
        		mManager.sendMessage(Events.GATT_SERVER_FAILED);
        		return;
        	}
        }

        if ( !mGattServer.addService(mGattService) ) {
        	Log.e(TAG, "ERROR: service was not added succesfully.");
        	mManager.sendMessage(Events.GATT_SERVER_FAILED);
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

		/*TODO Jlo
		 * - Different between property & permission?
		 * - How to set a characteristic READ & WRITE
		 * - Do we need descriptor?
		*/ 
		int count = OnboardingProfile.Characteristics.values().length;
		BluetoothGattCharacteristic[] characs = new BluetoothGattCharacteristic[count];
		for(int i=0; i < count; i++) {
			OnboardingProfile.Characteristics c = OnboardingProfile.Characteristics.values()[i];
			characs[i] = new BluetoothGattCharacteristic(c.uuid, c.properties , c.permissions );
			characs[i].setValue(0, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		}
		
		return characs;
	}

	public boolean updateCharacteristic(UUID uuid, byte value, boolean notify) {
		return updateCharacteristic( uuid, new byte[] { value }, notify );
	}

	public boolean updateCharacteristic(UUID uuid, byte[] value, boolean notify) {
		
		BluetoothGattCharacteristic characteristic = null;
		boolean ret;
		
		if( ret = (mGattService != null) ) {
			characteristic = mGattService.getCharacteristic(uuid);
			if( ret = (characteristic != null) ) {
				ret = characteristic.setValue(value);
				if(!ret) Log.e(TAG, "ERROR: charectristic's value couldn't be updated (uuid: %s)", uuid);
			} else
				Log.e(TAG, "ERROR: charectristic does not exist (uuid: %s)", uuid);
		} else
			Log.e(TAG, "ERROR: GattService is null!");
		
		if(ret && notify) 
			ret = notifyCharacteristicChange(characteristic);
		
		return ret;
	}

	public boolean updateCharacteristic(UUID uuid, String value, boolean notify) {

		BluetoothGattCharacteristic characteristic = null;
		boolean ret;
		
		if( ret = (mGattService != null) ) {
			characteristic = mGattService.getCharacteristic(uuid);
			if( ret = (characteristic != null) ) {
				ret = characteristic.setValue(value);
				if(!ret) Log.e(TAG, "ERROR: charectristic's value couldn't be updated (uuid: %s)", uuid);
			} else
				Log.e(TAG, "ERROR: charectristic does not exist (uuid: %s)", uuid);
		} else
			Log.e(TAG, "ERROR: GattService is null!");

		if(ret && notify) 
			ret = notifyCharacteristicChange(characteristic);
		
		return ret;
	}
	
	private boolean notifyCharacteristicChange(BluetoothGattCharacteristic characteristic) {
		
		boolean ret;
		
		if( ret = (mGattServer != null) ) {
			List<BluetoothDevice> BtDevs = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER);
			if(BtDevs != null) {
				for (BluetoothDevice dev : BtDevs) {
					ret = mGattServer.notifyCharacteristicChanged(dev, characteristic, true); 
					if( !ret ) {
						Log.e(TAG, "ERROR: while notifying to device (%s) that charasteristic (%s) changed", dev.getAddress(), characteristic.getUuid() );
						break;
					}
				}
			} else
				Log.w(TAG, "WARNING: BluetoothDevices is NULL!!");
		} else 
			Log.e(TAG, "ERROR: mGattServer is NULL!!");
		
		return ret;
	}
}
