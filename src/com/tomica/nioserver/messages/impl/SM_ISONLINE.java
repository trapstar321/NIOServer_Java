package com.tomica.nioserver.messages.impl;

import java.nio.ByteBuffer;

import com.tomica.nioserver.messages.Message;
import com.tomica.nioserver.messages.ServerMessage;

public class SM_ISONLINE extends ServerMessage{
	public static final byte OPCODE=(byte)28;	
	private boolean isOnline;
	
	public SM_ISONLINE() {
	
	}
	
	public SM_ISONLINE(boolean isOnline){
		this.isOnline=isOnline;
		putBool(isOnline);		
	}

	@Override
	public byte getOpCode() {
		return OPCODE;
	}
	
	public boolean isOnline(){
		return isOnline;
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
	public String toString(){
		return "opCode="+String.valueOf(OPCODE)+" isOnline="+String.valueOf(isOnline);
	}

}
