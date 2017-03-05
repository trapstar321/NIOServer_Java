package com.tomica.nioserver.messages.impl;

import java.nio.ByteBuffer;

import com.tomica.nioserver.messages.ClientMessage;
import com.tomica.nioserver.messages.Message;

public class CM_ISONLINE extends ClientMessage{
	public static final byte OPCODE=(byte)27;	
	private String username;
	
	public CM_ISONLINE(){
		
	}
	
	public CM_ISONLINE(byte[] data){
		this.data = data.clone();
		username = getString();
	}
	
	@Override
	public byte[] getBytes() {
		byte[] data = getData();
		ByteBuffer b = ByteBuffer.allocate(Message.BYTE_BYTES+Message.INT_BYTES+data.length);
		b.putInt(data.length);
		b.put(getOpCode());
		b.put(data);
		return b.array();
	}

	@Override
	public byte getOpCode() {
		return OPCODE;
	}

	public String getUsername(){
		return username;
	}
	
	@Override
	public String toString(){
		return "opCode="+String.valueOf(OPCODE)+" username="+username;
	}	
}
