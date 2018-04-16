package net.rubyeye.xmemcached.test.unittest;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.concurrent.Future;
import com.google.code.yanf4j.core.CodecFactory.Decoder;
import com.google.code.yanf4j.core.CodecFactory.Encoder;
import com.google.code.yanf4j.core.Handler;
import com.google.code.yanf4j.core.Session;
import com.google.code.yanf4j.core.impl.FutureImpl;

public class MockSession implements Session {
  private boolean closed = false;
  protected final int port;

  public MockSession(int port) {
    this.port = port;
  }

  public void write(Object packet) {
    // TODO Auto-generated method stub

  }

  public Handler getHandler() {
    // TODO Auto-generated method stub
    return null;
  }

  public InetAddress getLocalAddress() {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean isIdle() {
    // TODO Auto-generated method stub
    return false;
  }

  public void clearAttributes() {
    // TODO Auto-generated method stub

  }

  public Object getAttribute(String key) {
    // TODO Auto-generated method stub
    return null;
  }

  public void removeAttribute(String key) {
    // TODO Auto-generated method stub

  }

  public void setAttribute(String key, Object value) {
    // TODO Auto-generated method stub

  }

  public void close() {
    this.closed = true;

  }

  public void flush() {

  }

  public Decoder getDecoder() {

    return null;
  }

  public Encoder getEncoder() {

    return null;
  }

  public ByteOrder getReadBufferByteOrder() {

    return null;
  }

  public InetSocketAddress getRemoteSocketAddress() {
    return new InetSocketAddress("localhost", this.port);
  }

  public boolean isClosed() {

    return this.closed;
  }

  public boolean isExpired() {

    return false;
  }

  public boolean isHandleReadWriteConcurrently() {

    return false;
  }

  public boolean isUseBlockingRead() {

    return false;
  }

  public boolean isUseBlockingWrite() {

    return false;
  }

  public Future<Boolean> asyncWrite(Object packet) {

    return new FutureImpl<Boolean>();
  }

  public void setDecoder(Decoder decoder) {

  }

  public void setEncoder(Encoder encoder) {

  }

  public void setHandleReadWriteConcurrently(boolean handleReadWriteConcurrently) {

  }

  public void setReadBufferByteOrder(ByteOrder readBufferByteOrder) {

  }

  public void setUseBlockingRead(boolean useBlockingRead) {

  }

  public void setUseBlockingWrite(boolean useBlockingWrite) {

  }

  public void start() {

  }

  public long getScheduleWritenBytes() {
    return 0;
  }

  public void updateTimeStamp() {}

  public long getLastOperationTimeStamp() {
    return 0;
  }

  public final boolean isLoopbackConnection() {
    return false;
  }

  public long getSessionIdleTimeout() {
    return 0;
  }

  public void setSessionIdleTimeout(long sessionIdelTimeout) {}

  public long getSessionTimeout() {
    return 0;
  }

  public void setSessionTimeout(long sessionTimeout) {}

  public Object setAttributeIfAbsent(String key, Object value) {
    return null;
  }

  @Override
  public String toString() {
    return "localhost:" + this.port;
  }

}
