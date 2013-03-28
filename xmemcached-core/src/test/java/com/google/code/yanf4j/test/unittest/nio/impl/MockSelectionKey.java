package com.google.code.yanf4j.test.unittest.nio.impl;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-24 ÏÂÎç02:01:20
 */

public class MockSelectionKey extends SelectionKey {
    MockSelectableChannel channel;
    int interestOps;
    boolean valid = true;
    Selector selector;


    @Override
    public void cancel() {
        this.valid = false;

    }


    @Override
    public SelectableChannel channel() {
        return this.channel;
    }


    @Override
    public int interestOps() {
        return this.interestOps;
    }


    @Override
    public SelectionKey interestOps(int ops) {
        this.interestOps = ops;
        return this;
    }


    @Override
    public boolean isValid() {
        return this.valid;
    }


    @Override
    public int readyOps() {
        return this.interestOps;
    }


    @Override
    public Selector selector() {
        return this.selector;
    }

}
