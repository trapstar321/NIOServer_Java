package com.tomica.nioserver.tests;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.logging.Level;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;

import com.tomica.nioserver.NIOServer;
import com.tomica.nioserver.messages.ClientMessage;
import com.tomica.nioserver.messages.impl.CM_PLAYERINFO;
import com.tomica.nioserver.messages.impl.SM_PLAYERINFO;
import com.tomica.nioserver.objects.PlayerInfo;

public class MakeMessageTest {
	private StopWatch watch = new StopWatch();
	
	
	@Test
	public void testMakeMessage() throws IOException {		
		PlayerInfo info = new PlayerInfo(22, "trapstar321", new Date(), (double)1000000000);
		watch.start();
		SM_PLAYERINFO message = new SM_PLAYERINFO(info, true);
		watch.stop();
		System.out.println("ServerMessage instance created in "+watch.getTime());
		watch.reset();
		
		watch.start();
		CM_PLAYERINFO msg = (CM_PLAYERINFO)makeMessage(new CM_PLAYERINFO(), message.getOpCode(), message.getBytes());
		info = msg.getPlayerInfo();
		watch.stop();
		System.out.println("ClientMessage instance created in "+watch.getTime());
		watch.reset();
		assertNotNull(msg);
	}

	private ClientMessage makeMessage(ClientMessage msg, byte opCode, byte[] data){
		try {
			Class<?> clazz = msg.getClass();
			Constructor<?> ctor = clazz.getConstructor(byte[].class);
			Object object = ctor.newInstance(new Object[] { data });
			return (ClientMessage)object;		
		} catch (IllegalArgumentException e) {
			NIOServer.log(Level.WARNING,"IllegalArgumentException: "+e.getMessage() ,e);
		} catch (InstantiationException e) {
			NIOServer.log(Level.WARNING,"InstantiationException: "+e.getMessage() ,e);			
		} catch (IllegalAccessException e) {
			NIOServer.log(Level.WARNING,"IllegalAccessException: "+e.getMessage() ,e);
		} catch (InvocationTargetException e) {
			NIOServer.log(Level.WARNING,"InvocationTargetException: "+e.getMessage() ,e);
		} catch (SecurityException e) {
			NIOServer.log(Level.WARNING,"SecurityException: "+e.getMessage() ,e);
		} catch (NoSuchMethodException e) {
			NIOServer.log(Level.WARNING,"NoSuchMethodException: "+e.getMessage() ,e);
		}
		return null;
	}
	
}
