/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.rubyeye.xmemcached.utils;

import com.google.code.yanf4j.util.CircularQueue;
import java.util.ArrayDeque;
import java.util.LinkedList;

/**
 *
 * @author dennis
 */
public class QueuePerformanceTest {

    static final int COUNT = 100000;

    public static void main(String[] args) {
        testLinkedList();
        testArrayDequeue();
        testCircularQueue();
    }

    public static void testLinkedList() {
        LinkedList dequeue = new LinkedList();
        long start = System.nanoTime();
        for (int i = 0; i < COUNT; i++) {
            dequeue.add(i);
        }
        for (int i = 0; i < COUNT; i++) {
            dequeue.addFirst(i);
        }
        for (int i = 0; i < 2 * COUNT; i++) {
            dequeue.remove();
        }
        System.out.println("linkedlist:" + (System.nanoTime() - start));

    }

     public static void testArrayDequeue() {
        ArrayDeque dequeue = new ArrayDeque(COUNT);
        long start = System.nanoTime();
        for (int i = 0; i < COUNT; i++) {
            dequeue.add(i);
        }
        for (int i = 0; i < COUNT; i++) {
            dequeue.addFirst(i);
        }
        for (int i = 0; i < 2 * COUNT; i++) {
            dequeue.remove();
        }
        System.out.println("ArrayDeque:" + (System.nanoTime() - start));

    }
      public static void testCircularQueue() {
        CircularQueue dequeue = new CircularQueue(COUNT);
        long start = System.nanoTime();
        for (int i = 0; i < COUNT; i++) {
            dequeue.add(i);
        }
        for (int i = 0; i < COUNT; i++) {
            dequeue.add(0, i);
        }
        for (int i = 0; i < 2 * COUNT; i++) {
            dequeue.remove();
        }
        System.out.println("CircularQueue:" + (System.nanoTime() - start));

    }
}
