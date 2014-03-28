package com.cozybit.onbordee.ble;

import com.cozybit.onbordee.ble.BtConnectionManager.Events;
import com.cozybit.onbordee.ble.BtConnectionManager.SubEvents;

import android.os.Message;

public interface IBtConnectionManager {

	public void sendMessage(Message msg);
	public void sendMessage(Events event);
	public void sendMessage(Events event, SubEvents subevent);

}