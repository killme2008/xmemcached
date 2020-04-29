/**
 * Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)] Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License
 */
package net.rubyeye.xmemcached;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.SocketOption;
import com.google.code.yanf4j.util.SystemUtils;
import net.rubyeye.xmemcached.auth.AuthInfo;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.codec.MemcachedCodecFactory;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.ServerAddressAware;
import net.rubyeye.xmemcached.command.TextCommandFactory;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.exception.NoValueException;
import net.rubyeye.xmemcached.impl.ArrayMemcachedSessionLocator;
import net.rubyeye.xmemcached.impl.ClosedMemcachedTCPSession;
import net.rubyeye.xmemcached.impl.DefaultKeyProvider;
import net.rubyeye.xmemcached.impl.IndexMemcachedSessionComparator;
import net.rubyeye.xmemcached.impl.KeyIteratorImpl;
import net.rubyeye.xmemcached.impl.MemcachedClientStateListenerAdapter;
import net.rubyeye.xmemcached.impl.MemcachedConnector;
import net.rubyeye.xmemcached.impl.MemcachedHandler;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.impl.ReconnectRequest;
import net.rubyeye.xmemcached.monitor.Constants;
import net.rubyeye.xmemcached.monitor.MemcachedClientNameHolder;
import net.rubyeye.xmemcached.monitor.XMemcachedMbeanServer;
import net.rubyeye.xmemcached.networking.Connector;
import net.rubyeye.xmemcached.networking.MemcachedSession;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.AddrUtil;
import net.rubyeye.xmemcached.utils.ByteUtils;
import net.rubyeye.xmemcached.utils.InetSocketAddressWrapper;
import net.rubyeye.xmemcached.utils.Protocol;

/**
 * Memcached Client for connecting to memcached server and do operations.
 *
 * @author dennis(killme2008@gmail.com)
 *
 */
public class XMemcachedClient implements XMemcachedClientMBean, MemcachedClient {

  private static final Logger log = LoggerFactory.getLogger(XMemcachedClient.class);
  protected MemcachedSessionLocator sessionLocator;
  protected MemcachedSessionComparator sessionComparator;
  private volatile boolean shutdown;
  protected MemcachedConnector connector;
  @SuppressWarnings("unchecked")
  private Transcoder transcoder;
  private boolean sanitizeKeys;
  private MemcachedHandler memcachedHandler;
  protected CommandFactory commandFactory;
  protected long opTimeout = DEFAULT_OP_TIMEOUT;
  private long connectTimeout = DEFAULT_CONNECT_TIMEOUT;
  protected int connectionPoolSize = DEFAULT_CONNECTION_POOL_SIZE;
  protected int maxQueuedNoReplyOperations = DEFAULT_MAX_QUEUED_NOPS;
  protected boolean resolveInetAddresses = true;

  protected final AtomicInteger serverOrderCount = new AtomicInteger();

  private Map<InetSocketAddress, AuthInfo> authInfoMap = new HashMap<InetSocketAddress, AuthInfo>();
  private Map<String, AuthInfo> authInfoStringMap = new HashMap<String, AuthInfo>();

  private String name; // cache name

  private boolean failureMode;

  private int timeoutExceptionThreshold = DEFAULT_MAX_TIMEOUTEXCEPTION_THRESHOLD;

  private final CopyOnWriteArrayList<MemcachedClientStateListenerAdapter> stateListenerAdapters =
      new CopyOnWriteArrayList<MemcachedClientStateListenerAdapter>();
  private Thread shutdownHookThread;

  private volatile boolean isHutdownHookCalled = false;
  // key provider for pre-processing keys before sending them to memcached
  // added by dennis,2012-07-14
  private KeyProvider keyProvider = DefaultKeyProvider.INSTANCE;
  /**
   * namespace thread local.
   */
  public static final ThreadLocal<String> NAMESPACE_LOCAL = new ThreadLocal<String>();

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#setMergeFactor(int)
   */
  public final void setMergeFactor(final int mergeFactor) {
    if (mergeFactor < 0) {
      throw new IllegalArgumentException("mergeFactor<0");
    }
    this.connector.setMergeFactor(mergeFactor);
  }

  public int getTimeoutExceptionThreshold() {
    return this.timeoutExceptionThreshold;
  }

  public void setTimeoutExceptionThreshold(final int timeoutExceptionThreshold) {
    if (timeoutExceptionThreshold <= 0) {
      throw new IllegalArgumentException(
          "Illegal timeoutExceptionThreshold value " + timeoutExceptionThreshold);
    }
    if (timeoutExceptionThreshold < 100) {
      log.warn(
          "Too small timeoutExceptionThreshold value may cause connections disconnect/reconnect frequently.");
    }
    this.timeoutExceptionThreshold = timeoutExceptionThreshold;
  }

  public <T> T withNamespace(final String ns, final MemcachedClientCallable<T> callable)
      throws MemcachedException, InterruptedException, TimeoutException {
    beginWithNamespace(ns);
    try {
      return callable.call(this);
    } finally {
      endWithNamespace();
    }
  }

  public void endWithNamespace() {
    NAMESPACE_LOCAL.remove();
  }

  public void beginWithNamespace(final String ns) {
    if (ns == null || ns.trim().length() == 0) {
      throw new IllegalArgumentException("Blank namespace");
    }
    if (NAMESPACE_LOCAL.get() != null) {
      throw new IllegalStateException("Previous namespace wasn't ended.");
    }
    NAMESPACE_LOCAL.set(ns);
  }

  public KeyProvider getKeyProvider() {
    return this.keyProvider;
  }

  public void setKeyProvider(final KeyProvider keyProvider) {
    if (keyProvider == null) {
      throw new IllegalArgumentException("Null key provider");
    }
    this.keyProvider = keyProvider;
  }

  public final MemcachedSessionLocator getSessionLocator() {
    return this.sessionLocator;
  }

  public final MemcachedSessionComparator getSessionComparator() {
    return this.sessionComparator;
  }

  public final CommandFactory getCommandFactory() {
    return this.commandFactory;
  }

