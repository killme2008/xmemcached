package com.google.code.yanf4j.test.unittest.nio.impl;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashSet;
import java.util.Set;

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
 * @since 1.0, 2009-12-24 下午01:18:18
 */

public class ReactorUnitTest {
    private Reactor reactor;
    private SelectorManager selectorManager;


    @Before
    public void setUp() throws Exception {
        Configuration configuration = new Configuration();
        TCPController controller = new TCPController(configuration);
        this.selectorManager = new SelectorManager(1, controller, configuration);
        this.selectorManager.start();
        this.reactor = this.selectorManager.getReactorByIndex(0);
        controller.setSessionTimeout(1000);
        controller.getConfiguration().setSessionIdleTimeout(1000);
    }


    @Test
    public void testRegisterOpenChannel() throws Exception {
        MockSelectableChannel channel = new MockSelectableChannel();
        channel.selectionKey = new MockSelectionKey();
        this.reactor.registerChannel(channel, 1, "hello");
        Thread.sleep(Reactor.DEFAULT_WAIT * 3);
        Assert.assertSame(this.reactor.getSelector(), channel.selector);
        Assert.assertSame(this.reactor.getSelector(), channel.selectionKey.selector);
        Assert.assertEquals(1, channel.ops);
        Assert.assertEquals("hello", channel.attch);
    }


    @Test
    public void testRegisterCloseChannel() throws Exception {
        MockSelectableChannel channel = new MockSelectableChannel();
        channel.close();
        this.reactor.registerChannel(channel, 1, "hello");
        Thread.sleep(Reactor.DEFAULT_WAIT * 3);
        Assert.assertNull(channel.selector);
        Assert.assertEquals(0, channel.ops);
        Assert.assertNull(channel.attch);
    }


    @Test
    public void testRegisterOpenSession() throws Exception {
        IMocksControl control = EasyMock.createControl();
        NioSession session = control.createMock(NioSession.class);
        session.onEvent(EventType.ENABLE_READ, this.reactor.getSelector());
        EasyMock.expectLastCall();
        EasyMock.expect(session.isClosed()).andReturn(false);

        control.replay();
        this.reactor.registerSession(session, EventType.ENABLE_READ);
        Thread.sleep(Reactor.DEFAULT_WAIT * 3);
        control.verify();
    }


    @Test
    public void testRegisterCloseSession() throws Exception {
        IMocksControl control = EasyMock.createControl();
        NioSession session = control.createMock(NioSession.class);
        EasyMock.expect(session.isClosed()).andReturn(true);
        control.replay();
        this.reactor.registerSession(session, EventType.ENABLE_READ);
        Thread.sleep(Reactor.DEFAULT_WAIT * 3);
        control.verify();
    }


    @Test
    public void testDispatchEventAllValid() throws Exception {
        IMocksControl control = EasyMock.createControl();
        Set<SelectionKey> keySet = new HashSet<SelectionKey>();
        addReadableKey(keySet, control, this.reactor.getSelector());
        addWritableKey(keySet, control, this.reactor.getSelector());
        control.replay();
        this.reactor.dispatchEvent(keySet);
        Assert.assertEquals(0, keySet.size());
        control.verify();
    }


    @Test
    public void testDispatchEventSomeInValid() throws Exception {
        IMocksControl control = EasyMock.createControl();
        Set<SelectionKey> keySet = new HashSet<SelectionKey>();
        addInvalidKey(keySet);
        addWritableKey(keySet, control, this.reactor.getSelector());
        control.replay();
        this.reactor.dispatchEvent(keySet);
        Assert.assertEquals(0, keySet.size());
        control.verify();
    }


    @Test
    public void testPostSelectOneTimeout() {
        IMocksControl mocksControl = EasyMock.createControl();
        Set<SelectionKey> selectedKeys = new HashSet<SelectionKey>();
        Set<SelectionKey> allKeys = new HashSet<SelectionKey>();
        addTimeoutKey(mocksControl, allKeys);
        allKeys.addAll(selectedKeys);

        mocksControl.replay();
        this.reactor.postSelect(selectedKeys, allKeys);
        mocksControl.verify();

    }


