package net.rubyeye.xmemcached.command.kestrel;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import net.rubyeye.xmemcached.command.text.TextDeleteCommand;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.utils.ByteUtils;

/**
 * @deprecated Kestrel support is deprecated and will be removed in a future version.
 */
@Deprecated
public class KestrelDeleteCommand extends TextDeleteCommand {

  public KestrelDeleteCommand(String key, byte[] keyBytes, int time, CountDownLatch latch,
      boolean noreply) {
    super(key, keyBytes, time, latch, noreply);
  }

  @Override
  public boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
    if (buffer == null || !buffer.hasRemaining()) {
      return false;
    }
    if (this.result == null) {
      if (buffer.remaining() < 2) {
        return false;
      }
      byte first = buffer.get(buffer.position());
      byte second = buffer.get(buffer.position() + 1);
      if (first == 'E' && second == 'N') {
        this.setResult(Boolean.TRUE);
        this.countDownLatch();
        // END\r\n
        return ByteUtils.stepBuffer(buffer, 5);
      } else if (first == 'D' && second == 'E') {
        this.setResult(Boolean.TRUE);
        this.countDownLatch();
        // DELETED\r\n
        return ByteUtils.stepBuffer(buffer, 9);
      } else {
        return this.decodeError(session, buffer);
      }
    } else {
      Boolean result = (Boolean) this.result;
      if (result) {
        // END\r\n
        return ByteUtils.stepBuffer(buffer, 5);
      } else {
        return this.decodeError(session, buffer);
      }
    }
  }

}
