package model;

import strategy.TotalOrderMulticastWithSequencer;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created with IntelliJ IDEA.
 * User: treziapov
 * Date: 3/31/14
 * Time: 5:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class MessageWaitTask extends TimerTask {
    private TotalOrderMulticastWithSequencer _owner;
    private Timer _timer;

    public MessageWaitTask(TotalOrderMulticastWithSequencer owner)
    {
        _owner = owner;
        _timer = new Timer();
        _timer.schedule(this, 0, Profile.getInstance().getDelay() * 1000);
    }

    public Timer getTimer() {
        return _timer;
    }

    @Override
    public void run() {
        if (_owner == null) {
            System.out.println("MessageWaitTask totalordersequencer uninitialized");
            return;
        }

        System.out.println("MessageWaitTask waiting for message");
        boolean received = _owner.waitForNextMessage();

        if (received) {
            System.out.println("MessageWaitTask received a message");
        }
    }
}
