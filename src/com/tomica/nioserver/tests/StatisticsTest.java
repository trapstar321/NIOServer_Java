package com.tomica.nioserver.tests;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.junit.Before;
import org.junit.Test;

import com.tomica.nioserver.NIOServer;
import com.tomica.nioserver.events.OnServerReceivedListener;
import com.tomica.nioserver.events.ServerReceivedEvent;
import com.tomica.nioserver.messages.ServerMessage;
import com.tomica.nioserver.messages.impl.CM_ISONLINE;
import com.tomica.nioserver.messages.impl.CM_PING;
import com.tomica.nioserver.messages.impl.CM_PLAYERINFO;
import com.tomica.nioserver.messages.impl.CM_SETPOS;
import com.tomica.nioserver.messages.impl.SM_ISONLINE;
import com.tomica.nioserver.messages.impl.SM_LASTPOS;
import com.tomica.nioserver.messages.impl.SM_PLAYERINFO;
import com.tomica.nioserver.messages.impl.SM_PONG;

public class StatisticsTest implements OnServerReceivedListener{

	private NIOServer server;
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() {
		try {
			server = new NIOServer(new InetSocketAddress(10023),  4, 4, true);
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
			server.registerClientMessages(clientMessages);
			server.registerServerMessages(serverMessages);			
			server.addOnServerReceivedListener(this);					
		
			server.start();
			Thread.sleep(10000);		
			
			SendStatistics[] stats = server.getStats();
			
			for(SendStatistics s: stats){
				System.out.println(s);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void received(ServerReceivedEvent event) {
		try {
			server.write(event.getClientID(), new ServerMessage[]{new SM_PONG(new byte[32])});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

}
