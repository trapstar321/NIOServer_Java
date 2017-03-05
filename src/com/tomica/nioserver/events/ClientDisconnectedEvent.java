package com.tomica.nioserver.events;

import java.net.InetAddress;

public class ClientDisconnectedEvent extends ClientConnectedEvent{

	public ClientDisconnectedEvent(InetAddress address, Integer clientID) {
		super(address, clientID);		
	}
}
