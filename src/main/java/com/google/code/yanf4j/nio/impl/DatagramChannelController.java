/**
 *Copyright [2009-2010] [dennis zhuang]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package com.google.code.yanf4j.nio.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Queue;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.CodecFactory;
import com.google.code.yanf4j.core.EventType;
import com.google.code.yanf4j.core.Handler;
import com.google.code.yanf4j.core.WriteMessage;
import com.google.code.yanf4j.core.impl.StandardSocketOption;
import com.google.code.yanf4j.nio.NioSession;
import com.google.code.yanf4j.util.SystemUtils;




/**
 * Nio��UDP������
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����06:08:54
 */
public abstract class DatagramChannelController extends NioController {

    protected DatagramChannel channel;
    protected NioSession udpSession;
    protected int maxDatagramPacketLength;


    public DatagramChannelController() {
        super();
        this.maxDatagramPacketLength = 4096;
    }


    @Override
    protected void doStart() throws IOException {
        buildDatagramChannel();
        initialSelectorManager();
        buildUDPSession();
    }


    public DatagramChannelController(Configuration configuration) {
        super(configuration, null, null);
        setMaxDatagramPacketLength(configuration.getSessionReadBufferSize() > 9216 ? 4096 : configuration
            .getSessionReadBufferSize());
    }


    public DatagramChannelController(Configuration configuration, CodecFactory codecFactory) {
        super(configuration, null, codecFactory);
        setMaxDatagramPacketLength(configuration.getSessionReadBufferSize() > 9216 ? 4096 : configuration
            .getSessionReadBufferSize());
    }


    public DatagramChannelController(Configuration configuration, Handler handler, CodecFactory codecFactory) {
        super(configuration, handler, codecFactory);
        setMaxDatagramPacketLength(configuration.getSessionReadBufferSize() > 9216 ? 4096 : configuration
            .getSessionReadBufferSize());
    }


    public int getMaxDatagramPacketLength() {
        return this.maxDatagramPacketLength;
    }


    @Override
    public void setReadThreadCount(int readThreadCount) {
        if (readThreadCount > 1) {
            throw new IllegalArgumentException("UDP controller could not have more than 1 read thread");
        }
        super.setReadThreadCount(readThreadCount);
    }


    public void setMaxDatagramPacketLength(int maxDatagramPacketLength) {
        if (isStarted()) {
            throw new IllegalStateException();
        }
        if (SystemUtils.isLinuxPlatform() && maxDatagramPacketLength > 9216) {
            throw new IllegalArgumentException(
                "The maxDatagramPacketLength could not be larger than 9216 bytes on linux");
        }
        else if (maxDatagramPacketLength > 65507) {
            throw new IllegalArgumentException("The maxDatagramPacketLength could not be larger than 65507 bytes");
        }
        this.maxDatagramPacketLength = maxDatagramPacketLength;
    }


    public void closeChannel(Selector selector) throws IOException {
        if (this.udpSession != null && !this.udpSession.isClosed()) {
            this.udpSession.close();
            this.udpSession = null;
        }
        if (this.channel != null) {
            this.channel.close();
            this.channel = null;
        }
        selector.selectNow();
    }


    protected void buildUDPSession() {
        Queue<WriteMessage> queue = buildQueue();
        this.udpSession = new NioUDPSession(buildSessionConfig(this.channel, queue), this.maxDatagramPacketLength);
        this.selectorManager.registerSession(this.udpSession, EventType.ENABLE_READ);
        this.udpSession.start();
    }


    /**
     * ����Socketѡ��
     * @throws IOException
     * @throws SocketException
     * @throws ClosedChannelException
     */
    protected void buildDatagramChannel() throws IOException, SocketException, ClosedChannelException {
        this.channel = DatagramChannel.open();
        this.channel.socket().setSoTimeout(this.soTimeout);

        if (this.socketOptions.get(StandardSocketOption.SO_REUSEADDR) != null) {
            this.channel.socket().setReuseAddress(
                StandardSocketOption.SO_REUSEADDR.type()
                    .cast(this.socketOptions.get(StandardSocketOption.SO_REUSEADDR)));
        }
        if (this.socketOptions.get(StandardSocketOption.SO_RCVBUF) != null) {
            this.channel.socket().setReceiveBufferSize(
                StandardSocketOption.SO_RCVBUF.type().cast(this.socketOptions.get(StandardSocketOption.SO_RCVBUF)));

        }
        if (this.socketOptions.get(StandardSocketOption.SO_SNDBUF) != null) {
            this.channel.socket().setSendBufferSize(
                StandardSocketOption.SO_SNDBUF.type().cast(this.socketOptions.get(StandardSocketOption.SO_SNDBUF)));
        }
        this.channel.configureBlocking(false);
        if (this.localSocketAddress != null) {
            this.channel.socket().bind(this.localSocketAddress);
        }
        else {
            this.channel.socket().bind(new InetSocketAddress("localhost", 0));
        }
        setLocalSocketAddress((InetSocketAddress) this.channel.socket().getLocalSocketAddress());

    }


    @Override
    protected void dispatchReadEvent(SelectionKey key) {
        if (this.udpSession != null) {
            this.udpSession.onEvent(EventType.READABLE, key.selector());
        }
        else {
            log.warn("NO session to dispatch read event");
        }

    }


    @Override
    protected void dispatchWriteEvent(SelectionKey key) {
        if (this.udpSession != null) {
            this.udpSession.onEvent(EventType.WRITEABLE, key.selector());
        }
        else {
            log.warn("NO session to dispatch write event");
        }

    }

}
