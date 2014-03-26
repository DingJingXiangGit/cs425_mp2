package strategy;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import model.Member;
import model.Message;
import model.Profile;
import model.RetransmissionTask;

public class ReliableUnicastSender {
	private static int EXPIRE_TIME = 10000;
	private static ReliableUnicastSender _instance = new ReliableUnicastSender();
	
	private DatagramSocket _socket;
	private Map<Integer, Hashtable<Integer, Timer>> _timerTable;
	private Map<Integer, Hashtable<Integer, Message>> _cachedMessages;
	private Map<Integer, Integer> _nextSendSequence;

	private Object _mutex;
	
	public static ReliableUnicastSender getInstance(){
		return _instance;
	}
	
	private ReliableUnicastSender(){
		_mutex = new Object();
		_cachedMessages = new Hashtable<Integer, Hashtable<Integer, Message>> ();
		_timerTable =  new Hashtable<Integer, Hashtable<Integer, Timer>> ();
		_nextSendSequence = new Hashtable<Integer, Integer>();
		try{
			_socket = new DatagramSocket();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void send(Message message, Member member){
		Timer timer;
		TimerTask timerTask;
		InetAddress address;
		byte[] data;
		int memberId;
		int sendSequence;
		DatagramPacket sendPacket;
		memberId = member.getId();
		
		if(_nextSendSequence.containsKey(memberId) == false){
			_nextSendSequence.put(memberId, 0);
		}
		if(_timerTable.containsKey(memberId) == false){
			_timerTable.put(memberId, new Hashtable<Integer, Timer>());
		}
		
		if(_cachedMessages.containsKey(memberId) == false){
			_cachedMessages.put(memberId, new Hashtable<Integer, Message>());
		}
		
		sendSequence = _nextSendSequence.get(memberId);
		try{
			_nextSendSequence.put(memberId, sendSequence + 1);
			message._sequence = sendSequence;
			System.out.println("send message content: "+message.toString());
			data =  message.getBytes();
			address = InetAddress.getByName(member._ip);
			sendPacket = new DatagramPacket(data, data.length, address,  member._port);
			timer = new Timer();
			timerTask = new RetransmissionTask(message._sequence, member, this);
			_cachedMessages.get(memberId).put(message._sequence, message);
			_timerTable.get(memberId).put(message._sequence, timer);
			_socket.send(sendPacket);
			timer.schedule(timerTask, EXPIRE_TIME);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void resend(RetransmissionTask task) {
		Timer timer;
		Message message;
		Member member;
		int sequence;
		byte[] data;
		DatagramPacket sendPacket;
		InetAddress address;
		TimerTask timerTask;
		
		member = task.getMember();
		synchronized(_mutex){
			sequence = task.getSequence();
			message = _cachedMessages.get(member.getId()).get(sequence);
			timer = _timerTable.get(member.getId()).get(sequence);
			task.cancel();
			timer.cancel();
			timer.purge();
			
			timer = new Timer();
			timerTask = new RetransmissionTask(message.getSequence(), member, this);
			data =  message.getBytes();
			try {
				address = InetAddress.getByName(member.getIP());
				sendPacket = new DatagramPacket(data, data.length, address,  member.getPort());
				_timerTable.get(member.getId()).remove(message.getSequence());
				_timerTable.get(member.getId()).put(message.getSequence(), timer);
				_socket.send(sendPacket);
				timer.schedule(timerTask, EXPIRE_TIME);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void ack(Message message){
		Timer timer;
		int memberId = message._id;
		int sequence = message._sequence;
		synchronized(_mutex){
			timer = _timerTable.get(memberId).get(sequence);
			if(timer != null){
				timer.cancel();
				_timerTable.get(memberId).remove(sequence);
				_cachedMessages.get(memberId).remove(sequence);
			}else{
				System.out.println("null timer.");
			}
		}
	}

	public void sendAck(Message msg, Member member) throws IOException {
		InetAddress address;
		Message message;
		byte[] data;
		DatagramPacket sendPacket;
		message = new Message(msg);
		message.setId(Profile.getInstance().id);
		message.setAction("ack");
		//System.out.println("send ack message content: "+message.toString());
		data =  message.getBytes();
		address = InetAddress.getByName(member._ip);
		sendPacket = new DatagramPacket(data, data.length, address,  member._port);
		_socket.send(sendPacket);
	}
}

