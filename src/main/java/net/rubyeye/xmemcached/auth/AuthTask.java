package net.rubyeye.xmemcached.auth;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.binary.BaseBinaryCommand;
import net.rubyeye.xmemcached.command.binary.ResponseStatus;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class AuthTask extends Thread {
	private AuthInfo authInfo;
	private CommandFactory commandFactory;
	private MemcachedTCPSession memcachedTCPSession;
	public static final byte[] EMPTY_BYTES = new byte[0];
	static final Logger log = LoggerFactory.getLogger(AuthTask.class);

	public AuthTask(AuthInfo authInfo, CommandFactory commandFactory,
			MemcachedTCPSession memcachedTCPSession) {
		super();
		this.authInfo = authInfo;
		this.commandFactory = commandFactory;
		this.memcachedTCPSession = memcachedTCPSession;
	}

	public void run() {

		try {
			SaslClient saslClient = Sasl.createSaslClient(authInfo
					.getMechanisms(), null, "memcached", memcachedTCPSession
					.getRemoteSocketAddress().toString(), null, this.authInfo
					.getCallbackHandler());

			final AtomicBoolean done = new AtomicBoolean(false);
			byte[] response = saslClient.hasInitialResponse() ? saslClient
					.evaluateChallenge(EMPTY_BYTES) : EMPTY_BYTES;
			CountDownLatch latch = new CountDownLatch(1);
			Command command = this.commandFactory.createAuthStartCommand(
					saslClient.getMechanismName(), latch, response);
			if (!this.memcachedTCPSession.isClosed())
				this.memcachedTCPSession.write(command);
			else {
				log
						.error("Authentication fail,because the connection has been closed");
				throw new RuntimeException(
						"Authentication fai,connection has been close");
			}

			while (!done.get()) {
				try {
					latch.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					done.set(true);
				}
				ResponseStatus responseStatus = ((BaseBinaryCommand) command)
						.getResponseStatus();
				switch (responseStatus) {
				case NO_ERROR:
					done.set(true);
					log.info("Authentication to "
							+ this.memcachedTCPSession.getRemoteSocketAddress()
							+ " successfully");
					break;
				case AUTH_REQUIRED:
					log
							.error("Authentication failed to "
									+ this.memcachedTCPSession
											.getRemoteSocketAddress());
					done.set(true);
					break;
				case FUTHER_AUTH_REQUIRED:
					String result = (String) command.getResult();
					response = saslClient.evaluateChallenge(ByteUtils
							.getBytes(result));
					latch = new CountDownLatch(1);
					command = commandFactory.createAuthStepCommand(saslClient
							.getMechanismName(), latch, response);
					if (!this.memcachedTCPSession.isClosed())
						this.memcachedTCPSession.write(command);
					else {
						log
								.error("Authentication fail,because the connection has been closed");
						throw new RuntimeException(
								"Authentication fai,connection has been close");
					}

					break;
				default:
					done.set(true);
					log.error("Authentication failed to "
							+ this.memcachedTCPSession.getRemoteSocketAddress()
							+ ",response status=" + responseStatus);
					break;

				}

			}

		} catch (SaslException e) {
			log.error("Create saslClient error", e);
			throw new IllegalStateException("Can't make sasl running", e);
		}

	}

}
