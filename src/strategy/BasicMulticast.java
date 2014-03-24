package strategy;

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import model.MemberIndexer;
import model.Message;
import model.BasicMulticastMessage;
import model.Member;
import model.Profile;

public class BasicMulticast implements Multicast{
	private static BasicMulticast _instance = new BasicMulticast();
	
	private Map<Integer, Integer> _sourceGroupSequence;
	private ReliableUnicastSender _sender;
	private BasicMulticast(){
		_sourceGroupSequence = new Hashtable<Integer, Integer>();
		_sender = ReliableUnicastSender.getInstance();
	}
	public static BasicMulticast getInstance(){
		return _instance;
	}
	
	
	@Override
	public void send(int groupId, String content) {
		BasicMulticastMessage message = new BasicMulticastMessage();
		message.content = content + " ";
		message.groupId = groupId;
		if(_sourceGroupSequence.containsKey(groupId) == false){
			_sourceGroupSequence.put(groupId, 0);
		}
		message.sourceGroupSequence = _sourceGroupSequence.get(groupId);
		_sourceGroupSequence.put(groupId, message.sourceGroupSequence + 1);
		multicast(groupId, message.toString());
	}
	
	private void multicast(int groupId, String data ){
		MemberIndexer memberIndexer = MemberIndexer.getInstance();
		Map<Integer, Member> groupMembers = memberIndexer.getByGroupId(groupId);
		Profile profile = Profile.getInstance();
		Message message = new Message();
		message.setAction("delivery");
		message.setContent(data);
		message.setId(profile.getId());

		for(Entry<Integer, Member> entry: groupMembers.entrySet()){
			_sender.send(message, entry.getValue());
		}
	}

	@Override
	public void delivery(String message) {
		BasicMulticastMessage rmm = BasicMulticastMessage.parse(message);
		System.out.println(rmm.getContent());
	}
}
