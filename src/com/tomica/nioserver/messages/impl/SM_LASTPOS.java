package com.tomica.nioserver.messages.impl;

import java.nio.ByteBuffer;

import com.tomica.nioserver.messages.Message;
import com.tomica.nioserver.messages.ServerMessage;

public class SM_LASTPOS extends ServerMessage{
	public static final byte OPCODE = (byte)26;	
	private long x;
	private float y;
	private int z;
	private byte flag;
	private short w;
	private double q;
	private String str;
	private char c;
		
	public SM_LASTPOS(){
		
	}

	public SM_LASTPOS(long x, float y, int z, byte flag, short w, double q, String str, char c) {		
		this.x = x;
		this.y = y;
		this.z=z;
		this.flag=flag;
		this.w=w;
		this.q=q;
		this.str=str;
		this.c=c;
		
		putLong(x);
		putFloat(y);
		putInt(z);
		putByte(flag);
		putShort(w);
		putDouble(q);
		putString(str);
		putChar(c);
	}

	@Override
	public byte getOpCode() {
		return OPCODE;
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
		return "opCode="+String.valueOf(OPCODE)+" x="+String.valueOf(x)+",y="+String.valueOf(y)+",z="+String.valueOf(z);
	}
}
