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
    private Map<Integer, TotalOrderMulticastMessage> holdbackMessageMap;
    private Map<Integer, TotalOrderMulticastMessage> orderMessageMap;
    private Set<Integer> orderSequencesReceived;
    private Object _mutex;

    private static int messagesSentCounter = 0;
    private static int nextTotalOrderSequence = 0;

    private TotalOrderMulticastWithSequencer()
    {
        basicMulticast = BasicMulticast.getInstance();
        holdbackMessageMap = new HashMap<Integer, TotalOrderMulticastMessage>();
        orderMessageMap = new HashMap<Integer, TotalOrderMulticastMessage>();
        orderSequencesReceived = new HashSet<Integer>();
        _mutex = new Object();
        TimerTask timerTask = new MessageWaitTask(this);
    }

    public static TotalOrderMulticastWithSequencer getInstance()
    {
        return _instance;
    }

    /*
        Send the Chat user's input to all other Chat clients and the Total Order Sequencer
     */
    public void send(int groupId, String groupMessage) {
        int selfId = Profile.getInstance().getId();
        int messageId = Integer.parseInt(String.format("%d%d", selfId, messagesSentCounter));
        messagesSentCounter++;

        TotalOrderMulticastMessage tomm = new TotalOrderMulticastMessage();
        tomm.setMessageType(TotalOrderMessageType.INITIAL);
        tomm.setContent(groupMessage);
        tomm.setSource(selfId);
        tomm.setMessageId(messageId);
        tomm.setGroupId(groupId);
        tomm.setTotalOrderSequence(-1);

        basicMulticast.send(groupId, tomm);
        //System.out.println("sent: " + tomm.toString());
    }

    /*
        Store messages from other Chat clients in holdback map to be printed at the right order
        Order messages from the Sequencer are used to update the order of holdback messages
            or stored if corresponding message is not in holdback
     */
    public void delivery(IMessage message) {
        //System.out.println("received: " + message.toString());
        // Don't skip the message to yourself to print in Total Order
        TotalOrderMulticastMessage tomm = (TotalOrderMulticastMessage)message;
        TotalOrderMessageType messageType = tomm.getMessageType();
        int messageId = tomm.getMessageId();

        synchronized (_mutex)
        {
            if (messageType == TotalOrderMessageType.INITIAL)
            {
                // Already received the Total Order sequence number for this message
                if (orderMessageMap.containsKey(messageId))
                {
                    tomm.setTotalOrderSequence(orderMessageMap.get(messageId).getTotalOrderSequence());
                    orderMessageMap.remove(messageId);
                }

                // Put the message in the holdback map to be printed at it's turn
                holdbackMessageMap.put(messageId, tomm);
            }
            else if (messageType == TotalOrderMessageType.ORDER)
            {
                // Record received total order sequence numbers
                int totalOrderSequence = tomm.getTotalOrderSequence();
                orderSequencesReceived.add(totalOrderSequence);

                // If corresponding message already arrived, update it's total order sequence number
                if (holdbackMessageMap.containsKey(messageId)) {
                    TotalOrderMulticastMessage updated_message = holdbackMessageMap.get(messageId);
                    updated_message.setTotalOrderSequence(totalOrderSequence);
                    holdbackMessageMap.put(messageId, updated_message);
                }
                else {
                    orderMessageMap.put(messageId, tomm);
                }
            }
        }
    }

    /*
        The wait loop for the next message in total order
        Returns whether message was found or not
     */
    public boolean waitForNextMessage()
    {
        synchronized (_mutex)
        {
            // Check if the order message for the next message was received
            if (!orderSequencesReceived.contains(nextTotalOrderSequence)) {
                return false;
            }

            // Find the next total order message in the holdback map
            int nextMessageId = Integer.MIN_VALUE;
            for (TotalOrderMulticastMessage m : holdbackMessageMap.values())
            {
                if (m.getTotalOrderSequence() == nextTotalOrderSequence)
                {
                    nextMessageId = m.getMessageId();
                    nextTotalOrderSequence++;
                    String out = String.format("message from %d: %s", m.getSource(), m.getContent());
                    System.out.println(out);
                }
            }

            if (nextMessageId != Integer.MIN_VALUE)
            {
                holdbackMessageMap.remove(nextMessageId);
                return true;
            }
            else {
                return false;
            }
        }
    }
}
