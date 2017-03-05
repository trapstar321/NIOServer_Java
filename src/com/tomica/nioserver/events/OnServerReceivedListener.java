package com.tomica.nioserver.events;

public interface OnServerReceivedListener extends EventListener{	
	public void received(ServerReceivedEvent event);
	
}
