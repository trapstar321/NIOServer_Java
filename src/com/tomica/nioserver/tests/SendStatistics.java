package com.tomica.nioserver.tests;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class SendStatistics{
		private long minWait=0;
		private long maxWait=0;
		
		private long minRead=0;
		private long maxRead=0;
		
		private long minWrite=0;
		private long maxWrite=0;
		
		private long minSelect=0;
		private long maxSelect=0;
		
		private long minRun=0;
		private long maxRun=0;
		
		private List<Long> allWaits = new ArrayList<Long>();
		private List<Long> allReads = new ArrayList<Long>();
		private List<Long> allWrites = new ArrayList<Long>();
		private List<Long> allSelects = new ArrayList<Long>();
		private List<Long> allRuns = new ArrayList<Long>();
		
		private String topTime(List<Long> results, String name){
			Map<Long, Integer> stats = new HashMap<Long, Integer>();
			
			for(Long time: results){
				Integer occurrence = stats.get(time);
				if(occurrence==null)
					stats.put(time, 1);
				else
					stats.put(time, occurrence+1);
			}
			
			long maxTime=0;
			int maxOccurrence=0;			
			for(Long time: stats.keySet()){
				Integer occurrence = stats.get(time);
				if(occurrence>maxOccurrence){
					maxOccurrence=occurrence;
					maxTime=time;
				}
			}
			return maxTime+", n="+maxOccurrence+"/"+results.size();
		}
		
		private String waitStats(List<Long> results, String name){
			Map<Long, Integer> stats = new HashMap<Long, Integer>();
			
			for(Long time: results){
				Integer occurrence = stats.get(time);
				if(occurrence==null)
					stats.put(time, 1);
				else
					stats.put(time, occurrence+1);
			}
			
			StringBuilder b = new StringBuilder();
			for(Long time: stats.keySet()){
				Integer occurrence = stats.get(time);
				b.append("\n\ttime="+time+", occurrences="+occurrence+"/"+results.size());
			}
			return b.toString();
		}
		
		private int dispatcherID;
		
		public SendStatistics(int dispatcherID){
			this.dispatcherID=dispatcherID;
		}
		
		private boolean communicationFinished;
		
		public long getMinWait(){
			return minWait;			
		}
		
		public long getMaxWait(){
			return maxWait;
		}
		
		public boolean communicationFinished(){
			return communicationFinished;
		}
		
		public void reportLastWait(long wait){
			allWaits.add(wait);
			if(minWait>wait || minWait==0)
				minWait=wait;
			if(maxWait<wait)
				maxWait=wait;
		}
		
		public void reportLastRead(long time){
			allReads.add(time);
			if(minRead>time || minRead==0)
				minRead=time;
			if(maxRead<time)
				maxRead=time;
		}
		
		public void reportLastWrite(long time){
			allWrites.add(time);
			if(minWrite>time || minWrite==0)
				minWrite=time;
			if(maxWrite<time)
				maxWrite=time;
		}
		
		public void reportLastSelect(long time){
			allSelects.add(time);
			if(minSelect>time || minSelect==0)
				minSelect=time;
			if(maxSelect<time)
				maxSelect=time;
		}
		
		public void reportLastRun(long time){
			allRuns.add(time);
			if(minRun>time || minRun==0)
				minRun=time;
			if(maxRun<time)
				maxRun=time;
		}		
	
		@Override
		public String toString(){
			return "DispatcherID: "+dispatcherID+"\nminWait="+minWait+", maxWait="+maxWait+", topWait="+topTime(allWaits, "Waits")+","+waitStats(allWaits, "Waits")+"\n"+
					"minRead="+minRead+", maxRead="+maxRead+", topRead="+topTime(allReads, "Reads")+", "+waitStats(allReads, "Reads")+"\n"+
					"minWrite="+minWrite+", maxWrite="+maxWrite+", topWrite="+topTime(allWrites, "Writes")+", "+waitStats(allWrites, "Writes")+"\n"+
					"minSelect="+minSelect+", maxSelect="+maxSelect+", topSelect="+topTime(allSelects, "Selects")+", "+waitStats(allSelects, "Selects")+"\n"+
					"minRun="+minRun+", maxRun="+maxRun+", topRun="+topTime(allRuns, "Runs")+", "+waitStats(allRuns, "Runs");
		}
		
	}