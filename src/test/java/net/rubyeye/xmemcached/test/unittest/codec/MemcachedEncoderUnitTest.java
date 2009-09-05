package net.rubyeye.xmemcached.test.unittest.codec;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.codec.MemcachedCodecFactory;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.ServerAddressAware;
import net.rubyeye.xmemcached.command.TextCommandFactory;

import com.google.code.yanf4j.core.CodecFactory.Encoder;

public class MemcachedEncoderUnitTest extends TestCase {
	private Encoder<Command> encoder;

	public void testEncode() {
		this.encoder = new MemcachedCodecFactory().getEncoder();
		Command command = new TextCommandFactory().createVersionCommand(
				new CountDownLatch(1), null);
		command.encode(new SimpleBufferAllocator());
		ByteBuffer buffer = this.encoder.encode(command, null);
		assertEquals(buffer, ServerAddressAware.VERSION);
	}

}
