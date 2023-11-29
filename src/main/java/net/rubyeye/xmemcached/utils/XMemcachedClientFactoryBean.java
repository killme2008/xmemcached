/**
 * Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)] Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License
 */
/**
 * Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)] Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License
 */
package net.rubyeye.xmemcached.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.KeyProvider;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.MemcachedSessionComparator;
import net.rubyeye.xmemcached.MemcachedSessionLocator;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.auth.AuthInfo;
import net.rubyeye.xmemcached.aws.AWSElasticCacheClient;
import net.rubyeye.xmemcached.aws.AWSElasticCacheClientBuilder;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.command.TextCommandFactory;
import net.rubyeye.xmemcached.impl.ArrayMemcachedSessionLocator;
import net.rubyeye.xmemcached.impl.DefaultKeyProvider;
import net.rubyeye.xmemcached.impl.IndexMemcachedSessionComparator;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import org.springframework.beans.factory.FactoryBean;
import com.google.code.yanf4j.config.Configuration;

/**
 * Implement spring's factory bean,for integrating to spring framework.
 * 
 * @author dennis
 * 
 */
public class XMemcachedClientFactoryBean implements FactoryBean {

  private MemcachedSessionLocator sessionLocator = new ArrayMemcachedSessionLocator();
  private MemcachedSessionComparator sessionComparator = new IndexMemcachedSessionComparator();
  private BufferAllocator bufferAllocator = new SimpleBufferAllocator();
  private String servers;
  private String autoDiscoveryServers;
  private List<Integer> weights;
  @SuppressWarnings("unchecked")
  private Transcoder transcoder = new SerializingTranscoder();
  private Configuration configuration = XMemcachedClientBuilder.getDefaultConfiguration();
  private CommandFactory commandFactory = new TextCommandFactory();

  private Map<InetSocketAddress, AuthInfo> authInfoMap = new HashMap<InetSocketAddress, AuthInfo>();

  private String name;

  private int connectionPoolSize = MemcachedClient.DEFAULT_CONNECTION_POOL_SIZE;

  private MemcachedClient memcachedClient;

  private boolean failureMode;

  private long opTimeout = MemcachedClient.DEFAULT_OP_TIMEOUT;

  private long connectTimeout = MemcachedClient.DEFAULT_CONNECT_TIMEOUT;

  private KeyProvider keyProvider = DefaultKeyProvider.INSTANCE;

  private int maxQueuedNoReplyOperations = MemcachedClient.DEFAULT_MAX_QUEUED_NOPS;

  private long healSessionInterval = MemcachedClient.DEFAULT_HEAL_SESSION_INTERVAL;

  private boolean enableHealSession = true;

  private int timeoutExceptionThreshold = MemcachedClient.DEFAULT_MAX_TIMEOUTEXCEPTION_THRESHOLD;

  private long pollConfigIntervalMs = AWSElasticCacheClient.DEFAULT_POLL_CONFIG_INTERVAL_MS;

  public int getTimeoutExceptionThreshold() {
    return timeoutExceptionThreshold;
  }

  public void setTimeoutExceptionThreshold(int timeoutExceptionThreshold) {
    this.timeoutExceptionThreshold = timeoutExceptionThreshold;
  }

  public long getHealSessionInterval() {
    return healSessionInterval;
  }

  public void setHealSessionInterval(long healSessionInterval) {
    this.healSessionInterval = healSessionInterval;
  }

  public boolean isEnableHealSession() {
    return enableHealSession;
  }

  public void setEnableHealSession(boolean enableHealSession) {
    this.enableHealSession = enableHealSession;
  }

  public long getOpTimeout() {
    return opTimeout;
  }

  public KeyProvider getKeyProvider() {
    return keyProvider;
  }

  public void setKeyProvider(KeyProvider keyProvider) {
    this.keyProvider = keyProvider;
  }

  public void setOpTimeout(long opTimeout) {
    this.opTimeout = opTimeout;
  }

  public final CommandFactory getCommandFactory() {
    return this.commandFactory;
  }

  public final void setCommandFactory(CommandFactory commandFactory) {
    this.commandFactory = commandFactory;
  }

  public XMemcachedClientFactoryBean() {

  }

  public Map<InetSocketAddress, AuthInfo> getAuthInfoMap() {
    return this.authInfoMap;
  }

  public void setAuthInfoMap(Map<InetSocketAddress, AuthInfo> authInfoMap) {
    this.authInfoMap = authInfoMap;
  }

  public boolean isFailureMode() {
    return this.failureMode;
  }

