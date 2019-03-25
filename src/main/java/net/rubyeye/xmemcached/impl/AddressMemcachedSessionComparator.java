package net.rubyeye.xmemcached.impl;

import java.io.Serializable;
import net.rubyeye.xmemcached.MemcachedSessionComparator;
import net.rubyeye.xmemcached.networking.MemcachedSession;
import com.google.code.yanf4j.core.Session;

/**
 * Connection comparator,compare with Address
 * 
 * @author Jungsub Shin
 * 
 */
public class AddressMemcachedSessionComparator implements MemcachedSessionComparator, Serializable {
  static final long serialVersionUID = -1L;

  public int compare(Session o1, Session o2) {
    MemcachedSession session1 = (MemcachedSession) o1;
    MemcachedSession session2 = (MemcachedSession) o2;
    if (session1 == null || session1.getInetSocketAddressWrapper() == null
        || session1.getInetSocketAddressWrapper().getInetSocketAddress() == null) {
      return -1;
    }
    if (session2 == null || session2.getInetSocketAddressWrapper() == null
        || session2.getInetSocketAddressWrapper().getInetSocketAddress() == null) {
      return 1;
    }
    return session1.getInetSocketAddressWrapper().getInetSocketAddress().toString()
        .compareTo(session2.getInetSocketAddressWrapper().getInetSocketAddress().toString());
  }
}
