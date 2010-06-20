package com.google.code.yanf4j.nio;

import java.nio.channels.SelectableChannel;
import java.util.Queue;

import com.google.code.yanf4j.core.CodecFactory;
import com.google.code.yanf4j.core.Dispatcher;
import com.google.code.yanf4j.core.Handler;
import com.google.code.yanf4j.core.SessionConfig;
import com.google.code.yanf4j.core.WriteMessage;
import com.google.code.yanf4j.nio.impl.SelectorManager;
import com.google.code.yanf4j.statistics.Statistics;




/**
 * Nio session���ö���
 * 
 * @author boyan
 * 
 */
public class NioSessionConfig extends SessionConfig {

    public final SelectableChannel selectableChannel;
    public final SelectorManager selectorManager;


    public NioSessionConfig(SelectableChannel sc, Handler handler, SelectorManager reactor, CodecFactory codecFactory,
            Statistics statistics, Queue<WriteMessage> queue, Dispatcher dispatchMessageDispatcher,
            boolean handleReadWriteConcurrently, long sessionTimeout, long sessionIdleTimeout) {
        super(handler, codecFactory, statistics, queue, dispatchMessageDispatcher, handleReadWriteConcurrently,
            sessionTimeout, sessionIdleTimeout);
        this.selectableChannel = sc;
        this.selectorManager = reactor;
    }

}
