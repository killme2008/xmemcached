package com.google.code.yanf4j.test.unittest.utils;

import java.util.NoSuchElementException;
import java.util.Queue;

import com.google.code.yanf4j.util.SimpleQueue;

import junit.framework.TestCase;

public class QueueTest extends TestCase {
	private Queue<String> queue;

	@Override
	protected void setUp() throws Exception {
		queue = new SimpleQueue<String>();
	}

	public void testADD() {
		assertEquals(0, queue.size());
		assertTrue(queue.isEmpty());
		queue.add("a");
		assertEquals(1, queue.size());
		assertFalse(queue.isEmpty());

		queue.add("a");
		assertEquals(2, queue.size());
		assertFalse(queue.isEmpty());

		queue.add("b");
		assertEquals(3, queue.size());
		assertFalse(queue.isEmpty());
	}

	public void testOffer() {
		assertEquals(0, queue.size());
		assertTrue(queue.isEmpty());
		queue.offer("a");
		assertEquals(1, queue.size());
		assertFalse(queue.isEmpty());

		queue.offer("a");
		assertEquals(2, queue.size());
		assertFalse(queue.isEmpty());

		queue.offer("b");
		assertEquals(3, queue.size());
		assertFalse(queue.isEmpty());
	}

	public void testPoll() {
		assertNull(queue.poll());
		queue.add("a");
		assertEquals("a", queue.poll());
		assertNull(queue.poll());
		queue.add("a");
		queue.add("b");
		assertEquals("a", queue.poll());
		assertEquals("b", queue.poll());
		assertNull(queue.poll());
	}

	public void testPeek() {
		assertNull(queue.peek());
		queue.add("a");
		assertEquals("a", queue.peek());
		queue.add("b");
		assertEquals("a", queue.peek());
		queue.add("c");
		assertEquals("a", queue.peek());
		queue.poll();
		assertEquals("b", queue.peek());
		queue.poll();
		assertEquals("c", queue.peek());
		queue.poll();
		assertNull(queue.peek());
	}

	public void testRemove() {
		try {
			this.queue.remove();
			fail();
		} catch (NoSuchElementException e) {

		}
		queue.add("a");
		assertEquals("a", queue.remove());
		try {
			this.queue.remove();
			fail();
		} catch (NoSuchElementException e) {

		}
		queue.add("b");
		queue.add("c");
		assertEquals("b", queue.remove());
		assertEquals("c", queue.remove());
		try {
			this.queue.remove();
			fail();
		} catch (NoSuchElementException e) {

		}
	}

	@Override
	protected void tearDown() throws Exception {
		queue.clear();
		assertEquals(0, queue.size());
		assertTrue(queue.isEmpty());
	}

}
