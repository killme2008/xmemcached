package net.rubyeye.xmemcached.aws;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.SocketOption;
import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.MemcachedClientStateListener;
import net.rubyeye.xmemcached.MemcachedSessionComparator;
import net.rubyeye.xmemcached.MemcachedSessionLocator;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.auth.AuthInfo;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.TextCommandFactory;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.ArrayMemcachedSessionLocator;
import net.rubyeye.xmemcached.impl.IndexMemcachedSessionComparator;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.InetSocketAddressWrapper;

/**
 * AWS ElasticCache Client.
 * 
 * @since 2.3.0
 * @author dennis
 * 
 */
public class AWSElasticCacheClient extends XMemcachedClient implements ConfigUpdateListener {

  private static final Logger log = LoggerFactory.getLogger(AWSElasticCacheClient.class);

  private boolean firstTimeUpdate = true;

  private List<InetSocketAddress> configAddrs = new ArrayList<InetSocketAddress>();

  public synchronized void onUpdate(ClusterConfigration config) {

    if (firstTimeUpdate) {
      firstTimeUpdate = false;
      removeConfigAddrs();
    }

    List<CacheNode> oldList = this.currentClusterConfiguration != null
        ? this.currentClusterConfiguration.getNodeList() : Collections.EMPTY_LIST;
    List<CacheNode> newList = config.getNodeList();

    List<CacheNode> addNodes = new ArrayList<CacheNode>();
    List<CacheNode> removeNodes = new ArrayList<CacheNode>();

    for (CacheNode node : newList) {
      if (!oldList.contains(node)) {
        addNodes.add(node);
      }
    }

    for (CacheNode node : oldList) {
      if (!newList.contains(node)) {
        removeNodes.add(node);
      }
    }

    // Begin to update server list
    for (CacheNode node : addNodes) {
      try {
        this.connect(new InetSocketAddressWrapper(node.getInetSocketAddress(),
            this.configPoller.getCacheNodeOrder(node), 1, null));
      } catch (IOException e) {
        log.error("Connect to " + node + "failed.", e);
      }
    }

    for (CacheNode node : removeNodes) {
      try {
        this.removeAddr(node.getInetSocketAddress());
      } catch (Exception e) {
        log.error("Remove " + node + " failed.");
      }
    }

    this.currentClusterConfiguration = config;
  }