  public void setFailureMode(boolean failureMode) {
    this.failureMode = failureMode;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getConnectionPoolSize() {
    return this.connectionPoolSize;
  }

  public final void setConnectionPoolSize(int poolSize) {
    this.connectionPoolSize = poolSize;
  }

  public void setSessionLocator(MemcachedSessionLocator sessionLocator) {
    this.sessionLocator = sessionLocator;
  }

  public void setSessionComparator(MemcachedSessionComparator sessionComparator) {
    this.sessionComparator = sessionComparator;
  }

  public void setBufferAllocator(BufferAllocator bufferAllocator) {
    this.bufferAllocator = bufferAllocator;
  }

  @SuppressWarnings("unchecked")
  public void setTranscoder(Transcoder transcoder) {
    this.transcoder = transcoder;
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  public String getServers() {
    return this.servers;
  }

  public void setServers(String servers) {
    this.servers = servers;
  }

  public MemcachedSessionLocator getSessionLocator() {
    return this.sessionLocator;
  }

  public MemcachedSessionComparator getSessionComparator() {
    return this.sessionComparator;
  }

  public BufferAllocator getBufferAllocator() {
    return this.bufferAllocator;
  }

  @SuppressWarnings("unchecked")
  public Transcoder getTranscoder() {
    return this.transcoder;
  }

  public List<Integer> getWeights() {
    return this.weights;
  }

  /**
   * Set max queued noreply operations number
   * 
   * @see MemcachedClient#DEFAULT_MAX_QUEUED_NOPS
   * @param maxQueuedNoReplyOperations
   * @since 1.3.8
   */
  public void setMaxQueuedNoReplyOperations(int maxQueuedNoReplyOperations) {
    if (maxQueuedNoReplyOperations <= 1)
      throw new IllegalArgumentException("maxQueuedNoReplyOperations<=1");
    this.maxQueuedNoReplyOperations = maxQueuedNoReplyOperations;
  }

  public void setWeights(List<Integer> weights) {
    this.weights = weights;
  }

  public Configuration getConfiguration() {
    return this.configuration;
  }

  public Object getObject() throws Exception {
    this.checkAttribute();
    MemcachedClientBuilder builder = this.newBuilder();
    this.configBuilder(builder);
    this.memcachedClient = builder.build();
    this.memcachedClient.setOpTimeout(opTimeout);
    this.memcachedClient.setTimeoutExceptionThreshold(timeoutExceptionThreshold);
    return this.memcachedClient;
  }

  private MemcachedClientBuilder newBuilder() {
    if (this.autoDiscoveryServers == null) {
      Map<InetSocketAddress, InetSocketAddress> serverMap = this.getServerMap();
      int[] weightsArray = this.getWeightsArray(serverMap);
      return this.newBuilder(serverMap, weightsArray);
    } else {
      return this.newBuilder(this.autoDiscoveryServers);
    }
  }

  private AWSElasticCacheClientBuilder newBuilder(String autoDiscoveryServers) {
    AWSElasticCacheClientBuilder builder = new AWSElasticCacheClientBuilder(autoDiscoveryServers);
    builder.setPollConfigIntervalMs(this.pollConfigIntervalMs);
    return builder;
  }

  private MemcachedClientBuilder newBuilder(Map<InetSocketAddress, InetSocketAddress> serverMap,
      int[] weightsArray) {
    MemcachedClientBuilder builder;
    if (weightsArray == null) {
      builder = new XMemcachedClientBuilder(serverMap);
    } else {
      builder = new XMemcachedClientBuilder(serverMap, weightsArray);
    }
    return builder;
  }

  private void configBuilder(MemcachedClientBuilder builder) {
    builder.setConfiguration(this.configuration);
    builder.setBufferAllocator(this.bufferAllocator);
    builder.setSessionLocator(this.sessionLocator);
    builder.setSessionComparator(this.sessionComparator);
    builder.setTranscoder(this.transcoder);
    builder.setCommandFactory(this.commandFactory);
    builder.setConnectionPoolSize(this.connectionPoolSize);
    builder.setAuthInfoMap(this.authInfoMap);
    builder.setFailureMode(this.failureMode);
    builder.setKeyProvider(keyProvider);
    builder.setMaxQueuedNoReplyOperations(this.maxQueuedNoReplyOperations);
    builder.setName(this.name);
    builder.setEnableHealSession(this.enableHealSession);
    builder.setHealSessionInterval(this.healSessionInterval);
    builder.setConnectTimeout(connectTimeout);
    builder.setOpTimeout(opTimeout);
  }

  private int[] getWeightsArray(Map<InetSocketAddress, InetSocketAddress> serverMap) {
    int[] weightsArray = null;
    if (serverMap != null && serverMap.size() > 0 && this.weights != null) {
      if (this.weights.size() < serverMap.size()) {
        throw new IllegalArgumentException("Weight list's size is less than server list's size");
      }
      weightsArray = new int[this.weights.size()];
      for (int i = 0; i < weightsArray.length; i++) {
        weightsArray[i] = this.weights.get(i);
      }
    }
    return weightsArray;
  }

  private Map<InetSocketAddress, InetSocketAddress> getServerMap() {
    Map<InetSocketAddress, InetSocketAddress> serverMap = null;

    if (this.servers != null && this.servers.length() > 0) {
      serverMap = AddrUtil.getAddressMap(this.servers);

    }
    return serverMap;
  }

  private void checkAttribute() {
    if (this.bufferAllocator == null) {
      throw new NullPointerException("Null BufferAllocator");
    }
    if (this.sessionLocator == null) {
      throw new NullPointerException("Null MemcachedSessionLocator");
    }
    if (this.sessionComparator == null) {
      throw new NullPointerException("Null MemcachedSessionComparator");
    }
    if (this.configuration == null) {
      throw new NullPointerException("Null networking configuration");
    }
    if (this.commandFactory == null) {
      throw new NullPointerException("Null command factory");
    }
    if (this.weights != null && this.servers == null) {
      throw new NullPointerException("Empty server list");
    }
  }

  public void shutdown() throws IOException {
    if (this.memcachedClient != null) {
      this.memcachedClient.shutdown();
    }
  }

  @SuppressWarnings("rawtypes")
  public Class getObjectType() {
    return MemcachedClient.class;
  }

  public boolean isSingleton() {
    return true;
  }

  public long getConnectTimeout() {
    return connectTimeout;
  }

  public void setConnectTimeout(long connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public long getPollConfigIntervalMs() {
    return pollConfigIntervalMs;
  }

  public void setPollConfigIntervalMs(long pollConfigIntervalMs) {
    this.pollConfigIntervalMs = pollConfigIntervalMs;
  }
}
