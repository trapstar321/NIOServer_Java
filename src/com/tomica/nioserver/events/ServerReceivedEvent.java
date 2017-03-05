package com.tomica.nioserver.events;

import com.tomica.nioserver.messages.ClientMessage;

public class ServerReceivedEvent implements Event{
	private ClientMessage message;
	private int clientID;
	
	public ServerReceivedEvent(ClientMessage message, int clientID){
		this.message=message;
		this.clientID=clientID;
	}
	
	public ClientMessage getMessage(){
		return message;
	}
	
	public int getClientID(){
		return clientID;
	}
}
