/**
 * Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)] Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License
 */
package net.rubyeye.xmemcached.impl;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import com.google.code.yanf4j.core.Session;
import net.rubyeye.xmemcached.HashAlgorithm;
import net.rubyeye.xmemcached.networking.MemcachedSession;
import net.rubyeye.xmemcached.utils.InetSocketAddressWrapper;

/**
 * ConnectionFactory instance that sets up a ketama compatible connection.
 * 
 * <p>
 * This implementation piggy-backs on the functionality of the <code>DefaultConnectionFactory</code>
 * in terms of connections and queue handling. Where it differs is that it uses both the <code>
 * KetamaNodeLocator</code> and the <code>HashAlgorithm.KETAMA_HASH</code> to provide consistent
 * node hashing.
 * 
 * @see http://www.last.fm/user/RJ/journal/2007/04/10/392555/
 * 
 *      </p>
 */
/**
 * Consistent Hash Algorithm implementation,based on TreeMap.tailMap(hash) method.
 * 
 * @author dennis
 * 
 */
public class KetamaMemcachedSessionLocator extends AbstractMemcachedSessionLocator {

  static final int NUM_REPS = 160;
  private transient volatile TreeMap<Long, List<Session>> ketamaSessions =
      new TreeMap<Long, List<Session>>();
  private final HashAlgorithm hashAlg;
  private int maxTries;

  /**
   * compatible with nginx-upstream-consistent,patched by wolfg1969
   */
  static final int DEFAULT_PORT = 11211;
  private final boolean cwNginxUpstreamConsistent;
  private final boolean gwhalinMemcachedJavaClientCompatibiltyConsistent;
  private boolean useRemoteIPConsistent = false;

  /**
   * Create a KetamaMemcachedSessionLocator with default config.
   */
  public KetamaMemcachedSessionLocator() {
    this.hashAlg = HashAlgorithm.KETAMA_HASH;
    this.cwNginxUpstreamConsistent = false;
    this.gwhalinMemcachedJavaClientCompatibiltyConsistent = false;
  }

  /**
   * Create a KetamaMemcachedSessionLocator
   * 
   * @param cwNginxUpstreamConsistent true if compatible with nginx up stream memcached consistent
   *        algorithm.
   */
  public KetamaMemcachedSessionLocator(boolean cwNginxUpstreamConsistent) {
    this.hashAlg = HashAlgorithm.KETAMA_HASH;
    this.cwNginxUpstreamConsistent = cwNginxUpstreamConsistent;
    this.gwhalinMemcachedJavaClientCompatibiltyConsistent = false;
  }

  /**
   * Create a KetamaMemcachedSessionLocator with a special hash algorithm.
   * 
   * @param alg
   */
  public KetamaMemcachedSessionLocator(HashAlgorithm alg) {
    this.hashAlg = alg;
    this.cwNginxUpstreamConsistent = false;
    this.gwhalinMemcachedJavaClientCompatibiltyConsistent = false;
  }

  public KetamaMemcachedSessionLocator(HashAlgorithm alg, boolean cwNginxUpstreamConsistent) {
    this.hashAlg = alg;
    this.cwNginxUpstreamConsistent = cwNginxUpstreamConsistent;
    this.gwhalinMemcachedJavaClientCompatibiltyConsistent = false;
  }

  public KetamaMemcachedSessionLocator(HashAlgorithm alg, boolean cwNginxUpstreamConsistent,
      boolean gwhalinMemcachedJavaClientCompatibiltyConsistent) {
    this.hashAlg = HashAlgorithm.KETAMA_HASH;
    this.cwNginxUpstreamConsistent = cwNginxUpstreamConsistent;
    this.gwhalinMemcachedJavaClientCompatibiltyConsistent =
        gwhalinMemcachedJavaClientCompatibiltyConsistent;
  }

  public KetamaMemcachedSessionLocator(List<Session> list, HashAlgorithm alg) {
    super();
    this.hashAlg = alg;
    this.cwNginxUpstreamConsistent = false;
    this.gwhalinMemcachedJavaClientCompatibiltyConsistent = false;
    this.buildMap(list, alg);
  }

