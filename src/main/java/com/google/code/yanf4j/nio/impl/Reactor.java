/*
 * 
 */

package com.google.code.yanf4j.nio.impl;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Date;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.EventType;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.nio.NioSession;
import com.google.code.yanf4j.util.SystemUtils;

/**
 * Reactor pattern
 * 
 * @author dennis
 * 
 */
public final class Reactor extends Thread {
  /**
   * JVM bug threshold
   */
  public static final int JVMBUG_THRESHHOLD =
      Integer.getInteger("com.googlecode.yanf4j.nio.JVMBUG_THRESHHOLD", 128);
  public static final int JVMBUG_THRESHHOLD2 = JVMBUG_THRESHHOLD * 2;
  public static final int JVMBUG_THRESHHOLD1 = (JVMBUG_THRESHHOLD2 + JVMBUG_THRESHHOLD) / 2;
  public static final int DEFAULT_WAIT = 1000;

  private static final Logger log = LoggerFactory.getLogger("remoting");

  private boolean jvmBug0;
  private boolean jvmBug1;

  private final int reactorIndex;

  private final SelectorManager selectorManager;

  private final AtomicInteger jvmBug = new AtomicInteger(0);

  private long lastJVMBug;

  private Selector selector;

  private final NioController controller;

  private final Configuration configuration;

  static public class PaddingAtomicBoolean extends AtomicBoolean {

    /**
    	 * 
    	 */
    private static final long serialVersionUID = 5227571972657902891L;
    public int p1;
    public long p2, p3, p4, p5, p6, p7, p8;

    PaddingAtomicBoolean(boolean v) {
      super(v);
    }
  }

  private final AtomicBoolean wakenUp = new PaddingAtomicBoolean(false);

  public static class RegisterEvent {
    SelectableChannel channel;
    int ops;
    EventType eventType;
    Object attachment;
    Session session;

    public RegisterEvent(SelectableChannel channel, int ops, Object attachment) {
      super();
      this.channel = channel;
      this.ops = ops;
      this.attachment = attachment;
    }

    public RegisterEvent(Session session, EventType eventType) {
      super();
      this.session = session;
      this.eventType = eventType;
    }
  }

  private Queue<RegisterEvent> register;

  private final Lock gate = new ReentrantLock();

  private int selectTries = 0;

  private long nextTimeout = 0;

  private long lastCheckTimestamp = 0L;

  Reactor(SelectorManager selectorManager, Configuration configuration, int index)
      throws IOException {
    super();
    reactorIndex = index;
    this.register = (Queue<Reactor.RegisterEvent>) SystemUtils.createTransferQueue();
    this.selectorManager = selectorManager;
    controller = selectorManager.getController();
    selector = SystemUtils.openSelector();
    this.configuration = configuration;
    setName("Xmemcached-Reactor-" + index);
    setDaemon(true);
  }

  public final Selector getSelector() {
    return selector;
  }

  public int getReactorIndex() {
    return reactorIndex;
  }

  @Override
  public void run() {
    selectorManager.notifyReady();
    while (selectorManager.isStarted() && selector.isOpen()) {
      try {
        beforeSelect();
        wakenUp.set(false);
        long before = -1;
        // Wether to look jvm bug
        if (SystemUtils.isLinuxPlatform() && !SystemUtils.isAfterJava6u4Version()) {
          before = System.currentTimeMillis();
        }
        long wait = DEFAULT_WAIT;
        if (nextTimeout > 0) {
          wait = nextTimeout;
        }
        int selected = selector.select(wait);
        if (selected == 0) {
          if (before != -1) {
            lookJVMBug(before, selected, wait);
          }
          selectTries++;
          // check timeout and idle
          nextTimeout = checkSessionTimeout();
          continue;
        } else {
          selectTries = 0;
        }

      } catch (ClosedSelectorException e) {
        break;
      } catch (IOException e) {
        log.error("Reactor select error", e);
        if (selector.isOpen()) {
          continue;
        } else {
          break;
        }
      }
      Set<SelectionKey> selectedKeys = selector.selectedKeys();
      gate.lock();
      try {
        postSelect(selectedKeys, selector.keys());
        dispatchEvent(selectedKeys);
      } finally {
        gate.unlock();
      }
    }
    if (selector != null) {
      if (selector.isOpen()) {
        try {
          controller.closeChannel(selector);
          selector.close();
        } catch (IOException e) {
          controller.notifyException(e);
          log.error("stop reactor error", e);
        }
      }
    }

  }

