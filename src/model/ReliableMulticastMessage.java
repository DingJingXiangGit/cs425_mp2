package model;

public class ReliableMulticastMessage {
	public int _groupId;
	public int _sourceId;
	public int _sourceGroupSequence;
	public String _content;
	
	public String toString(){
		return String.format("%d,\t%d,\t%s,\t%s", _groupId, _sourceId, _sourceGroupSequence, _content);
	}
	
	
	public static ReliableMulticastMessage parse(String message){
		ReliableMulticastMessage result = new ReliableMulticastMessage();
		message = message.substring(1, message.length() - 2);
		String[] tokens = message.split(",\t");
		System.out.println("input:"+message);
		result._groupId = Integer.parseInt(tokens[0]);
		result._sourceId = Integer.parseInt(tokens[1]);
		result._sourceGroupSequence = Integer.parseInt(tokens[2]);
		result._content = tokens[3];
		return result;
	}
}
