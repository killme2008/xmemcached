package net.rubyeye.xmemcached.command.text;

import java.util.HashMap;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.utils.ByteUtils;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextStatsCachedumpCommand extends TextStatsCommand {

  static Pattern itemPattern =
      Pattern.compile("ITEM (?<key>[^ ]+) \\[(?<size>\\d+) b; (?<expire>\\d+) s\\]");

  public TextStatsCachedumpCommand(InetSocketAddress server, CountDownLatch latch, int slabId,
      int limit) {
    super(server, latch, String.format("cachedump %s %s", slabId, limit));
    this.result = new HashMap<String, Integer[]>();
  }

  @Override
  public final boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
    String line = null;
    while ((line = ByteUtils.nextLine(buffer)) != null) {
      if (line.equals("END")) { // at the end
        return super.done(session);
      } else if (line.startsWith("ITEM")) {
        Matcher m = itemPattern.matcher(line);
        if (m.find()) {
          ((Map<String, Integer[]>) getResult())
              .put(m.group("key"), new Integer[]{Integer.parseInt(m.group("size")),
                  Integer.parseInt(m.group("expire"))});
        }
      } else {
        return decodeError(line);
      }
    }
    return false;
  }
}
