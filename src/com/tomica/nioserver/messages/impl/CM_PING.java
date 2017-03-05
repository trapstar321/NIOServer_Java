package com.tomica.nioserver.messages.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.tomica.nioserver.messages.ClientMessage;
import com.tomica.nioserver.messages.Message;

public class CM_PING extends ClientMessage{
	public static final byte OPCODE=(byte)39;
	
	@Override
	public byte getOpCode() {
		// TODO Auto-generated method stub
		return OPCODE;
	}

	public CM_PING(){
		
	}
	
	public CM_PING(byte[] data) throws IOException{
		this.data = data.clone();
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
		return "opCode="+String.valueOf(OPCODE)+" data="+data;
	}	
}
