package com.tomica.nioserver;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.tomica.nioserver.messages.ServerMessage;

public class ServerConnection {
	private SocketChannel channel;
	private ByteBuffer buffer;
	private IOTask task;
	private static int CLIENTID=0;
	private Integer clientID;	
	
	private List<ByteBuffer> writeQueue = new LinkedList<ByteBuffer>();
	
	private Object lock= new Object();
	
	
	public ServerConnection(Dispatcher dispatcher, SocketChannel channel, int bufferSize){		
		this.channel=channel;
		this.buffer=ByteBuffer.allocate(bufferSize);
		this.task=new IOTask(this, bufferSize);
		
		synchronized (lock) {
			CLIENTID+=1;
			clientID=CLIENTID;
		}
	}
	
	public SocketChannel getChannel(){
		return channel;
	}
	
	public ByteBuffer getBuffer(){
		return buffer;
	}
	
	public void setBuffer(ByteBuffer buffer){
		this.buffer=buffer;
	}
	
	public IOTask getTask(){
		return task;
	}
	
	public Integer getClientID(){
		return clientID;
	}
	
	public void write(ServerMessage[] messages){
		ByteBuffer[] data = new ByteBuffer[messages.length];
		
		for(int i=0; i<messages.length;i++)
			data[i]=ByteBuffer.wrap(messages[i].getBytes());
		
		synchronized (writeQueue) {
			writeQueue.addAll(Arrays.asList(data));	
		}				
	}
	
	public ByteBuffer getMessage(int i){		
		synchronized (writeQueue) {
			return writeQueue.get(i);
		}
	}
	
	public ByteBuffer removeMessage(int i){		
		synchronized (writeQueue) {
			return writeQueue.remove(i);
		}
	}
	
	public boolean isWriteQueueEmpty(){
		synchronized (writeQueue) {
			return writeQueue.isEmpty();
		}
	}
	
	public ByteBuffer getWriteBuffer(){
		synchronized (writeQueue) {
			int size=0;
			for(int i=0; i<writeQueue.size(); i++)
				size+=writeQueue.get(i).capacity();
			
			ByteBuffer b = ByteBuffer.allocate(size);
			
			while(!writeQueue.isEmpty()){
				ByteBuffer data = writeQueue.get(0);
				b.put(data.array());
				writeQueue.remove(0);
			}
			return b;
		}
	}
	
	@Override
	public String toString(){
		return "{ID="+clientID+", socket="+channel.socket()+"}";
		
	}
}