  /**
   * Look jvm bug
   * 
   * @param before
   * @param selected
   * @param wait
   * @return
   * @throws IOException
   */
  private boolean lookJVMBug(long before, int selected, long wait) throws IOException {
    boolean seeing = false;
    long now = System.currentTimeMillis();

    if (JVMBUG_THRESHHOLD > 0 && selected == 0 && wait > JVMBUG_THRESHHOLD
        && now - before < wait / 4 && !wakenUp.get() /* waken up */
        && !Thread.currentThread().isInterrupted()/* Interrupted */) {
      jvmBug.incrementAndGet();
      if (jvmBug.get() >= JVMBUG_THRESHHOLD2) {
        gate.lock();
        try {
          lastJVMBug = now;
          log.warn("JVM bug occured at " + new Date(lastJVMBug)
              + ",http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6403933,reactIndex="
              + reactorIndex);
          if (jvmBug1) {
            log.debug("seeing JVM BUG(s) - recreating selector,reactIndex=" + reactorIndex);
          } else {
            jvmBug1 = true;
            log.info("seeing JVM BUG(s) - recreating selector,reactIndex=" + reactorIndex);
          }
          seeing = true;
          final Selector new_selector = SystemUtils.openSelector();

          for (SelectionKey k : selector.keys()) {
            if (!k.isValid() || k.interestOps() == 0) {
              continue;
            }

            final SelectableChannel channel = k.channel();
            final Object attachment = k.attachment();

            channel.register(new_selector, k.interestOps(), attachment);
          }

          selector.close();
          selector = new_selector;

        } finally {
          gate.unlock();
        }
        jvmBug.set(0);

      } else if (jvmBug.get() == JVMBUG_THRESHHOLD || jvmBug.get() == JVMBUG_THRESHHOLD1) {
        if (jvmBug0) {
          log.debug("seeing JVM BUG(s) - cancelling interestOps==0,reactIndex=" + reactorIndex);
        } else {
          jvmBug0 = true;
          log.info("seeing JVM BUG(s) - cancelling interestOps==0,reactIndex=" + reactorIndex);
        }
        gate.lock();
        seeing = true;
        try {
          for (SelectionKey k : selector.keys()) {
            if (k.isValid() && k.interestOps() == 0) {
              k.cancel();
            }
          }
        } finally {
          gate.unlock();
        }
      }
    } else {
      jvmBug.set(0);
    }
    return seeing;
  }

