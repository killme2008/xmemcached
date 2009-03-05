/**
 *Copyright [2008] [dennis zhuang]
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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import com.google.code.yanf4j.nio.util.EventType;
import com.google.code.yanf4j.util.ByteBufferUtils;

/**
 * 会话接口
 * 
 * @author dennis
 * 
 */
public interface Session {

	/**
	 * 类说明: Write Message，包装Message，带一个缓冲区
	 * 
	 * @author dennis zhuang
	 */
	public static class WriteMessage implements Message {

		public Object message;

		public ByteBuffer[] buffers;

		public WriteMessage(Object message) {
			this.message = message;
		}

		public ByteBuffer[] getBuffers() {
			return buffers;
		}

		public Object getMessage() {
			return message;
		}

		public int getLength() {
			if (this.buffers != null)
				return ByteBufferUtils.remaining(buffers);
			return 0;
		}

	}

	public void start();

	public void onEvent(EventType event, java.nio.channels.Selector selector);

	public boolean send(Object packet) throws InterruptedException;

	public void attach(Object obj);

	public Object attachment();

	public boolean isClose();

	public void close();

	public InetSocketAddress getRemoteSocketAddress();

	public boolean isUseBlockingWrite();

	public void setUseBlockingWrite(boolean useBlockingWrite);

	public boolean isUseBlockingRead();

	public void setUseBlockingRead(boolean useBlockingRead);

	public void flush() throws IOException, InterruptedException;

	public boolean isExpired();

	@SuppressWarnings("unchecked")
	public CodecFactory.Encoder getEncoder();

	@SuppressWarnings("unchecked")
	public void setEncoder(CodecFactory.Encoder encoder);

	@SuppressWarnings("unchecked")
	public CodecFactory.Decoder getDecoder();

	@SuppressWarnings("unchecked")
	public void setDecoder(CodecFactory.Decoder decoder);

	@SuppressWarnings("unchecked")
	public boolean send(Object msg, long timeout) throws InterruptedException;

	public boolean isHandleReadWriteConcurrently();

	public void setHandleReadWriteConcurrently(
			boolean handleReadWriteConcurrently);

	public ByteOrder getReadBufferByteOrder();

	public void setReadBufferByteOrder(ByteOrder readBufferByteOrder);

	public long transferTo(long position, long count, FileChannel target)
			throws IOException;

	public long transferFrom(long position, long count, FileChannel source)
			throws IOException;
}