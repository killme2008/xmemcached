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
package com.google.code.yanf4j.nio;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.util.concurrent.Future;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.CodecFactory;
import com.google.code.yanf4j.core.Handler;
import com.google.code.yanf4j.core.impl.FutureImpl;
import com.google.code.yanf4j.nio.impl.DatagramChannelController;
import com.google.code.yanf4j.nio.impl.NioUDPSession;




/**
 * Controller for upd client
 * 
 * @author dennis
 * 
 */
public class UDPConnectorController extends DatagramChannelController implements SingleConnector {

    protected SocketAddress remoteAddress;


    public synchronized Future<Boolean> connect(SocketAddress remoteAddress) throws IOException {
        if (remoteAddress == null) {
            throw new NullPointerException("Null remoteAddress");
        }
        this.remoteAddress = remoteAddress;
        if (!isStarted()) {
            start();
        }
        this.channel.connect(remoteAddress);
        FutureImpl<Boolean> result = new FutureImpl<Boolean>();
        result.setResult(true);
        return result;

    }


    public boolean isConnected() {
        return this.udpSession != null && !this.udpSession.isClosed();
    }


    public SocketAddress getRemoteAddress() {
        return this.remoteAddress;
    }


    public void disconnect() throws IOException {
        if (this.channel != null) {
            this.channel.disconnect();
        }
        this.remoteAddress = null;
        stop();
    }


    public void awaitConnectUnInterrupt() throws IOException {
        // do nothing
    }


    public Future<Boolean> send(Object msg) {
        throw new UnsupportedOperationException("Please use send(DatagramPacket) insead");
    }


    public Future<Boolean> send(DatagramPacket packet) {
        if (!this.started) {
            throw new IllegalStateException("Controller has been stopped");
        }
        if (packet == null) {
            throw new NullPointerException("Null package");
        }
        if (this.remoteAddress != null && packet.getAddress() == null) {
            packet.setSocketAddress(this.remoteAddress);
        }
        if (this.remoteAddress == null && packet.getAddress() == null) {
            throw new IllegalArgumentException("Null targetAddress");
        }

        return ((NioUDPSession) this.udpSession).asyncWrite(packet);
    }


    public Future<Boolean> send(SocketAddress targetAddr, Object msg) {
        return ((NioUDPSession) this.udpSession).asyncWrite(targetAddr, msg);
    }


    public UDPConnectorController(Configuration configuration) {
        super(configuration, null, null);
        setMaxDatagramPacketLength(configuration.getSessionReadBufferSize());
    }


    public UDPConnectorController() {
        super();
    }


    public UDPConnectorController(Configuration configuration, CodecFactory codecFactory) {
        super(configuration, null, codecFactory);
        setMaxDatagramPacketLength(configuration.getSessionReadBufferSize());
    }


    public UDPConnectorController(Configuration configuration, Handler handler, CodecFactory codecFactory) {
        super(configuration, handler, codecFactory);
        setMaxDatagramPacketLength(configuration.getSessionReadBufferSize());
    }
}
