package com.google.code.yanf4j.extension;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.CodecFactory;
import com.google.code.yanf4j.core.EventType;
import com.google.code.yanf4j.core.Handler;
import com.google.code.yanf4j.core.impl.FutureImpl;
import com.google.code.yanf4j.nio.NioSession;
import com.google.code.yanf4j.nio.impl.SocketChannelController;




/**
 * Notify�����ӹ���������չSocketChannelController���ṩ����Controller�������ͻ������ӹ���
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����05:56:50
 */

public class NotifyTCPConnectorController extends SocketChannelController {

    public NotifyTCPConnectorController() {
        super();
    }


    public FutureImpl<NioSession> connect(InetSocketAddress remoteAddress) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        configureSocketChannel(socketChannel);
        FutureImpl<NioSession> resultFuture = new FutureImpl<NioSession>();
        if (!socketChannel.connect(remoteAddress)) {
            this.selectorManager.registerChannel(socketChannel, SelectionKey.OP_CONNECT, resultFuture);
        }
        else {
            resultFuture.setResult(createSession(socketChannel));
        }
        return resultFuture;
    }


    @Override
    @SuppressWarnings("unchecked")
    public void onConnect(SelectionKey key) throws IOException {
        key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
        FutureImpl<NioSession> future = (FutureImpl<NioSession>) key.attachment();
        try {
            if (!((SocketChannel) key.channel()).finishConnect()) {
                throw new IOException("Connect Fail");
            }
            future.setResult(createSession((SocketChannel) key.channel()));
        }
        catch (Exception e) {
            future.failure(e);
            throw new IOException(e.getMessage());
        }
    }


    protected NioSession createSession(SocketChannel socketChannel) {
        NioSession session = buildSession(socketChannel);
        this.selectorManager.registerSession(session, EventType.ENABLE_READ);
        setLocalSocketAddress((InetSocketAddress) socketChannel.socket().getLocalSocketAddress());
        session.start();
        session.onEvent(EventType.CONNECTED, null);
        return session;
    }


    public NotifyTCPConnectorController(Configuration configuration, CodecFactory codecFactory) {
        super(configuration, codecFactory);
    }


    public NotifyTCPConnectorController(Configuration configuration, Handler handler, CodecFactory codecFactory) {
        super(configuration, handler, codecFactory);
    }


    public NotifyTCPConnectorController(Configuration configuration) {
        super(configuration);
    }


    @Override
    protected void doStart() throws IOException {
        // do nothing
    }


    public void closeChannel(Selector selector) throws IOException {

    }

}
