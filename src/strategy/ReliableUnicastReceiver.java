package model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ReliableUnicastReceiver implements Runnable {
	private DatagramSocket _socket;
	private static int BUFFER_SIZE = 1024;
	
	public ReliableUnicastReceiver(String ip, int port) {
		InetAddress address;
		try {
			address = InetAddress.getByName(ip);
			_socket = new DatagramSocket(port, address);
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
				_socket.receive(packet);
				content = new String(packet.getData());
				message = Message.parse(content);
				mediator.receive(message);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
