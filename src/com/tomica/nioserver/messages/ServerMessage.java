package com.tomica.nioserver.messages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

public abstract class ServerMessage implements Message{
	private ByteBuffer data;
	
	protected void expandBuffer(int size){
		if(data==null)
			data=ByteBuffer.allocate(size);
		else{
			byte[] stored = data.array();
			data = ByteBuffer.allocate(data.capacity()+size);
			data.put(stored);
		}
	}
	
	public void putInt(int i){
		expandBuffer(Message.INT_BYTES);
		data.putInt(i);
	}
	
	public void putBool(boolean bool){
		expandBuffer(Message.BYTE_BYTES);
		if(bool)
			data.put((byte)1);
		else
			data.put((byte)0);
	}
	
	public void putByte(byte b){
		expandBuffer(Message.BYTE_BYTES);
		data.put(b);
	}
	
	public void putBytes(byte[] b){
		expandBuffer(b.length);
		data.put(b);
	}
	
	public void putShort(short s){
		expandBuffer(Message.SHORT_BYTES);
		data.putShort(s);
	}
	
	public void putLong(long l){
		expandBuffer(Message.LONG_BYTES);
		data.putLong(l);
	}
	
	public void putDouble(double d){
		expandBuffer(Message.DOUBLE_BYTES);
		data.putDouble(d);
	}
	
	public void putFloat(float f){
		expandBuffer(Message.FLOAT_BYTES);
		data.putFloat(f);
	}
	
	public void putChar(char c){
		expandBuffer(Message.CHAR_BYTES);
		data.putChar(c);
	}
	
	public void putString(String s){
		expandBuffer(INT_BYTES+s.length());
		data.putInt(s.length());
		data.put(s.getBytes());
	}
	
	public void putObject(Object obj) throws IOException{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(obj);
		
		byte[] bytes = bos.toByteArray();
		
		expandBuffer(INT_BYTES+bytes.length);
		data.putInt(bytes.length);
		data.put(bytes);
	}	
	
	@Override
	public byte[] getData(){
		return data.array();
	}
	
	
	public synchronized void reset(){
		data.flip();
	}
}
