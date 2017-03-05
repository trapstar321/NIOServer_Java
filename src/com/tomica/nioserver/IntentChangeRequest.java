package com.tomica.nioserver;

public class IntentChangeRequest {
	private int ops;
	private ServerConnection conn;
	
	public IntentChangeRequest(ServerConnection conn, int ops){
		this.ops=ops;
		this.conn=conn;
	}
	
	public ServerConnection getConnection(){
		return conn;		
	}
	
	public int getIntent(){
		return ops;
	}
}
