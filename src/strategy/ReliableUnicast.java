package strategy;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import model.Mediator;
import model.Member;
import model.Message;
import model.Profile;
import model.RetransmissionTask;

public class ReliableUnicast implements Runnable{
	private DatagramSocket _inSocket;
	private static int BUFFER_SIZE = 1024;
	private DatagramSocket _outSocket;
	private Map<Integer, Timer> _timerTable;
	private Map<Integer, Message> _cachedMessages;
	private Object _mutex;
	private static int EXPIRE_TIME = 3000;
	
	public ReliableUnicast(String ip, int port) {
		InetAddress address;
		try {
			address = InetAddress.getByName(ip);
			_inSocket = new DatagramSocket(port, address);
			_outSocket = new DatagramSocket();
			_mutex = new Object();
			_cachedMessages = new Hashtable<Integer, Message> ();
			_timerTable =  new Hashtable<Integer, Timer> ();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void run(){
		Mediator mediator = Mediator.getInstance();
		while(true){
			byte[] buf = new byte[BUFFER_SIZE];
			String content;
			Message message;
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			try {
				_inSocket.receive(packet);
				content = new String(packet.getData());
				message = Message.parse(content);
				mediator.receive(message);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	/*
	public void send(Message message, Member memebr){
		try{
			byte[] data =  message.toString().getBytes();
			Timer timer;
			TimerTask timerTask;
			InetAddress address;
			address = InetAddress.getByName(memebr._ip);

			DatagramPacket sendPacket = new DatagramPacket(data, data.length, address,  memebr._port);
			timer = new Timer();
			timerTask = new RetransmissionTask(message._sequence, this);
			_cachedMessages.put(message._sequence, message);
			_outSocket.send(sendPacket);
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

	public void sendAck(Message msg, Member member) throws IOException {
		Message message = new Message(msg);
		message._id = Profile.getInstance().id;
		String content = message.getAckString();
		byte[] data =  content.getBytes();
		InetAddress address;
		address = InetAddress.getByName(member._ip);
		DatagramPacket sendPacket = new DatagramPacket(data, data.length, address,  member._port);
		_outSocket.send(sendPacket);
	}
	*/
}
