package com.tomica.nioserver.objects;

import java.util.Date;
import java.io.Serializable;
import java.text.SimpleDateFormat;

@SuppressWarnings("serial")
public class PlayerInfo implements Serializable{
	private int ID;
	private String playerName;
	private Date accountCreated;
	private double amountOfGold;
	
	public PlayerInfo(int ID, String playerName, Date accountCreated, double amountOfGold){
		this.ID=ID;
		this.playerName=playerName;
		this.accountCreated=accountCreated;
		this.amountOfGold=amountOfGold;
	}
	
	public int getID(){
		return ID;
	}
	
	public String getPlayerName(){
		return playerName;
	}
	
	public Date getAccountCreatedDate(){
		return accountCreated;
	}
	
	public double getAmountOfGold(){
		return amountOfGold;
	}
	
	@Override
	public String toString(){
		String formattedDate = new SimpleDateFormat("dd/MM/yyyy, Ka").format(accountCreated);
		return String.format("ID=%d, playerName=%s, accountCreatedDate=%s, amountOfGold=%.2f", ID, playerName, formattedDate, amountOfGold);
	}
}