  private final void buildMap(Collection<Session> list, HashAlgorithm alg) {
    TreeMap<Long, List<Session>> sessionMap = new TreeMap<Long, List<Session>>();

    for (Session session : list) {
      String sockStr = this.getSockStr(session);
      /**
       * Duplicate 160 X weight references
       */
      int numReps = NUM_REPS;
      if (session instanceof MemcachedSession) {
        numReps *= ((MemcachedSession) session).getWeight();
      }
      if (alg == HashAlgorithm.KETAMA_HASH) {
        for (int i = 0; i < numReps / 4; i++) {
          byte[] digest = HashAlgorithm.computeMd5(sockStr + "-" + i);
          for (int h = 0; h < 4; h++) {
            long k =
                (long) (digest[3 + h * 4] & 0xFF) << 24 | (long) (digest[2 + h * 4] & 0xFF) << 16
                    | (long) (digest[1 + h * 4] & 0xFF) << 8 | digest[h * 4] & 0xFF;
            this.getSessionList(sessionMap, k).add(session);
          }

        }
      } else {
        for (int i = 0; i < numReps; i++) {
          long key = alg.hash(sockStr + "-" + i);
          this.getSessionList(sessionMap, key).add(session);
        }
      }
    }
    // sort session list.
    for (List<Session> sessions : sessionMap.values()) {
      Collections.sort(sessions, new Comparator<Session>() {

        public int compare(Session o1, Session o2) {
          String sockStr1 = KetamaMemcachedSessionLocator.this.getSockStr(o1);
          String sockStr2 = KetamaMemcachedSessionLocator.this.getSockStr(o2);
          return sockStr1.compareTo(sockStr2);
        }

      });
    }
    this.ketamaSessions = sessionMap;
    this.maxTries = list.size();
  }

  private String getSockStr(Session session) {
    String sockStr = null;
    if (this.cwNginxUpstreamConsistent) {
      InetSocketAddress serverAddress = session.getRemoteSocketAddress();
      sockStr = serverAddress.getAddress().getHostAddress();
      if (serverAddress.getPort() != DEFAULT_PORT) {
        sockStr = sockStr + ":" + serverAddress.getPort();
      }
    } else {
      if (session instanceof MemcachedSession) {
        MemcachedSession memcachedSession = (MemcachedSession) session;
        InetSocketAddressWrapper inetSocketAddressWrapper =
            memcachedSession.getInetSocketAddressWrapper();
        if (this.gwhalinMemcachedJavaClientCompatibiltyConsistent) {
          String host = inetSocketAddressWrapper.getInetSocketAddress().getHostName();
          if (this.useRemoteIPConsistent){
            InetAddress inetAddress = inetSocketAddressWrapper.getInetSocketAddress().getAddress();
            if (inetAddress instanceof Inet4Address) {
              host = ((Inet4Address) inetAddress).getHostAddress();
            } else {
              host = ((Inet6Address) inetAddress).getHostAddress();
            }
          }
          sockStr = host + ":"
              + inetSocketAddressWrapper.getInetSocketAddress().getPort();
        } else {
          // Always use the first time resolved address.
          sockStr = inetSocketAddressWrapper.getRemoteAddressStr();
        }
      }
      if (sockStr == null) {
        sockStr = String.valueOf(session.getRemoteSocketAddress());
      }
    }
    return sockStr;
  }

  private List<Session> getSessionList(TreeMap<Long, List<Session>> sessionMap, long k) {
    List<Session> sessionList = sessionMap.get(k);
    if (sessionList == null) {
      sessionList = new ArrayList<Session>();
      sessionMap.put(k, sessionList);
    }
    return sessionList;
  }

  public final Session getSessionByKey(final String key) {
    if (this.ketamaSessions == null || this.ketamaSessions.size() == 0) {
      return null;
    }
    long hash = this.hashAlg.hash(key);
    Session rv = this.getSessionByHash(hash);
    int tries = 0;
    while (!this.failureMode && (rv == null || rv.isClosed()) && tries++ < this.maxTries) {
      hash = this.nextHash(hash, key, tries);
      rv = this.getSessionByHash(hash);
    }
    return rv;
  }

  public final Session getSessionByHash(final long hash) {
    TreeMap<Long, List<Session>> sessionMap = this.ketamaSessions;
    if (sessionMap.size() == 0) {
      return null;
    }
    Long resultHash = hash;
    if (!sessionMap.containsKey(hash)) {
      // Java 1.6 adds a ceilingKey method, but xmemcached is compatible
      // with jdk5,So use tailMap method to do this.
      SortedMap<Long, List<Session>> tailMap = sessionMap.tailMap(hash);
      if (tailMap.isEmpty()) {
        resultHash = sessionMap.firstKey();
      } else {
        resultHash = tailMap.firstKey();
      }
    }
    //
    // if (!sessionMap.containsKey(resultHash)) {
    // resultHash = sessionMap.ceilingKey(resultHash);
    // if (resultHash == null && sessionMap.size() > 0) {
    // resultHash = sessionMap.firstKey();
    // }
    // }
    List<Session> sessionList = sessionMap.get(resultHash);
    if (sessionList == null || sessionList.size() == 0) {
      return null;
    }
    int size = sessionList.size();
    return sessionList.get((int) (resultHash % size));
  }

  public final long nextHash(long hashVal, String key, int tries) {
    long tmpKey = this.hashAlg.hash(tries + key);
    hashVal += (int) (tmpKey ^ tmpKey >>> 32);
    hashVal &= 0xffffffffL; /* truncate to 32-bits */
    return hashVal;
  }

  public final void updateSessions(final Collection<Session> list) {
    this.buildMap(list, this.hashAlg);
  }

  public void setUseRemoteIPConsistent(boolean useRemoteIPConsistent) {

    this.useRemoteIPConsistent = useRemoteIPConsistent;
  }
}
