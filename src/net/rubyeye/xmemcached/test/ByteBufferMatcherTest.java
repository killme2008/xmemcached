package net.rubyeye.xmemcached.test;

import java.nio.ByteBuffer;

import com.google.code.yanf4j.util.BMByteBufferMatcher;
import com.google.code.yanf4j.util.BNDMByteBufferMatcher;
import com.google.code.yanf4j.util.ByteBufferMatcher;
import com.google.code.yanf4j.util.ShiftAndByteBufferMatcher;

/**
 * 测试ByteBuffer匹配算法性能
 * 
 * @author dennis
 * 
 */
public class ByteBufferMatcherTest {
	static ByteBuffer SPLIT = ByteBuffer.wrap("\r\n".getBytes());
	static ByteBufferMatcher matcher1 = new BMByteBufferMatcher(SPLIT);
	static ByteBufferMatcher matcher2 = new ShiftAndByteBufferMatcher(SPLIT);
	static ByteBufferMatcher matcher3 = new BNDMByteBufferMatcher(SPLIT);

	static ByteBuffer[] buffers = new ByteBuffer[] {
			ByteBuffer.wrap("get a,b,c,d,e,f,g\r\nget h,i,j,k\r\n".getBytes()),
			ByteBuffer.wrap("deleted\r\n".getBytes()),
			ByteBuffer.wrap("NOT_FOUND\r\n".getBytes()),
			ByteBuffer.wrap("END\r\n".getBytes()),
			ByteBuffer.wrap("set key 0 0 1000 4\r\n".getBytes()) };

	static int num = 10000;
	static boolean debug = false;

	public static void main(String[] args) {
		testBNDM();
		testShiftAnd();
		testBM();

	}

	private static void testBM() {
		System.out.println("test BM matcher");
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
		System.out.println(System.nanoTime() - start);
		System.out.println(pos);
	}

	private static void testShiftAnd() {
		System.out.println("test shift-and matcher");
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
		System.out.println(System.nanoTime() - start);
		System.out.println(pos);
	}

	private static void testBNDM() {
		System.out.println("test bndm matcher");
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
		System.out.println(System.nanoTime() - start);
		System.out.println(pos);
	}
}
