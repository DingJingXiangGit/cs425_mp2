import model.*;
import strategy.CausalOrderMulticast;
import strategy.ReliableUnicastReceiver;
import strategy.TotalOrderMulticastWithSequencer;

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Chat {
	private ReliableUnicastReceiver _receiver;

	//private ReliableUnicastSender _sender;
	
	public Chat(int delayTime, double dropRate, String file, int id, String orderType){
		List<Member> members = new ArrayList<Member>();
		Profile profile = Profile.getInstance();
		MemberIndexer memberIndexer = MemberIndexer.getInstance();
		try {
			Scanner scanner = new Scanner(new File(file));
			while(scanner.hasNext()){
				String line = scanner.nextLine();
				String[] parts = line.split(" ");
				System.out.println(parts[0] + parts[1] +parts[2] + parts[3]);
				Member member = new Member();
				member._id =  Integer.parseInt(parts[0]);
				member._ip =  parts[1];
				member._port = Integer.parseInt(parts[2]);
				member._userName = parts[3];
				member._groupId = Integer.parseInt(parts[4]);

                if (member._id == TotalOrderSequencer._id && !orderType.equals("TotalOrder")) {
                    continue;
                }

				members.add(member);
				if(Integer.parseInt(parts[0]) == id){
					profile.id = id;
					profile.ip = parts[1];
					profile.port = Integer.parseInt(parts[2]);
					profile.name = parts[3];
					profile.delay = delayTime;
					profile.dropRate = dropRate;
					if(orderType.equals("TotalOrder")){
						profile.setMulticastType(MulticastType.TotalOrder);
					}else if(orderType.equals("CausalOrder")){
						profile.setMulticastType(MulticastType.CausalOrder);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		memberIndexer.addMembers(members);
		this._receiver = ReliableUnicastReceiver.getInstance(); 
		this._receiver.init(profile.ip, profile.port);
	}
	
	public void waitUserMessage(){
		Console console = System.console();
//        Iterator it = MemberIndexer.getInstance().getAllMembers().entrySet().iterator();
//        int myId = Profile.getInstance().getId();
//        while (it.hasNext()) {
//            Map.Entry pairs = (Map.Entry)it.next();
//            System.out.println(String.format("broadcasting to %d", pairs.getKey()));
//            multicastMessage(myId + " broadcast " + pairs.getKey());
//        }
		
		while (true) {
			System.out.print("type your message: ");
			String command = console.readLine();
			if(command.equals("exit")){
				break;
			}
			if(command.trim().length() > 0){
				multicastMessage(command);
			}
		}
		System.out.println("Bye.");
	}
	
	public void startListen(){
		(new Thread(this._receiver)).start();
	}
	
	private void multicastMessage(String content){
		//BasicMulticast reliableMulticast = BasicMulticast.getInstance();
		//reliableMulticast.send(1, content);
		
		if(Profile.getInstance().getMulticastType() == MulticastType.CausalOrder){
			CausalOrderMulticast causalOrderMulticast = CausalOrderMulticast.getInstance();
			causalOrderMulticast.send(1, content);
		}else if(Profile.getInstance().getMulticastType() == MulticastType.TotalOrder){
			TotalOrderMulticastWithSequencer totalOrderMulticast = TotalOrderMulticastWithSequencer.getInstance();
			totalOrderMulticast.send(1, content);
		}
	}
	
	public static void main(String[] args){
		if(args.length != 5){
			System.out.println("java Chat configFile delayTime dropRate selfId order");
			return;
		}
		Chat chat = new Chat(
				Integer.parseInt(args[1]),
				Double.parseDouble(args[2]),
				args[0],
				Integer.parseInt(args[3]),
				args[4]);
		chat.startListen();
		chat.waitUserMessage();
	}
}
