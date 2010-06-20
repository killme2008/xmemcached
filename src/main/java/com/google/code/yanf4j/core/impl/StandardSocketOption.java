package com.google.code.yanf4j.core.impl;

import java.net.ServerSocket;
import java.net.Socket;

import com.google.code.yanf4j.core.SocketOption;



public class StandardSocketOption {

	/**
	 * Keep connection alive.
	 * 
	 * <p>
	 * The value of this socket option is a {@code Boolean} that represents
	 * whether the option is enabled or disabled. When the {@code SO_KEEPALIVE}
	 * option is enabled the operating system may use a <em>keep-alive</em>
	 * mechanism to periodically probe the other end of a connection when the
	 * connection is otherwise idle. The exact semantics of the keep alive
	 * mechanism is system dependent and therefore unspecified.
	 * 
	 * <p>
	 * The initial value of this socket option is {@code FALSE}. The socket
	 * option may be enabled or disabled at any time.
	 * 
	 * @see <a href="http://www.ietf.org/rfc/rfc1122.txt">RFC&nbsp;1122 *
	 *      Requirements for Internet Hosts -- Communication Layers< /a>
	 * @see Socket#setKeepAlive
	 */
	public static final SocketOption<Boolean> SO_KEEPALIVE = new SocketOption<Boolean>(
			"SO_KEEPALIVE", Boolean.class);
	/**
	 * The size of the socket send buffer.
	 * 
	 * <p>
	 * The value of this socket option is an {@code Integer} that is the size of
	 * the socket send buffer in bytes. The socket send buffer is an output
	 * buffer used by the networking implementation. It may need to be increased
	 * for high-volume connections. The value of the socket option is a
	 * <em>hint</em> to the implementation to size the buffer and the actual
	 * size may differ. The socket option can be queried to retrieve the actual
	 * size.
	 * 
	 * <p>
	 * For datagram-oriented sockets, the size of the send buffer may limit the
	 * size of the datagrams that may be sent by the socket. Whether datagrams
	 * larger than the buffer size are sent or discarded is system dependent.
	 * 
	 * <p>
	 * The initial/default size of the socket send buffer and the range of
	 * allowable values is system dependent although a negative size is not
	 * allowed. An attempt to set the socket send buffer to larger than its
	 * maximum size causes it to be set to its maximum size.
	 * 
	 * <p>
	 * An implementation allows this socket option to be set before the socket
	 * is bound or connected. Whether an implementation allows the socket send
	 * buffer to be changed after the socket is bound is system dependent.
	 * 
	 * @see Socket#setSendBufferSize
	 */
	public static final SocketOption<Integer> SO_SNDBUF = new SocketOption<Integer>(
			"SO_SNDBUF", Integer.class);
	/**
	 * The size of the socket receive buffer.
	 * 
	 * <p>
	 * The value of this socket option is an {@code Integer} that is the size of
	 * the socket receive buffer in bytes. The socket receive buffer is an input
	 * buffer used by the networking implementation. It may need to be increased
	 * for high-volume connections or decreased to limit the possible backlog of
	 * incoming data. The value of the socket option is a <em>hint</em> to the
	 * implementation to size the buffer and the actual size may differ.
	 * 
	 * <p>
	 * For datagram-oriented sockets, the size of the receive buffer may limit
	 * the size of the datagrams that can be received. Whether datagrams larger
	 * than the buffer size can be received is system dependent. Increasing the
	 * socket receive buffer may be important for cases where datagrams arrive
	 * in bursts faster than they can be processed.
	 * 
	 * <p>
	 * In the case of stream-oriented sockets and the TCP/IP protocol, the size
	 * of the socket receive buffer may be used when advertising the size of the
	 * TCP receive window to the remote peer.
	 * 
	 * <p>
	 * The initial/default size of the socket receive buffer and the range of
	 * allowable values is system dependent although a negative size is not
	 * allowed. An attempt to set the socket receive buffer to larger than its
	 * maximum size causes it to be set to its maximum size.
	 * 
	 * <p>
	 * An implementation allows this socket option to be set before the socket
	 * is bound or connected. Whether an implementation allows the socket
	 * receive buffer to be changed after the socket is bound is system
	 * dependent.
	 * 
	 * @see <a href="http://www.ietf.org/rfc/rfc1323.txt">RFC&nbsp;1323: TCP *
	 *      Extensions for High Performance< /a>
	 * @see Socket#setReceiveBufferSize
	 * @see ServerSocket#setReceiveBufferSize
	 */
	public static final SocketOption<Integer> SO_RCVBUF = new SocketOption<Integer>(
			"SO_RCVBUF", Integer.class);
	/**
	 * Re-use address.
	 * 
	 * <p>
	 * The value of this socket option is a {@code Boolean} that represents
	 * whether the option is enabled or disabled. The exact semantics of this
	 * socket option are socket type and system dependent.
	 * 
	 * <p>
	 * In the case of stream-oriented sockets, this socket option will usually
	 * determine whether the socket can be bound to a socket address when a
	 * previous connection involving that socket address is in the
	 * <em>TIME_WAIT</em> state. On implementations where the semantics differ,
	 * and the socket option is not required to be enabled in order to bind the
	 * socket when a previous connection is in this state, then the
	 * implementation may choose to ignore this option.
	 * 
	 * <p>
	 * For datagram-oriented sockets the socket option is used to allow multiple
	 * programs bind to the same address. This option should be enabled when the
	 * socket is to be used for Internet Protocol (IP) multicasting.
	 * 
	 * <p>
	 * An implementation allows this socket option to be set before the socket
	 * is bound or connected. Changing the value of this socket option after the
	 * socket is bound has no effect. The default value of this socket option is
	 * system dependent.
	 * 
	 * @see <a href="http://www.ietf.org/rfc/rfc793.txt">RFC&nbsp;793: *
	 *      Transmission Control Protocol< /a>
	 * @see ServerSocket#setReuseAddress
	 */
	public static final SocketOption<Boolean> SO_REUSEADDR = new SocketOption<Boolean>(
			"SO_REUSEADDR", Boolean.class);
	/**
	 * Linger on close if data is present.
	 * 
	 * <p>
	 * The value of this socket option is an {@code Integer} that controls the
	 * action taken when unsent data is queued on the socket and a method to
	 * close the socket is invoked. If the value of the socket option is zero or
	 * greater, then it represents a timeout value, in seconds, known as the
	 * <em>linger interval</em>. The linger interval is the timeout for the
	 * {@code close} method to block while the operating system attempts to
	 * transmit the unsent data or it decides that it is unable to transmit the
	 * data. If the value of the socket option is less than zero then the option
	 * is disabled. In that case the {@code close} method does not wait until
	 * unsent data is transmitted; if possible the operating system will
	 * transmit any unsent data before the connection is closed.
	 * 
	 * <p>
	 * This socket option is intended for use with sockets that are configured
	 * in {@link java.nio.channels.SelectableChannel#isBlocking() blocking} mode
	 * only. The behavior of the {@code close} method when this option is
	 * enabled on a non-blocking socket is not defined.
	 * 
	 * <p>
	 * The initial value of this socket option is a negative value, meaning that
	 * the option is disabled. The option may be enabled, or the linger interval
	 * changed, at any time. The maximum value of the linger interval is system
	 * dependent. Setting the linger interval to a value that is greater than
	 * its maximum value causes the linger interval to be set to its maximum
	 * value.
	 * 
	 * @see Socket#setSoLinger
	 */
	public static final SocketOption<Integer> SO_LINGER = new SocketOption<Integer>(
			"SO_LINGER", Integer.class);
	/**
	 * Disable the Nagle algorithm.
	 * 
	 * <p>
	 * The value of this socket option is a {@code Boolean} that represents
	 * whether the option is enabled or disabled. The socket option is specific
	 * to stream-oriented sockets using the TCP/IP protocol. TCP/IP uses an
	 * algorithm known as <em>The Nagle Algorithm</em> to coalesce short
	 * segments and improve network efficiency.
	 * 
	 * <p>
	 * The default value of this socket option is {@code FALSE}. The socket
	 * option should only be enabled in cases where it is known that the
	 * coalescing impacts performance. The socket option may be enabled at any
	 * time. In other words, the Nagle Algorithm can be disabled. Once the
	 * option is enabled, it is system dependent whether it can be subsequently
	 * disabled. If it cannot, then invoking the {@code setOption} method to
	 * disable the option has no effect.
	 * 
	 * @see <a href="http://www.ietf.org/rfc/rfc1122.txt">RFC&nbsp;1122: *
	 *      Requirements for Internet Hosts -- Communication Layers< /a>
	 * @see Socket#setTcpNoDelay
	 */
	public static final SocketOption<Boolean> TCP_NODELAY = new SocketOption<Boolean>(
			"TCP_NODELAY", Boolean.class);
	
}
