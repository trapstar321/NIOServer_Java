package com.tomica.nioserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.logging.Level;

import com.tomica.nioserver.messages.Message;
import com.tomica.nioserver.messages.ServerMessage;

public class IOTask implements Runnable{
	private ByteBuffer buffer;	
	private int bufferSize;	
	private ServerConnection connection;

	
	public IOTask(ServerConnection connection, int bufferSize){
		this.connection=connection;
		this.bufferSize=bufferSize;		
		buffer=ByteBuffer.allocate(bufferSize);	
	}
	
	/*private void write(){		
		while(true){
			ServerMessage message = connection.getNextMessage();
			if(message==null){
				return;
			}else{
				byte[] data = message.getBytes();
				byte opCode = message.getOpCode();
				
				ByteBuffer bb = ByteBuffer.allocate(data.length+Message.BYTE_BYTES+Message.INT_BYTES);
				bb.putInt(data.length);
				bb.put(opCode);
				bb.put(data);				
				bb.flip();
				
				NIOServer.log(Level.INFO, messageDataToString(data));
				
				try{
					connection.getChannel().write(bb);
					NIOServer.log(Level.INFO, "Sent message "+message+" to client "+connection.getClientID());
				}catch(IOException ex){
					NIOServer.log(Level.SEVERE, "IOException:  "+ex.getMessage()+" while sending message to client "+connection, ex);
				}
				
				
			}
		}
	}*/
	
	/*private void read(){
		SocketChannel client = connection.getChannel();
		
		offset=0;
		
		if(!client.isOpen()){
			NIOServer.log(Level.INFO, "Channel closed, exit task");
			return;
		}
		
		try {
			int read = client.read(buffer);
			//NIOServer.log(Level.INFO, "Read "+read+" bytes from client "+client.socket());
			
			if(read==-1)
				connection.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
			connection.disconnect();
		}

		int dataSize=buffer.position();
		byte[] data = buffer.array();
        		
		if(dataSize==buffer.limit()){
			//enlarge buffer and restore data
			byte[] b = buffer.array();
			buffer = ByteBuffer.allocate(buffer.limit()+bufferSize);
			buffer.put(b);
        }	
		
        totalRead=buffer.position();

        //2.tražiti sep pos   
        int lastSepPos = 0;
        boolean sepFound = false;
        for (int sepPos = offset; sepPos < buffer.position(); sepPos++)
        {
        	//3.ima sep pos?
        	if (data[sepPos] == separator)
        	{
        		sepFound = true;
        		lastSepPos = sepPos;
        		
                //3.1.šalji poruku
        		byte opCode = data[offset];
        		byte[] dataCopy = Arrays.copyOfRange(data, offset+1, sepPos);
        		connection.notifyListeners(opCode, dataCopy);
        		lastSepPos = sepPos;
        		offset = sepPos + 1;
        	}
        }

        //4. naðen separator i ima još byte-ova za èitati, reset data array, offset=0, totalRead=totalRead - lastSepPos - 1
        if (sepFound && lastSepPos < totalRead)
        {        	
        	/*byte[] tmp = new byte[dataSize];         
            System.arraycopy(buffer.array(), 0, tmp, 0, dataSize);
            
            data = new byte[totalRead - lastSepPos - 1];            
                        
        	System.arraycopy(tmp,
        			lastSepPos + 1,
        			data,
        			0,
        			totalRead - lastSepPos - 1);
                 	offset = 0;             
                 	
             buffer=ByteBuffer.allocate(bufferSize);
             buffer.put(data);
        	
            byte[] newMsgData = new byte[totalRead - lastSepPos - 1];            
                        
        	System.arraycopy(data,
        			lastSepPos + 1,
        			newMsgData,
        			0,
        			totalRead - lastSepPos - 1);
                 	offset = 0;             
                 	
             buffer=ByteBuffer.allocate(bufferSize);
             buffer.put(newMsgData);
        }	
	}*/
	
