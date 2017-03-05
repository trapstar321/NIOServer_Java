package com.tomica.nioserver;

import java.util.logging.Level;

import com.tomica.nioserver.events.ClientConnectedEvent;
import com.tomica.nioserver.events.ClientDisconnectedEvent;
import com.tomica.nioserver.events.Event;
import com.tomica.nioserver.events.EventListener;
import com.tomica.nioserver.events.OnClientConnectedListener;
import com.tomica.nioserver.events.OnClientDisconnectedListener;
import com.tomica.nioserver.events.OnServerReceivedListener;
import com.tomica.nioserver.events.ServerReceivedEvent;

public class DispatchEventTask implements Runnable{
	public EventListener listener;
	private Event event;
	
	public DispatchEventTask(EventListener listener, Event event){
		this.listener=listener;
		this.event = event;
	}

	@Override
	public void run() {		
		try{
			if(listener instanceof OnServerReceivedListener)
				((OnServerReceivedListener)listener).received((ServerReceivedEvent)event);
			else if(listener instanceof OnClientConnectedListener)
				((OnClientConnectedListener)listener).connected((ClientConnectedEvent)event);
			else if(listener instanceof OnClientDisconnectedListener)
				((OnClientDisconnectedListener)listener).disconnected((ClientDisconnectedEvent)event);
		}catch(Exception ex){
			NIOServer.log(Level.SEVERE, "Exception: "+ex.getMessage(), ex);
		}
	}
}
