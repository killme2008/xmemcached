package net.rubyeye.xmemcached.test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;

public class ExceptionTest {
	static XMemcachedClient client;

	public static void main(String args[]) {
		initClient();

		testGetBlankKey();

		testSetBlankKey();

		stopClient();
	}

	private static void testSetBlankKey() {
		try {
			client.set(" ", 0, 100);
			System.err.println("error");
		} catch (InterruptedException e) {

		} catch (TimeoutException e) {
			e.printStackTrace();
		} catch (MemcachedException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {

		}
		try {
			client.set("a", -100, 100);
			System.err.println("test error");
		} catch (InterruptedException e) {

		} catch (TimeoutException e) {
			e.printStackTrace();
		} catch (MemcachedException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
		}
		try {
			client.set(null, 0, 100);
			System.err.println("test error");
		} catch (InterruptedException e) {

		} catch (TimeoutException e) {
			e.printStackTrace();
		} catch (MemcachedException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
		}
	}

	private static void testGetBlankKey() {
		try {
			client.get("");
			System.err.println("error");
		} catch (InterruptedException e) {

		} catch (TimeoutException e) {
			e.printStackTrace();
		} catch (MemcachedException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {

		}
	}

	private static void stopClient() {
		if (client != null)
			try {
				client.shutdown();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	private static void initClient() {
		try {
			client = new XMemcachedClient();
			client.addServer("192.168.222.100", 12000);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
