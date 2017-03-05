import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.logging.Level;

import com.tomica.nioserver.NIOServer;
import com.tomica.nioserver.events.ClientConnectedEvent;
import com.tomica.nioserver.events.ClientDisconnectedEvent;
import com.tomica.nioserver.events.OnClientConnectedListener;
import com.tomica.nioserver.events.OnClientDisconnectedListener;
import com.tomica.nioserver.events.OnServerReceivedListener;
import com.tomica.nioserver.events.ServerReceivedEvent;
import com.tomica.nioserver.messages.ServerMessage;
import com.tomica.nioserver.messages.impl.*;
import com.tomica.nioserver.objects.PlayerInfo;

public class RunServer implements OnServerReceivedListener, OnClientConnectedListener, OnClientDisconnectedListener{	
	private NIOServer server;
	public static void main(String[] args) {
		RunServer m = new RunServer();		
		try {
			m.server = new NIOServer(new InetSocketAddress(10023), 4, 4);
			com.tomica.nioserver.messages.ClientMessage[] clientMessages = new com.tomica.nioserver.messages.ClientMessage[4];		
			clientMessages[0]=new CM_SETPOS();
			clientMessages[1]=new CM_ISONLINE();
			clientMessages[2]=new CM_PLAYERINFO();
			clientMessages[3]=new CM_PING();
			
			com.tomica.nioserver.messages.ServerMessage[] serverMessages = new com.tomica.nioserver.messages.ServerMessage[4];
			serverMessages[0]=new SM_ISONLINE();
			serverMessages[1]=new SM_LASTPOS();
			serverMessages[2]=new SM_PLAYERINFO();
			serverMessages[3]=new SM_PONG();
			m.server.registerClientMessages(clientMessages);
			m.server.registerServerMessages(serverMessages);			
			m.server.addOnServerReceivedListener(m);			
			
			m.server.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		try {
			Thread.sleep(10000);
			m.server.shutdown();			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void received(ServerReceivedEvent event) {		
		System.out.println("ServerReceivedEvent: Command "+event.getMessage()+" from client "+event.getClientID());
		//server.write(clientID, new ServerMessage[]{new SM_ISONLINE(true)});
		PlayerInfo info = new PlayerInfo(22, "trapstar321", new Date(), (double)1000000000);
		try{
			switch(event.getMessage().getOpCode()){
				case CM_ISONLINE.OPCODE:
					server.write(event.getClientID(), new ServerMessage[]{new SM_ISONLINE(true)});
					break;
				case CM_PLAYERINFO.OPCODE:
					server.write(event.getClientID(), new ServerMessage[]{new SM_PLAYERINFO(info, true)});
					break;
				case CM_PING.OPCODE:
					server.write(event.getClientID(), new ServerMessage[]{new SM_PONG(new byte[32])});
					break;
				default:
						NIOServer.log(Level.WARNING, "Received unwanted message");
			}
		}catch(IOException ex){
			NIOServer.log(Level.SEVERE, "Exception thrown when writting to client");	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void connected(ClientConnectedEvent event) {
		System.out.println("ClientConnectedEvent: Client ID="+event.getClientID()+", Address="+event.getAddress()+" connected");		
	}

	@Override
	public void disconnected(ClientDisconnectedEvent event) {
		System.out.println("ClientDisconnectedEvent: Client ID="+event.getClientID()+", Address="+event.getAddress()+" disconnected");		
	}
}
