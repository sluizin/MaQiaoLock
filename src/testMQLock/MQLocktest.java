package testMQLock;

import java.util.ArrayList;
import java.util.BitSet;
import org.junit.Test;
//import sunj.sub.MaQiaoLock.Constants;
import MaQiao.MaQiaoLock.MQlock;
import MaQiao.MaQiaoLock.lock;
import MaQiao.MaQiaoLock.MQlockMaster;

/**
 * 要求：同组属性要求同步<br/>
 * 锁定同组属性，并把锁定放入多线程的方法体内
 * @author Sunjian
 */
public class MQLocktest {
	@lock
	private int size = 0;
	@lock
	private int maxSize = 1;
	@lock(Group = 1)
	private static int size2 = 2;
	@lock(Group = 1)
	private static double doubkl = 2;
	@lock(Group = 1)
	private static Double dou2 = (double) 2;
	@lock(Group = 1)
	private int maxSize2 = 3;
	@lock(Group = 5)
	private int size5 = 4;
	@lock(Group = 7)
	private int size7 = 8;
	@lock(Group = 8)
	private String string99 = "112233";
	@lock(Group = 8)
	private Integer Integer = new Integer(1000);
	@lock(Group = 9)
	private Integer Integer2 = new Integer(2000);

	@lock(Group = 21)
	private ArrayList<String> places = new ArrayList<String>();
	@lock(Group = 22)
	private ArrayList<String> placesAA = new ArrayList<String>();
	@lock(Group = 25)
	private BitSet bitset = new BitSet();
	@SuppressWarnings("unused")
	private void fun(){
		for(int i=0;i<10;i++)
			System.out.println("i:"+i);
	}
	@Test
	public void test() {
		try (MQlock mqlock = new MQlock(this, 1,7,22,21,21,7,0)) {
			size2 = 11;
			maxSize2 = 221;
			size7 = 232;
			//size=24;
			doubkl=3;
			dou2=(double) 8;
			//Integer=1010;
			//string99="taaa";
			//places.add("TT");
			//bitset.set(5);
			placesAA.add("ccc");
			B();
			//mqlock.show(this);
		}
	}
	public void B(){
		try (MQlock mqlock = new MQlock(this, 1,7,8,9)) {
			size2=13;
			string99="BBBb";
			//bitset.set(5);
		}
	}
	@SuppressWarnings("unused")
	private transient MQlockMaster lockMaster = new MQlockMaster(this);
}
