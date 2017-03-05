package com.tomica.nioserver;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.tomica.nioserver.events.ClientConnectedEvent;
import com.tomica.nioserver.events.ClientDisconnectedEvent;
import com.tomica.nioserver.events.OnClientConnectedListener;
import com.tomica.nioserver.events.OnClientDisconnectedListener;
import com.tomica.nioserver.events.OnServerReceivedListener;
import com.tomica.nioserver.events.ServerReceivedEvent;
import com.tomica.nioserver.messages.*;
import com.tomica.nioserver.tests.SendStatistics;


public class NIOServer implements Runnable{

	private Selector selector = Selector.open();
    private ServerSocketChannel server = ServerSocketChannel.open();    
  
	private Thread worker;
	private InetSocketAddress address;
    
	private Object startedStateLock = new Object();	
	private Object selectorLock = new Object();
    private boolean started = false;
    private boolean start=false;
    private boolean shutdown=false;
    
    public static boolean LOG=true;
	
	private final int bufferSize=2048;
    
	private int lastDispatcher=0;
	
    private static Logger logger;
        
    private Hashtable<Integer, SocketChannel> connectionIDs = new Hashtable<Integer, SocketChannel>();
	private Hashtable<SocketChannel, ServerConnection> connections = new Hashtable<SocketChannel,ServerConnection>();
    
	private List<OnServerReceivedListener> serverReceivedlisteners = new ArrayList<OnServerReceivedListener>();
	private List<OnClientConnectedListener> clientConnectedListeners = new ArrayList<OnClientConnectedListener>();
	private List<OnClientDisconnectedListener> clientDisconnectedListeners = new ArrayList<OnClientDisconnectedListener>();
	
	private ExecutorService eventThreadPool;
	
	private ClientMessage[] clientMessages=null;
	private ServerMessage[] serverMessages=null;
	
	Dispatcher[] dispatchers;
	
	private Dispatcher getDispatcher(){		
		if(lastDispatcher==dispatchers.length-1){
			lastDispatcher=0;
			return dispatchers[lastDispatcher];
		}else{
			lastDispatcher+=1;
			return dispatchers[lastDispatcher];
		}		
	}
	
	{
		logger = Logger.getLogger(NIOServer.class.getName());
        logger.setUseParentHandlers(false);

        LogFormatter formatter = new LogFormatter();
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(formatter);

        logger.addHandler(handler);
	}
	
    public NIOServer(InetSocketAddress address, int nIOThreads, int nEventHandlerThreads) throws IOException {
        this.address=address;
        
        
        if(nIOThreads>1){
        	dispatchers = new Dispatcher[nIOThreads];
	        for(int i=0; i<nIOThreads; i++){
	        	dispatchers[i]=new Dispatcher(this, bufferSize);
	        }
        }else{
        	dispatchers = new Dispatcher[1];
        	dispatchers[0]= new Dispatcher(this, bufferSize);
        }
        
        if(nEventHandlerThreads<1){
        	eventThreadPool=Executors.newFixedThreadPool(1);
        }else{
        	eventThreadPool=Executors.newFixedThreadPool(nEventHandlerThreads);
        }
    }	
    
