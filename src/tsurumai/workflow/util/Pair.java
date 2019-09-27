package tsurumai.workflow.util;
/**ふたつの値のペア*/
public class Pair<T1, T2>{
	public T1 leader;
	public T2 trailer;
	public Pair(T1 l, T2 t){leader = l;trailer=t;}
	public String toString(){
		return "<"+(leader!=null?leader.toString():"")+","+(trailer!=null?trailer.toString():"")+">";
	}
}