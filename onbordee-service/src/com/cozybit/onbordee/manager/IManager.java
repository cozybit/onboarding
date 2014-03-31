package com.cozybit.onbordee.manager;


import android.os.Message;

public interface IManager {

	public void sendMessage(Message msg);
	public void sendMessage(Enum<?> event);
	public void sendMessage(Enum<?> event, Enum<?> subevent);

}