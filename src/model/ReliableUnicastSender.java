package model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ReliableUnicastSender {
	private Member _member;
	private DatagramSocket _socket;
	private Map<Integer, Timer> _timerTable;
	private Map<Integer, Message> _cachedMessages;
	private Object _mutex;
	private static int EXPIRE_TIME = 3000;
	
	public ReliableUnicastSender(Member member){
		_member = member;
		_mutex = new Object();
		_cachedMessages = new Hashtable<Integer, Message> ();
		_timerTable =  new Hashtable<Integer, Timer> ();
		try{
			_socket = new DatagramSocket();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void send(Message message){
		try{
			byte[] data =  message.toString().getBytes();
			Timer timer;
			TimerTask timerTask;
			InetAddress address;
			address = InetAddress.getByName(_member._ip);

			DatagramPacket sendPacket = new DatagramPacket(data, data.length, address,  _member._port);
			timer = new Timer();
			timerTask = new RetransmissionTask(message._sequence, this);
			_cachedMessages.put(message._sequence, message);
			_socket.send(sendPacket);
			_timerTable.put(message._sequence, timer);
			timer.schedule(timerTask, EXPIRE_TIME);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void resend(RetransmissionTask task) {
		synchronized(_mutex){
			int sequence = task.getSequence();
			Message message = _cachedMessages.get(sequence);
			Timer timer = _timerTable.get(sequence);
			_timerTable.remove(sequence);
			send(message);
			task.cancel();
			timer.cancel();
		}
	}
	
	public void ack(int sequence){
		synchronized(_mutex){
			Timer timer = _timerTable.get(sequence);
			if(timer != null){
				timer.cancel();
				_timerTable.remove(sequence);
				_cachedMessages.remove(sequence);
			}
		}
	}

	public void sendAck(Message msg) throws IOException {
		Message message = new Message(msg);
		message._id = Profile.getInstance().id;
		String content = message.getAckString();
		byte[] data =  content.getBytes();
		InetAddress address;
		address = InetAddress.getByName(_member._ip);
		DatagramPacket sendPacket = new DatagramPacket(data, data.length, address,  _member._port);
		_socket.send(sendPacket);
	}
}

