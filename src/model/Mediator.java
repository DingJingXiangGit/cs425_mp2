package model;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class Mediator {
	private static Mediator _instance = new Mediator();
	private Map<Integer, Member> _memberTable;
	private Map<Integer, Hashtable<Integer, Member>> _groupMemberTable;
	
	
	private Mediator(){
		_memberTable = new Hashtable<Integer, Member>();
		_groupMemberTable = new Hashtable<Integer, Hashtable<Integer, Member>>();
	}
	
	public static Mediator getInstance(){
		return _instance;
	}
	

	public void addMembers(List<Member> members){
		System.out.println("member list:");
		for(Member member: members){
			_memberTable.put(member._id, member);
			System.out.println(member._id +": "+member._ip +": "+member._port);
		}
		
	}
	
	public void receive(Message message){
		try {
			int sender = message._id;
			_memberTable.get(sender).receive(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Map<Integer, Member> getGroupMembers(int groupId){
		return _groupMemberTable.get(groupId);
	}
	
	public void publish(Message msg) {
		if(msg._id != Profile.getInstance().id){
			System.out.println("received message: "+msg.toString());
		}
	}
}