	private void read(){
		SocketChannel client = connection.getChannel();		
		
		if(!client.isOpen()){
			NIOServer.log(Level.INFO, "Channel closed, exit task");
			return;
		}
		
		try {
			int read = client.read(buffer);
			//NIOServer.log(Level.INFO, "Read "+read+" bytes from client "+client.socket());
			
			//if(read==-1)
				//connection.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
			//connection.disconnect();
		}

		int dataSize=buffer.position();
		byte[] data = buffer.array();
        		
		if(dataSize==buffer.limit()){
			//enlarge buffer and restore data
			byte[] b = buffer.array();
			buffer = ByteBuffer.allocate(buffer.limit()+bufferSize);
			buffer.put(b);
        }	
		
		buffer.flip();

		int length;
		byte opcode;
		byte[] messageData;
		
		boolean resetBuffer = true;
		
		while(buffer.position()<buffer.limit()){			
			int left = buffer.limit()-buffer.position();
			
			//ima header
			if(left>=5){
				length = buffer.getInt();
				opcode = buffer.get();
				
				//ima i podataka
				if(left-5>=length){
					messageData = new byte[length];
					buffer.get(messageData, 0, length);
				
					NIOServer.log(Level.INFO, "Read message, opcode="+opcode+", length="+length+" data="+messageDataToString(messageData));					
					NIOServer.log(Level.INFO, "At position: "+buffer.position());
					//connection.notifyListeners(opcode, messageData);
				//nema podataka pa utrpaj ostatak u buffer	
				}else{
					NIOServer.log(Level.INFO, "Opcode="+opcode+", length="+length+". Whole message not yet received");					
					NIOServer.log(Level.INFO, "At position: "+buffer.position());
					
					NIOServer.log(Level.INFO, "Message not complete, put received back to buffer");
					
					buffer.position(buffer.position()-5);
					
					byte[] newMsgData = new byte[buffer.limit()-buffer.position()];            
	                
		        	System.arraycopy(buffer.array(),
		        			buffer.position(),
		        			newMsgData,
		        			0,
		        			buffer.limit()-buffer.position());	                 	             
		        
		        	//moguæe da je buffer full proširen pa ima više byte-ova u poruci nego šta stane u buffer
		        	if(left>buffer.limit())
		        		buffer=ByteBuffer.allocate(left);
		        	//isto moguæe da je buffer proširen jer ima još byte-ova a poruka nije gotova
		        	else if(buffer.capacity()>bufferSize)
		        		buffer=ByteBuffer.allocate(buffer.capacity());
		        	else
		        		buffer=ByteBuffer.allocate(bufferSize);
		            
		        	buffer.put(newMsgData);
		        	resetBuffer = false;
		            break;
				}
			//nema headera, utrpaj ostatak u buffer
			}else{				
				NIOServer.log(Level.INFO, "Message not complete, put received back to buffer");
				
				byte[] newMsgData = new byte[buffer.limit()-buffer.position()];            
                
	        	System.arraycopy(buffer.array(),
	        			buffer.position(),
	        			newMsgData,
	        			0,
	        			buffer.limit()-buffer.position());	                 	             
	        	
	        	//moguæe da je buffer full proširen pa ima više byte-ova u poruci nego šta stane u buffer
	        	if(left>buffer.limit())
	        		buffer=ByteBuffer.allocate(left);
	        	//isto moguæe da je buffer proširen jer ima još byte-ova a poruka nije gotova
	        	else if(buffer.capacity()>bufferSize)
	        		buffer=ByteBuffer.allocate(buffer.capacity());
	        	else
	        		buffer=ByteBuffer.allocate(bufferSize);
				
	        	buffer.put(newMsgData);
	        	resetBuffer = false;
				break;
			}			
		}
		
		if(resetBuffer){
			NIOServer.log(Level.INFO, "Reset buffer");
			buffer = ByteBuffer.allocate(bufferSize);
		}		
		data = buffer.array();
		NIOServer.log(Level.INFO, "Buffer: "+messageDataToString(data));
	}
	
	public void run(){		
		
		/*switch(connection.getIntentOps()){
			case SelectionKey.OP_READ:
				NIOServer.log(Level.INFO, "Start read IOTask for client "+connection);
				read();
				break;
			case SelectionKey.OP_WRITE:
				NIOServer.log(Level.INFO, "Start write IOTask for client "+connection);
				write();
				break;
			default:
				NIOServer.log(Level.WARNING, "Intent ops not OP_READ or OP_WRITE. Exiting task.");
		}*/
		
		NIOServer.log(Level.INFO, "IOTask for client "+connection+" done");
	}
	
	private String messageDataToString(byte[] data){
		StringBuilder b = new StringBuilder();
		b.append("[");
		
		for(int i=0;i<data.length; i++){
			if(i<data.length-1){
				b.append(data[i]+",");
			}else{
				b.append(data[i]);
			}
		}	
		
		b.append("]");
		return b.toString();
	}
}
