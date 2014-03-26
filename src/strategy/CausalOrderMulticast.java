package strategy;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import model.CausalOrderMulticastMessage;
import model.IMessage;
import model.MemberIndexer;
import model.Profile;

public class CausalOrderMulticast{
	private Map<Integer, Integer[]> groupTimeVector;
	private static CausalOrderMulticast _instance = new CausalOrderMulticast();
	private BasicMulticast _basicMulticast;
	private Map<Integer, List<CausalOrderMulticastMessage>> holdbackQueueTable;

	private CausalOrderMulticast(){
		groupTimeVector = new Hashtable<Integer, Integer[]>();
		holdbackQueueTable = new Hashtable<Integer, List<CausalOrderMulticastMessage>>();
		_basicMulticast = BasicMulticast.getInstance();
	}
	
	public static CausalOrderMulticast getInstance(){
		return _instance;
	}
	
	public void send(int groupId, String groupMessage) {
		Integer[] timeVector;
		CausalOrderMulticastMessage comm;
		int selfId = Profile.getInstance().getId();
		if(groupTimeVector.containsKey(groupId)==false){
			MemberIndexer memberIndexer = MemberIndexer.getInstance();
			int size = memberIndexer.getGroupSize(groupId);
			timeVector = new Integer[size];
			for(int i = 0; i < size; ++i){
				timeVector[i] = 0;
			}
		}else{
			timeVector = groupTimeVector.get(groupId);
		}
		timeVector[selfId] += 1;
		comm = new CausalOrderMulticastMessage();
		comm.setContent(groupMessage);
		comm.setTimeVector(timeVector.clone());
		comm.setSource(selfId);
		comm.setGroupId(groupId);
		_basicMulticast.send(groupId, comm);	
		groupTimeVector.put(groupId, timeVector);
	}

	public void delivery(IMessage message) {
		CausalOrderMulticastMessage comm = (CausalOrderMulticastMessage)message;
		List<CausalOrderMulticastMessage> holdbackQueue;
		List<CausalOrderMulticastMessage> deleteQueue;
		Integer[] timeVector;
		System.out.println(comm);
		System.out.println(message);
		System.out.println("table size = "+holdbackQueueTable.size());
		int groupId = comm.getGroupId();
		
		if(holdbackQueueTable.containsKey(groupId) == false){
			holdbackQueueTable.put(groupId, new LinkedList<CausalOrderMulticastMessage>());
		}
		if(groupTimeVector.containsKey(groupId) == false){
			MemberIndexer memberIndexer = MemberIndexer.getInstance();
			int size = memberIndexer.getGroupSize(groupId);
			timeVector = new Integer[size];
			for(int i = 0; i < size; ++i){
				timeVector[i] = 0;
			}
			groupTimeVector.put(groupId, timeVector);
		}
		
		deleteQueue = new LinkedList<CausalOrderMulticastMessage>();
		timeVector = groupTimeVector.get(comm.getGroupId());
		holdbackQueue = holdbackQueueTable.get(comm.getGroupId());
		holdbackQueue.add(comm);
		for(CausalOrderMulticastMessage msg : holdbackQueue){
			Integer[] msgTimeVector = msg.getTimeVector();
			int sourceId = comm.getSource();
			boolean isReady = true;
			if(msgTimeVector[sourceId] == timeVector[sourceId] + 1){
				for(int i = 0; i < msgTimeVector.length; ++i){
					if(i != sourceId && msgTimeVector[i] > timeVector[i]){
						isReady = false;
						break;
					}
				}
			}else{
				isReady = false;
			}
			if(isReady){
				deleteQueue.add(msg);
				timeVector[sourceId] += 1;
				System.out.println(comm.getContent());
			}
		}
		
		holdbackQueue.removeAll(deleteQueue);
		groupTimeVector.put(comm.getGroupId(), timeVector);
	}
}
