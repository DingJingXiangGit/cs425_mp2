package strategy;

import model.*;

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
    private BasicMulticast basicMulticast;
    Queue<TotalOrderMulticastMessage> holdbackQueue;
    Queue<TotalOrderMulticastMessage> orderQueue;
    Set<Integer> orderSequencesReceived;
    private Object _mutex;

    private static int nextTotalOrderSequence = 0;

    private TotalOrderMulticastWithSequencer()
    {
        basicMulticast = BasicMulticast.getInstance();
        holdbackQueue = new LinkedList<TotalOrderMulticastMessage>();
        orderQueue = new LinkedList<TotalOrderMulticastMessage>();
        orderSequencesReceived = new HashSet<Integer>();
        _mutex = new Object();
        TimerTask timerTask = new MessageWaitTask(this);
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
//        synchronized (_mutex)
//        {
//            nextTotalOrderSequence++;
//        }
    }

    public void delivery(IMessage message) {
        TotalOrderMulticastMessage tomm = (TotalOrderMulticastMessage)message;

//        if (tomm.getSource() == Profile.getInstance().getId()){
//            return;
//        }

        System.out.println("received: " + tomm.toString());

        TotalOrderMessageType messageType = tomm.getMessageType();
        synchronized (_mutex)
        {
            if (messageType == TotalOrderMessageType.INITIAL)
            {
                for (TotalOrderMulticastMessage m : orderQueue)
                {
                    if (m.getSource().equals(tomm.getSource()) && m.getContent().equals(tomm.getContent())) {
                        //                    System.out.println(
                        //                        String.format("changed tos from %d to %d", m.getTotalOrderSequence(), tomm.getTotalOrderSequence())
                        //                    );
                        tomm.setTotalOrderSequence(m.getTotalOrderSequence());
                        orderQueue.remove(m);
                    }
                }

                holdbackQueue.add(tomm);
            }
            else if (messageType == TotalOrderMessageType.ORDER)
            {
                orderSequencesReceived.add(tomm.getTotalOrderSequence());
                orderQueue.add(tomm);
                for (TotalOrderMulticastMessage m : holdbackQueue)
                {
                    //System.out.println(m.toString());
                    if (m.getSource().equals(tomm.getSource()) && m.getContent().equals(tomm.getContent())) {
    //                    System.out.println(
    //                        String.format("changed tos from %d to %d", m.getTotalOrderSequence(), tomm.getTotalOrderSequence())
    //                    );
                        m.setTotalOrderSequence(tomm.getTotalOrderSequence());
                        orderQueue.remove(tomm);
                    }
                    else {

                    }
                }
            }
        }


    }

    public boolean waitForNextMessage()
    {
        System.out.println(String.format("looking for message with TO sequence - %d", nextTotalOrderSequence));

        synchronized (_mutex)
        {
            if (!orderSequencesReceived.contains(nextTotalOrderSequence)) {
                return false;
            }

            for (TotalOrderMulticastMessage m : holdbackQueue)
            {
                System.out.println(m.toString());
                if (m.getTotalOrderSequence() == nextTotalOrderSequence)
                {
                    String out = String.format("message from %d: %s", m.getSource(), m.getContent());
                    System.out.println(out);
                    holdbackQueue.remove(m);
                    nextTotalOrderSequence++;
                    return true;
                }
            }

            return false;
        }
    }
}
