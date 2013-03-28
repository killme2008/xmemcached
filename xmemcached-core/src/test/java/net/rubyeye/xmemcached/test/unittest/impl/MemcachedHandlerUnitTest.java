package net.rubyeye.xmemcached.test.unittest.impl;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.binary.BinaryStoreCommand;
import net.rubyeye.xmemcached.command.binary.BinaryVersionCommand;
import net.rubyeye.xmemcached.command.text.TextStoreCommand;
import net.rubyeye.xmemcached.command.text.TextVersionCommand;
import net.rubyeye.xmemcached.impl.MemcachedHandler;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.utils.Protocol;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Before;
import org.junit.Test;

public class MemcachedHandlerUnitTest {
	private MemcachedHandler handler;

	private IMocksControl mocksControl;

	private MemcachedClient memcachedClient;

	private MemcachedTCPSession session;

	@Before
	public void setUp() {
		this.mocksControl = EasyMock.createControl();
		this.memcachedClient = this.mocksControl
				.createMock(MemcachedClient.class);
		this.session = this.mocksControl.createMock(MemcachedTCPSession.class);
		this.handler = new MemcachedHandler(this.memcachedClient);
	}

	@Test
	public void testOnMessageSent_TextCommand() {
		Command cmd = new TextVersionCommand(new CountDownLatch(1),
				new InetSocketAddress(8080));
		this.session.addCommand(cmd);
		EasyMock.expectLastCall();
		this.mocksControl.replay();
		this.handler.onMessageSent(this.session, cmd);
		this.mocksControl.verify();
	}

	@Test
	public void testOnMessageSent_TextCommand_NoReply() {
		Command cmd = new TextStoreCommand(null, null, CommandType.SET, null, 1, 1, null,
				true, null);
		EasyMock.expect(this.memcachedClient.getProtocol()).andReturn(
				Protocol.Text);
		this.mocksControl.replay();
		this.handler.onMessageSent(this.session, cmd);
		this.mocksControl.verify();
	}

	@Test
	public void testOnMessageSent_BinaryCommand() {
		Command cmd = new BinaryVersionCommand(null, null);
		this.session.addCommand(cmd);
		EasyMock.expectLastCall();
		this.mocksControl.replay();
		this.handler.onMessageSent(this.session, cmd);
		this.mocksControl.verify();
	}

	@Test
	public void testOnMessageSent_BinaryCommand_NoReply() {
		Command cmd = new BinaryStoreCommand(null, null, CommandType.ADD, null,
				1, 1, null, true, null);
		EasyMock.expect(this.memcachedClient.getProtocol()).andReturn(
				Protocol.Binary);
		this.session.addCommand(cmd);
		EasyMock.expectLastCall();
		this.mocksControl.replay();
		this.handler.onMessageSent(this.session, cmd);
		this.mocksControl.verify();
	}

}
