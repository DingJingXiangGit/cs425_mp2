package strategy;

import model.*;

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

public class BasicMulticast{
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
	
	public void send(int groupId, IMessage content) {
		BasicMulticastMessage message = new BasicMulticastMessage();
		message.content = content;
		message.groupId = groupId;
		if(_sourceGroupSequence.containsKey(groupId) == false){
			_sourceGroupSequence.put(groupId, 0);
		}
		message.sourceGroupSequence = _sourceGroupSequence.get(groupId);
		_sourceGroupSequence.put(groupId, message.sourceGroupSequence + 1);
		multicast(groupId, message);
	}
	
	public void reply(int groupId, int source, IMessage content){
		MemberIndexer memberIndexer;
		Map<Integer, Member> groupMembers;
		Profile profile;
		Message message;
		BasicMulticastMessage basicMessage;
		
		basicMessage = new BasicMulticastMessage();
		basicMessage.content = content;
		basicMessage.groupId = groupId;
		if(_sourceGroupSequence.containsKey(groupId) == false){
			_sourceGroupSequence.put(groupId, 0);
		}
		basicMessage.sourceGroupSequence = _sourceGroupSequence.get(groupId);
		
		memberIndexer = MemberIndexer.getInstance();
		groupMembers = memberIndexer.getByGroupId(groupId);
		profile = Profile.getInstance();
		message = new Message();
		message.setAction("delivery");
		message.setContent(basicMessage);
		message.setId(profile.getId());
		
		_sender.send(message, groupMembers.get(source));
	}
	
	private void multicast(int groupId, BasicMulticastMessage data ){
		MemberIndexer memberIndexer = MemberIndexer.getInstance();
		Map<Integer, Member> groupMembers = memberIndexer.getByGroupId(groupId);
		Profile profile = Profile.getInstance();
        Random _rand = new Random();

        // Delay based on input argument
        int meanDelay = profile.getDelay();
        if (meanDelay != 0) {
            int variance = meanDelay / 2;
            double randomizedDelay = meanDelay + _rand.nextGaussian() * variance;
            randomizedDelay = Math.max(randomizedDelay, 0.001d);
            //System.out.println("multicast: delay -" + randomizedDelay);

            try {
                Thread.sleep((long)randomizedDelay * 1000l);
            }
            catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        for(Entry<Integer, Member> entry: groupMembers.entrySet()){
			Message message = new Message();
			message.setAction("delivery");
			message.setContent(data);
			message.setId(profile.getId());
			_sender.send(message, entry.getValue());
		}
	}

	public void delivery(BasicMulticastMessage message) {
        if (Profile.getInstance().id == TotalOrderSequencer._id) {
            TotalOrderSequencer tos = TotalOrderSequencer.getInstance();
            tos.delivery(message.getContent());
        } else if (Profile.getInstance().getMulticastType() == MulticastType.CausalOrder){
			CausalOrderMulticast com = CausalOrderMulticast.getInstance();
			com.delivery(message.getContent());
		} else if (Profile.getInstance().getMulticastType() == MulticastType.TotalOrder){
			TotalOrderMulticastWithSequencer tom = TotalOrderMulticastWithSequencer.getInstance();
			tom.delivery(message.getContent());
		}
		
	}
}
