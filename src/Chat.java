import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import model.Member;
import model.MemberIndexer;
import model.MulticastType;
import model.Profile;
import model.TotalOrderSequencer;
import strategy.CausalOrderMulticast;
import strategy.ReliableUnicastReceiver;
import strategy.TotalOrderMulticast;

public class Chat {
	private ReliableUnicastReceiver _receiver;

    /*
	 *
     */
	public Chat(int delayTime, double dropRate, String file, int id, String orderType, String mode, String boost){
		List<Member> members = new ArrayList<Member>();
		Profile profile = Profile.getInstance();
		MemberIndexer memberIndexer = MemberIndexer.getInstance();
		try {
			Scanner scanner = new Scanner(new File(file));
			while(scanner.hasNext()){
				String line = scanner.nextLine();
				String[] parts = line.split(" ");
				Member member = new Member();
				member._id =  Integer.parseInt(parts[0]);
				member._ip =  parts[1];
				member._port = Integer.parseInt(parts[2]);
				member._userName = parts[3];
				member._groupId = Integer.parseInt(parts[4]);

                // Ignore the sequencer if it's not Total Order configuration
                if (member._id == TotalOrderSequencer._id && !orderType.toUpperCase().equals("TOTAL")) {
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
					if (orderType.toUpperCase().equals("TOTAL")) {
						profile.setMulticastType(MulticastType.TotalOrder);
					}
                    else if (orderType.toUpperCase().equals("CAUSAL")) {
						profile.setMulticastType(MulticastType.CausalOrder);
					}
					if(mode.equals("detail")){
						profile.isDetailMode = true;
					}else if(mode.equals("brief")){
						profile.isDetailMode = false;
					}else{
						System.err.println("invalid mode.");
						System.exit(-1);
					}
					
					if(boost.equals("boost")){
						profile.isBoost = true;
					}else if(boost.equals("normal")){
						profile.isBoost = false;
					}else{
						System.err.println("invalid mode.");
						System.exit(-1);
					}
				}
			}
		}
        catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		memberIndexer.addMembers(members);

        // Set up networking
		this._receiver = ReliableUnicastReceiver.getInstance(); 
		this._receiver.init(profile.ip, profile.port);
	}
	
	public void waitUserMessage(){
		Console console = System.console();
		if(Profile.getInstance().isBoost){
	        int myId = Profile.getInstance().getId();
	        for(int i = 1; i <= 20; ++i) {
	            //System.out.println(String.format("broadcasting %d", i));
	            multicastMessage(myId + " broadcast " + i);
	            try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	        }
		}
		
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
		if(Profile.getInstance().getMulticastType() == MulticastType.CausalOrder){
			CausalOrderMulticast causalOrderMulticast = CausalOrderMulticast.getInstance();
			causalOrderMulticast.send(1, content);
		}else if(Profile.getInstance().getMulticastType() == MulticastType.TotalOrder){
			TotalOrderMulticast totalOrderMulticast = TotalOrderMulticast.getInstance();
			totalOrderMulticast.send(1, content);
			//TotalOrderMulticastWithSequencer totalOrderMulticast = TotalOrderMulticastWithSequencer.getInstance();
			//totalOrderMulticast.send(1, content);
		}
	}
	
	public static void main(String[] args){
		
		
		if(args.length != 7){
			System.out.println("usage: java Chat [configFile] [delayTime(s)] [dropRate(0-1)] [selfId] [causal|total] [detail|brief] [boost|normal]");
			return;
		}
		Chat chat = new Chat(
				Integer.parseInt(args[1]),
				Double.parseDouble(args[2]),
				args[0],
				Integer.parseInt(args[3]),
				args[4],
				args[5],
				args[6]);
		chat.startListen();
		chat.waitUserMessage();
	}
}
