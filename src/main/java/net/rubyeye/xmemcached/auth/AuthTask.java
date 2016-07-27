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

import net.rubyeye.xmemcached.MemcachedClient;

/**
 * Authentication task
 * 
 * @author dennis
 * 
 */
public class AuthTask extends Thread {
	private final AuthInfo authInfo;
	private final CommandFactory commandFactory;
	private MemcachedTCPSession memcachedTCPSession;
	public static final byte[] EMPTY_BYTES = new byte[0];
	static final Logger log = LoggerFactory.getLogger(AuthTask.class);
	private SaslClient saslClient;

	public AuthTask(AuthInfo authInfo, CommandFactory commandFactory,
			MemcachedTCPSession memcachedTCPSession) {
		super();
		this.authInfo = authInfo;
		this.commandFactory = commandFactory;
		this.memcachedTCPSession = memcachedTCPSession;
	}

	public void run() {
		if (this.authInfo.isValid()) {
			doAuth();
			this.authInfo.increaseAttempts();
		}
	}

	private void doAuth() {
		try {
			final AtomicBoolean done = new AtomicBoolean(false);
			Command command = startAuth();

			while (!done.get()) {
				// wait previous command response
				waitCommand(command, done);
				// process response
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
					log.error("Authentication failed to "
							+ this.memcachedTCPSession.getRemoteSocketAddress());
					log.warn("Reopen connection to "
							+ this.memcachedTCPSession.getRemoteSocketAddress()
							+ ",beacause auth fail");
					this.memcachedTCPSession.setAuthFailed(true);

					// It it is not first time ,try to sleep 1 second
					if (!this.authInfo.isFirstTime()) {
						Thread.sleep(1000);
					}
					this.memcachedTCPSession.close();
					done.set(true);
					break;
				case FUTHER_AUTH_REQUIRED:
					String result = String.valueOf(command.getResult());
					byte[] response = saslClient.evaluateChallenge(ByteUtils
							.getBytes(result));
					CountDownLatch latch = new CountDownLatch(1);
					command = commandFactory.createAuthStepCommand(
							saslClient.getMechanismName(), latch, response);
					if (!this.memcachedTCPSession.isClosed())
						this.memcachedTCPSession.write(command);
					else {
						log.error("Authentication fail,because the connection has been closed");
						throw new RuntimeException(
								"Authentication fai,connection has been close");
					}

					break;
				default:
					log.error("Authentication failed to "
							+ this.memcachedTCPSession.getRemoteSocketAddress()
							+ ",response status=" + responseStatus);
					command = startAuth();
					break;

				}

			}
		} catch (Exception e) {
			log.error("Create saslClient error", e);
		} finally {
			destroySaslClient();
		}
	}

	private void destroySaslClient() {
		if (saslClient != null) {
			try {
				saslClient.dispose();
			} catch (SaslException e) {
				log.error("Dispose saslClient error", e);
			}
			this.saslClient = null;
		}
	}

	private Command startAuth() throws SaslException {
		// destroy previous client.
		destroySaslClient();

		this.saslClient = Sasl.createSaslClient(authInfo.getMechanisms(), null,
				"memcached", memcachedTCPSession.getRemoteSocketAddress()
						.toString(), null, this.authInfo.getCallbackHandler());
		byte[] response = saslClient.hasInitialResponse() ? saslClient
				.evaluateChallenge(EMPTY_BYTES) : EMPTY_BYTES;
		CountDownLatch latch = new CountDownLatch(1);
		Command command = this.commandFactory.createAuthStartCommand(
				saslClient.getMechanismName(), latch, response);
		if (!this.memcachedTCPSession.isClosed())
			this.memcachedTCPSession.write(command);
		else {
			log.error("Authentication fail,because the connection has been closed");
			throw new RuntimeException(
					"Authentication fai,connection has been close");
		}
		return command;
	}

	private void waitCommand(Command cmd, AtomicBoolean done) {
		try {
			cmd.getLatch().await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			done.set(true);
		}
	}

}
