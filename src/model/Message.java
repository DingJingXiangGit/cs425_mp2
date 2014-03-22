package model;
public class Message {
	public String _content;
	public int _sequence;
	public int _id;
	public String _action;
	
	public Message(){
		
	}
	
	public Message(Message msg) {
		this._content =msg._content;
		this._id = msg._id;
		this._sequence = msg._sequence;
		this._action = msg._action;
	}

	public String toString(){
		return String.format("{%d, %d, %s, %s}", _sequence, _id, _action, _content);
	}
	
	public static Message parse(String message){
		Message result = new Message();
		message = message.trim();
		message = message.substring(1, message.length() - 2);
		String[] tokens = message.split(", ");
		System.out.println("input:"+message);
		result._sequence = Integer.parseInt(tokens[0]);
		result._id = Integer.parseInt(tokens[1]);
		result._action = tokens[2];
		result._content = tokens[3];
		return result;
	}
	
	public String getAckString(){
		return String.format("{%d, %d, ack, null}", _sequence,_id);
	}
}
