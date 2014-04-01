package strategy;

import model.Member;
import model.Message;
import model.Profile;
import model.RetransmissionTask;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

public class ReliableUnicastSender {
	private static int EXPIRE_TIME = 2000;
	private static ReliableUnicastSender _instance = new ReliableUnicastSender();
	
	private DatagramSocket _socket;
	private Map<Integer, Hashtable<Integer, Timer>> _timerTable;
	private Map<Integer, Hashtable<Integer, Message>> _cachedMessages;
	private Map<Integer, Hashtable<Integer, TimerTask>> _cachedRetransmissionTask;
	private Map<Integer, Integer> _nextSendSequence;
	private Profile _profile;
	private Random _rand;
	private Object _mutex;
	
	public static ReliableUnicastSender getInstance(){
		return _instance;
	}
	
	private ReliableUnicastSender(){
		_mutex = new Object();
		_cachedMessages = new Hashtable<Integer, Hashtable<Integer, Message>> ();
		_timerTable =  new Hashtable<Integer, Hashtable<Integer, Timer>> ();
		_nextSendSequence = new Hashtable<Integer, Integer>();
		_cachedRetransmissionTask = new Hashtable<Integer, Hashtable<Integer, TimerTask>>();
		_profile = Profile.getInstance();
		_rand = new Random();
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
		synchronized(_mutex){
			if(_nextSendSequence.containsKey(memberId) == false){
				_nextSendSequence.put(memberId, 0);
			}
			if(_timerTable.containsKey(memberId) == false){
				_timerTable.put(memberId, new Hashtable<Integer, Timer>());
			}
			
			if(_cachedMessages.containsKey(memberId) == false){
				_cachedMessages.put(memberId, new Hashtable<Integer, Message>());
			}
			
			if(_cachedRetransmissionTask.containsKey(memberId) == false){
				_cachedRetransmissionTask.put(memberId, new Hashtable<Integer, TimerTask>());
			}
			
			sendSequence = _nextSendSequence.get(memberId);
			try {
				
				message.setSequence(new Integer(sendSequence));

                System.out.println("send message content to : " +member.getId()+" with : "+ message);

                data =  message.getBytes();
				address = InetAddress.getByName(member._ip);
				sendPacket = new DatagramPacket(data, data.length, address,  member._port);
				timer = new Timer();
				timerTask = new RetransmissionTask(new Integer(sendSequence), member, this);
				_cachedMessages.get(memberId).put(new Integer(sendSequence), message);
				_timerTable.get(memberId).put(new Integer(sendSequence), timer);
				_cachedRetransmissionTask.get(memberId).put(new Integer(sendSequence), timerTask);
				
				if (_rand.nextDouble() >= _profile.getDropRate()) {
                    int meanDelay = _profile.getDelay();
                    int variance = meanDelay / 2;
                    double randomizedDelay = meanDelay + _rand.nextGaussian() * variance;
                    randomizedDelay = Math.max(randomizedDelay, 0.1d);
                    System.out.println("delaying sending message for " + randomizedDelay);

                    Thread.sleep((long)randomizedDelay * 1000l);
					_socket.send(sendPacket);
				}else{
					System.out.println("message dropped");
				}
				timer.schedule(timerTask, EXPIRE_TIME);
				_nextSendSequence.put(memberId, sendSequence + 1);
			}
            catch (IOException e) {
				e.printStackTrace();
			}
            catch (InterruptedException e) {
                e.printStackTrace();
            }
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
		sequence = task.getSequence();
		System.out.println("start resend = target "+member.getId()+" sequence "+sequence);

		synchronized(_mutex){
			message = _cachedMessages.get(member.getId()).get(sequence);
			timer = _timerTable.get(member.getId()).get(sequence);
            if (timer == null) {
                System.out.println("null timer");
                return;
            }
//			System.out.println("out message: "+message.getSequence() +" source "+message.getId());
			task.cancel();
			timer.cancel();
			timer.purge();
			message.getId();
			//System.out.println(message.toString());
			timer = new Timer();
			timerTask = new RetransmissionTask(sequence, member, this);
			data =  message.getBytes();
			try {
				address = InetAddress.getByName(member.getIP());
				sendPacket = new DatagramPacket(data, data.length, address,  member.getPort());
				_timerTable.get(member.getId()).remove(sequence);
				_timerTable.get(member.getId()).put(sequence, timer);
				_cachedRetransmissionTask.get(member.getId()).put(sequence, timerTask);
				_socket.send(sendPacket);
				timer.schedule(timerTask, EXPIRE_TIME);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void ack(Message message) {
		Timer timer;
		TimerTask task;
		int memberId = message.getId();
		int sequence = message.getSequence();
		// System.out.println("[start]ack sequence target " + memberId + " sequence" + sequence);
		synchronized(_mutex){
			timer = _timerTable.get(memberId).get(sequence);
			task = _cachedRetransmissionTask.get(memberId).get(sequence);
			if(timer != null){
				task.cancel();
				timer.cancel();
				timer.purge();
				_timerTable.get(memberId).remove(sequence);
				_cachedMessages.get(memberId).remove(sequence);
				_cachedRetransmissionTask.get(memberId).remove(sequence);
			}else{
				System.out.println("null timer.");
			}
		}
		//System.out.println("[end]ack sequence "+sequence);

	}

	public void sendAck(Message msg, Member member) throws IOException {
		InetAddress address;
		Message message;
		byte[] data;
		DatagramPacket sendPacket;
		message = new Message(msg);
		message.setId(Profile.getInstance().id);
		message.setAction("ack");
		message.setSequence(msg.getSequence());
		data =  message.getBytes();
		address = InetAddress.getByName(member._ip);
		sendPacket = new DatagramPacket(data, data.length, address,  member._port);
		//System.out.println("send ack from "+message.getId() +" sequence "+ message.getSequence());
		_socket.send(sendPacket);
	}
}

