package model;

import strategy.BasicMulticast;
import strategy.ReliableUnicastReceiver;
import strategy.ReliableUnicastSender;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created with IntelliJ IDEA.
 * User: treziapov
 * Date: 3/30/14
 * Time: 7:41 PM
 */
public class TotalOrderSequencer {
    private static TotalOrderSequencer _instance;
    private ReliableUnicastReceiver _receiver;
    //private ReliableUnicastSender _unicastSender;
    private BasicMulticast _basicMulticast = BasicMulticast.getInstance();

    //private static int BUFFER_SIZE = 1024;

    public static int _id = 100;
    private static int _sequence = 0;

    public static TotalOrderSequencer getInstance()
    {
        return _instance;
    }

    /*
        Constructor
        Parse the given file if it exists (assume correct format)
        Set up networking components
     */
    private TotalOrderSequencer(String file)
    {
        // Parse the config file
        List<Member> members = new ArrayList<Member>();
        Profile profile = Profile.getInstance();
        MemberIndexer memberIndexer = MemberIndexer.getInstance();

        try {
            Scanner scanner = new Scanner(new File(file));
            while(scanner.hasNext()) {
                String line = scanner.nextLine();
                String[] parts = line.split(" ");
                System.out.println(parts[0] + parts[1] +parts[2] + parts[3]);
                Member member = new Member();
                member._id =  Integer.parseInt(parts[0]);
                member._ip =  parts[1];
                member._port = Integer.parseInt(parts[2]);
                member._userName = parts[3];
                member._groupId = Integer.parseInt(parts[4]);

                // Don't add yourself to the members/neighbors
                if(Integer.parseInt(parts[0]) == _id){
                    profile.id = _id;
                    profile.ip = parts[1];
                    profile.port = Integer.parseInt(parts[2]);
                    profile.name = parts[3];
                    continue;
                }

                members.add(member);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        memberIndexer.addMembers(members);

        // Network setup
        this._receiver = ReliableUnicastReceiver.getInstance();
        this._receiver.init(profile.ip, profile.port);

        System.out.println(
            String.format("sequencer: listening on %s:%d", profile.ip, profile.port)
        );
    }

    /*
        Reply with Order message if receiving a message (initial) from a Chat client
     */
    public void delivery(IMessage m) {
        TotalOrderMulticastMessage message = (TotalOrderMulticastMessage)m;
        if(message.getMessageType() == TotalOrderMessageType.INITIAL) {
            TotalOrderMulticastMessage tomm = new TotalOrderMulticastMessage();
            tomm.setMessageType(TotalOrderMessageType.ORDER);
            tomm.setMessageId(message.getMessageId());
            tomm.setSource(message.getSource());
            tomm.setContent(message.getContent());
            tomm.setGroupId(1);
            tomm.setSequence(-1);
            tomm.setTotalOrderSequence(_sequence);

            _basicMulticast.send(1, tomm);
            _sequence++;

            System.out.println(
                String.format("[message from %d: %s], total order sequence: %d",
                    tomm.getSource(),
                    tomm.getContent(),
                    tomm.getTotalOrderSequence()
                )
            );
        }
    }

    /*
        Start the Total Order Sequencer and listen for messages
     */
    public static void main(String[] args){
        if (args.length != 1) {
            System.out.println("usage: java TotalOrderSequencer configFile");
            return;
        }

        _instance = new TotalOrderSequencer(args[0]);
        (new Thread(_instance._receiver)).start();

        while(true)
        {
            try {
                Thread.sleep(1000);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
