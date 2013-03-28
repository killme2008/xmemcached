package com.google.code.yanf4j.test.unittest.nio.impl;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-24 ÏÂÎç01:40:15
 */

public class MockSelectableChannel extends SelectableChannel {
    Selector selector;
    int ops;
    Object attch;
    MockSelectionKey selectionKey = new MockSelectionKey();


    @Override
    public Object blockingLock() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SelectableChannel configureBlocking(boolean block) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public boolean isBlocking() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean isRegistered() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public SelectionKey keyFor(Selector sel) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SelectorProvider provider() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SelectionKey register(Selector sel, int ops, Object att) throws ClosedChannelException {
        this.selector = sel;
        this.ops = ops;
        this.attch = att;
        this.selectionKey.channel = this;
        this.selectionKey.selector = sel;
        return this.selectionKey;
    }


    @Override
    public int validOps() {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    protected void implCloseChannel() throws IOException {
        // TODO Auto-generated method stub

    }

}
