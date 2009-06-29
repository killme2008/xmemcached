package net.rubyeye.xmemcached.codec;

import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.utils.ByteUtils;

import com.google.code.yanf4j.nio.Session;
import com.google.code.yanf4j.nio.CodecFactory.Decoder;
import com.google.code.yanf4j.util.ByteBufferMatcher;
import com.google.code.yanf4j.util.ShiftAndByteBufferMatcher;

/**
 * Memcached protocol decoder
 * 
 * @author dennis
 * 
 */
public class MemcachedDecoder implements Decoder<Command> {

	public static final Log log = LogFactory.getLog(MemcachedDecoder.class);

	public MemcachedDecoder() {
		super();
	}

	/**
	 * shift-and algorithm for ByteBuffer's match
	 */
	public static final ByteBufferMatcher SPLIT_MATCHER = new ShiftAndByteBufferMatcher(
			ByteUtils.SPLIT);

	@Override
	public Command decode(ByteBuffer buffer, Session origSession) {
		MemcachedTCPSession session = (MemcachedTCPSession) origSession;
		if (session.peekCurrentExecutingCommand() == null)
			return null;
		if (session.peekCurrentExecutingCommand().decode(session, buffer)) {
			return session.pollCurrentExecutingCommand();
		} else
			return null;

	}
}
