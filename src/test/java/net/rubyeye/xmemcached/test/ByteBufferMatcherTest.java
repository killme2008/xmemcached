package net.rubyeye.xmemcached.test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.code.yanf4j.util.BMByteBufferMatcher;
import com.google.code.yanf4j.util.BNDMByteBufferMatcher;
import com.google.code.yanf4j.util.ByteBufferMatcher;
import com.google.code.yanf4j.util.ByteBufferUtils;
import com.google.code.yanf4j.util.KMPByteBufferMatcher;
import com.google.code.yanf4j.util.ShiftAndByteBufferMatcher;
import com.google.code.yanf4j.util.ShiftOrByteBufferMatcher;

/**
 * 测试ByteBuffer匹配算法性能
 * 
 * @author dennis
 * 
 */
class TestResult implements Comparable<TestResult> {
	String name;
	long result;

	public TestResult(String name, long result) {
		super();
		this.name = name;
		this.result = result;
	}

	@Override
	public int compareTo(TestResult o) {
		return (int) (this.result - o.result);
	}

}

public class ByteBufferMatcherTest {
	static ByteBuffer SPLIT = ByteBuffer.wrap("\r\n".getBytes());
	static ByteBufferMatcher matcher1 = new BMByteBufferMatcher(SPLIT);
	static ByteBufferMatcher matcher2 = new ShiftAndByteBufferMatcher(SPLIT);
	static ByteBufferMatcher matcher3 = new BNDMByteBufferMatcher(SPLIT);
	static ByteBufferMatcher matcher4 = new ShiftOrByteBufferMatcher(SPLIT);

	static ByteBufferMatcher matcher5 = new KMPByteBufferMatcher(SPLIT);

	static List<TestResult> result = new ArrayList<TestResult>();

	static ByteBuffer[] buffers = new ByteBuffer[] {
			ByteBuffer.wrap("get a,b,c,d,e,f,g\r\nget h,i,j,k\r\n".getBytes()),
			ByteBuffer.wrap("deleted\r\n".getBytes()),
			ByteBuffer.wrap("NOT_FOUND\r\n".getBytes()),
			ByteBuffer.wrap("END\r\n".getBytes()),
			ByteBuffer.wrap("set key 0 0 1000 4\r\n".getBytes()) };

	static int num = 100000;
	static boolean debug = false;

	public static void main(String[] args) {
		testBNDM();
		testShiftOr();
		testShiftAnd();
		testBM();
		testKMP();
		testSimple();
		printResult();
	}

	public static void printResult() {
		Collections.sort(result);
		for (TestResult r : result) {
			System.out.println(r.name + ":" + r.result);
		}
	}

	private static void testBM() {
		long start = System.nanoTime();
		int pos = -1;
		for (int i = 0; i < num; i++) {
			for (ByteBuffer buffer : buffers) {
				pos = matcher1.matchFirst(buffer);
				if (debug)
					System.out.print(pos + " ");
			}
		}
		if (debug)
			System.out.println();
		result.add(new TestResult("BM", System.nanoTime() - start));
	}

	private static void testShiftAnd() {
		long start = System.nanoTime();
		int pos = -1;
		for (int i = 0; i < num; i++) {
			for (ByteBuffer buffer : buffers) {
				pos = matcher2.matchFirst(buffer);
				if (debug)
					System.out.print(pos + " ");
			}
		}
		if (debug)
			System.out.println();
		result.add(new TestResult("Shift-And", System.nanoTime() - start));
	}

	private static void testShiftOr() {
		long start = System.nanoTime();
		int pos = -1;
		for (int i = 0; i < num; i++) {
			for (ByteBuffer buffer : buffers) {
				pos = matcher4.matchFirst(buffer);
				if (debug)
					System.out.print(pos + " ");
			}
		}
		if (debug)
			System.out.println();
		result.add(new TestResult("Shift-Or", System.nanoTime() - start));
	}

	private static void testBNDM() {
		long start = System.nanoTime();
		int pos = -1;
		for (int i = 0; i < num; i++) {
			for (ByteBuffer buffer : buffers) {
				pos = matcher3.matchFirst(buffer);
				if (debug)
					System.out.print(pos + " ");
			}
		}
		if (debug)
			System.out.println();
		result.add(new TestResult("BNDM", System.nanoTime() - start));
	}

	private static void testKMP() {
		long start = System.nanoTime();
		int pos = -1;
		for (int i = 0; i < num; i++) {
			for (ByteBuffer buffer : buffers) {
				pos = matcher5.matchFirst(buffer);
				if (debug)
					System.out.print(pos + " ");
			}
		}
		if (debug)
			System.out.println();
		result.add(new TestResult("KMP", System.nanoTime() - start));
	}

	private static void testSimple() {
		long start = System.nanoTime();
		int pos = -1;
		for (int i = 0; i < num; i++) {
			for (ByteBuffer buffer : buffers) {
				pos = ByteBufferUtils.indexOf(buffer, SPLIT);
				if (debug)
					System.out.print(pos + " ");
			}
		}
		if (debug)
			System.out.println();
		result.add(new TestResult("Simple", System.nanoTime() - start));
	}
}
