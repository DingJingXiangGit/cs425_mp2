package model;

import java.io.Serializable;


public class TotalOrderMulticastMessage implements IMessage, Serializable, Comparable<TotalOrderMulticastMessage>{
	private static final long serialVersionUID = 34278371231L;
	private String content;
	private Integer groupId;
	private Integer source;
	private Integer sequence;
	private Integer messageId;
	public Integer getMessageId() {
		return messageId;
	}

	public void setMessageId(Integer messageId) {
		this.messageId = messageId;
	}

	private TotalOrderMessageType messageType;
	
	public TotalOrderMessageType getMessageType() {
		return messageType;
	}

	public void setMessageType(TotalOrderMessageType messageType) {
		this.messageType = messageType;
	}

	public String getContent() {
		return content;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	public Integer getGroupId() {
		return groupId;
	}
	
	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}
	public Integer getSource() {
		return source;
	}
	public void setSource(Integer source) {
		this.source = source;
	}
	public Integer getSequence() {
		return sequence;
	}
	public void setSequence(Integer sequence) {
		this.sequence = sequence;
	}
	
	public boolean isDeliverable(){
		return this.messageType == TotalOrderMessageType.FINAL;
	}
	
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	
	
	@Override
	public int compareTo(TotalOrderMulticastMessage o) {
		return this.sequence - o.sequence;
	}
	
}
