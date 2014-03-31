package model;

import strategy.BasicMulticast;
import strategy.ReliableUnicastReceiver;
import strategy.ReliableUnicastSender;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Created with IntelliJ IDEA.
 * User: treziapov
 * Date: 3/30/14
 * Time: 7:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class TotalOrderSequencer {
    public static int _id = 100;
    private static int _sequence = 0;

    private static TotalOrderSequencer _instance;
    private ReliableUnicastReceiver _receiver;
    private ReliableUnicastSender _unicastSender;
    private BasicMulticast _basicMulticast = BasicMulticast.getInstance();

    private DatagramSocket _socket;
    private static int BUFFER_SIZE = 1024;

    public static TotalOrderSequencer getInstance()
    {
        return _instance;
    }

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

        System.out.println(String.format("listening on %s:%d", profile.ip, profile.port));
    }

    public void delivery(IMessage m) {
        TotalOrderMulticastMessage message = (TotalOrderMulticastMessage)m;
        MemberIndexer memberIndexer = MemberIndexer.getInstance();

        if(message.getMessageType() == TotalOrderMessageType.INITIAL) {
            // Send an order message with the next sequence number
            TotalOrderMulticastMessage tomm = new TotalOrderMulticastMessage();
            tomm.setMessageId(message.getMessageId());
            tomm.setSource(message.getSource());
            tomm.setContent(message.getContent());
            tomm.setGroupId(1);
            tomm.setMessageType(TotalOrderMessageType.ORDER);
            tomm.setTotalOrderSequence(_sequence);
            tomm.setSequence(-1);

            _sequence++;
            _basicMulticast.send(1, tomm);
            System.out.println("multicasted next total order sequence");
        }
    }

    public void run(){
        byte[] buf;
        byte[] data;
        Message message;
        Member member;
        DatagramPacket packet;
        MemberIndexer memberIndexer;
        int senderId;

        memberIndexer = MemberIndexer.getInstance();
        while (true) {
            buf = new byte[BUFFER_SIZE];
            packet = new DatagramPacket(buf, buf.length);

            try {
                _socket.receive(packet);
                data = Arrays.copyOf(packet.getData(), packet.getLength());
                packet.setLength(packet.getLength());
                message = Message.parse(data);
                senderId = message.getId();
                member = memberIndexer.getById(senderId);

                //System.out.println("receive: " + message.toString());
                if(message.getAction().equals("delivery")) {
                    //avoid duplicated message
                    _unicastSender.sendAck(message, member);
                    //delivery(message);
                }else{
                    //receive ack message, cancel retransmission
                    _unicastSender.ack(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args){
        if (args.length != 1) {
            System.out.println("java TotalOrderSequencer configFile");
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