  private void removeConfigAddrs() {
    for (InetSocketAddress configAddr : this.configAddrs) {
      this.removeAddr(configAddr);
      while (this.getConnector().getSessionByAddress(configAddr) != null
          && this.getConnector().getSessionByAddress(configAddr).size() > 0) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  private final ConfigurationPoller configPoller;

  /**
   * Default elasticcache configuration poll interval, it's one minute.
   */
  public static final long DEFAULT_POLL_CONFIG_INTERVAL_MS = 60000;

  /**
   * Construct an AWSElasticCacheClient instance with one config address and default poll interval.
   * 
   * @since 2.3.0
   * @param addr config server address.
   * @throws IOException
   */
  public AWSElasticCacheClient(InetSocketAddress addr) throws IOException {
    this(addr, DEFAULT_POLL_CONFIG_INTERVAL_MS);
  }

  /**
   * Construct an AWSElasticCacheClient instance with one config address and poll interval.
   * 
   * @since 2.3.0
   * @param addr config server address.
   * @param pollConfigIntervalMills config poll interval in milliseconds.
   * @throws IOException
   */
  public AWSElasticCacheClient(InetSocketAddress addr, long pollConfigIntervalMills)
      throws IOException {
    this(addr, pollConfigIntervalMills, new TextCommandFactory());
  }

  public AWSElasticCacheClient(InetSocketAddress addr, long pollConfigIntervalMills,
      CommandFactory cmdFactory) throws IOException {
    this(asList(addr), pollConfigIntervalMills, cmdFactory);
  }

  private static List<InetSocketAddress> asList(InetSocketAddress addr) {
    List<InetSocketAddress> addrs = new ArrayList<InetSocketAddress>();
    addrs.add(addr);
    return addrs;
  }

  /**
   * Construct an AWSElasticCacheClient instance with config server addresses and default config
   * poll interval.
   * 
   * @since 2.3.0
   * @param addrs config server list.
   * @throws IOException
   */
  public AWSElasticCacheClient(List<InetSocketAddress> addrs) throws IOException {
    this(addrs, DEFAULT_POLL_CONFIG_INTERVAL_MS);
  }

  /**
   * Construct an AWSElasticCacheClient instance with config server addresses.
   * 
   * @since 2.3.0
   * @param addrs
   * @param pollConfigIntervalMills
   * @throws IOException
   */
  public AWSElasticCacheClient(List<InetSocketAddress> addrs, long pollConfigIntervalMills)
      throws IOException {
    this(addrs, pollConfigIntervalMills, new TextCommandFactory());
  }

  /**
   * Construct an AWSElasticCacheClient instance with config server addresses.
   * 
   * @since 2.3.0
   * @param addrs config server list.
   * @param pollConfigIntervalMills config poll interval in milliseconds.
   * @param commandFactory protocol command factory.
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  public AWSElasticCacheClient(List<InetSocketAddress> addrs, long pollConfigIntervalMills,
      CommandFactory commandFactory) throws IOException {
    this(new ArrayMemcachedSessionLocator(), new IndexMemcachedSessionComparator(),
        new SimpleBufferAllocator(), XMemcachedClientBuilder.getDefaultConfiguration(),
        XMemcachedClientBuilder.getDefaultSocketOptions(), new TextCommandFactory(),
        new SerializingTranscoder(), (List<MemcachedClientStateListener>) Collections.EMPTY_LIST,
        (Map<InetSocketAddress, AuthInfo>) Collections.EMPTY_MAP, 1,
        XMemcachedClient.DEFAULT_CONNECT_TIMEOUT, null, true, addrs, pollConfigIntervalMills);

  }

  private static Map<InetSocketAddress, InetSocketAddress> getAddressMapFromConfigAddrs(
      List<InetSocketAddress> configAddrs) {
    Map<InetSocketAddress, InetSocketAddress> m =
        new HashMap<InetSocketAddress, InetSocketAddress>();
    for (InetSocketAddress addr : configAddrs) {
      m.put(addr, null);
    }
    return m;
  }

  AWSElasticCacheClient(MemcachedSessionLocator locator, MemcachedSessionComparator comparator,
      BufferAllocator allocator, Configuration conf, Map<SocketOption, Object> socketOptions,
      CommandFactory commandFactory, Transcoder transcoder,
      List<MemcachedClientStateListener> stateListeners, Map<InetSocketAddress, AuthInfo> map,
      int poolSize, long connectTimeout, String name, boolean failureMode,
      List<InetSocketAddress> configAddrs, long pollConfigIntervalMills) throws IOException {
    super(locator, comparator, allocator, conf, socketOptions, commandFactory, transcoder,
        getAddressMapFromConfigAddrs(configAddrs), stateListeners, map, poolSize, connectTimeout,
        name, failureMode);
    if (pollConfigIntervalMills <= 0) {
      throw new IllegalArgumentException("Invalid pollConfigIntervalMills value.");
    }
    // Use failure mode by default.
    this.commandFactory = commandFactory;
    this.setFailureMode(true);
    this.configAddrs = configAddrs;
    this.configPoller = new ConfigurationPoller(this, pollConfigIntervalMills);
    // Run at once to get config at startup.
    // It will call onUpdate in the same thread.
    this.configPoller.run();
    if (this.currentClusterConfiguration == null) {
      throw new IllegalStateException(
          "Retrieve ElasticCache config from `" + configAddrs.toString() + "` failed.");
    }
    this.configPoller.start();
  }

  private volatile ClusterConfigration currentClusterConfiguration;

  /**
   * Get cluster config from cache node by network command.
   * 
   * @return
   */
  public ClusterConfigration getConfig()
      throws MemcachedException, InterruptedException, TimeoutException {
    return this.getConfig("cluster");
  }

  /**
   * Get config by key from cache node by network command.
   * 
   * @since 2.3.0
   * @return clusetr config.
   */
  public ClusterConfigration getConfig(String key)
      throws MemcachedException, InterruptedException, TimeoutException {
    Command cmd = this.commandFactory.createAWSElasticCacheConfigCommand("get", key);
    final Session session = this.sendCommand(cmd);
    this.latchWait(cmd, opTimeout, session);
    cmd.getIoBuffer().free();
    this.checkException(cmd);
    String result = (String) cmd.getResult();
    if (result == null) {
      throw new MemcachedException("Operation fail,may be caused by networking or timeout");
    }
    return AWSUtils.parseConfiguration(result);
  }

  @Override
  protected void shutdown0() {
    super.shutdown0();
    if (this.configPoller != null) {
      try {
        this.configPoller.stop();
      } catch (Exception e) {
        // ignore
      }
    }
  }

  /**
   * Get the current using configuration in memory.
   * 
   * @since 2.3.0
   * @return current cluster config.
   */
  public ClusterConfigration getCurrentConfig() {
    return this.currentClusterConfiguration;
  }
}
