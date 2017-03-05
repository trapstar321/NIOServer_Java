package com.tomica.nioserver.messages;

public interface Message {
	public final static int LONG_BYTES=8;
	public final static int DOUBLE_BYTES=8;
	public final static int FLOAT_BYTES=4;
	public final static int INT_BYTES=4;
	public final static int BYTE_BYTES=1;
	public final static int SHORT_BYTES=2;
	public final static int CHAR_BYTES=2;
	
	public byte[] getBytes();
	
	public byte[] getData();
	
	public byte getOpCode();
}
