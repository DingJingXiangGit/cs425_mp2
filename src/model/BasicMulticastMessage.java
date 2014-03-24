package model;

public class BasicMulticastMessage {
	public int groupId;
	public int sourceId;
	public int sourceGroupSequence;
	public String content;
	
	public int getGroupId() {
		return groupId;
	}


	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}


	public int getSourceId() {
		return sourceId;
	}


	public void setSourceId(int sourceId) {
		this.sourceId = sourceId;
	}


	public int getSourceGroupSequence() {
		return sourceGroupSequence;
	}


	public void setSourceGroupSequence(int sourceGroupSequence) {
		this.sourceGroupSequence = sourceGroupSequence;
	}


	public String getContent() {
		return content;
	}


	public void setContent(String content) {
		this.content = content;
	}


	public String toString(){
		return String.format("%d,\t%d,\t%s,\t%s", groupId, sourceId, sourceGroupSequence, content);
	}
	
	
	public static BasicMulticastMessage parse(String message){
		BasicMulticastMessage result = new BasicMulticastMessage();
		String[] tokens = message.split(",\t");
		result.groupId = Integer.parseInt(tokens[0]);
		result.sourceId = Integer.parseInt(tokens[1]);
		result.sourceGroupSequence = Integer.parseInt(tokens[2]);
		result.content = tokens[3];
		return result;
	}
}
