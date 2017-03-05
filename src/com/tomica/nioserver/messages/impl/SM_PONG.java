package com.tomica.nioserver.messages.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.tomica.nioserver.messages.Message;
import com.tomica.nioserver.messages.ServerMessage;

public class SM_PONG extends ServerMessage{
	public static final byte OPCODE=(byte)40;
	private byte[] data;
	
	public SM_PONG() {
		
	}
	
	public SM_PONG(byte[] data) throws IOException{
		this.data=data;
		putBytes(data);
	}
	
	@Override
	public byte getOpCode() {
		// TODO Auto-generated method stub
		return OPCODE;
	}
	
	@Override
	public byte[] getBytes() {
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
