package net.rubyeye.xmemcached.codec;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.monitor.Constants;
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

	private static final Log log = LogFactory.getLog(MemcachedDecoder.class);

	public MemcachedDecoder() {
		super();
	}

	public static final ByteBuffer SPLIT = ByteBuffer.wrap(Constants.CRLF);

	/**
	 * shift-and algorithm for ByteBuffer's match
	 */
	private static final ByteBufferMatcher SPLIT_MATCHER = new ShiftAndByteBufferMatcher(
			SPLIT);

	/**
	 * 获取下一行
	 * 
	 * @param buffer
	 */
	public static final String nextLine(Session session, ByteBuffer buffer) {
		// if (session.getAttribute(MemcachedTCPSession.CURRENT_LINE_ATTR) !=
		// null) {
		// return (String) session
		// .getAttribute(MemcachedTCPSession.CURRENT_LINE_ATTR);
		// }

		/**
		 * 测试表明采用 Shift-And算法匹配 >BM算法匹配效率 > 朴素匹配 > KMP匹配，
		 * 如果你有更好的建议，请email给我(killme2008@gmail.com)
		 */
		int index = SPLIT_MATCHER.matchFirst(buffer);
		// int index = ByteBufferUtils.indexOf(buffer, SPLIT);
		if (index >= 0) {
			int limit = buffer.limit();
			buffer.limit(index);
			byte[] bytes = new byte[buffer.remaining()];
			buffer.get(bytes);
			buffer.limit(limit);
			buffer.position(index + SPLIT.remaining());
			try {
				String line = new String(bytes, "utf-8");
				// session.setAttribute(MemcachedTCPSession.CURRENT_LINE_ATTR,
				// line);
				return line;
			} catch (UnsupportedEncodingException e) {
				log.error(e, e);

			}

		}
		return null;

	}

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
