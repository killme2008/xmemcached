package com.google.code.yanf4j.nio.impl;

/**
 *Copyright [2008-2009] [dennis zhuang]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.net.DatagramSocket;

import com.google.code.yanf4j.nio.CodecFactory;
import com.google.code.yanf4j.nio.Handler;
import com.google.code.yanf4j.nio.UDPSession;
import com.google.code.yanf4j.nio.impl.ByteBufferCodecFactory.ByteBufferDecoder;
import com.google.code.yanf4j.nio.util.EventType;
import com.google.code.yanf4j.statistics.Statistics;
import com.google.code.yanf4j.statistics.impl.DefaultStatistics;
import com.google.code.yanf4j.util.ByteBufferUtils;
import com.google.code.yanf4j.util.Queue;
import com.google.code.yanf4j.nio.Session;



public class DefaultUDPSession extends AbstractSession implements UDPSession {
	@SuppressWarnings("unchecked")
	public DefaultUDPSession(SelectionKey sk, DatagramChannel sc,
			SessionEventManager reactor, Handler handler,
			int maxDatagramPacketLength, Statistics statistics,
			CodecFactory codecFactory, Queue<Session.WriteMessage> queue,
			boolean handleReadWriteConcurrently) {
		super(sc, sk, handler, reactor, codecFactory, statistics, queue,
				handleReadWriteConcurrently);
		this.readBuffer = ByteBuffer.allocate(maxDatagramPacketLength);
		this.handler.onSessionCreated(this);
	}

	@SuppressWarnings("unchecked")
	protected Object writeToChannel(SelectableChannel channel,
			Session.WriteMessage obj) throws IOException {
		WriteMessage message = (WriteMessage) obj;
		ByteBuffer gatherBuffer = ByteBufferUtils.gather(message.buffers);
		int length = gatherBuffer.remaining();
		while (gatherBuffer.hasRemaining()) {
			((DatagramChannel) selectableChannel).send(gatherBuffer, message
					.getTargetAddress());
		}
		statistics.statisticsWrite(length);
		return message.message;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.code.yanf4j.nio.TCPHandler#send(com.google.code.yanf4j.protocol.Packet)
	 */
	public boolean send(Object msg) throws InterruptedException {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unchecked")
	public boolean send(DatagramPacket packet) throws InterruptedException {
		if (isClose())
			return false;
		Object message = wrapMessage(packet);
		writeQueue.getLock().lock();
		try {
			if (writeQueue.isEmpty()) {
				if (writeQueue.push(message)) {
					sessionEventManager.register(this, EventType.ENABLE_WRITE); // 列表为空，注册监听写事件
					return true;
				} else
					return false;

			} else
				return writeQueue.push(message);
		} finally {
			writeQueue.getLock().unlock();
			selectionKey.selector().wakeup();
		}
	}

	protected WriteMessage wrapMessage(Object obj) {
		DatagramPacket packet = (DatagramPacket) obj;
		WriteMessage message = new WriteMessage(packet.getSocketAddress(),
				packet.getData());
		return message;
	}

	@SuppressWarnings("unchecked")
	public boolean send(SocketAddress targetAddr, Object msg)
			throws InterruptedException {
		if (isClose())
			return false;
		WriteMessage message = new WriteMessage(targetAddr, msg);
		writeQueue.getLock().lock();
		try {
			if (writeQueue.isEmpty()) {
				if (writeQueue.push(message)) {
					sessionEventManager.register(this, EventType.ENABLE_WRITE); // 列表为空，注册监听写事件
					return true;
				} else
					return false;
			} else
				return writeQueue.push(message);
		} finally {
			writeQueue.getLock().unlock();
			selectionKey.selector().wakeup();
		}

	}

	protected void readFromBuffer() {
		if (closed)
			return;
		readBuffer.clear();
		try {
			SocketAddress address = ((DatagramChannel) selectableChannel)
					.receive(readBuffer);
			readBuffer.flip();
			statistics.statisticsRead(readBuffer.remaining());
			if (address != null) {
				if (!(this.decoder instanceof ByteBufferDecoder)) {
					Object msg = this.decoder.decode(readBuffer);
					if (msg != null)
						dispatchReceivedMessage(address, msg);
				} else {
					byte[] bytes = new byte[readBuffer.remaining()];
					readBuffer.get(bytes);
					DatagramPacket datagramPacket = new DatagramPacket(bytes,
							bytes.length, address);
					dispatchReceivedMessage(datagramPacket);
				}
			}
			sessionEventManager.register(this, EventType.ENABLE_READ);
			setSessionStatus(SessionStatus.IDLE);
		} catch (IOException e) {
			handler.onException(this, e);
			log.error("read error", e);
		} catch (Throwable e) {
			log.error(e);
			handler.onException(this, e);
			close();
		}
	}

	@SuppressWarnings("unchecked")
	protected void dispatchReceivedMessage(SocketAddress address, Object pkt) {
		long start = -1;
		if (!(this.statistics instanceof DefaultStatistics))
			start = System.currentTimeMillis();
		((UDPHandlerAdapter) handler).onReceive(this, address, pkt);
		if (start != -1) {
			this.statistics.statisticsProcess(System.currentTimeMillis()
					- start);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.code.yanf4j.nio.TCPHandler#getRemoteSocketAddress()
	 */
	public InetSocketAddress getRemoteSocketAddress() {
		return null;
	}

	/**
	 * 类说明: Write Message，包装Message，带一个缓冲区
	 * 
	 * @author dennis zhuang
	 */
	class WriteMessage extends Session.WriteMessage {

		private SocketAddress targetAddress;

		@SuppressWarnings("unchecked")
		private WriteMessage(SocketAddress targetAddress, Object message) {
			super(message);
			this.targetAddress = targetAddress;
			if (message instanceof byte[])
				this.buffers = new ByteBuffer[] { ByteBuffer
						.wrap((byte[]) message) };
			else
				this.buffers = encoder.encode(message);
		}

		public SocketAddress getTargetAddress() {
			return targetAddress;
		}
	}

	public boolean isUseBlockingWrite() {
		return false;
	}

	public void setUseBlockingWrite(boolean useBlockingWrite) {
		throw new UnsupportedOperationException();
	}

	public boolean isUseBlockingRead() {
		return false;
	}

	public void setUseBlockingRead(boolean useBlockingRead) {
		throw new UnsupportedOperationException();
	}

	public DatagramSocket getSocket() {
		return ((DatagramChannel) this.selectableChannel).socket();
	}

}
