package net.rubyeye.xmemcached.aws;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AWS ElastiCache configuration poller
 * 
 * @author dennis
 * 
 */
public class ConfigurationPoller implements Runnable {

  /**
   * Return current ClusterConfigration.
   * 
   * @return
   */
  public ClusterConfigration getClusterConfiguration() {
    return clusterConfigration;
  }

  private final AtomicInteger serverOrderCounter = new AtomicInteger(0);

  private Map<String, Integer> ordersMap = new HashMap<String, Integer>();

  public synchronized int getCacheNodeOrder(CacheNode node) {
    Integer order = this.ordersMap.get(node.getCacheKey());
    if (order != null) {
      return order;
    }
    order = this.serverOrderCounter.incrementAndGet();
    this.ordersMap.put(node.getCacheKey(), order);
    return order;
  }

  public synchronized void removeCacheNodeOrder(CacheNode node) {
    this.ordersMap.remove(node.getCacheKey());
  }

  private static final Logger log = LoggerFactory.getLogger(ConfigurationPoller.class);

  private final AWSElasticCacheClient client;

  private final long pollIntervalMills;

  private ScheduledExecutorService scheduledExecutorService;

  private volatile ClusterConfigration clusterConfigration = null;

  public ConfigurationPoller(AWSElasticCacheClient client, long pollIntervalMills) {
    super();
    this.client = client;
    this.pollIntervalMills = pollIntervalMills;
    this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

      public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "AWSElasticCacheConfigPoller");
        t.setDaemon(true);
        if (t.getPriority() != Thread.NORM_PRIORITY) {
          t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
      }
    });
  }

  public void start() {
    this.scheduledExecutorService.scheduleWithFixedDelay(this, this.pollIntervalMills,
        this.pollIntervalMills, TimeUnit.MILLISECONDS);
  }

  public void stop() {
    this.scheduledExecutorService.shutdown();
  }

  public void run() {
    try {
      ClusterConfigration newConfig = this.client.getConfig();
      if (newConfig != null) {
        ClusterConfigration currentConfig = this.clusterConfigration;
        if (currentConfig == null) {
          this.clusterConfigration = newConfig;
        } else {
          if (newConfig.getVersion() < currentConfig.getVersion()) {
            log.warn("Ignored new config from ElasticCache node, it's too old, current version is: "
                + currentConfig.getVersion() + ", but the new version is: "
                + newConfig.getVersion());
            return;
          } else {
            this.clusterConfigration = newConfig;
          }
        }
        log.info("Retrieved new  config from ElasticCache node: " + this.clusterConfigration);
        this.client.onUpdate(newConfig);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      log.error("Poll config from ElasticCache node failed", e);
    }
  }
}
