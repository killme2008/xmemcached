package net.rubyeye.xmemcached.test.unittest;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeoutException;
import net.rubyeye.xmemcached.KeyProvider;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.MemcachedClientCallable;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.utils.AddrUtil;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class XMemcachedClientWithKeyProviderIT extends XMemcachedClientIT {

  private KeyProvider keyProvider;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    keyProvider = new KeyProvider() {

      public String process(String key) {
        return "prefix-" + key;
      }
    };
  }

  @Override
  public MemcachedClientBuilder createBuilder() throws Exception {

    MemcachedClientBuilder builder = new XMemcachedClientBuilder(
        AddrUtil.getAddresses(this.properties.getProperty("test.memcached.servers")));
    ByteUtils.testing = true;
    return builder;
  }

  @Override
  public MemcachedClientBuilder createWeightedBuilder() throws Exception {
    List<InetSocketAddress> addressList =
        AddrUtil.getAddresses(this.properties.getProperty("test.memcached.servers"));
    int[] weights = new int[addressList.size()];
    for (int i = 0; i < weights.length; i++) {
      weights[i] = i + 1;
    }

    MemcachedClientBuilder builder = new XMemcachedClientBuilder(addressList, weights);
    ByteUtils.testing = true;
    return builder;
  }

  public void testKeyProvider() {
    String process = keyProvider.process("namespace:a");
    assertEquals("prefix-namespace:a", process);
  }

  public void testWithNamespaceAndKeyProvider() throws Exception {
    memcachedClient.setKeyProvider(keyProvider);
    memcachedClient.withNamespace("a", new MemcachedClientCallable<Void>() {

      public Void call(MemcachedClient client)
          throws MemcachedException, InterruptedException, TimeoutException {
        client.set("name", 0, "Mike Liu");
        return null;
      }
    });

    memcachedClient.invalidateNamespace("a");

    Object result = memcachedClient.withNamespace("a", new MemcachedClientCallable<Object>() {
      public Object call(MemcachedClient client)
          throws MemcachedException, InterruptedException, TimeoutException {
        return memcachedClient.get("name");
      }
    });

    assertNull(result);
  }

}
