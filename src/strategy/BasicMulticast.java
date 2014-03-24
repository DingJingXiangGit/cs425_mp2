package strategy;

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import model.ReliableMulticastMessage;
import model.Mediator;
import model.Member;

public class ReliableMulticast implements Multicast{
	private Map<Integer, Integer> _sourceGroupSequence;
	
	public ReliableMulticast(){
		_sourceGroupSequence = new Hashtable<Integer, Integer>();
	}
	
	@Override
	public void send(int groupId, String content) {
		ReliableMulticastMessage message = new ReliableMulticastMessage();
		message._content = content;
		message._groupId = groupId;
		message._sourceGroupSequence = _sourceGroupSequence.get(groupId);
		_sourceGroupSequence.put(groupId, message._sourceGroupSequence + 1);
		multicast(groupId, message.toString());
	}
	
	private void multicast(int groupId, String data ){
		Mediator mediator = Mediator.getInstance();
		Map<Integer, Member> groupMembers = mediator.getGroupMembers(groupId);
		for(Entry<Integer, Member> entry: groupMembers.entrySet()){
			entry.getValue().send(data);
		}
	}

	@Override
	public void delivery(String message) {
	}

}
