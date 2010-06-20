package com.google.code.yanf4j.test.unittest.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.google.code.yanf4j.buffer.IoBuffer;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.impl.HandlerAdapter;
import com.google.code.yanf4j.nio.TCPConnectorController;
import com.google.code.yanf4j.nio.TCPController;
import com.google.code.yanf4j.test.unittest.core.impl.AbstractControllerUnitTest;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-25 ÉÏÎç11:24:30
 */

public class TCPConnectorControllerUnitTest extends AbstractControllerUnitTest {

    private static final int PORT = 8080;
    private TCPConnectorController connector;


    @Override
    @After
    public void tearDown() throws Exception {
        if (this.connector != null) {
            this.connector.disconnect();
        }
        super.tearDown();

    }


    @Test
    public void connectServerDown() throws Exception {
        TCPController server = new TCPController();
        server.setHandler(new HandlerAdapter());
        server.bind(8080);
        this.connector = new TCPConnectorController();
        this.connector.setHandler(new HandlerAdapter());
        this.connector.connect(new InetSocketAddress("localhost", PORT));
        this.connector.awaitConnectUnInterrupt();
        Assert.assertTrue(this.connector.isConnected());

        server.stop();
        Thread.sleep(1000);
        Assert.assertFalse(this.connector.isConnected());
    }


    @Test
    public void testConnectFail() throws Exception {
        this.connector = new TCPConnectorController();
        this.connector.setHandler(new HandlerAdapter());

        Future<Boolean> future = this.connector.connect(new InetSocketAddress("localhost", PORT));
        try {
            this.connector.awaitConnectUnInterrupt();
            Assert.fail();
        }
        catch (IOException e) {

        }
        try {
            future.get();
            Assert.fail();
        }
        catch (ExecutionException e) {

        }
    }


    @Test
    public void testConnectAndSendMessageDisconnect() throws Exception {
        TCPController server = new TCPController();
        final AtomicInteger connectedCount = new AtomicInteger();
        final AtomicInteger recvSize = new AtomicInteger();
        server.setHandler(new HandlerAdapter() {

            @Override
            public void onSessionCreated(Session session) {
                connectedCount.incrementAndGet();
            }


            @Override
            public void onMessageReceived(Session session, Object message) {
                recvSize.addAndGet(((IoBuffer) message).remaining());
            }

        });
        server.bind(8080);

        this.connector = new TCPConnectorController();
        this.connector.setHandler(new HandlerAdapter());
        Assert.assertFalse(this.connector.isConnected());
        try {
            this.connector.connect(null);
            Assert.fail();
        }
        catch (IllegalArgumentException e) {
            Assert.assertEquals("Null remote address", e.getMessage());

        }

        try {
            this.connector.send(IoBuffer.allocate(1));
            Assert.fail();
        }
        catch (IllegalStateException e) {
            Assert.assertEquals("SocketChannel has not been connected", e.getMessage());
        }

        Future<Boolean> future = this.connector.connect(new InetSocketAddress("localhost", PORT));
        this.connector.awaitConnectUnInterrupt();
        Assert.assertTrue(this.connector.isConnected());
        Assert.assertTrue(future.get());
        Assert.assertEquals(1, connectedCount.get());

        Assert.assertTrue(this.connector.send(IoBuffer.allocate(10)).get());
        Thread.sleep(1000);
        Assert.assertEquals(10, recvSize.get());

        this.connector.disconnect();

        Assert.assertFalse(this.connector.isConnected());

        try {
            this.connector.send(IoBuffer.allocate(1));
            Assert.fail();
        }
        catch (IllegalStateException e) {
            Assert.assertEquals("SocketChannel has not been connected", e.getMessage());
        }
        server.stop();

    }

}
