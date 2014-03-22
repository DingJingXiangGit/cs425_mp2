package strategy;


public interface Multicast {
	public void send(int groupId, String content);
	public void delivery(String message);
}
