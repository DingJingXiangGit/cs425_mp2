import java.util.PriorityQueue;


public class test implements Comparable<test>{
	public Integer value;
	
	public test(int v){
		this.value = v;
	}
	
	public static void main(String[] args){
		PriorityQueue<test> queue = new PriorityQueue<test>();
		queue.add(new test(10));
		queue.add(new test(12));
		queue.add(new test(5));
		queue.add(new test(1));
		queue.add(new test(17));
		
		while(queue.isEmpty() == false){
			
			System.out.println(queue.poll().value);
		}
	}
	@Override
	public int compareTo(test o) {
		// TODO Auto-generated method stub
		return this.value - o.value;
	}
}
