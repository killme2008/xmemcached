package com.google.code.yanf4j.nio.impl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.Future;

import com.google.code.yanf4j.buffer.IoBuffer;
import com.google.code.yanf4j.core.EventType;
import com.google.code.yanf4j.core.UDPSession;
import com.google.code.yanf4j.core.WriteMessage;
import com.google.code.yanf4j.core.impl.ByteBufferCodecFactory;
import com.google.code.yanf4j.core.impl.FutureImpl;
import com.google.code.yanf4j.core.impl.UDPHandlerAdapter;
import com.google.code.yanf4j.core.impl.WriteMessageImpl;
import com.google.code.yanf4j.nio.NioSessionConfig;
import com.google.code.yanf4j.statistics.impl.DefaultStatistics;




/**
 * Nio UDP���ӷ�װ
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����06:10:25
 */
public class NioUDPSession extends AbstractNioSession implements UDPSession {

    public NioUDPSession(NioSessionConfig sessionConfig, int maxDatagramPacketLength) {
        super(sessionConfig);
        setReadBuffer(IoBuffer.allocate(maxDatagramPacketLength));
        onCreated();
    }


    @Override
    protected Object writeToChannel(final WriteMessage msg) throws IOException {
        // Check if it is canceled
        if (msg.getWriteFuture() != null && !msg.isWriting() && msg.getWriteFuture().isCancelled()) {
            return msg.getMessage();
        }
        UDPWriteMessage message = (UDPWriteMessage) msg;
        IoBuffer gatherBuffer = message.getWriteBuffer();
        int length = gatherBuffer.remaining();
        // begin writing
        msg.writing();
        while (gatherBuffer.hasRemaining()) {
            ((DatagramChannel) this.selectableChannel).send(gatherBuffer.buf(), message.getTargetAddress());
        }
        this.statistics.statisticsWrite(length);
        this.scheduleWritenBytes.addAndGet(0 - length);
        if (message.getWriteFuture() != null) {
            message.getWriteFuture().setResult(Boolean.TRUE);
        }
        return message.getMessage();
    }


    @Override
    public Future<Boolean> asyncWrite(Object packaet) {
        if (packaet instanceof DatagramPacket) {
            if (isClosed()) {
                FutureImpl<Boolean> future = new FutureImpl<Boolean>();
                future.failure(new IOException("�����Ѿ����ر�"));
                return future;
            }
            FutureImpl<Boolean> future = new FutureImpl<Boolean>();
            WriteMessage message = wrapMessage(packaet, future);
            this.scheduleWritenBytes.addAndGet(message.getWriteBuffer().remaining());
            write0(message);
            return future;
        }
        else {
            throw new IllegalArgumentException("UDP session must write DatagramPacket");
        }

    }


    @Override
    public void write(Object packet) {
        if (packet instanceof DatagramPacket) {
            if (isClosed()) {
                return;
            }
            WriteMessage message = wrapMessage(packet, null);
            this.scheduleWritenBytes.addAndGet(message.getWriteBuffer().remaining());
            write0(message);
        }
        else {
            throw new IllegalArgumentException("UDP session must write DatagramPacket");
        }
    }


    @Override
    protected WriteMessage wrapMessage(Object obj, Future<Boolean> writeFuture) {
        DatagramPacket packet = (DatagramPacket) obj;
        WriteMessage message =
                new UDPWriteMessage(packet.getSocketAddress(), packet.getData(), (FutureImpl<Boolean>) writeFuture);
        return message;
    }


    public Future<Boolean> asyncWrite(SocketAddress targetAddr, Object msg) {
        if (isClosed()) {
            throw new IllegalStateException("Closed session");
        }
        FutureImpl<Boolean> future = new FutureImpl<Boolean>();
        WriteMessage message = new UDPWriteMessage(targetAddr, msg, future);
        this.scheduleWritenBytes.addAndGet(message.getWriteBuffer().remaining());
        write0(message);
        return future;
    }


    public void write(SocketAddress targetAddr, Object packet) {
        if (isClosed()) {
            throw new IllegalStateException("Closed session");
        }
        WriteMessage message = new UDPWriteMessage(targetAddr, packet, null);
        this.scheduleWritenBytes.addAndGet(message.getWriteBuffer().remaining());
        write0(message);
    }


    @Override
    protected synchronized void readFromBuffer() {
        if (this.closed) {
            return;
        }
        this.readBuffer.clear();
        try {
            decode();
            this.selectorManager.registerSession(this, EventType.ENABLE_READ);
        }
        catch (Throwable e) {
            log.error("Read from buffer error", e);
            onException(e);
            close();
        }
    }


    @Override
    public void decode() {
        try {
            SocketAddress address = ((DatagramChannel) this.selectableChannel).receive(this.readBuffer.buf());
            this.readBuffer.flip();
            this.statistics.statisticsRead(this.readBuffer.remaining());
            if (address != null) {
                if (!(this.decoder instanceof ByteBufferCodecFactory.ByteBufferDecoder)) {
                    Object msg = this.decoder.decode(this.readBuffer, this);
                    if (msg != null) {
                        dispatchReceivedMessage(address, msg);
                    }
                }
                else {
                    byte[] bytes = new byte[this.readBuffer.remaining()];
                    this.readBuffer.get(bytes);
                    DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length, address);
                    dispatchReceivedMessage(datagramPacket);
                }
            }
        }
        catch (IOException e) {
            onException(e);
            log.error("Decode error", e);
        }
        catch (Throwable e) {
            log.error("Decode error", e);
            onException(e);
            close();
        }
    }


    protected void dispatchReceivedMessage(SocketAddress address, Object message) {
        long start = -1;
        if (!(this.statistics instanceof DefaultStatistics)) {
            start = System.currentTimeMillis();
        }
        if (this.handler instanceof UDPHandlerAdapter) {
            ((UDPHandlerAdapter) this.handler).onMessageReceived(this, address, message);
        }
        else {
            this.handler.onMessageReceived(this, message);
        }
        if (start != -1) {
            this.statistics.statisticsProcess(System.currentTimeMillis() - start);
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.google.code.yanf4j.nio.TCPHandler#getRemoteSocketAddress()
     */
    public InetSocketAddress getRemoteSocketAddress() {
        throw new UnsupportedOperationException();
    }

    /**
     * UDP��Ϣ��װ������һ��Ŀ���ַ
     * 
     * 
     * 
     * @author boyan
     * 
     * @since 1.0, 2009-12-16 ����06:10:42
     */
    class UDPWriteMessage extends WriteMessageImpl {

        private final SocketAddress targetAddress;


        private UDPWriteMessage(SocketAddress targetAddress, Object message, FutureImpl<Boolean> writeFuture) {
            super(message, writeFuture);
            this.targetAddress = targetAddress;
            if (message instanceof byte[]) {
                this.buffer = IoBuffer.wrap((byte[]) message);
            }
            else {
                this.buffer = NioUDPSession.this.encoder.encode(message, NioUDPSession.this);
            }
        }


        public SocketAddress getTargetAddress() {
            return this.targetAddress;
        }
    }


    @Override
    public boolean isUseBlockingWrite() {
        return false;
    }


    @Override
    public void setUseBlockingWrite(boolean useBlockingWrite) {
        throw new UnsupportedOperationException();
    }


    @Override
    public boolean isUseBlockingRead() {
        return false;
    }


    @Override
    public void setUseBlockingRead(boolean useBlockingRead) {
        throw new UnsupportedOperationException();
    }


    public DatagramSocket socket() {
        return ((DatagramChannel) this.selectableChannel).socket();
    }
}
