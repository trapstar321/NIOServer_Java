package com.tomica.nioserver.events;

public interface OnClientConnectedListener extends EventListener{	
	public void connected(ClientConnectedEvent event);	
}