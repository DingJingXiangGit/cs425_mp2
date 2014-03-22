package model;

import java.util.TimerTask;

public class RetransmissionTask extends TimerTask {
	private int _sequence = 0;
	private ReliableUnicastSender _owner;
	public RetransmissionTask(int seq, ReliableUnicastSender owner){
		this._sequence = seq;
		this._owner = owner;
	}

	public int getSequence(){
		return _sequence;
	}

		
	@Override
	public void run() {
		_owner.resend(this);
	}
	
}
