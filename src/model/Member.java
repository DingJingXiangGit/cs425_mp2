package model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Member {
	public String _ip;
	public int _id;
	public int _port;
	public String _userName;
	private Map<Integer, Message> _messageBuffer;
	private int _nextReceiveSequence;
	private int _nextSendSequence;
	private ReliableUnicastSender _unicastSender;
	private Object _mutex = new Object();
	
	public Member(){
		_nextReceiveSequence = 0;
		_nextSendSequence = 0;
		_messageBuffer = new HashMap<Integer, Message>();
		_unicastSender = new ReliableUnicastSender(this);
	}
	
	public void send(String content){
		Profile profile = Profile.getInstance();
		Message message = new Message();
		message._content = content;
		message._action = "delivery";
		message._id = profile.id;
		message._sequence = _nextSendSequence;
		_unicastSender.send(message);
		_nextSendSequence++;
	}
	
	public void receive(Message msg) throws IOException{
		synchronized(_mutex){
			if(msg._action.equals("delivery")){
				if(msg._sequence >= _nextReceiveSequence){
					//avoid duplicated message
					_unicastSender.sendAck(msg);
					if(msg._sequence == _nextReceiveSequence){
						Mediator mediator = Mediator.getInstance();
						mediator.publish(msg);
						++_nextReceiveSequence;
						while(_messageBuffer.containsKey(_nextReceiveSequence)){
							mediator.publish(_messageBuffer.get(_nextReceiveSequence));
							++_nextReceiveSequence;
						}
					}else{
						if(_messageBuffer.containsKey(msg._sequence) == false){
							_messageBuffer.put(msg._sequence, msg);
						}
					}
				}
			}else{
				//receive ack message, cancel retransmission
				_unicastSender.ack(msg._sequence);
			}
		}
	}
}
