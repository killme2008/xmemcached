package net.rubyeye.xmemcached.test.unittest.codec;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;
import net.rubyeye.xmemcached.codec.MemcachedCodecFactory;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.TextCommandFactory;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;

import com.google.code.yanf4j.buffer.IoBuffer;
import com.google.code.yanf4j.core.CodecFactory.Decoder;
import com.google.code.yanf4j.core.impl.ByteBufferCodecFactory;
import com.google.code.yanf4j.core.impl.HandlerAdapter;
import com.google.code.yanf4j.nio.NioSessionConfig;

public class MemcachedDecoderUnitTest extends TestCase {
	private Decoder decoder;

	public void testDecode() {
		decoder = new MemcachedCodecFactory().getDecoder();
		MemcachedTCPSession session = buildSession();
		Command versionCommand = new TextCommandFactory().createVersionCommand(
				new CountDownLatch(1), null);
		session.addCommand(versionCommand);
		Command decodedCommand = (Command) decoder.decode(IoBuffer
				.wrap(ByteBuffer.wrap("VERSION 1.28\r\n".getBytes())), session);
		assertSame(decodedCommand, versionCommand);
		assertEquals("1.28", decodedCommand.getResult());
	}

	public MemcachedTCPSession buildSession() {
		NioSessionConfig sessionConfig = new NioSessionConfig(null,
				new HandlerAdapter(), null, new ByteBufferCodecFactory(), null,
				null, null, true, 0, 0);
		return new MemcachedTCPSession(sessionConfig, 16 * 1024, null, 0,
				new TextCommandFactory());
	}
}
