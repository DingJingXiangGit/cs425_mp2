package strategy;

import model.IMessage;
import model.Profile;
import model.TotalOrderMessageType;
import model.TotalOrderMulticastMessage;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: treziapov
 * Date: 3/30/14
 * Time: 7:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class TotalOrderMulticastWithSequencer {
    private static TotalOrderMulticastWithSequencer _instance = new TotalOrderMulticastWithSequencer();
    private BasicMulticast basicMulticast = BasicMulticast.getInstance();
    Queue<TotalOrderMulticastMessage> holdbackQueue;
    Set<Integer> orderSequencesReceived;

    private static int nextTotalOrderSequence = 0;

    private TotalOrderMulticastWithSequencer()
    {
        holdbackQueue = new LinkedList<TotalOrderMulticastMessage>();
        orderSequencesReceived = new HashSet<Integer>();
    }

    public static TotalOrderMulticastWithSequencer getInstance()
    {
        return _instance;
    }

    public void send(int groupId, String groupMessage) {
        TotalOrderMulticastMessage tomm;
        int selfId = Profile.getInstance().getId();
        int messageId;
        Map<Integer, TotalOrderMulticastMessage > cachedMessage;

        tomm = new TotalOrderMulticastMessage();
        tomm.setContent(groupMessage);
        tomm.setSource(selfId);
        tomm.setGroupId(groupId);
        tomm.setTotalOrderSequence(-1);
        tomm.setMessageType(TotalOrderMessageType.INITIAL);

        basicMulticast.send(groupId, tomm);
        nextTotalOrderSequence++;
    }

    public void delivery(IMessage message) {
        TotalOrderMulticastMessage tomm = (TotalOrderMulticastMessage)message;

        if (tomm.getSource() == Profile.getInstance().getId()){
            return;
        }

        //System.out.println("received: " + message.toString());

        TotalOrderMessageType messageType = tomm.getMessageType();
        if (messageType == TotalOrderMessageType.INITIAL)
        {
            holdbackQueue.add(tomm);
        }
        else if (messageType == TotalOrderMessageType.ORDER)
        {
            orderSequencesReceived.add(tomm.getTotalOrderSequence());
            for (TotalOrderMulticastMessage m : holdbackQueue)
            {
                //System.out.println(m.toString());
                if (m.getSource().equals(tomm.getSource()) && m.getContent().equals(tomm.getContent())) {
//                    System.out.println(
//                        String.format("changed tos from %d to %d", m.getTotalOrderSequence(), tomm.getTotalOrderSequence())
//                    );
                    m.setTotalOrderSequence(tomm.getTotalOrderSequence());
                }
                else {
//                    System.out.print("same source: ");
//                    System.out.print(m.getSource().equals(tomm.getSource()));
//                    System.out.print(", same content: ");
//                    System.out.println(m.getContent().equals(tomm.getContent()));
                }
            }
        }

        while(orderSequencesReceived.contains(nextTotalOrderSequence))
        {
//            System.out.println(String.format("looking for message with TO sequence - %d", nextTotalOrderSequence));
            for (TotalOrderMulticastMessage m : holdbackQueue)
            {
//                System.out.println(m.toString());
                if (m.getTotalOrderSequence() == nextTotalOrderSequence)
                {
                    String out = String.format("message from %d: %s", m.getSource(), m.getContent());
                    System.out.println(out);
                    holdbackQueue.remove(m);
                }
            }
            nextTotalOrderSequence++;
        }
    }
}