    private void addTimeoutKey(IMocksControl mocksControl, Set<SelectionKey> allKeys) {
        MockSelectionKey key = new MockSelectionKey();
        NioSession session = mocksControl.createMock(NioSession.class);
        key.attach(session);
        // 判断session是否过期，假设为过期
        EasyMock.expect(session.isExpired()).andReturn(true);
        // 过期就会调用onSessionExpired，最后关闭连接
        // 同时，expired的session不会判断idle
        session.onEvent(EventType.EXPIRED, this.reactor.getSelector());
        EasyMock.expectLastCall();
        session.close();
        EasyMock.expectLastCall();

        allKeys.add(key);
    }


    @Test
    public void testPostSelectOneIdle() {
        IMocksControl mocksControl = EasyMock.createControl();
        Set<SelectionKey> selectedKeys = new HashSet<SelectionKey>();
        Set<SelectionKey> allKeys = new HashSet<SelectionKey>();
        addIdleKey(mocksControl, allKeys);
        allKeys.addAll(selectedKeys);

        mocksControl.replay();
        this.reactor.postSelect(selectedKeys, allKeys);
        mocksControl.verify();

    }


    @Test
    public void testPostSelectMoreKeys() {
        IMocksControl mocksControl = EasyMock.createControl();
        Set<SelectionKey> selectedKeys = new HashSet<SelectionKey>();
        Set<SelectionKey> allKeys = new HashSet<SelectionKey>();
        addIdleKey(mocksControl, allKeys);
        addTimeoutKey(mocksControl, allKeys);
        selectedKeys.add(new MockSelectionKey());
        selectedKeys.add(new MockSelectionKey());
        allKeys.addAll(selectedKeys);

        mocksControl.replay();
        this.reactor.postSelect(selectedKeys, allKeys);
        mocksControl.verify();
    }


    private void addIdleKey(IMocksControl mocksControl, Set<SelectionKey> allKeys) {
        MockSelectionKey key = new MockSelectionKey();
        NioSession session = mocksControl.createMock(NioSession.class);
        key.attach(session);
        // 判断session是否过期，不过期
        EasyMock.expect(session.isExpired()).andReturn(false);
        // 不过期就会判断session是否idle，假设idle为真
        EasyMock.expect(session.isIdle()).andReturn(true);
        // 如果idle条件为真，那么会调用onSessionIdle
        session.onEvent(EventType.IDLE, this.reactor.getSelector());
        EasyMock.expectLastCall();

        allKeys.add(key);
    }


    private void addInvalidKey(Set<SelectionKey> keySet) {
        MockSelectionKey key = new MockSelectionKey();
        key.valid = false;
        keySet.add(key);
    }


    private void addReadableKey(Set<SelectionKey> keySet, IMocksControl control, Selector selector) {
        MockSelectionKey key = new MockSelectionKey();
        key.interestOps = SelectionKey.OP_READ;
        NioSession session = control.createMock(NioSession.class);
        session.onEvent(EventType.READABLE, selector);
        EasyMock.expectLastCall();
        key.attach(session);
        key.selector = selector;
        keySet.add(key);
    }


    private void addWritableKey(Set<SelectionKey> keySet, IMocksControl control, Selector selector) {
        MockSelectionKey key = new MockSelectionKey();
        key.interestOps = SelectionKey.OP_WRITE;
        NioSession session = control.createMock(NioSession.class);
        session.onEvent(EventType.WRITEABLE, selector);
        EasyMock.expectLastCall();
        key.attach(session);
        key.selector = selector;
        keySet.add(key);
    }


    @After
    public void tearDown() throws Exception {
        if (this.selectorManager != null) {
            this.selectorManager.stop();
        }
    }

}