  /**
   * Dispatch selected event
   * 
   * @param selectedKeySet
   */
  public final void dispatchEvent(Set<SelectionKey> selectedKeySet) {
    Iterator<SelectionKey> it = selectedKeySet.iterator();
    boolean skipOpRead = false;
    while (it.hasNext()) {
      SelectionKey key = it.next();
      it.remove();
      if (!key.isValid()) {
        if (key.attachment() != null) {
          controller.closeSelectionKey(key);
        } else {
          key.cancel();
        }
        continue;
      }
      try {
        if (key.isValid() && key.isAcceptable()) {
          controller.onAccept(key);
          continue;
        }
        if (key.isValid() && (key.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
          // Remove write interest
          key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
          controller.onWrite(key);
          if (!controller.isHandleReadWriteConcurrently()) {
            skipOpRead = true;
          }
        }
        if (!skipOpRead && key.isValid()
            && (key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
          key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
          if (!controller.getStatistics().isReceiveOverFlow()) {
            // Remove read interest
            controller.onRead(key);
          } else {
            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
          }

        }
        if ((key.readyOps() & SelectionKey.OP_CONNECT) == SelectionKey.OP_CONNECT) {
          controller.onConnect(key);
        }

      } catch (CancelledKeyException e) {
        // ignore
      } catch (RejectedExecutionException e) {

        if (key.attachment() instanceof AbstractNioSession) {
          ((AbstractNioSession) key.attachment()).onException(e);
        }
        controller.notifyException(e);
        if (selector.isOpen()) {
          continue;
        } else {
          break;
        }
      } catch (Exception e) {
        if (key.attachment() instanceof AbstractNioSession) {
          ((AbstractNioSession) key.attachment()).onException(e);
        }
        controller.closeSelectionKey(key);
        controller.notifyException(e);
        log.error("Reactor dispatch events error", e);
        if (selector.isOpen()) {
          continue;
        } else {
          break;
        }
      }
    }
  }

  final void unregisterChannel(SelectableChannel channel) throws IOException {
    Selector selector = this.selector;
    if (selector != null) {
      if (channel != null) {
        SelectionKey key = channel.keyFor(selector);
        if (key != null) {
          key.cancel();
        }
      }
    }
    wakeup();
  }

  /**
   * Check session timeout or idle
   * 
   * @return
   */
  private final long checkSessionTimeout() {
    long nextTimeout = 0;
    if (configuration.getCheckSessionTimeoutInterval() > 0) {
      gate.lock();
      try {
        if (isNeedCheckSessionIdleTimeout()) {
          nextTimeout = configuration.getCheckSessionTimeoutInterval();
          for (SelectionKey key : selector.keys()) {

            if (key.attachment() != null) {
              long n = checkExpiredIdle(key, getSessionFromAttchment(key));
              nextTimeout = n < nextTimeout ? n : nextTimeout;
            }
          }
          selectTries = 0;
          lastCheckTimestamp = System.currentTimeMillis();
        }
      } finally {
        gate.unlock();
      }
    }
    return nextTimeout;
  }

  private boolean isNeedCheckSessionIdleTimeout() {
    return selectTries * 1000 >= configuration.getCheckSessionTimeoutInterval()
        || System.currentTimeMillis() - this.lastCheckTimestamp >= configuration
            .getCheckSessionTimeoutInterval();
  }

  private final Session getSessionFromAttchment(SelectionKey key) {
    if (key.attachment() instanceof Session) {
      return (Session) key.attachment();
    }
    return null;
  }

  public final void registerSession(Session session, EventType event) {
    final Selector selector = this.selector;
    if (isReactorThread() && selector != null) {
      dispatchSessionEvent(session, event);
    } else {
      register.offer(new RegisterEvent(session, event));
      wakeup();
    }
  }

  private final boolean isReactorThread() {
    return Thread.currentThread() == this;
  }

  final void beforeSelect() {
    controller.checkStatisticsForRestart();
    processRegister();
  }

  private final void processRegister() {
    RegisterEvent event = null;
    while ((event = register.poll()) != null) {
      if (event.session != null) {
        dispatchSessionEvent(event.session, event.eventType);
      } else {
        registerChannelNow(event.channel, event.ops, event.attachment);
      }
    }
  }

  Configuration getConfiguration() {
    return configuration;
  }

  private final void dispatchSessionEvent(Session session, EventType event) {
    if (session.isClosed() && event != EventType.UNREGISTER) {
      return;
    }
    switch (event) {
      case REGISTER:
        controller.registerSession(session);
        break;
      case UNREGISTER:
        controller.unregisterSession(session);
        break;
      default:
        ((NioSession) session).onEvent(event, selector);
        break;
    }
  }

  public final void postSelect(Set<SelectionKey> selectedKeys, Set<SelectionKey> allKeys) {
    if (controller.getSessionTimeout() > 0 || controller.getSessionIdleTimeout() > 0) {
      if (isNeedCheckSessionIdleTimeout()) {
        for (SelectionKey key : allKeys) {
          if (!selectedKeys.contains(key)) {
            if (key.attachment() != null) {
              checkExpiredIdle(key, getSessionFromAttchment(key));
            }
          }
        }
        lastCheckTimestamp = System.currentTimeMillis();
      }
    }
  }

  private long checkExpiredIdle(SelectionKey key, Session session) {
    if (session == null) {
      return 0;
    }
    long nextTimeout = 0;
    boolean expired = false;
    if (controller.getSessionTimeout() > 0) {
      expired = checkExpired(key, session);
      nextTimeout = controller.getSessionTimeout();
    }
    if (controller.getSessionIdleTimeout() > 0 && !expired) {
      checkIdle(session);
      nextTimeout = controller.getSessionIdleTimeout();
    }
    return nextTimeout;
  }

  private final void checkIdle(Session session) {
    if (controller.getSessionIdleTimeout() > 0) {
      if (session.isIdle()) {
        ((NioSession) session).onEvent(EventType.IDLE, selector);
      }
    }
  }

  private final boolean checkExpired(SelectionKey key, Session session) {
    if (session != null && session.isExpired()) {
      ((NioSession) session).onEvent(EventType.EXPIRED, selector);
      controller.closeSelectionKey(key);
      return true;
    }
    return false;
  }

  public final void registerChannel(SelectableChannel channel, int ops, Object attachment) {
    if (isReactorThread()) {
      registerChannelNow(channel, ops, attachment);
    } else {
      register.offer(new RegisterEvent(channel, ops, attachment));
      wakeup();
    }

  }

  private void registerChannelNow(SelectableChannel channel, int ops, Object attachment) {
    if (channel.isOpen()) {
      gate.lock();
      try {
        channel.register(selector, ops, attachment);

      } catch (ClosedChannelException e) {
        log.error("Register channel error", e);
        controller.notifyException(e);
      } finally {
        gate.unlock();
      }
    }
  }

  final void wakeup() {
    if (wakenUp.compareAndSet(false, true)) {
      final Selector selector = this.selector;
      if (selector != null) {
        selector.wakeup();
      }
    }
  }

  final void selectNow() throws IOException {
    final Selector selector = this.selector;
    if (selector != null) {
      selector.selectNow();
    }
  }
}
