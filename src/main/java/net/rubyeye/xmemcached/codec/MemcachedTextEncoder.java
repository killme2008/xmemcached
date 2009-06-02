package net.rubyeye.xmemcached.codec;

import java.nio.ByteBuffer;

import net.rubyeye.xmemcached.command.Command;

import com.google.code.yanf4j.nio.Session;
import com.google.code.yanf4j.nio.CodecFactory.Encoder;
/**
 * memcached text protocol encoder
 * @author dennis
 *
 */
public class MemcachedTextEncoder implements Encoder<Command> {

	@Override
	public ByteBuffer encode(Command message, Session session) {
		return message.getIoBuffer().getByteBuffer();
	}

}
