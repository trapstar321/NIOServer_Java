package com.tomica.nioserver.messages;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class ClientMessage implements Message{
	private int lastPos=0;
	protected byte[] data;	

	public synchronized void reset(){
		lastPos=0;
	}
	
	public byte getByte(){
		byte[] slice = Arrays.copyOfRange(data, lastPos, lastPos+Message.BYTE_BYTES);		
		ByteBuffer bb = ByteBuffer.wrap(slice);
		lastPos+=Message.BYTE_BYTES;
		return bb.get();
	}
	
	public short getShort(){
		byte[] slice = Arrays.copyOfRange(data, lastPos, lastPos+Message.SHORT_BYTES);		
		ByteBuffer bb = ByteBuffer.wrap(slice);
		lastPos+=Message.SHORT_BYTES;
		return bb.getShort();
	}
	
	public long getLong(){
		byte[] slice = Arrays.copyOfRange(data, lastPos, lastPos+Message.LONG_BYTES);		
		ByteBuffer bb = ByteBuffer.wrap(slice);
		lastPos+=Message.LONG_BYTES;
		return bb.getLong();
	}
	
	public double getDouble(){
		byte[] slice = Arrays.copyOfRange(data, lastPos, lastPos+Message.DOUBLE_BYTES);		
		ByteBuffer bb = ByteBuffer.wrap(slice);
		lastPos+=Message.DOUBLE_BYTES;
		return bb.getDouble();
	}
	
	public int getInt(){
		byte[] slice = Arrays.copyOfRange(data, lastPos, lastPos+Message.INT_BYTES);		
		ByteBuffer bb = ByteBuffer.wrap(slice);
		lastPos+=Message.INT_BYTES;
		return bb.getInt();
	}
	
	public float getFloat(){
		byte[] slice = Arrays.copyOfRange(data, lastPos, lastPos+Message.FLOAT_BYTES);		
		ByteBuffer bb = ByteBuffer.wrap(slice);
		lastPos+=Message.FLOAT_BYTES;
		return bb.getFloat();
	}
	
	public char getChar(){
		byte[] slice = Arrays.copyOfRange(data, lastPos, lastPos+Message.CHAR_BYTES);		
		ByteBuffer bb = ByteBuffer.wrap(slice);
		lastPos+=Message.CHAR_BYTES;
		return bb.getChar();
	}
	
	public String getString(){
		byte[] slice = Arrays.copyOfRange(data, lastPos, lastPos+Message.INT_BYTES);		
		ByteBuffer bb = ByteBuffer.wrap(slice);		
		int length = bb.getInt();			
		
		int start=lastPos+Message.INT_BYTES;
		lastPos+=Message.INT_BYTES+length;
		return new String(Arrays.copyOfRange(data, start, start+length));
	}
	
	public boolean getBool(){
		byte[] slice = Arrays.copyOfRange(data, lastPos, lastPos+Message.BYTE_BYTES);		
		ByteBuffer bb = ByteBuffer.wrap(slice);
		lastPos+=Message.BYTE_BYTES;
		if(bb.get()==(byte)0)
			return false;
		return true;
	} 
	
	public Object getObject() throws IOException, ClassNotFoundException{
		byte[] slice = Arrays.copyOfRange(data, lastPos, lastPos+Message.INT_BYTES);		
		ByteBuffer bb = ByteBuffer.wrap(slice);		
		int length = bb.getInt();			
		
		int start=lastPos+Message.INT_BYTES;
		lastPos+=Message.INT_BYTES+length;
		
		byte[] bytes = Arrays.copyOfRange(data, start, start+length);
		
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = new ObjectInputStream(bis);
		return ois.readObject();
	}
	
	@Override
	public byte[] getData(){
		return data.clone();
	}
}
