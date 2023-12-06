package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import com.google.code.yanf4j.buffer.IoBuffer;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.networking.MemcachedSession;
import net.rubyeye.xmemcached.utils.ByteUtils;

/**
 * AWS ElasticCache and GCP Auto Discovery config command.
 *
 * @see <a href=
 *      "http://docs.aws.amazon.com/AmazonElastiCache/latest/UserGuide/AutoDiscovery.AddingToYourClientLibrary.html">Adding
 *      AWS Auto Discovery To Your Client Library</a>
 * @see <a href=
 *      "https://cloud.google.com/memorystore/docs/memcached/about-auto-discovery">Adding
 *      GCP Auto Discovery To Your Client Library</a>
 * Only supports Cache Engine version 1.4.14 or higher.
 * 
 * @author dennis
 * 
 */
public class TextAutoDiscoveryCacheConfigCommand extends Command {

  private String key;

  private String subCommand;

  public TextAutoDiscoveryCacheConfigCommand(final CountDownLatch latch, String subCommand,
      String key) {
    super(subCommand + key, CommandType.AUTO_DISCOVERY_CONFIG, latch);
    this.key = key;
    this.subCommand = subCommand;
    this.result = new StringBuilder();
  }

  @Override
  public boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
    String line = null;
    while ((line = ByteUtils.nextLine(buffer)) != null) {
      if (line.equals("END")) { // at the end
        return done(session);
      } else if (line.startsWith("CONFIG")) {
        // ignore
      } else {
        ((StringBuilder) this.getResult()).append(line);
      }
    }
    return false;
  }

  private final boolean done(MemcachedSession session) {
    setResult(this.getResult().toString());
    countDownLatch();
    return true;
  }

  @Override
  public void encode() {
    // config [sub-command] [key]
    final byte[] subCmdBytes = ByteUtils.getBytes(this.subCommand);
    final byte[] keyBytes = ByteUtils.getBytes(this.key);
    this.ioBuffer = IoBuffer.allocate(6 + 1 + subCmdBytes.length + 1 + keyBytes.length + 2);
    ByteUtils.setArguments(this.ioBuffer, "config", subCmdBytes, keyBytes);
    this.ioBuffer.flip();
  }

}
