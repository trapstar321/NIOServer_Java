package com.tomica.nioserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.lang3.time.StopWatch;

import com.tomica.nioserver.messages.Message;
import com.tomica.nioserver.messages.ServerMessage;
import com.tomica.nioserver.tests.SendStatistics;

public class Dispatcher implements Runnable{
	private Selector selector = Selector.open();
	
    private Hashtable<Integer, SocketChannel> connectionIDs = new Hashtable<Integer, SocketChannel>();
	private Hashtable<SocketChannel, ServerConnection> connections = new Hashtable<SocketChannel,ServerConnection>();
	
	private int bufferSize;
	private NIOServer server;
	
	private static int DISPATCHERID=0;
	private Integer id;
	
	private Object Lock= new Object();	
	
	private boolean shutdown=false;
	
	private SendStatistics stats;
	private StopWatch watch = new StopWatch();
	private StopWatch runWatch = new StopWatch();
	
	private List<IntentChangeRequest> intentRequests = new ArrayList<IntentChangeRequest>();
	
	public Dispatcher(NIOServer server, int bufferSize) throws IOException {
		synchronized (Lock) {
			DISPATCHERID+=1;
			id=DISPATCHERID;
		}
		
		this.bufferSize=bufferSize;
		this.server=server;	
	}
	
	private Object selectorLock = new Object();

	public void saveStats(){
		stats = new SendStatistics(id);
	}
	
	public SendStatistics getStats(){
		if(stats==null)
			return stats;
		synchronized (stats) {
			return stats;
		}
	}
	
	public void shutdown(){
		synchronized (Lock) {
			if(shutdown){
				NIOServer.log(Level.INFO, "Dispatcher "+getDispatcherID()+": shutdown already in progress");
				return;
			}
			
			shutdown=true;
		}
		
		//remove op keys if nothing to write, or add write op if write queue has data
		List<IntentChangeRequest> requests = new ArrayList<IntentChangeRequest>();
		synchronized (connections) {
			for(ServerConnection conn: connections.values()){
				if(conn.isWriteQueueEmpty()){
					IntentChangeRequest request = new IntentChangeRequest(conn, 0);
					requests.add(request);
				}else{
					IntentChangeRequest request = new IntentChangeRequest(conn, SelectionKey.OP_WRITE);
					requests.add(request);
				}
			}	
		}
		
		synchronized (intentRequests) {
			intentRequests.addAll(requests);
		}
		
		synchronized (selectorLock) {
			selector.wakeup();
		}
	}
	
	@Override
	public void run() {
		while(true){
			runWatch.reset();
			runWatch.start();	
			try{
				synchronized(intentRequests) {					
		          Iterator<IntentChangeRequest> changes = intentRequests.iterator();
		          while (changes.hasNext()) {
		        	  IntentChangeRequest change = (IntentChangeRequest) changes.next();
		        	  ServerConnection connection = change.getConnection();
		        	  SelectionKey key = connection.getChannel().keyFor(selector);
		        	  key.interestOps(change.getIntent());
		        	  
		        	  switch(change.getIntent()){
		        	  	case SelectionKey.OP_READ:
		        	  		NIOServer.log(Level.INFO, "Dispatcher "+getDispatcherID()+": changed intentOps to OP_READ for connection "+connection);
		        	  		break;
		        	  	case SelectionKey.OP_WRITE:
		        	  		NIOServer.log(Level.INFO, "Dispatcher "+getDispatcherID()+": changed intentOps to OP_WRITE for connection "+connection);
		        	  		break;
		        	  	default:
		        	  		NIOServer.log(Level.INFO, "Dispatcher "+getDispatcherID()+": changed intentOps to "+change.getIntent()+" for connection "+connection);
		        	  		break;
		        	  }		        	  
		          }		          
		          intentRequests.clear();		          
				}
				
				//disconnect clients if all messages have been written
				boolean shutDown = false;
				synchronized (Lock) {
					if(shutdown){
						shutDown=true;
					}
				}
				
				if(shutDown){
					boolean done=true;
					for(ServerConnection conn: connections.values()){
						if(!conn.isWriteQueueEmpty()){
							done=false;
							break;
						}
					}				
					
					if(done){
						NIOServer.log(Level.INFO, "Dispatcher "+getDispatcherID()+": all clients write queue empty, disconnect clients");
						synchronized (connections) {
							for(ServerConnection conn: connections.values()){
								disconnectClient(conn);
							}
						}
						synchronized (this) {
							notify();
						}
						NIOServer.log(Level.INFO, "Dispatcher "+getDispatcherID()+": shutdown complete");
						return;
					}
				}
				
				watch.reset();
				watch.start();
				selector.select();
				watch.stop();
				updateSelectStatistics(watch.getTime());
		
		        // Iterate over the set of keys for which events are available
		        Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
		        while (selectedKeys.hasNext()) {
		          SelectionKey key = (SelectionKey) selectedKeys.next();
		          selectedKeys.remove();
		
		          if (!key.isValid()) {
		            continue;
		          }
		
		          if (key.isReadable()) {
		            read(key);
		          }else if(key.isWritable()){
		        	write(key);  
		          }
		        }
		        
		        synchronized (selectorLock) {
					
				}
			}catch(IOException ex){
				NIOServer.log(Level.SEVERE, "Dispatcher "+getDispatcherID()+": error selecting keys", ex);  
			}
			runWatch.stop();
			updateRunStatistics(runWatch.getTime());
		}
	}
	
