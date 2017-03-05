package com.tomica.nioserver.events;

public interface OnClientDisconnectedListener extends EventListener{
	public void disconnected(ClientDisconnectedEvent event);
}
