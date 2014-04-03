package strategy;

import model.*;

import java.util.*;

public class TotalOrderMulticast{
	private static TotalOrderMulticast _instance = new TotalOrderMulticast();
	private Map<Integer, PriorityQueue<TotalOrderMulticastMessage>> holdbackQueueTable;
	private Map<Integer, Integer> groupLastSequence;
	private Map<Integer, Map<Integer, List<Integer>>> groupProposalSequence;
	
	private Map<Integer, Integer> groupMessageCounter;
	private Map<Integer, Map<Integer, TotalOrderMulticastMessage >> bufferMessageTable;
	private BasicMulticast basicMulticast;

	private TotalOrderMulticast(){
		holdbackQueueTable = new Hashtable<Integer, PriorityQueue<TotalOrderMulticastMessage>>();
		groupLastSequence = new Hashtable<Integer, Integer>();
		basicMulticast = BasicMulticast.getInstance();
		bufferMessageTable = new Hashtable<Integer, Map<Integer, TotalOrderMulticastMessage>>();
		groupProposalSequence = new Hashtable<Integer, Map<Integer, List<Integer>>>();
		groupMessageCounter = new Hashtable<Integer, Integer>();
	}
	
	
	public static TotalOrderMulticast getInstance(){
		return _instance;
	}
	
	public void send(int groupId, String groupMessage) {
		TotalOrderMulticastMessage tomm;
		int selfId = Profile.getInstance().getId();
		int messageId; 
		Map<Integer, TotalOrderMulticastMessage > cachedMessage;
		
		tomm = new TotalOrderMulticastMessage();
		tomm.setContent(groupMessage);
		tomm.setSource(selfId);
		tomm.setGroupId(groupId);
		tomm.setMessageType(TotalOrderMessageType.INITIAL);
		tomm.setSequence(-1);
		if(groupMessageCounter.containsKey(groupId)==false){
			groupMessageCounter.put(groupId, 0);
		}
		
		if(bufferMessageTable.containsKey(groupId) == false){
			bufferMessageTable.put(groupId, new Hashtable<Integer, TotalOrderMulticastMessage>());
		}
		messageId = groupMessageCounter.get(groupId);
		cachedMessage = bufferMessageTable.get(groupId);
		tomm.setMessageId(messageId);
		cachedMessage.put(messageId, tomm);
		
		groupMessageCounter.put(groupId, messageId + 1);
		basicMulticast.send(groupId, tomm);
	}

	public void delivery(IMessage message) {
		TotalOrderMulticastMessage tomm = (TotalOrderMulticastMessage)message;
		int groupId;
		int sequence;
		int selfId;
		PriorityQueue<TotalOrderMulticastMessage> priorityQueue;
		TotalOrderMessageType messageType;
		
		
		groupId = tomm.getGroupId();
		selfId = Profile.getInstance().getId();
		
		if(holdbackQueueTable.containsKey(groupId) == false){
			holdbackQueueTable.put(groupId, new PriorityQueue<TotalOrderMulticastMessage>());
		}
		if(groupLastSequence.containsKey(groupId) == false){
			groupLastSequence.put(groupId, 0);
		}
		
	
		priorityQueue = holdbackQueueTable.get(groupId);
		messageType = tomm.getMessageType();
		sequence = groupLastSequence.get(groupId);
		
		if(messageType == TotalOrderMessageType.INITIAL){
			TotalOrderMulticastMessage reply;
			sequence += 1;
			groupLastSequence.put(groupId, sequence);
			
			reply = new TotalOrderMulticastMessage();
			reply.setSource(selfId);
			reply.setGroupId(groupId);
			reply.setMessageType(TotalOrderMessageType.PROPOSAL);
			reply.setSequence(sequence);
			reply.setMessageId(tomm.getMessageId());
			//System.out.println("reply message" + reply);
			basicMulticast.reply(groupId, tomm.getSource(), reply);
			
			tomm.setSequence(sequence);
			priorityQueue.add(tomm);
		}else if(messageType == TotalOrderMessageType.PROPOSAL){
			List<Integer> cachedSequence;
			Map<Integer, List<Integer>> cachedSequenceTable;
			int messageId = tomm.getMessageId();
			int proposeSequence = tomm.getSequence();
			if(groupProposalSequence.containsKey(groupId) == false){
				groupProposalSequence.put(groupId, new Hashtable<Integer, List<Integer>>());
			}
			cachedSequenceTable = groupProposalSequence.get(groupId);
			if(cachedSequenceTable.containsKey(messageId) == false){
				cachedSequenceTable.put(messageId, new LinkedList<Integer>());
			}
			
			cachedSequence = cachedSequenceTable.get(messageId);
			cachedSequence.add(proposeSequence);
			//System.out.println("receive proposed message: " + tomm);
			if(cachedSequence.size() == MemberIndexer.getInstance().getGroupSize(groupId)){
				int finalSequence = 0;
				finalSequence = sequence > Collections.max(cachedSequence)?sequence:Collections.max(cachedSequence);
				TotalOrderMulticastMessage finalMessage = bufferMessageTable.get(groupId).get(messageId);
				finalMessage.setSequence(finalSequence);
				finalMessage.setMessageType(TotalOrderMessageType.FINAL);
				basicMulticast.send(groupId, finalMessage);
				bufferMessageTable.get(groupId).remove(finalMessage);
				groupLastSequence.put(groupId, finalSequence);
			}
			
			
		}else if(messageType == TotalOrderMessageType.FINAL){
			//System.out.println("receive final message: " + tomm);
			
			for(TotalOrderMulticastMessage entry: priorityQueue){
				//System.out.println("entry mid = "+entry.getMessageId() + " v.s. tomm mid=" + tomm.getMessageId());
				if(entry.getMessageId().equals(tomm.getMessageId())){
					//System.out.println("finalize message "+entry.getMessageId());
					entry.setMessageType(TotalOrderMessageType.FINAL);
					entry.setSequence(tomm.getSequence());
				}
			}
			if(sequence < tomm.getSequence()){
				sequence = tomm.getSequence();
				groupLastSequence.put(groupId, sequence);
			}
			//System.out.println("priority queue size:"+priorityQueue.size());
			System.out.println("start check=====");
			Iterator<TotalOrderMulticastMessage> iterator = priorityQueue.iterator();
			while(iterator.hasNext()){
				System.out.println(iterator.next());
			}
			System.out.println("end check=====");
			while(priorityQueue.isEmpty() == false){
				TotalOrderMulticastMessage entry = priorityQueue.peek();
				System.out.println(entry+" is "+entry.isDeliverable());
				if(entry == null || entry.isDeliverable() == false){
					break;
				}else if (entry.isDeliverable()){
					System.out.println("deliver total order message: " + entry.getContent());
					priorityQueue.poll();
				}
			}
		}
	}
}