	public ServerConnection accept(SocketChannel client) throws IOException{		
		// non blocking please
        client.configureBlocking(false);
        // show out intentions
 
        ServerConnection conn = new ServerConnection(this, client, bufferSize);		                            	
        putConnection(client, conn);
 
        
        synchronized (selectorLock) {
        	selector.wakeup();
        	client.register(selector, SelectionKey.OP_READ);	
		}
        
        NIOServer.log(Level.INFO, "Dispatcher "+getDispatcherID()+": added connection "+conn+" to dispatcher "+getDispatcherID());
        
        return conn;
	}
	
	//ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
	
	private void read(SelectionKey key){
		watch.reset();
		watch.start();
		SocketChannel client = (SocketChannel)key.channel();
		ServerConnection connection = getConnection(client);
		NIOServer.log(Level.INFO, "Dispatcher "+getDispatcherID()+": reading from "+connection+" on dispatcher "+getDispatcherID());		
		ByteBuffer buffer = connection.getBuffer();
		
		
		if(!client.isOpen()){
			NIOServer.log(Level.INFO, "Dispatcher "+getDispatcherID()+": channel closed, exit task");
			return;
		}	
		try {			
			int read = client.read(buffer);
			
			//NIOServer.log(Level.INFO, "Read "+read+" bytes from client "+client.socket());
			//watch.stop();
			//updateReadStatistics(watch.getTime());
			if(read==-1){
				disconnectClient(connection);
				key.cancel();
			}
		} catch (IOException e) {
			e.printStackTrace();
			disconnectClient(connection);
			key.cancel();
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
				
					NIOServer.log(Level.INFO, "Dispatcher "+getDispatcherID()+": read message, opcode="+opcode+", length="+length+" data="+messageDataToString(messageData));					
					NIOServer.log(Level.INFO, "Dispatcher "+getDispatcherID()+": at position: "+buffer.position());
					notifyClientReceivedListeners(opcode, messageData, connection.getClientID());					
				//nema podataka pa utrpaj ostatak u buffer	
				}else{
					NIOServer.log(Level.INFO, "Dispatcher "+getDispatcherID()+": opcode="+opcode+", length="+length+". Whole message not yet received");					
					NIOServer.log(Level.INFO, "Dispatcher "+getDispatcherID()+": at position: "+buffer.position());
					
					NIOServer.log(Level.INFO, "Dispatcher "+getDispatcherID()+": message not complete, put received back to buffer");
					
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
				NIOServer.log(Level.INFO, "Dispatcher "+getDispatcherID()+": message not complete, put received back to buffer");
				
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
			NIOServer.log(Level.INFO, "Dispatcher "+getDispatcherID()+": reset buffer");
			buffer = ByteBuffer.allocate(bufferSize);
		}		
		data = buffer.array();
		connection.setBuffer(buffer);
		NIOServer.log(Level.INFO, "Dispatcher "+getDispatcherID()+": buffer: "+messageDataToString(data));
		watch.stop();
		updateReadStatistics(watch.getTime());
	}
	
	private void write(SelectionKey key){
		watch.reset();
		watch.start();
		SocketChannel client = (SocketChannel)key.channel();
		ServerConnection connection = getConnection(client);
		
		ByteBuffer buffer = connection.getWriteBuffer();		

		try{
			while (buffer.position() > 0)
			{
				try
				{
					buffer.flip();					
					int count = client.write(buffer);					
					if (count == 0)
					{
						IntentChangeRequest request = new IntentChangeRequest(connection, SelectionKey.OP_WRITE);
						addIntentChangeRequest(request);
						break;
					}
				}
				finally
				{
					buffer.compact();
				}
			}
			if (buffer.position() == 0)
			{
				IntentChangeRequest request = new IntentChangeRequest(connection, SelectionKey.OP_READ);
				addIntentChangeRequest(request);	
			}
		}catch(IOException ex){
			NIOServer.log(Level.SEVERE, "Dispatcher "+getDispatcherID()+": IOException:  "+ex.getMessage()+" while sending message to client "+connection, ex);
			disconnectClient(connection);				
		}
		watch.stop();
		updateWriteStatistics(watch.getTime());
	}
	
