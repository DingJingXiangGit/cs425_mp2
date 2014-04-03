package model;

import strategy.TotalOrderMulticastWithSequencer;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created with IntelliJ IDEA.
 * User: treziapov
 * Date: 3/31/14
 * Time: 5:19 PM
 *
 * A thread Task to check for the available Total Order message
 */
public class MessageWaitTask extends TimerTask {
    private TotalOrderMulticastWithSequencer _owner;
    private Timer _timer;

    public MessageWaitTask(TotalOrderMulticastWithSequencer owner)
    {
        _owner = owner;
        _timer = new Timer();
        _timer.schedule(this, 0, 1000 + Profile.getInstance().getDelay() * 1000);
    }

    public Timer getTimer() {
        return _timer;
    }

    /*
        Invoke the Total Order Multicast instance's processing method
     */
    @Override
    public void run() {
        if (_owner == null) {
            return;
        }

        boolean result = _owner.waitForNextMessage();
        //System.out.println("wait for next message - " + (result ? "found" : "failed"));
    }
}
