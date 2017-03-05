package com.tomica.nioserver.events;

import java.net.InetAddress;

public class ClientConnectedEvent implements Event{
	private InetAddress address;
	private Integer clientID;	
	
	public ClientConnectedEvent(InetAddress address, Integer clientID){
		this.address=address;
		this.clientID=clientID;
	}
	
	public InetAddress getAddress(){
		return address;
	}
	
	public Integer getClientID(){
		return clientID;
	}
}