	/*private void write(SelectionKey key){
		watch.reset();
		watch.start();
		SocketChannel client = (SocketChannel)key.channel();
		ServerConnection connection = getConnection(client);
		while(!connection.isWriteQueueEmpty()){
			ByteBuffer message = connection.getMessage(0);			
			
			try{
				//message.flip();
				int wrote = client.write(message);
			
				//done so remove
				if(message.remaining()==0){			
					NIOServer.log(Level.INFO, "Sent message to client "+connection);
					connection.removeMessage(0);
					continue;		
				//wait for selector for next op_write
				}else if(wrote==0 && message.remaining()>0){
					//addIntentChangeRequest(new IntentChangeRequest(connection, SelectionKey.OP_WRITE));
					NIOServer.log(Level.INFO, "Wrote 0 bytes to client "+connection);
					break;
				//not all written continue
				}else if(wrote>0 && message.remaining()>0){
					NIOServer.log(Level.INFO, "Wrote "+wrote+" from "+message.remaining() +" to client "+connection);
					continue;
				}
			}catch(IOException ex){
				NIOServer.log(Level.SEVERE, "IOException:  "+ex.getMessage()+" while sending message to client "+connection, ex);
				disconnectClient(connection);
				break;
			}finally {
				//message.compact();
			}
		}
		
		if(connection.isWriteQueueEmpty()){
			IntentChangeRequest request = new IntentChangeRequest(connection, SelectionKey.OP_READ);
			addIntentChangeRequest(request);			
		}
		watch.stop();
		updateWriteStatistics(watch.getTime());
	}*/
	
	public void notifyClientReceivedListeners(byte opCode, byte[] data, int clientID){
		server.notifyClientReceivedListeners(opCode, data, clientID);
	}
	
	public void write(int clientID, ServerMessage[] messages) throws Exception{
		synchronized (Lock) {
			if(shutdown)
				throw new Exception("Dispatcher "+getDispatcherID()+": shutdown in progress");
		}
    	ServerConnection conn = getConnection(clientID);
    	
    	if(conn!=null){
    		conn.write(messages);
    		NIOServer.log(Level.INFO, "Dispatcher "+getDispatcherID()+": added "+messages.length+" messages for client "+conn+" to write queue");    		
    		
    		IntentChangeRequest request = new IntentChangeRequest(conn, SelectionKey.OP_WRITE);    		
    		addIntentChangeRequest(request);
    	}
    }
	
	private ServerConnection getConnection(SocketChannel key){
    	synchronized (connections){
    		return connections.get(key);
    	}
    }
	
	private ServerConnection getConnection(Integer clientID){
    	SocketChannel channel=null;
    	synchronized (connectionIDs){
    		channel=connectionIDs.get(clientID);
    	}
    	if(channel!=null)
    		return getConnection(channel);
    	return null;
    }
    
	
	private void putConnection(SocketChannel key, ServerConnection conn){
    	synchronized (connectionIDs) {
    		connectionIDs.put(conn.getClientID(), key);			
		}    	
    	
    	synchronized (connections) {			
	    	connections.put(key, conn);		
		}
    }
    
    private void removeConnection(ServerConnection conn){
    	synchronized (connectionIDs) {
    		connectionIDs.remove(conn.getClientID());	
		}
    	
    	synchronized (connections) {
    		connections.remove(conn.getChannel());	
		}    	
    }
    
    public void addIntentChangeRequest(IntentChangeRequest request){
    	synchronized(intentRequests) {
	    	intentRequests.add(request);	    	
    	}
    	
    	synchronized (selectorLock) {
    		selector.wakeup();
		}
    }
    
    public void disconnectClient(ServerConnection conn){
		SocketChannel channel = conn.getChannel();
		if(conn.getChannel().isOpen()){
			server.notifyClientDisconnectedListener(conn);
			NIOServer.log(Level.INFO, "Dispatcher "+getDispatcherID()+": client "+conn+" disconnected");
			try{
				channel.close();
			}catch(IOException ex){
				NIOServer.log(Level.SEVERE, "Dispatcher "+getDispatcherID()+": client "+conn, ex);
			}
			removeConnection(conn);
		}
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
    
	public Integer getDispatcherID(){
		return id;
	}
	
	private void updateWriteStatistics(long time){
		if(stats!=null){
			synchronized (stats) {
				stats.reportLastWrite(time);	
			}			
		}
	}
	
	private void updateReadStatistics(long time){
		if(stats!=null){
			synchronized (stats) {
				stats.reportLastRead(time);
			}
		}
	}
	
	private void updateSelectStatistics(long time){
		if(stats!=null){
			synchronized (stats) {
				stats.reportLastSelect(time);
			}
		}
	}
	
	private void updateRunStatistics(long time){
		if(stats!=null){
			synchronized (stats) {
				stats.reportLastRun(time);
			}
		}
	}
}
