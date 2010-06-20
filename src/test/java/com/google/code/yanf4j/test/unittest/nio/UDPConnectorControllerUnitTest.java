package com.google.code.yanf4j.test.unittest.nio;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.impl.HandlerAdapter;
import com.google.code.yanf4j.core.impl.UDPHandlerAdapter;
import com.google.code.yanf4j.nio.UDPConnectorController;
import com.google.code.yanf4j.nio.UDPController;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-25 ÏÂÎç01:46:11
 */

public class UDPConnectorControllerUnitTest {
    private UDPConnectorController connector;


    @After
    public void tearDown() throws IOException {
        if (this.connector != null) {
            this.connector.disconnect();
        }
    }


    @Test
    public void testConnectAndSendThenDisconnect() throws Exception {
        UDPController server = new UDPController();
        final AtomicInteger recvSize = new AtomicInteger();
        server.setHandler(new UDPHandlerAdapter() {

            @Override
            public void onMessageReceived(Session session, Object message) {
                DatagramPacket packet = (DatagramPacket) message;
                recvSize.addAndGet(packet.getLength());
            }

        });
        server.start();

        this.connector = new UDPConnectorController();
        this.connector.setHandler(new HandlerAdapter());
        Assert.assertFalse(this.connector.isConnected());
        this.connector.connect(server.getLocalSocketAddress());
        Assert.assertTrue(this.connector.isConnected());

        this.connector.send(new DatagramPacket("test".getBytes(), 4)).get();
        Thread.sleep(1000);
        Assert.assertEquals(4, recvSize.get());

        this.connector.disconnect();
        try {
            this.connector.send(new DatagramPacket("test".getBytes(), 4)).get();
            Assert.fail();
        }
        catch (IllegalStateException e) {

        }
        Thread.sleep(1000);
        Assert.assertEquals(4, recvSize.get());

        // reconnect
        this.connector.connect(server.getLocalSocketAddress());
        Assert.assertTrue(this.connector.isConnected());
        this.connector.send(new DatagramPacket("test".getBytes(), 4)).get();
        Thread.sleep(1000);
        Assert.assertEquals(8, recvSize.get());

        server.stop();
    }
}
