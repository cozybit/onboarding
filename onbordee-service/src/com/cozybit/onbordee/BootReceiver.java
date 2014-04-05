package com.cozybit.onbordee;

import com.cozybit.onbordee.service.OnboardeeService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

//This Broadcast Receiver listens for the specific event to launch the service!
public class BootReceiver extends BroadcastReceiver {   

    @Override
    public void onReceive(Context context, Intent intent) {
    	String action = intent.getAction();
    	if(action != null && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
    		Intent serviceIntent = new Intent(context, OnboardeeService.class);
    		context.startService(serviceIntent);
    	}
    }
}