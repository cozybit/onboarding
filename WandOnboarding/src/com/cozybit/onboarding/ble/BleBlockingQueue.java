package com.cozybit.onboarding.ble;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.bluetooth.BluetoothGattCharacteristic;

public class BleBlockingQueue extends Thread {
	
	private BlockingDeque<Request> requestQueue;
	private BlockingQueue<Response> responseQueue;
	private volatile boolean mQuit = false; 
	
	
	public BleBlockingQueue() {
		requestQueue = new LinkedBlockingDeque<Request>(100);
		responseQueue = new LinkedBlockingQueue<Response>(100);
	}
	
    public void quit() {
        mQuit = true;
        interrupt();
    }
	
	public void executeRequest() throws InterruptedException {
		
		// This will wait until a request is available
		Request req = requestQueue.take();
		
		// Execute the request
		req.runnable.run();
		
		if (req.requiresResponse) {
			// Wait for response be received on the responseQueue
			Response resp = responseQueue.poll(5, TimeUnit.SECONDS);
			
			if (resp != null && resp.characteristic.equals(req.characteristic)) {
				// The resp is probably from request
			} 
		} else {
			// TODO: This is a hack to give some time to the the notifications to be registered
			sleep(1000);
		}
	}
	
	@Override 
	public void run() {
		
		while (true) {
			try {
				executeRequest();
			} catch (InterruptedException e) {
                if (mQuit) {
                    return;
                }
			}
		}
	}
	
	private class Request {
		
		Runnable runnable;
		BluetoothGattCharacteristic characteristic;
		boolean requiresResponse;
		
		public Request(BluetoothGattCharacteristic characteristic, Runnable runnable, boolean requiresResponse) {
			this.characteristic = characteristic;
			this.runnable = runnable;
			this.requiresResponse = requiresResponse;
		}
	};
	
	private class Response {
		BluetoothGattCharacteristic characteristic;
		
		public Response(BluetoothGattCharacteristic characteristic) {
			this.characteristic = characteristic;
		}
	}

	public void newRequest(BluetoothGattCharacteristic characteristic,
			Runnable runnable, boolean requiresResponse) {
		requestQueue.offer(new Request(characteristic, runnable, requiresResponse));
	};
	
	public void newResponse(BluetoothGattCharacteristic characteristic) {
		responseQueue.offer(new Response(characteristic));
	};

}