  public String getName() {
    return this.name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#getConnectTimeout()
   */
  public long getConnectTimeout() {
    return this.connectTimeout;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#setConnectTimeout(long)
   */
  public void setConnectTimeout(final long connectTimeout) {
    if (connectTimeout < 0) {
      throw new IllegalArgumentException("connectTimeout<0");
    }
    this.connectTimeout = connectTimeout;
  }

  public void setEnableHeartBeat(final boolean enableHeartBeat) {
    this.memcachedHandler.setEnableHeartBeat(enableHeartBeat);
  }

  /**
   * get operation timeout setting
   *
   * @return
   */
  public final long getOpTimeout() {
    return this.opTimeout;
  }

  /**
   * set operation timeout,default is one second.
   *
   * @param opTimeout
   */
  public final void setOpTimeout(final long opTimeout) {
    if (opTimeout < 0) {
      throw new IllegalArgumentException("opTimeout<0");
    }
    this.opTimeout = opTimeout;
  }

  public void setHealSessionInterval(final long healConnectionInterval) {
    if (healConnectionInterval <= 0) {
      throw new IllegalArgumentException("Invalid heal session interval:" + healConnectionInterval);
    }
    if (null != this.connector) {
      this.connector.setHealSessionInterval(healConnectionInterval);
    } else {
      throw new IllegalStateException("The client hasn't been started");
    }
  }

  public long getHealSessionInterval() {
    if (null != this.connector) {
      return this.connector.getHealSessionInterval();
    }
    return -1L;
  }

  public Map<InetSocketAddress, AuthInfo> getAuthInfoMap() {
    return this.authInfoMap;
  }

  public void setAuthInfoMap(final Map<InetSocketAddress, AuthInfo> map) {
    this.authInfoMap = map;
    this.authInfoStringMap = new HashMap<String, AuthInfo>();
    for (Map.Entry<InetSocketAddress, AuthInfo> entry : map.entrySet()) {
      String server = AddrUtil.getServerString(entry.getKey());
      this.authInfoStringMap.put(server, entry.getValue());
    }
  }

  public Map<String, AuthInfo> getAuthInfoStringMap() {
    return this.authInfoStringMap;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#getConnector()
   */
  public final Connector getConnector() {
    return this.connector;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#setOptimizeMergeBuffer(boolean)
   */
  public final void setOptimizeMergeBuffer(final boolean optimizeMergeBuffer) {
    this.connector.setOptimizeMergeBuffer(optimizeMergeBuffer);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#isShutdown()
   */
  public final boolean isShutdown() {
    return this.shutdown;
  }

  @SuppressWarnings("unchecked")
  private final <T> GetsResponse<T> gets0(final String key, final byte[] keyBytes,
      final Transcoder<T> transcoder)
      throws MemcachedException, TimeoutException, InterruptedException {
    GetsResponse<T> result = (GetsResponse<T>) this.fetch0(key, keyBytes, CommandType.GETS_ONE,
        this.opTimeout, transcoder);
    return result;
  }

  protected final Session sendCommand(final Command cmd) throws MemcachedException {
    if (this.shutdown) {
      throw new MemcachedException("Xmemcached is stopped");
    }
    return this.connector.send(cmd);
  }

  /**
   * XMemcached constructor,default weight is 1
   *
   * @param server �����P
   * @param port ����ㄧ���
   * @throws IOException
   */
  public XMemcachedClient(final String server, final int port) throws IOException {
    this(server, port, 1);
  }

  /**
   * XMemcached constructor
   *
   * @param host server host
   * @param port server port
   * @param weight server weight
   * @throws IOException
   */
  public XMemcachedClient(final String host, final int port, final int weight) throws IOException {
    super();
    if (weight <= 0) {
      throw new IllegalArgumentException("weight<=0");
    }
    checkServerPort(host, port);
    buildConnector(new ArrayMemcachedSessionLocator(), new IndexMemcachedSessionComparator(),
        new SimpleBufferAllocator(), XMemcachedClientBuilder.getDefaultConfiguration(),
        XMemcachedClientBuilder.getDefaultSocketOptions(), new TextCommandFactory(),
        new SerializingTranscoder());
    start0();
    connect(new InetSocketAddressWrapper(newSocketAddress(host, port),
        this.serverOrderCount.incrementAndGet(), weight, null, this.resolveInetAddresses));
  }

  protected InetSocketAddress newSocketAddress(final String server, final int port) {
    return new InetSocketAddress(server, port);
  }

  private void checkServerPort(final String server, final int port) {
    if (server == null || server.length() == 0) {
      throw new IllegalArgumentException();
    }
    if (port <= 0) {
      throw new IllegalArgumentException();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#addServer(java.lang.String, int)
   */
  public final void addServer(final String server, final int port) throws IOException {
    this.addServer(server, port, 1);
  }

  /**
   * add a memcached server to MemcachedClient
   *
   * @param server
   * @param port
   * @param weight
   * @throws IOException
   */
  public final void addServer(final String server, final int port, final int weight)
      throws IOException {
    if (weight <= 0) {
      throw new IllegalArgumentException("weight<=0");
    }
    checkServerPort(server, port);
    connect(new InetSocketAddressWrapper(newSocketAddress(server, port),
        this.serverOrderCount.incrementAndGet(), weight, null, this.resolveInetAddresses));
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#addServer(java.net. InetSocketAddress )
   */
  public final void addServer(final InetSocketAddress inetSocketAddress) throws IOException {
    this.addServer(inetSocketAddress, 1);
  }

  public final void addServer(final InetSocketAddress inetSocketAddress, final int weight)
      throws IOException {
    if (inetSocketAddress == null) {
      throw new IllegalArgumentException("Null InetSocketAddress");
    }
    if (weight <= 0) {
      throw new IllegalArgumentException("weight<=0");
    }
    connect(new InetSocketAddressWrapper(inetSocketAddress, this.serverOrderCount.incrementAndGet(),
        weight, null, this.resolveInetAddresses));
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#addServer(java.lang.String)
   */
  public final void addServer(final String hostList) throws IOException {
    Map<InetSocketAddress, InetSocketAddress> addresses = AddrUtil.getAddressMap(hostList);
    if (addresses != null && addresses.size() > 0) {
      for (Map.Entry<InetSocketAddress, InetSocketAddress> entry : addresses.entrySet()) {
        final InetSocketAddress mainNodeAddr = entry.getKey();
        final InetSocketAddress standbyNodeAddr = entry.getValue();
        connect(new InetSocketAddressWrapper(mainNodeAddr, this.serverOrderCount.incrementAndGet(),
            1, null, this.resolveInetAddresses));
        if (standbyNodeAddr != null) {
          connect(new InetSocketAddressWrapper(standbyNodeAddr,
              this.serverOrderCount.incrementAndGet(), 1, mainNodeAddr, this.resolveInetAddresses));
        }
      }
    }
  }

  public void addOneServerWithWeight(final String server, final int weight) throws IOException {
    Map<InetSocketAddress, InetSocketAddress> addresses = AddrUtil.getAddressMap(server);
    if (addresses == null) {
      throw new IllegalArgumentException("Null Server");
    }
    if (addresses.size() != 1) {
      throw new IllegalArgumentException("Please add one server at one time");
    }
    if (weight <= 0) {
      throw new IllegalArgumentException("weight<=0");
    }
    if (addresses != null && addresses.size() > 0) {
      for (Map.Entry<InetSocketAddress, InetSocketAddress> entry : addresses.entrySet()) {
        final InetSocketAddress mainNodeAddr = entry.getKey();
        final InetSocketAddress standbyNodeAddr = entry.getValue();
        connect(new InetSocketAddressWrapper(mainNodeAddr, this.serverOrderCount.incrementAndGet(),
            1, null, this.resolveInetAddresses));
        if (standbyNodeAddr != null) {
          connect(new InetSocketAddressWrapper(standbyNodeAddr,
              this.serverOrderCount.incrementAndGet(), 1, mainNodeAddr, this.resolveInetAddresses));
        }
      }
    }

  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#getServersDescription()
   */
  public final List<String> getServersDescription() {
    final List<String> result = new ArrayList<String>();
    for (Session session : this.connector.getSessionSet()) {
      InetSocketAddress socketAddress = session.getRemoteSocketAddress();
      int weight = ((MemcachedSession) session).getInetSocketAddressWrapper().getWeight();
      result.add(SystemUtils.getRawAddress(socketAddress) + ":" + socketAddress.getPort()
          + "(weight=" + weight + ")");
    }
    return result;
  }

  public final void setServerWeight(final String server, final int weight) {
    InetSocketAddress socketAddress = AddrUtil.getOneAddress(server);
    Queue<Session> sessionQueue = this.connector.getSessionByAddress(socketAddress);
    if (sessionQueue == null) {
      throw new IllegalArgumentException("There is no server " + server);
    }
    for (Session session : sessionQueue) {
      if (session != null) {
        ((MemcachedTCPSession) session).getInetSocketAddressWrapper().setWeight(weight);
      }
    }
    this.connector.updateSessions();
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#removeServer(java.lang.String)
   */
  public final void removeServer(final String hostList) {
    List<InetSocketAddress> addresses = AddrUtil.getAddresses(hostList);
    if (addresses != null && addresses.size() > 0) {
      for (InetSocketAddress address : addresses) {
        removeServer(address);
      }

    }

  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#removeServer(java.net.InetSocketAddress)
   */
  public void removeServer(final InetSocketAddress address) {
    // Close main sessions
    Queue<Session> sessionQueue = this.connector.getSessionByAddress(address);
    if (sessionQueue != null) {
      for (Session session : sessionQueue) {
        if (session != null) {
          // Disable auto reconnection
          ((MemcachedSession) session).setAllowReconnect(false);
          // Close connection
          ((MemcachedSession) session).quit();
        }
      }
    }
    // Close standby sessions
    List<Session> standBySession = this.connector.getStandbySessionListByMainNodeAddr(address);
    if (standBySession != null) {
      for (Session session : standBySession) {
        if (session != null) {
          this.connector.removeReconnectRequest(session.getRemoteSocketAddress());
          // Disable auto reconnection
          ((MemcachedSession) session).setAllowReconnect(false);
          // Close connection
          ((MemcachedSession) session).quit();
        }
      }
    }
    this.connector.removeReconnectRequest(address);
  }

  protected void connect(final InetSocketAddressWrapper inetSocketAddressWrapper)
      throws IOException {
    // creat connection pool
    InetSocketAddress inetSocketAddress = inetSocketAddressWrapper.getInetSocketAddress();
    if (this.connectionPoolSize > 1) {
      log.warn(
          "You are using connection pool for xmemcached client,it's not recommended unless you have test it that it can boost performance in your app.");
    }
    for (int i = 0; i < this.connectionPoolSize; i++) {
      Future<Boolean> future = null;
      boolean connected = false;
      Throwable throwable = null;
      try {
        future = this.connector.connect(inetSocketAddressWrapper);

        if (!future.get(this.connectTimeout, TimeUnit.MILLISECONDS)) {
          log.error("connect to " + SystemUtils.getRawAddress(inetSocketAddress) + ":"
              + inetSocketAddress.getPort() + " fail");
        } else {
          connected = true;
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        throwable = e;
        log.error("connect to " + SystemUtils.getRawAddress(inetSocketAddress) + ":"
            + inetSocketAddress.getPort() + " error", e);
      } catch (TimeoutException e) {
        throwable = e;
        log.error("connect to " + SystemUtils.getRawAddress(inetSocketAddress) + ":"
            + inetSocketAddress.getPort() + " timeout", e);
      } catch (Exception e) {
        throwable = e;
        log.error("connect to " + SystemUtils.getRawAddress(inetSocketAddress) + ":"
            + inetSocketAddress.getPort() + " error", e);
      }
      // If it is not connected,it will be added to waiting queue for
      // reconnecting.
      if (!connected) {
        if (future != null) {
          future.cancel(true);
        }
        // If we use failure mode, add a mock session at first
        if (this.failureMode) {
          this.connector.addSession(new ClosedMemcachedTCPSession(inetSocketAddressWrapper));
        }
        this.connector.addToWatingQueue(
            new ReconnectRequest(inetSocketAddressWrapper, 0, getHealSessionInterval()));
        log.error("Connect to " + SystemUtils.getRawAddress(inetSocketAddress) + ":"
            + inetSocketAddress.getPort() + " fail", throwable);
        // throw new IOException(throwable);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private final <T> Object fetch0(final String key, final byte[] keyBytes,
      final CommandType cmdType, final long timeout, Transcoder<T> transcoder)
      throws InterruptedException, TimeoutException, MemcachedException {
    final Command command =
        this.commandFactory.createGetCommand(key, keyBytes, cmdType, this.transcoder);
    latchWait(command, timeout, sendCommand(command));
    command.getIoBuffer().free(); // free buffer
    checkException(command);
    CachedData data = (CachedData) command.getResult();
    if (data == null) {
      return null;
    }
    if (transcoder == null) {
      transcoder = this.transcoder;
    }
    if (cmdType == CommandType.GETS_ONE) {
      return new GetsResponse<T>(data.getCas(), transcoder.decode(data));
    } else {
      return transcoder.decode(data);
    }
  }

  private final void start0() throws IOException {
    registerMBean();
    startConnector();
    MemcachedClientNameHolder.clear();
  }

  private final void startConnector() throws IOException {
    if (this.shutdown) {
      this.shutdown = false;
      this.connector.start();
      this.memcachedHandler.start();
      if (AddrUtil.isEnableShutDownHook()) {
        this.shutdownHookThread = new Thread() {
          @Override
          public void run() {
            try {
              XMemcachedClient.this.isHutdownHookCalled = true;
              XMemcachedClient.this.shutdown();
            } catch (IOException e) {
              log.error("Shutdown XMemcachedClient error", e);
            }
          }
        };
        Runtime.getRuntime().addShutdownHook(this.shutdownHookThread);
      }
    }
  }

  /**
   * Set max queued noreply operations number
   *
   * @param maxQueuedNoReplyOperations
   */
  void setMaxQueuedNoReplyOperations(final int maxQueuedNoReplyOperations) {
    if (maxQueuedNoReplyOperations <= 1) {
      throw new IllegalArgumentException("maxQueuedNoReplyOperations<=1");
    }
    this.maxQueuedNoReplyOperations = maxQueuedNoReplyOperations;
  }

  @SuppressWarnings("unchecked")
  private void buildConnector(MemcachedSessionLocator locator,
      MemcachedSessionComparator comparator, BufferAllocator bufferAllocator,
      Configuration configuration, final Map<SocketOption, Object> socketOptions,
      CommandFactory commandFactory, Transcoder transcoder) {
    if (locator == null) {
      locator = new ArrayMemcachedSessionLocator();
    }
    if (comparator == null) {
      comparator = new IndexMemcachedSessionComparator();
    }
    if (bufferAllocator == null) {
      bufferAllocator = new SimpleBufferAllocator();
    }
    if (configuration == null) {
      configuration = XMemcachedClientBuilder.getDefaultConfiguration();
    }
    if (transcoder == null) {
      transcoder = new SerializingTranscoder();
    }
    if (commandFactory == null) {
      commandFactory = new TextCommandFactory();
    }
    if (this.name == null) {
      this.name = "MemcachedClient-" + Constants.MEMCACHED_CLIENT_COUNTER.getAndIncrement();
      MemcachedClientNameHolder.setName(this.name);
    }
    this.commandFactory = commandFactory;
    ByteUtils.setProtocol(this.commandFactory.getProtocol());
    log.info("XMemcachedClient is using " + this.commandFactory.getProtocol().name() + " protocol");
    this.commandFactory.setBufferAllocator(bufferAllocator);
    this.shutdown = true;
    this.transcoder = transcoder;
    this.sessionLocator = locator;
    this.sessionComparator = comparator;
    this.connector =
        newConnector(bufferAllocator, configuration, this.sessionLocator, this.sessionComparator,
            this.commandFactory, this.connectionPoolSize, this.maxQueuedNoReplyOperations);
    this.memcachedHandler = new MemcachedHandler(this);
    this.connector.setHandler(this.memcachedHandler);
    this.connector.setCodecFactory(new MemcachedCodecFactory());
    this.connector.setSessionTimeout(-1);
    this.connector.setSocketOptions(socketOptions);
    if (isFailureMode()) {
      log.info("XMemcachedClient in failure mode.");
    }
    this.connector.setFailureMode(this.failureMode);
    this.sessionLocator.setFailureMode(this.failureMode);
  }

  protected MemcachedConnector newConnector(final BufferAllocator bufferAllocator,
      final Configuration configuration, final MemcachedSessionLocator memcachedSessionLocator,
      final MemcachedSessionComparator memcachedSessionComparator,
      final CommandFactory commandFactory, final int poolSize,
      final int maxQueuedNoReplyOperations) {
    // make sure dispatch message thread count is zero
    configuration.setDispatchMessageThreadCount(0);
    return new MemcachedConnector(configuration, memcachedSessionLocator,
        memcachedSessionComparator, bufferAllocator, commandFactory, poolSize,
        maxQueuedNoReplyOperations);
  }

  private final void registerMBean() {
    if (this.shutdown) {
      XMemcachedMbeanServer.getInstance().registMBean(this, this.getClass().getPackage().getName()
          + ":type=" + this.getClass().getSimpleName() + "-" + MemcachedClientNameHolder.getName());
    }
  }

  public void setOptimizeGet(final boolean optimizeGet) {
    this.connector.setOptimizeGet(optimizeGet);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#setBufferAllocator(net.rubyeye
   * .xmemcached.buffer.BufferAllocator)
   */
  public final void setBufferAllocator(final BufferAllocator bufferAllocator) {
    this.connector.setBufferAllocator(bufferAllocator);
  }

  /**
   * XMemcached Constructor.
   *
   * @param inetSocketAddress
   * @param weight
   * @throws IOException
   */
  public XMemcachedClient(final InetSocketAddress inetSocketAddress, final int weight,
      final CommandFactory cmdFactory) throws IOException {
    super();
    if (inetSocketAddress == null) {
      throw new IllegalArgumentException("Null InetSocketAddress");

    }
    if (cmdFactory == null) {
      throw new IllegalArgumentException("Null command factory.");
    }
    if (weight <= 0) {
      throw new IllegalArgumentException("weight<=0");
    }
    buildConnector(new ArrayMemcachedSessionLocator(), new IndexMemcachedSessionComparator(),
        new SimpleBufferAllocator(), XMemcachedClientBuilder.getDefaultConfiguration(),
        XMemcachedClientBuilder.getDefaultSocketOptions(), cmdFactory, new SerializingTranscoder());
    start0();
    connect(new InetSocketAddressWrapper(inetSocketAddress, this.serverOrderCount.incrementAndGet(),
        weight, null, this.resolveInetAddresses));
  }

  public XMemcachedClient(final InetSocketAddress inetSocketAddress, final int weight)
      throws IOException {
    this(inetSocketAddress, weight, new TextCommandFactory());
  }

  public XMemcachedClient(final InetSocketAddress inetSocketAddress) throws IOException {
    this(inetSocketAddress, 1);
  }

  public XMemcachedClient() throws IOException {
    super();
    buildConnector(new ArrayMemcachedSessionLocator(), new IndexMemcachedSessionComparator(),
        new SimpleBufferAllocator(), XMemcachedClientBuilder.getDefaultConfiguration(),
        XMemcachedClientBuilder.getDefaultSocketOptions(), new TextCommandFactory(),
        new SerializingTranscoder());
    start0();
  }

  /**
   * XMemcachedClient constructor.Every server's weight is one by default. You should not new client
   * instance by this method, use MemcachedClientBuilder instead.
   *
   * @param locator
   * @param comparator
   * @param allocator
   * @param conf
   * @param commandFactory
   * @param transcoder
   * @param addressList
   * @param stateListeners
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  public XMemcachedClient(final MemcachedSessionLocator locator,
      final MemcachedSessionComparator comparator, final BufferAllocator allocator,
      final Configuration conf, final Map<SocketOption, Object> socketOptions,
      final CommandFactory commandFactory, final Transcoder transcoder,
      final Map<InetSocketAddress, InetSocketAddress> addressMap,
      final List<MemcachedClientStateListener> stateListeners,
      final Map<InetSocketAddress, AuthInfo> map, final int poolSize, final long connectTimeout,
      final String name, final boolean failureMode, final boolean resolveInetAddresses)
      throws IOException {
    super();
    setConnectTimeout(connectTimeout);
    setFailureMode(failureMode);
    setName(name);
    optimiezeSetReadThreadCount(conf, addressMap == null ? 0 : addressMap.size());
    buildConnector(locator, comparator, allocator, conf, socketOptions, commandFactory, transcoder);
    if (stateListeners != null) {
      for (MemcachedClientStateListener stateListener : stateListeners) {
        addStateListener(stateListener);
      }
    }
    setAuthInfoMap(map);
    setConnectionPoolSize(poolSize);
    this.resolveInetAddresses = resolveInetAddresses;
    start0();
    if (addressMap != null) {
      for (Map.Entry<InetSocketAddress, InetSocketAddress> entry : addressMap.entrySet()) {
        final InetSocketAddress mainNodeAddr = entry.getKey();
        final InetSocketAddress standbyNodeAddr = entry.getValue();
        connect(new InetSocketAddressWrapper(mainNodeAddr, this.serverOrderCount.incrementAndGet(),
            1, null, this.resolveInetAddresses));
        if (standbyNodeAddr != null) {
          connect(new InetSocketAddressWrapper(standbyNodeAddr,
              this.serverOrderCount.incrementAndGet(), 1, mainNodeAddr, this.resolveInetAddresses));
        }
      }
    }
  }

  /**
   * XMemcachedClient constructor.
   *
   * @param locator
   * @param comparator
   * @param allocator
   * @param conf
   * @param commandFactory
   * @param transcoder
   * @param addressList
   * @param weights
   * @param stateListeners weight array for address list
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  XMemcachedClient(final MemcachedSessionLocator locator,
      final MemcachedSessionComparator comparator, final BufferAllocator allocator,
      final Configuration conf, final Map<SocketOption, Object> socketOptions,
      final CommandFactory commandFactory, final Transcoder transcoder,
      final Map<InetSocketAddress, InetSocketAddress> addressMap, final int[] weights,
      final List<MemcachedClientStateListener> stateListeners,
      final Map<InetSocketAddress, AuthInfo> infoMap, final int poolSize, final long connectTimeout,
      final String name, final boolean failureMode, final boolean resolveInetAddresses)
      throws IOException {
    super();
    setConnectTimeout(connectTimeout);
    setFailureMode(failureMode);
    setName(name);
    if (weights == null && addressMap != null) {
      throw new IllegalArgumentException("Null weights");
    }
    if (weights != null && addressMap == null) {
      throw new IllegalArgumentException("Null addressList");
    }

    if (weights != null) {
      for (int weight : weights) {
        if (weight <= 0) {
          throw new IllegalArgumentException("Some weights<=0");
        }
      }
    }
    if (weights != null && addressMap != null && weights.length < addressMap.size()) {
      throw new IllegalArgumentException("weights.length is less than addressList.size()");
    }
    optimiezeSetReadThreadCount(conf, addressMap == null ? 0 : addressMap.size());
    buildConnector(locator, comparator, allocator, conf, socketOptions, commandFactory, transcoder);
    if (stateListeners != null) {
      for (MemcachedClientStateListener stateListener : stateListeners) {
        addStateListener(stateListener);
      }
    }
    setAuthInfoMap(infoMap);
    setConnectionPoolSize(poolSize);
    this.resolveInetAddresses = resolveInetAddresses;
    start0();
    if (addressMap != null && weights != null) {
      int i = 0;
      for (Map.Entry<InetSocketAddress, InetSocketAddress> entry : addressMap.entrySet()) {
        final InetSocketAddress mainNodeAddr = entry.getKey();
        final InetSocketAddress standbyNodeAddr = entry.getValue();
        connect(new InetSocketAddressWrapper(mainNodeAddr, this.serverOrderCount.incrementAndGet(),
            weights[i], null, this.resolveInetAddresses));
        if (standbyNodeAddr != null) {
          connect(
              new InetSocketAddressWrapper(standbyNodeAddr, this.serverOrderCount.incrementAndGet(),
                  weights[i], mainNodeAddr, this.resolveInetAddresses));
        }
        i++;
      }
    }
  }

  private final void optimiezeSetReadThreadCount(final Configuration conf, final int addressCount) {
    if (conf != null && addressCount > 1) {
      if (!isWindowsPlatform() && conf.getReadThreadCount() == DEFAULT_READ_THREAD_COUNT) {
        int threadCount = SystemUtils.getSystemThreadCount();
        conf.setReadThreadCount(addressCount > threadCount ? threadCount : addressCount);
      }
    }
  }

  private final boolean isWindowsPlatform() {
    String osName = System.getProperty("os.name");
    if (osName != null && osName.toLowerCase().indexOf("windows") >= 0) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * XMemcached Constructor.Every server's weight is one by default.
   *
   * @param addressList
   * @throws IOException
   */
  public XMemcachedClient(final List<InetSocketAddress> addressList) throws IOException {
    this(addressList, new TextCommandFactory());
  }

  /**
   * XMemcached Constructor.Every server's weight is one by default.
   *
   * @param cmdFactory command factory
   * @param addressList memcached server socket address list.
   * @throws IOException
   */
  public XMemcachedClient(final List<InetSocketAddress> addressList,
      final CommandFactory cmdFactory) throws IOException {
    super();
    if (cmdFactory == null) {
      throw new IllegalArgumentException("Null command factory.");
    }
    if (addressList == null || addressList.isEmpty()) {
      throw new IllegalArgumentException("Empty address list");
    }
    BufferAllocator simpleBufferAllocator = new SimpleBufferAllocator();
    buildConnector(new ArrayMemcachedSessionLocator(), new IndexMemcachedSessionComparator(),
        simpleBufferAllocator, XMemcachedClientBuilder.getDefaultConfiguration(),
        XMemcachedClientBuilder.getDefaultSocketOptions(), cmdFactory, new SerializingTranscoder());
    start0();
    for (InetSocketAddress inetSocketAddress : addressList) {
      connect(new InetSocketAddressWrapper(inetSocketAddress,
          this.serverOrderCount.incrementAndGet(), 1, null, this.resolveInetAddresses));

    }
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#get(java.lang.String, long,
   * net.rubyeye.xmemcached.transcoders.Transcoder)
   */
  @SuppressWarnings("unchecked")
  public final <T> T get(final String key, final long timeout, final Transcoder<T> transcoder)
      throws TimeoutException, InterruptedException, MemcachedException {
    return (T) this.get0(key, timeout, CommandType.GET_ONE, transcoder);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#get(java.lang.String, long)
   */
  @SuppressWarnings("unchecked")
  public final <T> T get(final String key, final long timeout)
      throws TimeoutException, InterruptedException, MemcachedException {
    return (T) this.get(key, timeout, this.transcoder);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#get(java.lang.String,
   * net.rubyeye.xmemcached.transcoders.Transcoder)
   */
  public final <T> T get(final String key, final Transcoder<T> transcoder)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.get(key, this.opTimeout, transcoder);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#get(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  public final <T> T get(final String key)
      throws TimeoutException, InterruptedException, MemcachedException {
    return (T) this.get(key, this.opTimeout);
  }

  private <T> Object get0(String key, final long timeout, final CommandType cmdType,
      final Transcoder<T> transcoder)
      throws TimeoutException, InterruptedException, MemcachedException {
    key = preProcessKey(key);
    byte[] keyBytes = ByteUtils.getBytes(key);
    ByteUtils.checkKey(keyBytes);
    return this.fetch0(key, keyBytes, cmdType, timeout, transcoder);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#gets(java.lang.String, long,
   * net.rubyeye.xmemcached.transcoders.Transcoder)
   */
  @SuppressWarnings("unchecked")
  public final <T> GetsResponse<T> gets(final String key, final long timeout,
      final Transcoder<T> transcoder)
      throws TimeoutException, InterruptedException, MemcachedException {
    return (GetsResponse<T>) this.get0(key, timeout, CommandType.GETS_ONE, transcoder);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#gets(java.lang.String)
   */
  public final <T> GetsResponse<T> gets(final String key)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.gets(key, this.opTimeout);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#gets(java.lang.String, long)
   */
  @SuppressWarnings("unchecked")
  public final <T> GetsResponse<T> gets(final String key, final long timeout)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.gets(key, timeout, this.transcoder);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#gets(java.lang.String,
   * net.rubyeye.xmemcached.transcoders.Transcoder)
   */
  @SuppressWarnings("unchecked")
  public final <T> GetsResponse<T> gets(final String key, final Transcoder transcoder)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.gets(key, this.opTimeout, transcoder);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#get(java.util.Collection, long,
   * net.rubyeye.xmemcached.transcoders.Transcoder)
   */
  public final <T> Map<String, T> get(final Collection<String> keyCollections, final long timeout,
      final Transcoder<T> transcoder)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.getMulti0(keyCollections, timeout, CommandType.GET_MANY, transcoder);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#get(java.util.Collection,
   * net.rubyeye.xmemcached.transcoders.Transcoder)
   */
  public final <T> Map<String, T> get(final Collection<String> keyCollections,
      final Transcoder<T> transcoder)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.getMulti0(keyCollections, this.opTimeout, CommandType.GET_MANY, transcoder);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#get(java.util.Collection)
   */
  public final <T> Map<String, T> get(final Collection<String> keyCollections)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.get(keyCollections, this.opTimeout);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#get(java.util.Collection, long)
   */
  @SuppressWarnings("unchecked")
  public final <T> Map<String, T> get(final Collection<String> keyCollections, final long timeout)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.get(keyCollections, timeout, this.transcoder);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#gets(java.util.Collection, long,
   * net.rubyeye.xmemcached.transcoders.Transcoder)
   */
  @SuppressWarnings("unchecked")
  public final <T> Map<String, GetsResponse<T>> gets(final Collection<String> keyCollections,
      final long timeout, final Transcoder<T> transcoder)
      throws TimeoutException, InterruptedException, MemcachedException {
    return (Map<String, GetsResponse<T>>) this.getMulti0(keyCollections, timeout,
        CommandType.GETS_MANY, transcoder);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#gets(java.util.Collection)
   */
  public final <T> Map<String, GetsResponse<T>> gets(final Collection<String> keyCollections)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.gets(keyCollections, this.opTimeout);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#gets(java.util.Collection, long)
   */
  @SuppressWarnings("unchecked")
  public final <T> Map<String, GetsResponse<T>> gets(final Collection<String> keyCollections,
      final long timeout) throws TimeoutException, InterruptedException, MemcachedException {
    return this.gets(keyCollections, timeout, this.transcoder);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#gets(java.util.Collection,
   * net.rubyeye.xmemcached.transcoders.Transcoder)
   */
  public final <T> Map<String, GetsResponse<T>> gets(final Collection<String> keyCollections,
      final Transcoder<T> transcoder)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.gets(keyCollections, this.opTimeout, transcoder);
  }

  private final <T> Map<String, T> getMulti0(final Collection<String> keys, final long timeout,
      final CommandType cmdType, final Transcoder<T> transcoder)
      throws TimeoutException, InterruptedException, MemcachedException {
    if (keys == null || keys.size() == 0) {
      return null;
    }
    Collection<String> keyCollections = new ArrayList<String>(keys.size());
    for (String key : keys) {
      keyCollections.add(preProcessKey(key));
    }
    final CountDownLatch latch;
    final List<Command> commands;
    if (this.connector.getSessionSet().size() <= 1) {
      commands = new ArrayList<Command>(1);
      latch = new CountDownLatch(1);
      commands.add(this.sendGetMultiCommand(keyCollections, latch, cmdType, transcoder));

    } else {
      Collection<List<String>> catalogKeys = catalogKeys(keyCollections);
      commands = new ArrayList<Command>(catalogKeys.size());
      latch = new CountDownLatch(catalogKeys.size());
      for (List<String> catalogKeyCollection : catalogKeys) {
        commands.add(this.sendGetMultiCommand(catalogKeyCollection, latch, cmdType, transcoder));
      }
    }
    if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
      for (Command getCmd : commands) {
        getCmd.cancel();
      }
      throw new TimeoutException("Timed out waiting for operation");
    }
    return this.reduceResult(cmdType, transcoder, commands);
  }

  @SuppressWarnings("unchecked")
  private <T> Map<String, T> reduceResult(final CommandType cmdType, final Transcoder<T> transcoder,
      final List<Command> commands)
      throws MemcachedException, InterruptedException, TimeoutException {
    final Map<String, T> result = new HashMap<String, T>(commands.size());
    for (Command getCmd : commands) {
      getCmd.getIoBuffer().free();
      checkException(getCmd);
      Map<String, CachedData> map = (Map<String, CachedData>) getCmd.getResult();
      if (cmdType == CommandType.GET_MANY) {
        Iterator<Map.Entry<String, CachedData>> it = map.entrySet().iterator();
        while (it.hasNext()) {
          Map.Entry<String, CachedData> entry = it.next();
          String decodeKey = decodeKey(entry.getKey());
          if (decodeKey != null) {
            result.put(decodeKey, transcoder.decode(entry.getValue()));
          }
        }

      } else {
        Iterator<Map.Entry<String, CachedData>> it = map.entrySet().iterator();
        while (it.hasNext()) {
          Map.Entry<String, CachedData> entry = it.next();
          GetsResponse getsResponse =
              new GetsResponse(entry.getValue().getCas(), transcoder.decode(entry.getValue()));
          String decodeKey = decodeKey(entry.getKey());
          if (decodeKey != null) {
            result.put(decodeKey, (T) getsResponse);
          }
        }

      }

    }
    return result;
  }

  /**
   * Hash key to servers
   *
   * @param keyCollections
   * @return
   */
  private final Collection<List<String>> catalogKeys(final Collection<String> keyCollections) {
    final Map<Session, List<String>> catalogMap = new HashMap<Session, List<String>>();

    for (String key : keyCollections) {
      Session index = this.sessionLocator.getSessionByKey(key);
      List<String> tmpKeys = catalogMap.get(index);
      if (tmpKeys == null) {
        tmpKeys = new ArrayList<String>(10);
        catalogMap.put(index, tmpKeys);
      }
      tmpKeys.add(key);
    }

    Collection<List<String>> catalogKeys = catalogMap.values();
    return catalogKeys;
  }

  private final <T> Command sendGetMultiCommand(final Collection<String> keys,
      final CountDownLatch latch, final CommandType cmdType, final Transcoder<T> transcoder)
      throws InterruptedException, TimeoutException, MemcachedException {
    final Command command =
        this.commandFactory.createGetMultiCommand(keys, latch, cmdType, transcoder);
    sendCommand(command);
    return command;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#set(java.lang.String, int, T,
   * net.rubyeye.xmemcached.transcoders.Transcoder, long)
   */
  public final <T> boolean set(String key, final int exp, final T value,
      final Transcoder<T> transcoder, final long timeout)
      throws TimeoutException, InterruptedException, MemcachedException {
    key = preProcessKey(key);
    byte[] keyBytes = this.checkStoreArguments(key, exp, value);
    return this.sendStoreCommand(
        this.commandFactory.createSetCommand(key, keyBytes, exp, value, false, transcoder),
        timeout);
  }

  @SuppressWarnings("unchecked")
  public void setWithNoReply(final String key, final int exp, final Object value)
      throws InterruptedException, MemcachedException {
    this.setWithNoReply(key, exp, value, this.transcoder);
  }

  public <T> void setWithNoReply(String key, final int exp, final T value,
      final Transcoder<T> transcoder) throws InterruptedException, MemcachedException {
    key = preProcessKey(key);
    byte[] keyBytes = this.checkStoreArguments(key, exp, value);
    try {
      this.sendStoreCommand(
          this.commandFactory.createSetCommand(key, keyBytes, exp, value, true, transcoder),
          this.opTimeout);
    } catch (TimeoutException e) {
      throw new MemcachedException(e);
    }
  }

  private final <T> byte[] checkStoreArguments(final String key, final int exp, final T value) {
    byte[] keyBytes = ByteUtils.getBytes(key);
    ByteUtils.checkKey(keyBytes);
    if (value == null) {
      throw new IllegalArgumentException("value could not be null");
    }
    if (exp < 0) {
      throw new IllegalArgumentException("Expire time must be greater than or equal to 0");
    }
    return keyBytes;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#set(java.lang.String, int, java.lang.Object)
   */
  public final boolean set(final String key, final int exp, final Object value)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.set(key, exp, value, this.opTimeout);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#set(java.lang.String, int, java.lang.Object, long)
   */
  @SuppressWarnings("unchecked")
  public final boolean set(final String key, final int exp, final Object value, final long timeout)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.set(key, exp, value, this.transcoder, timeout);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#set(java.lang.String, int, T,
   * net.rubyeye.xmemcached.transcoders.Transcoder)
   */
  public final <T> boolean set(final String key, final int exp, final T value,
      final Transcoder<T> transcoder)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.set(key, exp, value, transcoder, this.opTimeout);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#add(java.lang.String, int, T,
   * net.rubyeye.xmemcached.transcoders.Transcoder, long)
   */
  public final <T> boolean add(String key, final int exp, final T value,
      final Transcoder<T> transcoder, final long timeout)
      throws TimeoutException, InterruptedException, MemcachedException {
    key = preProcessKey(key);
    return this.add0(key, exp, value, transcoder, timeout);
  }

  private <T> boolean add0(final String key, final int exp, final T value,
      final Transcoder<T> transcoder, final long timeout)
      throws InterruptedException, TimeoutException, MemcachedException {
    byte[] keyBytes = this.checkStoreArguments(key, exp, value);
    return this.sendStoreCommand(
        this.commandFactory.createAddCommand(key, keyBytes, exp, value, false, transcoder),
        timeout);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#add(java.lang.String, int, java.lang.Object)
   */
  public final boolean add(final String key, final int exp, final Object value)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.add(key, exp, value, this.opTimeout);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#add(java.lang.String, int, java.lang.Object, long)
   */
  @SuppressWarnings("unchecked")
  public final boolean add(final String key, final int exp, final Object value, final long timeout)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.add(key, exp, value, this.transcoder, timeout);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#add(java.lang.String, int, T,
   * net.rubyeye.xmemcached.transcoders.Transcoder)
   */
  public final <T> boolean add(final String key, final int exp, final T value,
      final Transcoder<T> transcoder)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.add(key, exp, value, transcoder, this.opTimeout);
  }

  @SuppressWarnings("unchecked")
  public void addWithNoReply(final String key, final int exp, final Object value)
      throws InterruptedException, MemcachedException {
    this.addWithNoReply(key, exp, value, this.transcoder);

  }

  public <T> void addWithNoReply(String key, final int exp, final T value,
      final Transcoder<T> transcoder) throws InterruptedException, MemcachedException {
    key = preProcessKey(key);
    byte[] keyBytes = this.checkStoreArguments(key, exp, value);
    try {
      this.sendStoreCommand(
          this.commandFactory.createAddCommand(key, keyBytes, exp, value, true, transcoder),
          this.opTimeout);
    } catch (TimeoutException e) {
      throw new MemcachedException(e);
    }

  }

  @SuppressWarnings("unchecked")
  public void replaceWithNoReply(final String key, final int exp, final Object value)
      throws InterruptedException, MemcachedException {
    this.replaceWithNoReply(key, exp, value, this.transcoder);

  }

  public <T> void replaceWithNoReply(String key, final int exp, final T value,
      final Transcoder<T> transcoder) throws InterruptedException, MemcachedException {
    key = preProcessKey(key);
    byte[] keyBytes = this.checkStoreArguments(key, exp, value);
    try {
      this.sendStoreCommand(
          this.commandFactory.createReplaceCommand(key, keyBytes, exp, value, true, transcoder),
          this.opTimeout);
    } catch (TimeoutException e) {
      throw new MemcachedException(e);
    }

  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#replace(java.lang.String, int, T,
   * net.rubyeye.xmemcached.transcoders.Transcoder, long)
   */
  public final <T> boolean replace(String key, final int exp, final T value,
      final Transcoder<T> transcoder, final long timeout)
      throws TimeoutException, InterruptedException, MemcachedException {
    key = preProcessKey(key);
    byte[] keyBytes = this.checkStoreArguments(key, exp, value);
    return this.sendStoreCommand(
        this.commandFactory.createReplaceCommand(key, keyBytes, exp, value, false, transcoder),
        timeout);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#replace(java.lang.String, int, java.lang.Object)
   */
  public final boolean replace(final String key, final int exp, final Object value)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.replace(key, exp, value, this.opTimeout);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#replace(java.lang.String, int, java.lang.Object,
   * long)
   */
  @SuppressWarnings("unchecked")
  public final boolean replace(final String key, final int exp, final Object value,
      final long timeout) throws TimeoutException, InterruptedException, MemcachedException {
    return this.replace(key, exp, value, this.transcoder, timeout);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#replace(java.lang.String, int, T,
   * net.rubyeye.xmemcached.transcoders.Transcoder)
   */
  public final <T> boolean replace(final String key, final int exp, final T value,
      final Transcoder<T> transcoder)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.replace(key, exp, value, transcoder, this.opTimeout);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#append(java.lang.String, java.lang.Object)
   */
  public final boolean append(final String key, final Object value)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.append(key, value, this.opTimeout);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#append(java.lang.String, java.lang.Object, long)
   */
  public final boolean append(String key, final Object value, final long timeout)
      throws TimeoutException, InterruptedException, MemcachedException {
    key = preProcessKey(key);
    byte[] keyBytes = this.checkStoreArguments(key, 0, value);
    return this.sendStoreCommand(
        this.commandFactory.createAppendCommand(key, keyBytes, value, false, this.transcoder),
        timeout);
  }

  public void appendWithNoReply(String key, final Object value)
      throws InterruptedException, MemcachedException {
    key = preProcessKey(key);
    byte[] keyBytes = this.checkStoreArguments(key, 0, value);
    try {
      this.sendStoreCommand(
          this.commandFactory.createAppendCommand(key, keyBytes, value, true, this.transcoder),
          this.opTimeout);
    } catch (TimeoutException e) {
      throw new MemcachedException(e);
    }

  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#prepend(java.lang.String, java.lang.Object)
   */
  public final boolean prepend(final String key, final Object value)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.prepend(key, value, this.opTimeout);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#prepend(java.lang.String, java.lang.Object, long)
   */
  public final boolean prepend(String key, final Object value, final long timeout)
      throws TimeoutException, InterruptedException, MemcachedException {
    key = preProcessKey(key);
    byte[] keyBytes = this.checkStoreArguments(key, 0, value);
    return this.sendStoreCommand(
        this.commandFactory.createPrependCommand(key, keyBytes, value, false, this.transcoder),
        timeout);
  }

  public void prependWithNoReply(String key, final Object value)
      throws InterruptedException, MemcachedException {
    key = preProcessKey(key);
    byte[] keyBytes = this.checkStoreArguments(key, 0, value);
    try {
      this.sendStoreCommand(
          this.commandFactory.createPrependCommand(key, keyBytes, value, true, this.transcoder),
          this.opTimeout);
    } catch (TimeoutException e) {
      throw new MemcachedException(e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#cas(java.lang.String, int, java.lang.Object, long)
   */
  public final boolean cas(final String key, final int exp, final Object value, final long cas)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.cas(key, exp, value, this.opTimeout, cas);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#cas(java.lang.String, int, T,
   * net.rubyeye.xmemcached.transcoders.Transcoder, long, long)
   */
  public final <T> boolean cas(String key, final int exp, final T value,
      final Transcoder<T> transcoder, final long timeout, final long cas)
      throws TimeoutException, InterruptedException, MemcachedException {
    key = preProcessKey(key);
    byte[] keyBytes = this.checkStoreArguments(key, 0, value);
    return this.sendStoreCommand(
        this.commandFactory.createCASCommand(key, keyBytes, exp, value, cas, false, transcoder),
        timeout);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#cas(java.lang.String, int, java.lang.Object, long,
   * long)
   */
  @SuppressWarnings("unchecked")
  public final boolean cas(final String key, final int exp, final Object value, final long timeout,
      final long cas) throws TimeoutException, InterruptedException, MemcachedException {
    return this.cas(key, exp, value, this.transcoder, timeout, cas);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#cas(java.lang.String, int, T,
   * net.rubyeye.xmemcached.transcoders.Transcoder, long)
   */
  public final <T> boolean cas(final String key, final int exp, final T value,
      final Transcoder<T> transcoder, final long cas)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.cas(key, exp, value, transcoder, this.opTimeout, cas);
  }

  private final <T> boolean cas0(final String key, final int exp,
      final GetsResponse<T> getsResponse, final CASOperation<T> operation,
      final Transcoder<T> transcoder, final byte[] keyBytes, final boolean noreply)
      throws TimeoutException, InterruptedException, MemcachedException {
    if (operation == null) {
      throw new IllegalArgumentException("CASOperation could not be null");
    }
    if (operation.getMaxTries() < 0) {
      throw new IllegalArgumentException("max tries must be greater than 0");
    }
    int tryCount = 0;
    GetsResponse<T> result = getsResponse;
    if (result == null) {
      throw new NoValueException("Null GetsResponse for key=" + key);
    }
    while (tryCount <= operation.getMaxTries() && result != null
        && !this.sendStoreCommand(this.commandFactory.createCASCommand(key, keyBytes, exp,
            operation.getNewValue(result.getCas(), result.getValue()), result.getCas(), noreply,
            transcoder), this.opTimeout)
        && !noreply) {
      tryCount++;
      result = this.gets0(key, keyBytes, transcoder);
      if (result == null) {
        throw new NoValueException("could not gets the value for Key=" + key + " for cas");
      }
      if (tryCount > operation.getMaxTries()) {
        throw new TimeoutException("CAS try times is greater than max");
      }
    }
    return true;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#cas(java.lang.String, int,
   * net.rubyeye.xmemcached.CASOperation, net.rubyeye.xmemcached.transcoders.Transcoder)
   */
  public final <T> boolean cas(String key, final int exp, final CASOperation<T> operation,
      final Transcoder<T> transcoder)
      throws TimeoutException, InterruptedException, MemcachedException {
    key = preProcessKey(key);
    byte[] keyBytes = ByteUtils.getBytes(key);
    ByteUtils.checkKey(keyBytes);
    GetsResponse<T> result = this.gets0(key, keyBytes, transcoder);
    return this.cas0(key, exp, result, operation, transcoder, keyBytes, false);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#cas(java.lang.String, int,
   * net.rubyeye.xmemcached.GetsResponse, net.rubyeye.xmemcached.CASOperation,
   * net.rubyeye.xmemcached.transcoders.Transcoder)
   */
  public final <T> boolean cas(String key, final int exp, final GetsResponse<T> getsReponse,
      final CASOperation<T> operation, final Transcoder<T> transcoder)
      throws TimeoutException, InterruptedException, MemcachedException {
    key = preProcessKey(key);
    byte[] keyBytes = ByteUtils.getBytes(key);
    ByteUtils.checkKey(keyBytes);
    return this.cas0(key, exp, getsReponse, operation, transcoder, keyBytes, false);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#cas(java.lang.String, int,
   * net.rubyeye.xmemcached.GetsResponse, net.rubyeye.xmemcached.CASOperation)
   */
  @SuppressWarnings("unchecked")
  public final <T> boolean cas(final String key, final int exp, final GetsResponse<T> getsReponse,
      final CASOperation<T> operation)
      throws TimeoutException, InterruptedException, MemcachedException {

    return this.cas(key, exp, getsReponse, operation, this.transcoder);
  }

  public <T> void casWithNoReply(final String key, final CASOperation<T> operation)
      throws TimeoutException, InterruptedException, MemcachedException {
    this.casWithNoReply(key, 0, operation);
  }

  public <T> void casWithNoReply(final String key, final GetsResponse<T> getsResponse,
      final CASOperation<T> operation)
      throws TimeoutException, InterruptedException, MemcachedException {
    this.casWithNoReply(key, 0, getsResponse, operation);

  }

  @SuppressWarnings("unchecked")
  public <T> void casWithNoReply(String key, final int exp, final CASOperation<T> operation)
      throws TimeoutException, InterruptedException, MemcachedException {
    key = preProcessKey(key);
    byte[] keyBytes = ByteUtils.getBytes(key);
    GetsResponse<T> result = this.gets0(key, keyBytes, this.transcoder);
    this.casWithNoReply(key, exp, result, operation);

  }

  @SuppressWarnings("unchecked")
  public <T> void casWithNoReply(String key, final int exp, final GetsResponse<T> getsReponse,
      final CASOperation<T> operation)
      throws TimeoutException, InterruptedException, MemcachedException {
    key = preProcessKey(key);
    byte[] keyBytes = ByteUtils.getBytes(key);
    ByteUtils.checkKey(keyBytes);
    this.cas0(key, exp, getsReponse, operation, this.transcoder, keyBytes, true);

  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#cas(java.lang.String,
   * net.rubyeye.xmemcached.GetsResponse, net.rubyeye.xmemcached.CASOperation)
   */
  public final <T> boolean cas(final String key, final GetsResponse<T> getsReponse,
      final CASOperation<T> operation)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.cas(key, 0, getsReponse, operation);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#cas(java.lang.String, int,
   * net.rubyeye.xmemcached.CASOperation)
   */
  @SuppressWarnings("unchecked")
  public final <T> boolean cas(final String key, final int exp, final CASOperation<T> operation)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.cas(key, exp, operation, this.transcoder);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#cas(java.lang.String,
   * net.rubyeye.xmemcached.CASOperation)
   */
  public final <T> boolean cas(final String key, final CASOperation<T> operation)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.cas(key, 0, operation);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#delete(java.lang.String, int)
   */
  public final boolean delete(final String key, final int time)
      throws TimeoutException, InterruptedException, MemcachedException {
    return delete0(key, time, 0, false, this.opTimeout);
  }

  public boolean delete(final String key, final long opTimeout)
      throws TimeoutException, InterruptedException, MemcachedException {
    return delete0(key, 0, 0, false, opTimeout);
  }

  public boolean delete(final String key, final long cas, final long opTimeout)
      throws TimeoutException, InterruptedException, MemcachedException {
    return delete0(key, 0, cas, false, opTimeout);
  }

  /**
   * Delete key's data item from memcached.This method doesn't wait for reply
   *
   * @param key
   * @param time
   * @throws InterruptedException
   * @throws MemcachedException
   */
  public final void deleteWithNoReply(final String key, final int time)
      throws InterruptedException, MemcachedException {
    try {
      delete0(key, time, 0, true, this.opTimeout);
    } catch (TimeoutException e) {
      throw new MemcachedException(e);
    }
  }

  public final void deleteWithNoReply(final String key)
      throws InterruptedException, MemcachedException {
    this.deleteWithNoReply(key, 0);
  }

  private boolean delete0(String key, final int time, final long cas, final boolean noreply,
      final long opTimeout) throws MemcachedException, InterruptedException, TimeoutException {
    key = preProcessKey(key);
    final byte[] keyBytes = ByteUtils.getBytes(key);
    ByteUtils.checkKey(keyBytes);
    final Command command =
        this.commandFactory.createDeleteCommand(key, keyBytes, time, cas, noreply);
    final Session session = sendCommand(command);
    if (!command.isNoreply()) {
      latchWait(command, opTimeout, session);
      command.getIoBuffer().free();
      checkException(command);
      if (command.getResult() == null) {
        throw new MemcachedException("Operation fail,may be caused by networking or timeout");
      }
    } else {
      return false;
    }
    return (Boolean) command.getResult();
  }

  protected void checkException(final Command command) throws MemcachedException {
    if (command.getException() != null) {
      if (command.getException() instanceof MemcachedException) {
        throw (MemcachedException) command.getException();
      } else {
        throw new MemcachedException(command.getException());
      }
    }
  }

  public boolean touch(String key, final int exp, final long opTimeout)
      throws TimeoutException, InterruptedException, MemcachedException {
    key = preProcessKey(key);
    final byte[] keyBytes = ByteUtils.getBytes(key);
    ByteUtils.checkKey(keyBytes);
    CountDownLatch latch = new CountDownLatch(1);
    final Command command =
        this.commandFactory.createTouchCommand(key, keyBytes, latch, exp, false);
    latchWait(command, opTimeout, sendCommand(command));
    command.getIoBuffer().free();
    checkException(command);
    if (command.getResult() == null) {
      throw new MemcachedException("Operation fail,may be caused by networking or timeout");
    }
    return (Boolean) command.getResult();
  }

  public boolean touch(final String key, final int exp)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.touch(key, exp, this.opTimeout);
  }

  @SuppressWarnings("unchecked")
  public <T> T getAndTouch(String key, final int newExp, final long opTimeout)
      throws TimeoutException, InterruptedException, MemcachedException {
    key = preProcessKey(key);
    final byte[] keyBytes = ByteUtils.getBytes(key);
    ByteUtils.checkKey(keyBytes);
    CountDownLatch latch = new CountDownLatch(1);
    final Command command =
        this.commandFactory.createGetAndTouchCommand(key, keyBytes, latch, newExp, false);
    latchWait(command, opTimeout, sendCommand(command));
    command.getIoBuffer().free();
    checkException(command);
    CachedData data = (CachedData) command.getResult();
    if (data == null) {
      return null;
    }
    return (T) this.transcoder.decode(data);
  }

  @SuppressWarnings("unchecked")
  public <T> T getAndTouch(final String key, final int newExp)
      throws TimeoutException, InterruptedException, MemcachedException {
    return (T) this.getAndTouch(key, newExp, this.opTimeout);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#incr(java.lang.String, int)
   */
  public final long incr(String key, final long delta)
      throws TimeoutException, InterruptedException, MemcachedException {
    key = preProcessKey(key);
    return sendIncrOrDecrCommand(key, delta, 0, CommandType.INCR, false, this.opTimeout, 0);
  }

  public long incr(String key, final long delta, final long initValue)
      throws TimeoutException, InterruptedException, MemcachedException {
    key = preProcessKey(key);
    return sendIncrOrDecrCommand(key, delta, initValue, CommandType.INCR, false, this.opTimeout, 0);
  }

  public long incr(String key, final long delta, final long initValue, final long timeout)
      throws TimeoutException, InterruptedException, MemcachedException {
    key = preProcessKey(key);
    return sendIncrOrDecrCommand(key, delta, initValue, CommandType.INCR, false, timeout, 0);
  }

  public long incr(String key, final long delta, final long initValue, final long timeout,
      final int exp) throws TimeoutException, InterruptedException, MemcachedException {
    key = preProcessKey(key);
    return sendIncrOrDecrCommand(key, delta, initValue, CommandType.INCR, false, timeout, exp);
  }

  public final void incrWithNoReply(String key, final long delta)
      throws InterruptedException, MemcachedException {
    key = preProcessKey(key);
    try {
      sendIncrOrDecrCommand(key, delta, 0, CommandType.INCR, true, this.opTimeout, 0);
    } catch (TimeoutException e) {
      throw new MemcachedException(e);
    }
  }

  public final void decrWithNoReply(String key, final long delta)
      throws InterruptedException, MemcachedException {
    key = preProcessKey(key);
    try {
      sendIncrOrDecrCommand(key, delta, 0, CommandType.DECR, true, this.opTimeout, 0);
    } catch (TimeoutException e) {
      throw new MemcachedException(e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#decr(java.lang.String, int)
   */
  public final long decr(String key, final long delta)
      throws TimeoutException, InterruptedException, MemcachedException {
    key = preProcessKey(key);
    return sendIncrOrDecrCommand(key, delta, 0, CommandType.DECR, false, this.opTimeout, 0);
  }

  public long decr(String key, final long delta, final long initValue)
      throws TimeoutException, InterruptedException, MemcachedException {
    key = preProcessKey(key);
    return sendIncrOrDecrCommand(key, delta, initValue, CommandType.DECR, false, this.opTimeout, 0);
  }

  public long decr(String key, final long delta, final long initValue, final long timeout)
      throws TimeoutException, InterruptedException, MemcachedException {
    key = preProcessKey(key);
    return sendIncrOrDecrCommand(key, delta, initValue, CommandType.DECR, false, timeout, 0);
  }

  public long decr(String key, final long delta, final long initValue, final long timeout,
      final int exp) throws TimeoutException, InterruptedException, MemcachedException {
    key = preProcessKey(key);
    return sendIncrOrDecrCommand(key, delta, initValue, CommandType.DECR, false, timeout, exp);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#flushAll()
   */
  public final void flushAll() throws TimeoutException, InterruptedException, MemcachedException {
    this.flushAll(this.opTimeout);
  }

  public void flushAllWithNoReply() throws InterruptedException, MemcachedException {
    try {
      flushAllMemcachedServers(this.opTimeout, true, 0);
    } catch (TimeoutException e) {
      throw new MemcachedException(e);
    }
  }

  public void flushAllWithNoReply(final int exptime)
      throws InterruptedException, MemcachedException {
    try {
      flushAllMemcachedServers(this.opTimeout, true, exptime);
    } catch (TimeoutException e) {
      throw new MemcachedException(e);
    }
  }

  public void flushAllWithNoReply(final InetSocketAddress address)
      throws MemcachedException, InterruptedException {
    try {
      flushSpecialMemcachedServer(address, this.opTimeout, true, 0);
    } catch (TimeoutException e) {
      throw new MemcachedException(e);
    }
  }

  public void flushAllWithNoReply(final InetSocketAddress address, final int exptime)
      throws MemcachedException, InterruptedException {
    try {
      flushSpecialMemcachedServer(address, this.opTimeout, true, exptime);
    } catch (TimeoutException e) {
      throw new MemcachedException(e);
    }
  }

  public final void flushAll(final int exptime, final long timeout)
      throws TimeoutException, InterruptedException, MemcachedException {
    flushAllMemcachedServers(timeout, false, exptime);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#flushAll(long)
   */
  public final void flushAll(final long timeout)
      throws TimeoutException, InterruptedException, MemcachedException {
    flushAllMemcachedServers(timeout, false, 0);
  }

  private void flushAllMemcachedServers(final long timeout, final boolean noreply,
      final int exptime) throws MemcachedException, InterruptedException, TimeoutException {
    final Collection<Session> sessions = this.connector.getSessionSet();
    CountDownLatch latch = new CountDownLatch(sessions.size());
    List<Command> commands = new ArrayList<Command>(sessions.size());
    for (Session session : sessions) {
      if (session != null && !session.isClosed()) {
        Command command = this.commandFactory.createFlushAllCommand(latch, exptime, noreply);

        session.write(command);
      } else {
        latch.countDown();
      }
    }
    if (!noreply) {
      if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
        for (Command cmd : commands) {
          cmd.cancel();
        }
        throw new TimeoutException("Timed out waiting for operation");
      }
    }
  }

  public void setLoggingLevelVerbosity(final InetSocketAddress address, final int level)
      throws TimeoutException, InterruptedException, MemcachedException {
    setMemcachedLoggingLevel(address, level, false);

  }

  private void setMemcachedLoggingLevel(final InetSocketAddress address, final int level,
      final boolean noreply) throws MemcachedException, InterruptedException, TimeoutException {
    if (address == null) {
      throw new IllegalArgumentException("Null adderss");
    }
    CountDownLatch latch = new CountDownLatch(1);

    Queue<Session> sessionQueue = this.connector.getSessionByAddress(address);
    if (sessionQueue == null || sessionQueue.peek() == null) {
      throw new MemcachedException(
          "could not find session for " + SystemUtils.getRawAddress(address) + ":"
              + address.getPort() + ",maybe it have not been connected");
    }

    Command command = this.commandFactory.createVerbosityCommand(latch, level, noreply);
    final Session session = sessionQueue.peek();
    session.write(command);
    if (!noreply) {
      latchWait(command, this.opTimeout, session);
    }
  }

  public void setLoggingLevelVerbosityWithNoReply(final InetSocketAddress address, final int level)
      throws InterruptedException, MemcachedException {
    try {
      setMemcachedLoggingLevel(address, level, true);
    } catch (TimeoutException e) {
      throw new MemcachedException(e);
    }

  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#flushAll(java.net. InetSocketAddress )
   */
  public final void flushAll(final InetSocketAddress address)
      throws MemcachedException, InterruptedException, TimeoutException {
    this.flushAll(address, this.opTimeout);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#flushAll(java.net. InetSocketAddress , long)
   */
  public final void flushAll(final InetSocketAddress address, final long timeout)
      throws MemcachedException, InterruptedException, TimeoutException {
    flushSpecialMemcachedServer(address, timeout, false, 0);
  }

  public final void flushAll(final InetSocketAddress address, final long timeout, final int exptime)
      throws MemcachedException, InterruptedException, TimeoutException {
    flushSpecialMemcachedServer(address, timeout, false, exptime);
  }

  private void flushSpecialMemcachedServer(final InetSocketAddress address, final long timeout,
      final boolean noreply, final int exptime)
      throws MemcachedException, InterruptedException, TimeoutException {
    if (address == null) {
      throw new IllegalArgumentException("Null adderss");
    }
    CountDownLatch latch = new CountDownLatch(1);

    Queue<Session> sessionQueue = this.connector.getSessionByAddress(address);
    if (sessionQueue == null || sessionQueue.peek() == null) {
      throw new MemcachedException(
          "could not find session for " + SystemUtils.getRawAddress(address) + ":"
              + address.getPort() + ",maybe it have not been connected");
    }
    Command command = this.commandFactory.createFlushAllCommand(latch, exptime, noreply);
    final Session session = sessionQueue.peek();
    session.write(command);
    if (!noreply) {
      latchWait(command, timeout, session);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#flushAll(java.lang.String)
   */
  public final void flushAll(final String host)
      throws TimeoutException, InterruptedException, MemcachedException {
    this.flushAll(AddrUtil.getOneAddress(host), this.opTimeout);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#stats(java.net.InetSocketAddress)
   */
  public final Map<String, String> stats(final InetSocketAddress address)
      throws MemcachedException, InterruptedException, TimeoutException {
    return this.stats(address, this.opTimeout);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#stats(java.net.InetSocketAddress, long)
   */
  @SuppressWarnings("unchecked")
  public final Map<String, String> stats(final InetSocketAddress address, final long timeout)
      throws MemcachedException, InterruptedException, TimeoutException {
    if (address == null) {
      throw new IllegalArgumentException("Null inetSocketAddress");
    }
    CountDownLatch latch = new CountDownLatch(1);

    Queue<Session> sessionQueue = this.connector.getSessionByAddress(address);
    if (sessionQueue == null || sessionQueue.peek() == null) {
      throw new MemcachedException(
          "could not find session for " + SystemUtils.getRawAddress(address) + ":"
              + address.getPort() + ",maybe it have not been connected");
    }
    Command command = this.commandFactory.createStatsCommand(address, latch, null);
    final Session session = sessionQueue.peek();
    session.write(command);
    latchWait(command, timeout, session);
    return (Map<String, String>) command.getResult();
  }

  public final Map<InetSocketAddress, Map<String, String>> getStats()
      throws MemcachedException, InterruptedException, TimeoutException {
    return this.getStats(this.opTimeout);
  }

  public final Map<InetSocketAddress, Map<String, String>> getStatsByItem(final String itemName)
      throws MemcachedException, InterruptedException, TimeoutException {
    return this.getStatsByItem(itemName, this.opTimeout);
  }

  @SuppressWarnings("unchecked")
  public final Map<InetSocketAddress, Map<String, String>> getStatsByItem(final String itemName,
      final long timeout) throws MemcachedException, InterruptedException, TimeoutException {
    final Set<Session> sessionSet = this.connector.getSessionSet();
    final Map<InetSocketAddress, Map<String, String>> collectResult =
        new HashMap<InetSocketAddress, Map<String, String>>();
    if (sessionSet.size() == 0) {
      return collectResult;
    }
    final CountDownLatch latch = new CountDownLatch(sessionSet.size());
    List<Command> commands = new ArrayList<Command>(sessionSet.size());
    for (Session session : sessionSet) {
      Command command =
          this.commandFactory.createStatsCommand(session.getRemoteSocketAddress(), latch, itemName);

      session.write(command);
      commands.add(command);

    }
    if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
      for (Command command : commands) {
        command.cancel();
      }
      throw new TimeoutException("Timed out waiting for operation");
    }
    for (Command command : commands) {
      checkException(command);
      collectResult.put(((ServerAddressAware) command).getServer(),
          (Map<String, String>) command.getResult());
    }
    return collectResult;
  }

  public final Map<InetSocketAddress, String> getVersions()
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.getVersions(this.opTimeout);
  }

  public final Map<InetSocketAddress, String> getVersions(final long timeout)
      throws TimeoutException, InterruptedException, MemcachedException {
    final Set<Session> sessionSet = this.connector.getSessionSet();
    Map<InetSocketAddress, String> collectResult = new HashMap<InetSocketAddress, String>();
    if (sessionSet.size() == 0) {
      return collectResult;
    }
    final CountDownLatch latch = new CountDownLatch(sessionSet.size());
    List<Command> commands = new ArrayList<Command>(sessionSet.size());
    for (Session session : sessionSet) {
      Command command =
          this.commandFactory.createVersionCommand(latch, session.getRemoteSocketAddress());
      session.write(command);
      commands.add(command);

    }

    if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
      for (Command command : commands) {
        command.cancel();
      }
      throw new TimeoutException("Timed out waiting for operation");
    }
    for (Command command : commands) {
      checkException(command);
      collectResult.put(((ServerAddressAware) command).getServer(), (String) command.getResult());
    }
    return collectResult;
  }

  public Map<InetSocketAddress, Map<String, String>> getStats(final long timeout)
      throws MemcachedException, InterruptedException, TimeoutException {
    return this.getStatsByItem(null, timeout);
  }

  /**
   * For subclass override.
   */
  protected void shutdown0() {

  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#shutdown()
   */
  public final void shutdown() throws IOException {
    if (this.shutdown) {
      return;
    }
    shutdown0();
    this.shutdown = true;
    this.connector.shuttingDown();
    this.connector.quitAllSessions();
    this.connector.stop();
    this.memcachedHandler.stop();
    XMemcachedMbeanServer.getInstance().shutdown();
    if (AddrUtil.isEnableShutDownHook() && !this.isHutdownHookCalled) {
      try {
        Runtime.getRuntime().removeShutdownHook(this.shutdownHookThread);
      } catch (Exception e) {
        // ignore;
      }
    }
  }

  private long sendIncrOrDecrCommand(final String key, final long delta, final long initValue,
      final CommandType cmdType, final boolean noreply, final long operationTimeout, final int exp)
      throws InterruptedException, TimeoutException, MemcachedException {
    final byte[] keyBytes = ByteUtils.getBytes(key);
    ByteUtils.checkKey(keyBytes);
    final Command command = this.commandFactory.createIncrDecrCommand(key, keyBytes, delta,
        initValue, exp, cmdType, noreply);
    final Session session = sendCommand(command);
    if (!command.isNoreply()) {
      latchWait(command, operationTimeout, session);
      command.getIoBuffer().free();
      checkException(command);
      if (command.getResult() == null) {
        throw new MemcachedException("Operation fail,may be caused by networking or timeout");
      }
      final Object result = command.getResult();
      if (result instanceof String) {
        if (((String) result).equals("NOT_FOUND")) {
          if (this.add0(key, exp, String.valueOf(initValue), this.transcoder, this.opTimeout)) {
            return initValue;
          } else {
            return sendIncrOrDecrCommand(key, delta, initValue, cmdType, noreply, operationTimeout,
                exp);
          }
        } else {
          throw new MemcachedException(
              "Unknown result type for incr/decr:" + result.getClass() + ",result=" + result);
        }
      } else {
        return (Long) command.getResult();
      }
    } else {
      return -1;
    }
  }

  public void setConnectionPoolSize(final int poolSize) {
    if (!this.shutdown && getAvaliableServers().size() > 0) {
      throw new IllegalStateException("Xmemcached client has been started");
    }
    if (poolSize <= 0) {
      throw new IllegalArgumentException("poolSize<=0");
    }
    this.connectionPoolSize = poolSize;
    this.connector.setConnectionPoolSize(poolSize);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#delete(java.lang.String)
   */
  public final boolean delete(final String key)
      throws TimeoutException, InterruptedException, MemcachedException {
    return this.delete(key, 0);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#getTranscoder()
   */
  @SuppressWarnings("unchecked")
  public final Transcoder getTranscoder() {
    return this.transcoder;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.rubyeye.xmemcached.MemcachedClient#setTranscoder(net.rubyeye. xmemcached
   * .transcoders.Transcoder)
   */
  @SuppressWarnings("unchecked")
  public final void setTranscoder(final Transcoder transcoder) {
    this.transcoder = transcoder;
  }

  private final <T> boolean sendStoreCommand(final Command command, final long timeout)
      throws InterruptedException, TimeoutException, MemcachedException {

    final Session session = sendCommand(command);
    if (!command.isNoreply()) {
      latchWait(command, timeout, session);
      command.getIoBuffer().free();
      checkException(command);
      if (command.getResult() == null) {
        throw new MemcachedException("Operation fail,may be caused by networking or timeout");
      }
    } else {
      return false;
    }
    return (Boolean) command.getResult();
  }

  private static final String CONTINUOUS_TIMEOUT_COUNTER = "ContinuousTimeouts";

  protected void latchWait(final Command cmd, final long timeout, final Session session)
      throws InterruptedException, TimeoutException {
    if (cmd.getLatch().await(timeout, TimeUnit.MILLISECONDS)) {
      AtomicInteger counter = getContinuousTimeoutCounter(session);
      // reset counter.
      if (counter.get() > 0) {
        counter.set(0);
      }
    } else {
      cmd.cancel();
      AtomicInteger counter = getContinuousTimeoutCounter(session);
      if (counter.incrementAndGet() > this.timeoutExceptionThreshold) {
        log.warn(session + " exceeded continuous timeout threshold,we will close it.");
        try {
          // reset counter.
          counter.set(0);
          session.close();
        } catch (Exception e) {
          // ignore it.
        }
      }
      throw new TimeoutException("Timed out(" + timeout
          + " milliseconds) waiting for operation while connected to " + session);
    }
  }

  private AtomicInteger getContinuousTimeoutCounter(final Session session) {
    AtomicInteger counter = (AtomicInteger) session.getAttribute(CONTINUOUS_TIMEOUT_COUNTER);
    if (counter == null) {
      counter = new AtomicInteger(0);
      AtomicInteger oldCounter =
          (AtomicInteger) session.setAttributeIfAbsent(CONTINUOUS_TIMEOUT_COUNTER, counter);
      if (oldCounter != null) {
        counter = oldCounter;
      }
    }
    return counter;
  }

  /**
   * Use getAvailableServers() instead
   *
   * @deprecated
   * @see MemcachedClient#getAvailableServers()
   */
  @Deprecated
  public final Collection<InetSocketAddress> getAvaliableServers() {
    return getAvailableServers();
  }

  public Collection<InetSocketAddress> getAvailableServers() {
    Set<Session> sessionSet = this.connector.getSessionSet();
    Set<InetSocketAddress> result = new HashSet<InetSocketAddress>();
    for (Session session : sessionSet) {
      result.add(session.getRemoteSocketAddress());
    }
    return Collections.unmodifiableSet(result);
  }

  public final int getConnectionSizeBySocketAddress(final InetSocketAddress address) {
    Queue<Session> sessionList = this.connector.getSessionByAddress(address);
    return sessionList == null ? 0 : sessionList.size();
  }

  public void addStateListener(final MemcachedClientStateListener listener) {
    MemcachedClientStateListenerAdapter adapter =
        new MemcachedClientStateListenerAdapter(listener, this);
    this.stateListenerAdapters.add(adapter);
    this.connector.addStateListener(adapter);
  }

  public Collection<MemcachedClientStateListener> getStateListeners() {
    final List<MemcachedClientStateListener> result =
        new ArrayList<MemcachedClientStateListener>(this.stateListenerAdapters.size());
    for (MemcachedClientStateListenerAdapter adapter : this.stateListenerAdapters) {
      result.add(adapter.getMemcachedClientStateListener());
    }
    return result;
  }

  public void setPrimitiveAsString(final boolean primitiveAsString) {
    this.transcoder.setPrimitiveAsString(primitiveAsString);
  }

  public void removeStateListener(final MemcachedClientStateListener listener) {
    for (MemcachedClientStateListenerAdapter adapter : this.stateListenerAdapters) {
      if (adapter.getMemcachedClientStateListener().equals(listener)) {
        this.stateListenerAdapters.remove(adapter);
        this.connector.removeStateListener(adapter);
      }
    }
  }

  public Protocol getProtocol() {
    return this.commandFactory.getProtocol();
  }

  public boolean isSanitizeKeys() {
    return this.sanitizeKeys;
  }

  public void setSanitizeKeys(final boolean sanitizeKeys) {
    this.sanitizeKeys = sanitizeKeys;
  }

  private String decodeKey(String key)
      throws MemcachedException, InterruptedException, TimeoutException {
    try {
      key = this.sanitizeKeys ? URLDecoder.decode(key, "UTF-8") : key;
    } catch (UnsupportedEncodingException e) {
      throw new MemcachedException("Unsupport encoding utf-8 when decodeKey", e);
    }
    String ns = NAMESPACE_LOCAL.get();
    if (ns != null && ns.trim().length() > 0) {
      String nsValue = getNamespace(ns);
      try {
        if (nsValue != null && key.startsWith(nsValue)) {
          // The extra length of ':'
          key = key.substring(nsValue.length() + 1);
        } else {
          return null;
        }
      } catch (Exception e) {
        throw new MemcachedException("Exception occured when decode key.", e);
      }
    }
    return key;
  }

  private String preProcessKey(String key) throws MemcachedException, InterruptedException {
    key = this.keyProvider.process(key);
    try {
      key = this.sanitizeKeys ? URLEncoder.encode(key, "UTF-8") : key;
    } catch (UnsupportedEncodingException e) {
      throw new MemcachedException("Unsupport encoding utf-8 when sanitize key", e);
    }
    String ns = NAMESPACE_LOCAL.get();
    if (ns != null && ns.trim().length() > 0) {
      try {
        key = getNamespace(ns) + ":" + key;
      } catch (TimeoutException e) {
        throw new MemcachedException("Timeout occured when gettting namespace value.", e);
      }
    }
    return key;
  }

  public void invalidateNamespace(final String ns, final long opTimeout)
      throws MemcachedException, InterruptedException, TimeoutException {
    String key = this.keyProvider.process(getNSKey(ns));
    sendIncrOrDecrCommand(key, 1, System.nanoTime(), CommandType.INCR, false, opTimeout, 0);
  }

  public void invalidateNamespace(final String ns)
      throws MemcachedException, InterruptedException, TimeoutException {
    this.invalidateNamespace(ns, this.opTimeout);
  }

  /**
   * Returns the real namespace of ns.
   *
   * @param ns
   * @return
   * @throws TimeoutException
   * @throws InterruptedException
   * @throws MemcachedException
   */
  public String getNamespace(final String ns)
      throws TimeoutException, InterruptedException, MemcachedException {
    String key = this.keyProvider.process(getNSKey(ns));
    byte[] keyBytes = ByteUtils.getBytes(key);
    ByteUtils.checkKey(keyBytes);
    Object item = this.fetch0(key, keyBytes, CommandType.GET_ONE, this.opTimeout, this.transcoder);
    while (item == null) {
      item = String.valueOf(System.nanoTime());
      boolean added = this.add0(key, 0, item, this.transcoder, this.opTimeout);
      if (!added) {
        item = this.fetch0(key, keyBytes, CommandType.GET_ONE, this.opTimeout, this.transcoder);
      }
    }
    String namespace = item.toString();
    if (!ByteUtils.isNumber(namespace)) {
      throw new IllegalStateException(
          "Namespace key already has value.The key is:" + key + ",and the value is:" + namespace);
    }
    return namespace;
  }

  private String getNSKey(final String ns) {
    String key = "namespace:" + ns;
    return key;
  }

  public Counter getCounter(final String key, final long initialValue) {
    return new Counter(this, key, initialValue);
  }

  public Counter getCounter(final String key) {
    return new Counter(this, key, 0);
  }

  /**
   * @deprecated memcached 1.6.x will remove cachedump stats command,so this method will be removed
   *             in the future
   */
  @Deprecated
  @SuppressWarnings("unchecked")
  public KeyIterator getKeyIterator(final InetSocketAddress address)
      throws MemcachedException, TimeoutException, InterruptedException {
    if (address == null) {
      throw new IllegalArgumentException("null address");
    }
    Queue<Session> sessions = this.connector.getSessionByAddress(address);
    if (sessions == null || sessions.size() == 0) {
      throw new MemcachedException(
          "The special memcached server has not been connected," + address);
    }
    Session session = sessions.peek();
    CountDownLatch latch = new CountDownLatch(1);
    Command command =
        this.commandFactory.createStatsCommand(session.getRemoteSocketAddress(), latch, "items");
    session.write(command);
    if (!latch.await(5000, TimeUnit.MILLISECONDS)) {
      throw new TimeoutException("Operation timeout");
    }
    if (command.getException() != null) {
      if (command.getException() instanceof MemcachedException) {
        throw (MemcachedException) command.getException();
      } else {
        throw new MemcachedException("stats items failed", command.getException());
      }
    }
    Map<String, String> result = (Map<String, String>) command.getResult();
    LinkedList<Integer> itemNumberList = new LinkedList<Integer>();
    for (Map.Entry<String, String> entry : result.entrySet()) {
      final String key = entry.getKey();
      final String[] keys = key.split(":");
      if (keys.length == 3 && keys[2].equals("number") && keys[0].equals("items")) {
        // has items,then add it to itemNumberList
        if (Integer.parseInt(entry.getValue()) > 0) {
          itemNumberList.add(Integer.parseInt(keys[1]));
        }
      }
    }
    return new KeyIteratorImpl(itemNumberList, this, address);
  }

  public void setEnableHealSession(final boolean enableHealSession) {
    if (this.connector != null) {
      this.connector.setEnableHealSession(enableHealSession);
    } else {
      throw new IllegalStateException("The client has not been started.");
    }
  }

  public void setFailureMode(final boolean failureMode) {
    this.failureMode = failureMode;
    if (this.sessionLocator != null) {
      this.sessionLocator.setFailureMode(failureMode);
    }
    if (this.connector != null) {
      this.connector.setFailureMode(failureMode);
    }
  }

  public boolean isFailureMode() {
    return this.failureMode;
  }

  public Queue<ReconnectRequest> getReconnectRequestQueue() {
    return this.connector != null ? this.connector.getReconnectRequestQueue() : null;
  }

}
