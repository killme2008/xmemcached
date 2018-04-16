package net.rubyeye.xmemcached.aws;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.utils.AddrUtil;

/**
 * AWSElasticCacheClient builder.
 * 
 * @author dennis
 * 
 */
public class AWSElasticCacheClientBuilder extends XMemcachedClientBuilder {

  /**
   * Returns pollConfigIntervalMs.
   * 
   * @return
   */
  public long getPollConfigIntervalMs() {
    return pollConfigIntervalMs;
  }

  /**
   * Set poll config interval in milliseconds.
   * 
   * @param pollConfigIntervalMs
   */
  public void setPollConfigIntervalMs(long pollConfigIntervalMs) {
    this.pollConfigIntervalMs = pollConfigIntervalMs;
  }

  /**
   * Returns initial ElasticCache server addresses.
   * 
   * @return
   */
  public List<InetSocketAddress> getConfigAddrs() {
    return configAddrs;
  }

  /**
   * Set initial ElasticCache server addresses.
   * 
   * @param configAddrs
   */
  public void setConfigAddrs(List<InetSocketAddress> configAddrs) {
    this.configAddrs = configAddrs;
  }

  private List<InetSocketAddress> configAddrs;

  private long pollConfigIntervalMs = AWSElasticCacheClient.DEFAULT_POLL_CONFIG_INTERVAL_MS;

  /**
   * Create a builder with an initial ElasticCache server list string in the form of "host:port
   * host2:port".
   * 
   * @param serverList server list string in the form of "host:port host2:port"
   */
  public AWSElasticCacheClientBuilder(String serverList) {
    this(AddrUtil.getAddresses(serverList));
  }

  /**
   * Create a builder with an initial ElasticCache server.
   * 
   * @param addr
   */
  public AWSElasticCacheClientBuilder(InetSocketAddress addr) {
    this(asList(addr));
  }

  private static List<InetSocketAddress> asList(InetSocketAddress addr) {
    List<InetSocketAddress> ret = new ArrayList<InetSocketAddress>();
    ret.add(addr);
    return ret;
  }

  /**
   * Create a builder with initial ElasticCache server addresses.
   * 
   * @param configAddrs
   */
  public AWSElasticCacheClientBuilder(List<InetSocketAddress> configAddrs) {
    super(configAddrs);
    this.configAddrs = configAddrs;
  }

  /**
   * Returns a new instanceof AWSElasticCacheClient.
   */
  @Override
  public AWSElasticCacheClient build() throws IOException {

    AWSElasticCacheClient memcachedClient = new AWSElasticCacheClient(this.sessionLocator,
        this.bufferAllocator, this.configuration, this.socketOptions, this.commandFactory,
        this.transcoder, this.stateListeners, this.authInfoMap, this.connectionPoolSize,
        this.connectTimeout, this.name, this.failureMode, configAddrs, this.pollConfigIntervalMs);
    this.configureClient(memcachedClient);

    return memcachedClient;
  }

}