    public NIOServer(InetSocketAddress address, int nIOThreads, int eventHandlerThreads,boolean saveStats) throws IOException {
        this(address, nIOThreads, eventHandlerThreads);
        for(Dispatcher d: dispatchers){
        	d.saveStats();
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
    
    public void write(int clientID, ServerMessage[] messages) throws Exception{    	
    	if(serverMessages!=null){
    		for(Dispatcher d: dispatchers){
    			d.write(clientID, messages);
    		}
    	}else{
    		log(Level.WARNING, "No server message has been registered. Will not write to client");
    	}
    }
    
	@Override
	public void run() {
		for(Dispatcher d: dispatchers){
			Thread t = new Thread(d);
			t.start();
		}
		while(true){
            try {            	
            	
            	//start shutdown process
            	synchronized (startedStateLock) {
            		//remove accept intent            		
					if(shutdown){
						NIOServer.log(Level.INFO, "Wait for dispatchers to process write queue and disconnect clients");
						for(Dispatcher d: dispatchers){
							synchronized (d) {
								try {
									d.shutdown();
									d.wait();									
								} catch (InterruptedException e) {
									NIOServer.log(Level.WARNING, "InterruptedException: "+e.getMessage()+
											" while waiting for dispatcher to disconnect clients");
								}
							}							
						}
						NIOServer.log(Level.INFO, "Shutdown complete");
						close();
						return;
					}					
				}
            	
                selector.select();                
            	
            	//if(selected!=0){
	                Iterator<SelectionKey> i = selector.selectedKeys().iterator();
	                while (i.hasNext()) {
	                    SelectionKey key = i.next();
	                    i.remove();
	                    if (!key.isValid()) {
	                        continue;
	                    }
	                    try {                    	
	                        // get a new connection
	                        if (key.isAcceptable()) {	                        	
	                        //if(key.readyOps()==SelectionKey.OP_ACCEPT){
	                            // accept them
	                            SocketChannel client = server.accept();
	                            log(Level.INFO, "New connection from "+client.socket());	                            
	                            if(client.isOpen()){
		                            ServerConnection conn = getDispatcher().accept(client);		                            
		                            putConnection(client, conn);
		                            notifyClientConnectedListener(conn);	
	                            }	                            
	                        }
	                            // read from the connection
	                        /*} else if (key.isReadable()) {
	                    	//}else if(key.readyOps()==SelectionKey.OP_READ){
	                            //  get the client                        	
	                            SocketChannel client = (SocketChannel) key.channel();                            
	                            
	                            if(client.isOpen()){
		                            ServerConnection conn = getConnection(client);
		                            
		                            if(conn==null){
		                            	conn = new ServerConnection(this, client, bufferSize);
		                            	
		                            	putConnection(client, conn);	
		                            }
		                            
		                            //we want task to read data from channel
		                            conn.setIntentOps(SelectionKey.OP_READ);
		                            threadPool.submit(conn.getTask());
	                            }
	                        }else if(key.isWritable()){
	                    	//}else if(key.readyOps()==SelectionKey.OP_WRITE){
	                        	SocketChannel client = (SocketChannel) key.channel();                        	
	                        	if(client.isOpen()){
		                            ServerConnection conn = getConnection(client);
		                            
		                            if(conn==null){
		                            	conn = new ServerConnection(this, client, bufferSize);
		                            	
		                            	putConnection(client, conn);	
		                            }	                           
		                            
		                            if(conn.hasMessagesToWrite()){	                            
			                            //we want task to write data from channel
			                            conn.setIntentOps(SelectionKey.OP_WRITE);
			                            threadPool.submit(conn.getTask());
		                            }
	                            }
	                        }*/
	                    } catch (Exception ex) {
	                    	log(Level.SEVERE, "Error handling client: " + key.channel(), ex);                                               
	                    }
	                }
	                synchronized (selectorLock) {
						
					}
            	//}
            } catch (IOException ex) {
                // call it quits
                //shutdown();
                // throw it as a runtime exception so that Bukkit can handle it
                throw new RuntimeException(ex);
            }
        }
		
	}

	/*public void readData() throws IOException {         
         handler.read(channel);
     }
	 
	 private void writeLine(String line) throws IOException {
         channel.write(encoder.encode(CharBuffer.wrap(line + "\r\n")));
     }*/
	
	public void shutdown() {
		if(server.isOpen()){
			synchronized (startedStateLock) {
				if(shutdown){
					NIOServer.log(Level.INFO, "Shutdown already in progress");
					return;
				}
				NIOServer.log(Level.INFO, "Begin shutdown");
				shutdown=true;
				synchronized (selectorLock) {
					selector.wakeup();
				}
			}
		}
    }
	
	public void registerServerMessages(ServerMessage[] messages){
		this.serverMessages=messages.clone();
	}
	
	public void registerClientMessages(ClientMessage[] messages){
		this.clientMessages=messages.clone();
	}
	
	public void addOnServerReceivedListener(OnServerReceivedListener listener){		
		synchronized (serverReceivedlisteners) {
			serverReceivedlisteners.add(listener);
		}
	}
	
	public void removeOnServerReceivedListener(OnServerReceivedListener listener){
		synchronized (serverReceivedlisteners) {
			serverReceivedlisteners.remove(listener);
		}
	}
	
	public void addOnClientConnectedListener(OnClientConnectedListener listener){		
		synchronized (clientConnectedListeners) {
			clientConnectedListeners.add(listener);
		}
	}
	
	public void removeOnClientConnectedListener(OnClientConnectedListener listener){
		synchronized (clientConnectedListeners) {
			clientConnectedListeners.remove(listener);
		}
	}
	
	public void addOnClientDisconnectedListener(OnClientDisconnectedListener listener){		
		synchronized (clientDisconnectedListeners) {
			clientDisconnectedListeners.add(listener);
		}
	}
	
	public void removeOnClientDisconnectedListener(OnClientDisconnectedListener listener){
		synchronized (clientDisconnectedListeners) {
			clientDisconnectedListeners.remove(listener);
		}
	}
	
	public synchronized void notifyClientConnectedListener(ServerConnection connection){
		synchronized (clientConnectedListeners) {
        	ClientConnectedEvent event = new ClientConnectedEvent(connection.getChannel().socket().getInetAddress(), connection.getClientID());
			for(OnClientConnectedListener listener: clientConnectedListeners){
				DispatchEventTask task = new DispatchEventTask(listener, event);
				eventThreadPool.execute(task);
				NIOServer.log(Level.INFO, "Added new task for connected event to thread pool");
			}	
		}
	}
	
	public synchronized void notifyClientDisconnectedListener(ServerConnection connection){
		synchronized (clientDisconnectedListeners) {
        	ClientDisconnectedEvent event = new ClientDisconnectedEvent(connection.getChannel().socket().getInetAddress(), connection.getClientID());
			for(OnClientDisconnectedListener listener: clientDisconnectedListeners){
				DispatchEventTask task = new DispatchEventTask(listener, event);
				eventThreadPool.execute(task);
				NIOServer.log(Level.INFO, "Added new task for disconnected event to thread pool");
			}
			removeConnection(connection);
		}
	}
	
	public synchronized void notifyClientReceivedListeners(byte opCode, byte[] data, int clientID){
		ClientMessage msg=null;
		if(clientMessages!=null){
			for(ClientMessage b: clientMessages){
				if(b.getOpCode()==opCode){
					msg = b;
					break;
				}
			}
			if(msg==null){
				log(Level.WARNING, "opCode "+opCode+" not registered. No event will be generated");
				return;
			}
		}else{
			log(Level.WARNING, "No client messages has been registered. All received messages will be ignored");
		}
		
		if(msg!=null){
			ClientMessage message = makeMessage(msg, opCode, data);
			ServerReceivedEvent event = new ServerReceivedEvent(message, clientID);
			
			ServerConnection conn = getConnection(clientID);
			log(Level.INFO, "New message from "+conn+": "+message);			
			
			synchronized (serverReceivedlisteners) {
				for(OnServerReceivedListener listener: serverReceivedlisteners){
					DispatchEventTask task = new DispatchEventTask(listener, event);
					eventThreadPool.execute(task);
					NIOServer.log(Level.INFO, "Added new task for received event to thread pool");
					//listener.received(event);
				}	
			}			
		}
	}
	
	private ClientMessage makeMessage(ClientMessage msg, byte opCode, byte[] data){
		try {
			Class<?> clazz = msg.getClass();
			Constructor<?> ctor = clazz.getConstructor(byte[].class);
			Object object = ctor.newInstance(new Object[] { data });
			return (ClientMessage)object;		
		} catch (IllegalArgumentException e) {
			log(Level.WARNING,"IllegalArgumentException: "+e.getMessage() ,e);
		} catch (InstantiationException e) {
			log(Level.WARNING,"InstantiationException: "+e.getMessage() ,e);			
		} catch (IllegalAccessException e) {
			log(Level.WARNING,"IllegalAccessException: "+e.getMessage() ,e);
		} catch (InvocationTargetException e) {
			log(Level.WARNING,"InvocationTargetException: "+e.getMessage() ,e);
		} catch (SecurityException e) {
			log(Level.WARNING,"SecurityException: "+e.getMessage() ,e);
		} catch (NoSuchMethodException e) {
			log(Level.WARNING,"NoSuchMethodException: "+e.getMessage() ,e);
		}
		return null;
	}
	
	public static void log(Level level, String message) {
		if(LOG)
			logger.log(level, "Thread "+Thread.currentThread().getId()+": "+message);
	}
	
	public static void log(Level level, String message, Throwable ex) {
		if(LOG)
			logger.log(level, "Thread "+Thread.currentThread().getId()+": "+message, ex);
	}
		
	public boolean isRunning(){
		synchronized (startedStateLock) {
			return started;
		}
	}
	
	public void start() throws IOException{
		synchronized (startedStateLock) {
			if(!start && started){
				log(Level.INFO, "Server alread running");
				return;
			}
			if(!start){
				log(Level.INFO, "Start server");
				start=true;
			}else{
				log(Level.INFO, "Already starting");
				return;
			}
		}
		server = ServerSocketChannel.open();
		server.socket().bind(address);
        server.configureBlocking(false);
        synchronized (selectorLock) {
        	server.register(selector, SelectionKey.OP_ACCEPT);	
		}          	
    	
    	worker = new Thread(this);
    	worker.start();
    	
    	synchronized (startedStateLock) {
    		start=false;
    		started=true;
		}
	}
	
	private void close(){
		if(server.isOpen()){
			synchronized (startedStateLock) {
				start=false;
				shutdown=false;
			}
			try {				
				server.close();
				eventThreadPool.shutdown();
			} catch (IOException e) {
				NIOServer.log(Level.INFO, "IOException: "+e.getMessage()+" while closing server socket");
			}
		}
	}
	
	public SendStatistics[] getStats(){
		SendStatistics[] stats = new SendStatistics[dispatchers.length];
		
		int i=0;
		for(Dispatcher d: dispatchers){
			stats[i]=d.getStats();
			i++;
		}
		return stats;
	}
}
