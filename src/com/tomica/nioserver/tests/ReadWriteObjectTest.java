package com.tomica.nioserver.tests;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import com.tomica.nioserver.messages.Message;
import com.tomica.nioserver.messages.impl.CM_PLAYERINFO;
import com.tomica.nioserver.messages.impl.SM_PLAYERINFO;
import com.tomica.nioserver.objects.PlayerInfo;

public class ReadWriteObjectTest {	
	private PlayerInfo info;
	private byte[] objectData;
	
	
	@Before
	public void setUp() throws Exception {
		info = new PlayerInfo(22, "trapstar321", new Date(), (double)1000000000);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(info);
		objectData=bos.toByteArray();
	}

	@Test
	public void testWrite() {
		SM_PLAYERINFO serverMessage=null;
		try {
			serverMessage = new SM_PLAYERINFO(info, true);
		} catch (IOException e) {
			fail("Exception thrown when writting to byte buffer");
		}
		
		byte[] written = serverMessage.getBytes();		
		
		byte[] slice = Arrays.copyOfRange(written, 0, Message.INT_BYTES);		
		ByteBuffer bb = ByteBuffer.wrap(slice);		
		int length = bb.getInt();
		assertEquals(length, objectData.length);
		
		written = Arrays.copyOfRange(written, Message.INT_BYTES, length+Message.INT_BYTES);
		assertArrayEquals(objectData, written);
	}
	
	@Test
	public void testRead(){
		byte[] data=null;;
		try{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(info);
			data = bos.toByteArray();
		}catch(IOException ex){
			fail("Exception thrown when reading from byte buffer");
		}
		
		ByteBuffer buff = ByteBuffer.allocate(data.length+Message.BYTE_BYTES+Message.INT_BYTES);
		buff.putInt(data.length);
		buff.put(data);
		buff.put((byte)1);
		
		
		try {
			CM_PLAYERINFO clientMessage=new CM_PLAYERINFO(buff.array());
			assertEquals(info.getAccountCreatedDate(), clientMessage.getPlayerInfo().getAccountCreatedDate());			
			assertEquals(info.getAmountOfGold(), clientMessage.getPlayerInfo().getAmountOfGold(), 0);
			assertEquals(info.getID(), clientMessage.getPlayerInfo().getID());
			assertEquals(info.getPlayerName(), clientMessage.getPlayerInfo().getPlayerName());
			assertEquals(clientMessage.isOnline(), true);
		} catch (Exception e) {
			fail("Exception thrown when reading from byte buffer");
		} 
	}

}
