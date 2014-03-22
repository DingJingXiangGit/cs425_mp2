

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.List;

import strategy.Multicast;
import strategy.ReliableMulticast;
import model.Mediator;
import model.Member;
import model.Profile;
import model.ReliableUnicastReceiver;

public class Chat {
	private ReliableUnicastReceiver _receiver;
	private  Multicast multicast;
	public Chat(int delayTime, double dropRate, String file, int id){
		List<Member> members = new ArrayList<Member>();
		Profile profile = Profile.getInstance();
		Mediator mediator = Mediator.getInstance();
		try {
			Scanner scanner = new Scanner(new File(file));
			while(scanner.hasNext()){
				String line = scanner.nextLine();
				String[] parts = line.split(" ");
				System.out.println(parts[0] + parts[1]+parts[2]+parts[3]);
				Member member = new Member();
				member._id =  Integer.parseInt(parts[0]);
				member._ip =  parts[1];
				member._port = Integer.parseInt(parts[2]);
				member._userName = parts[3];
				members.add(member);
				if(Integer.parseInt(parts[0]) == id){
					profile.id = id;
					profile.ip = parts[1];
					profile.port = Integer.parseInt(parts[2]);
					profile.name = parts[3];
					profile.delay = delayTime;
					profile.dropRate = dropRate;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		mediator.addMembers(members);
		this._receiver = new ReliableUnicastReceiver(profile.ip, profile.port);
		multicast = new ReliableMulticast();
	}
	
	public void waitUserMessage(){
		Console console = System.console();
		
		while(true){
			System.out.print("type your message:");
			String command = console.readLine();
			if(command.equals("exit")){
				break;
			}
			multicastMessage(command);
		}
		System.out.println("Bye.");
	}
	
	public void startListen(){
		(new Thread(this._receiver)).start();
	}
	
	private void multicastMessage(String content){
		multicast.send(1, content);
	}
	
	public static void main(String[] args){
		if(args.length != 4){
			System.out.println("java Char configFile delayTime dropRate selfId");
			return;
		}
		Chat chat = new Chat(Integer.parseInt(args[1]), Double.parseDouble(args[2]), args[0], Integer.parseInt(args[3]));
		chat.startListen();
		chat.waitUserMessage();
	}
}
