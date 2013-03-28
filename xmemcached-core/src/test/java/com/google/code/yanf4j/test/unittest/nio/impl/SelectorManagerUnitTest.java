package com.google.code.yanf4j.test.unittest.nio.impl;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.EventType;
import com.google.code.yanf4j.nio.NioSession;
import com.google.code.yanf4j.nio.TCPController;
import com.google.code.yanf4j.nio.impl.Reactor;
import com.google.code.yanf4j.nio.impl.SelectorManager;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-24 ÏÂÎç03:09:42
 */

public class SelectorManagerUnitTest {
    private SelectorManager selectorManager;
    int selectorPoolSize = 3;


    @Before
    public void setUp() throws Exception {
        Configuration configuration = new Configuration();
        TCPController controller = new TCPController(configuration);
        this.selectorManager = new SelectorManager(this.selectorPoolSize, controller, configuration);
        this.selectorManager.start();
        controller.setSessionTimeout(1000);
        controller.getConfiguration().setSessionIdleTimeout(1000);
    }


    @Test
    public void testNextReactor() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            Reactor reactor = this.selectorManager.nextReactor();
            Assert.assertNotNull(reactor);
            Assert.assertTrue(reactor.getReactorIndex() > 0);
        }
        System.out.println(System.currentTimeMillis() - start);
    }


    @Test
    public void testRegisterOpenChannel() throws Exception {
        MockSelectableChannel channel = new MockSelectableChannel();
        channel.selectionKey = new MockSelectionKey();
        Reactor reactor = this.selectorManager.registerChannel(channel, 1, "hello");
        Thread.sleep(Reactor.DEFAULT_WAIT * 3);
        Assert.assertSame(reactor.getSelector(), channel.selector);
        Assert.assertSame(reactor.getSelector(), channel.selectionKey.selector);
        Assert.assertEquals(1, channel.ops);
        Assert.assertEquals("hello", channel.attch);
    }


    @Test
    public void testRegisterCloseChannel() throws Exception {
        MockSelectableChannel channel = new MockSelectableChannel();
        channel.close();
        this.selectorManager.registerChannel(channel, 1, "hello");
        Thread.sleep(Reactor.DEFAULT_WAIT * 3);
        Assert.assertNull(channel.selector);
        Assert.assertEquals(0, channel.ops);
        Assert.assertNull(channel.attch);
    }


    @Test
    public void testRegisterOpenSession() throws Exception {
        IMocksControl control = EasyMock.createControl();
        NioSession session = control.createMock(NioSession.class);
        EasyMock.makeThreadSafe(session, true);
        // next reactorµÄindex=2
        Reactor nextReactor = this.selectorManager.getReactorByIndex(2);
        session.onEvent(EventType.ENABLE_READ, nextReactor.getSelector());
        EasyMock.expectLastCall();
        EasyMock.expect(session.isClosed()).andReturn(false).times(2);
        EasyMock.expect(session.getAttribute(SelectorManager.REACTOR_ATTRIBUTE)).andReturn(null);
        EasyMock.expect(session.setAttributeIfAbsent(SelectorManager.REACTOR_ATTRIBUTE, nextReactor)).andReturn(null);

        control.replay();
        this.selectorManager.registerSession(session, EventType.ENABLE_READ);
        Thread.sleep(Reactor.DEFAULT_WAIT * 3);
        control.verify();
    }


    @Test
    public void testRegisterCloseSession() throws Exception {
        IMocksControl control = EasyMock.createControl();
        NioSession session = control.createMock(NioSession.class);
        EasyMock.expect(session.isClosed()).andReturn(true);
        control.replay();
        this.selectorManager.registerSession(session, EventType.ENABLE_READ);
        Thread.sleep(Reactor.DEFAULT_WAIT * 3);
        control.verify();
    }


    @After
    public void tearDown() throws Exception {
        if (this.selectorManager != null) {
            this.selectorManager.getController().stop();
            this.selectorManager.stop();
        }
    }

}
