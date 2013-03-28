package com.google.code.yanf4j.test.unittest;

public class Main {
	public static void main(String[] args) {
		long start = System.nanoTime();
		int threadNum = 10000;
		int sum=0;
		for (int i = 0; i < threadNum; i++)
			sum+=testLock(i, i * 2);
		System.out.println((System.nanoTime() - start) / threadNum);
	    System.out.println(sum);
	}
	
	public final static synchronized int testLock(int a, int b) {
		return a + b;
	}

}
