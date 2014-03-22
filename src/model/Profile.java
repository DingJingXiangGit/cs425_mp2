package model;

public class Profile {
	public int id;
	public int port;
	public String ip;
	public String name;
	public int delay;
	public double dropRate;
	private static Profile _instance = new Profile();
	private Profile(){
		
	}
	
	public static Profile getInstance(){
		return _instance;
	}
}
