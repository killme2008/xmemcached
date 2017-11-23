package net.rubyeye.xmemcached.test.unittest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.rubyeye.xmemcached.CASOperation;
import net.rubyeye.xmemcached.Counter;
import net.rubyeye.xmemcached.GetsResponse;
import net.rubyeye.xmemcached.KeyIterator;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.MemcachedClientCallable;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.command.binary.OpCode;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.exception.UnknownCommandException;
import net.rubyeye.xmemcached.helper.BlankKeyChecker;
import net.rubyeye.xmemcached.helper.InValidKeyChecker;
import net.rubyeye.xmemcached.helper.MockTranscoder;
import net.rubyeye.xmemcached.helper.TooLongKeyChecker;
import net.rubyeye.xmemcached.helper.TranscoderChecker;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.test.unittest.mock.MockDecodeTimeoutBinaryGetOneCommand;
import net.rubyeye.xmemcached.test.unittest.mock.MockDecodeTimeoutTextGetOneCommand;
import net.rubyeye.xmemcached.test.unittest.mock.MockEncodeTimeoutBinaryGetCommand;
import net.rubyeye.xmemcached.test.unittest.mock.MockEncodeTimeoutTextGetOneCommand;
import net.rubyeye.xmemcached.test.unittest.mock.MockErrorBinaryGetOneCommand;
import net.rubyeye.xmemcached.test.unittest.mock.MockErrorCommand;
import net.rubyeye.xmemcached.test.unittest.mock.MockErrorTextGetOneCommand;
import net.rubyeye.xmemcached.transcoders.IntegerTranscoder;
import net.rubyeye.xmemcached.transcoders.StringTranscoder;
import net.rubyeye.xmemcached.utils.AddrUtil;
import net.rubyeye.xmemcached.utils.ByteUtils;
import net.rubyeye.xmemcached.utils.Protocol;

import com.google.code.yanf4j.buffer.IoBuffer;
import com.google.code.yanf4j.util.ResourcesUtils;

public abstract class XMemcachedClientIT extends TestCase {
	protected MemcachedClient memcachedClient;
	Properties properties;
	private MockTranscoder mockTranscoder;

	@Override
	public void setUp() throws Exception {
		createClients();
		mockTranscoder = new MockTranscoder();
	}

	public void testCreateClientWithEmptyServers() throws Exception {
		MemcachedClient client = new XMemcachedClient();
		assertFalse(client.isShutdown());
		client.shutdown();
		assertTrue(client.isShutdown());

		MemcachedClientBuilder builder = new XMemcachedClientBuilder();
		client = builder.build();
		assertFalse(client.isShutdown());
		client.shutdown();
		assertTrue(client.isShutdown());
	}

	protected void createClients() throws IOException, Exception,
			TimeoutException, InterruptedException, MemcachedException {
		properties = ResourcesUtils.getResourceAsProperties("test.properties");

		MemcachedClientBuilder builder = createBuilder();
		builder.getConfiguration().setStatisticsServer(true);
		memcachedClient = builder.build();
		memcachedClient.flushAll();
	}

	public MemcachedClientBuilder createBuilder() throws Exception {
		return null;
	}

	public MemcachedClientBuilder createWeightedBuilder() throws Exception {
		return null;
	}

	public void testGet() throws Exception {
		assertNull(memcachedClient.get("name"));

		memcachedClient.set("name", 1, "dennis", new StringTranscoder(), 1000);

		assertEquals("dennis",
				memcachedClient.get("name", new StringTranscoder()));
		new TranscoderChecker(mockTranscoder, 1) {
			@Override
			public void call() throws Exception {
				assertEquals("dennis",
						memcachedClient.get("name", mockTranscoder));
			}

		}.check();
		Thread.sleep(2000);
		// expire
		assertNull(memcachedClient.get("name"));
		// blank key
		new BlankKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.get("");
			}
		}.check();
		// null key
		new BlankKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.get((String) null);
			}
		}.check();

		// invalid key
		new InValidKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.get("test\r\n");
			}
		}.check();
		new InValidKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.get("test test2");
			}
		}.check();

		// key is too long
		new TooLongKeyChecker(memcachedClient) {
			@Override
			public void call() throws Exception {
				int keyLength = memcachedClient.getProtocol() == Protocol.Text
						? 256
						: 65536;
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < keyLength; i++) {
					sb.append(i);
				}
				memcachedClient.get(sb.toString());
			}
		}.check();

		// client is shutdown
		try {
			memcachedClient.shutdown();
			memcachedClient.get("name");
			fail();
		} catch (MemcachedException e) {
			assertEquals("Xmemcached is stopped", e.getMessage());
		}
	}

	public void testAppendPrepend() throws Exception {
		// append,prepend
		assertTrue(memcachedClient.set("name", 0, "dennis",
				new StringTranscoder(), 1000));
		assertTrue(memcachedClient.prepend("name", "hello "));
		assertEquals("hello dennis", memcachedClient.get("name"));
		assertTrue(memcachedClient.append("name", " zhuang"));
		assertEquals("hello dennis zhuang", memcachedClient.get("name"));
		memcachedClient.delete("name");
		assertFalse(memcachedClient.prepend("name", "hello ", 2000));
		assertFalse(memcachedClient.append("name", " zhuang", 2000));
		// append test
		// blank key
		new BlankKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.append("", 0, 1);
			}
		}.check();
		// null key
		new BlankKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.append((String) null, 0, 1);
			}
		}.check();

		// invalid key
		new InValidKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.append("test\r\n", 0, 1);
			}
		}.check();
		new InValidKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.append("test test2", 0, 1);
			}
		}.check();

		// key is too long
		new TooLongKeyChecker(memcachedClient) {
			@Override
			public void call() throws Exception {
				int keyLength = memcachedClient.getProtocol() == Protocol.Text
						? 256
						: 65536;
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < keyLength; i++) {
					sb.append(i);
				}
				memcachedClient.append(sb.toString(), 0, 1);
			}
		}.check();

		// prepend test
		// blank key
		new BlankKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.prepend("", 0, 1);
			}
		}.check();
		// null key
		new BlankKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.prepend((String) null, 0, 1);
			}
		}.check();

		// invalid key
		new InValidKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.prepend("test\r\n", 0, 1);
			}
		}.check();
		new InValidKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.prepend("test test2", 0, 1);
			}
		}.check();

		// key is too long
		new TooLongKeyChecker(memcachedClient) {
			@Override
			public void call() throws Exception {
				int keyLength = memcachedClient.getProtocol() == Protocol.Text
						? 256
						: 65536;
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < keyLength; i++) {
					sb.append(i);
				}
				memcachedClient.prepend(sb.toString(), 0, 1);
			}
		}.check();

	}

	public void testStoreCollection()
			throws TimeoutException, InterruptedException, MemcachedException {
		// store list
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < 100; i++) {
			list.add(String.valueOf(i));
		}
		assertTrue(memcachedClient.add("list", 0, list));
		List<String> listFromCache = memcachedClient.get("list");
		assertEquals(100, listFromCache.size());

		for (int i = 0; i < listFromCache.size(); i++) {
			assertEquals(list.get(i), listFromCache.get(i));
		}
		// store map
		Map<String, Integer> map = new HashMap<String, Integer>();

		for (int i = 0; i < 100; i++) {
			map.put(String.valueOf(i), i);
		}
		assertTrue(memcachedClient.add("map", 0, map));
		Map<String, Integer> mapFromCache = memcachedClient.get("map");
		assertEquals(100, listFromCache.size());

		for (int i = 0; i < listFromCache.size(); i++) {
			assertEquals(mapFromCache.get(i), map.get(i));
		}

	}

	public void testSet() throws Exception {
		assertTrue(memcachedClient.set("name", 0, "dennis"));
		assertEquals("dennis", memcachedClient.get("name", 2000));

		assertTrue(memcachedClient.set("name", 1, "zhuang",
				new StringTranscoder()));
		assertEquals("zhuang", memcachedClient.get("name", 2000));
		Thread.sleep(2000);
		// expired
		assertNull(memcachedClient.get("zhuang"));

		// blank key
		new BlankKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.set("", 0, 1);
			}
		}.check();
		// null key
		new BlankKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.set((String) null, 0, 1);
			}
		}.check();

		// invalid key
		new InValidKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.set("test\r\n", 0, 1);
			}
		}.check();
		new InValidKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.set("test test2", 0, 1);
			}
		}.check();

		// key is too long
		new TooLongKeyChecker(memcachedClient) {
			@Override
			public void call() throws Exception {
				int keyLength = memcachedClient.getProtocol() == Protocol.Text
						? 256
						: 65536;
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < keyLength; i++) {
					sb.append(i);
				}
				memcachedClient.set(sb.toString(), 0, 1);
			}
		}.check();

		// Transcoder
		new TranscoderChecker(mockTranscoder, 2) {
			@Override
			public void call() throws Exception {
				memcachedClient.set("name", 0, "xmemcached", mockTranscoder);
				assertEquals("xmemcached",
						memcachedClient.get("name", mockTranscoder));

			}
		}.check();
	}

	public void testReplace() throws Exception {
		assertTrue(memcachedClient.add("name", 0, "dennis"));
		assertFalse(memcachedClient.replace("unknownKey", 0, "test"));
		assertTrue(memcachedClient.replace("name", 1, "zhuang"));
		assertEquals("zhuang", memcachedClient.get("name", 2000));
		Thread.sleep(2000);
		// expire
		assertNull(memcachedClient.get("name"));

		// blank key
		new BlankKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.replace("", 0, 1);
			}
		}.check();
		// null key
		new BlankKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.replace((String) null, 0, 1);
			}
		}.check();

		// invalid key
		new InValidKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.replace("test\r\n", 0, 1);
			}
		}.check();
		new InValidKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.replace("test test2", 0, 1);
			}
		}.check();

		// key is too long
		new TooLongKeyChecker(memcachedClient) {
			@Override
			public void call() throws Exception {
				int keyLength = memcachedClient.getProtocol() == Protocol.Text
						? 256
						: 65536;
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < keyLength; i++) {
					sb.append(i);
				}
				memcachedClient.replace(sb.toString(), 0, 1);
			}
		}.check();

		// timeout
		// new TimeoutChecker(0) {
		// @Override
		// public void call() throws Exception {
		// XMemcachedClientTest.this.memcachedClient.replace("0", 0, 1, 0);
		// }
		// }.check();

		// Transcoder
		new TranscoderChecker(mockTranscoder, 2) {
			@Override
			public void call() throws Exception {
				memcachedClient.set("name", 0, 1);
				memcachedClient.replace("name", 0, "xmemcached",
						mockTranscoder);
				assertEquals("xmemcached",
						memcachedClient.get("name", mockTranscoder));

			}
		}.check();
	}

	public void testAdd() throws Exception {
		assertTrue(memcachedClient.add("name", 0, "dennis"));
		assertFalse(memcachedClient.add("name", 0, "dennis"));
		assertEquals("dennis", memcachedClient.get("name", 2000));

		// blank key
		new BlankKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.add("", 0, 1);
			}
		}.check();
		// null key
		new BlankKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.add((String) null, 0, 1);
			}
		}.check();

		// invalid key
		new InValidKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.add("test\r\n", 0, 1);
			}
		}.check();
		new InValidKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.add("test test2", 0, 1);
			}
		}.check();

		// key is too long
		new TooLongKeyChecker(memcachedClient) {
			@Override
			public void call() throws Exception {
				int keyLength = memcachedClient.getProtocol() == Protocol.Text
						? 256
						: 65536;
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < keyLength; i++) {
					sb.append(i);
				}
				memcachedClient.add(sb.toString(), 0, 1);
			}
		}.check();

		// Transcoder
		new TranscoderChecker(mockTranscoder, 2) {
			@Override
			public void call() throws Exception {
				memcachedClient.add("a", 0, 100, mockTranscoder);
				assertEquals(100, memcachedClient.get("a", mockTranscoder));

			}
		}.check();
	}

	public void testDelete() throws Exception {
		assertTrue(memcachedClient.set("name", 0, "dennis"));
		assertEquals("dennis", memcachedClient.get("name"));
		assertTrue(memcachedClient.delete("name"));
		assertNull(memcachedClient.get("name"));
		assertFalse(memcachedClient.delete("not_exists"));

		memcachedClient.set("name", 0, "dennis");
		assertEquals("dennis", memcachedClient.get("name"));
		assertTrue(memcachedClient.delete("name"));
		assertNull(memcachedClient.get("name"));
		memcachedClient.set("name", 0, "dennis");
		assertEquals("dennis", memcachedClient.get("name"));
		assertTrue(memcachedClient.delete("name", 2000L));
		assertNull(memcachedClient.get("name"));

		// add,replace success
		assertTrue(memcachedClient.add("name", 0, "zhuang"));
		assertTrue(memcachedClient.replace("name", 0, "zhuang"));
	}

	//
	public void testMultiGet() throws Exception {
		for (int i = 0; i < 50; i++) {
			assertTrue(memcachedClient.add(String.valueOf(i), 0, i));
		}

		List<String> keys = new ArrayList<String>();
		for (int i = 0; i < 100; i++) {
			keys.add(String.valueOf(i));
		}

		Map<String, Integer> result = memcachedClient.get(keys, 10000);
		assertEquals(50, result.size());

		for (int i = 0; i < 50; i++) {
			assertEquals((Integer) i, result.get(String.valueOf(i)));
		}

		// blank collection
		assertNull(memcachedClient.get((Collection) null));
		assertNull(memcachedClient.get(new HashSet<String>()));

	}

	public void testGets() throws Exception {
		memcachedClient.add("name", 0, "dennis");
		GetsResponse<String> getsResponse = memcachedClient.gets("name");
		GetsResponse<String> oldGetsResponse = getsResponse;
		assertEquals("dennis", getsResponse.getValue());
		long oldCas = getsResponse.getCas();
		getsResponse = memcachedClient.gets("name", 2000,
				new StringTranscoder());
		assertEquals("dennis", getsResponse.getValue());
		// check the same
		assertEquals(oldCas, getsResponse.getCas());
		assertEquals(oldGetsResponse, getsResponse);

		memcachedClient.set("name", 0, "zhuang");
		getsResponse = memcachedClient.gets("name", 2000);
		assertEquals("zhuang", getsResponse.getValue());
		assertFalse(oldCas == getsResponse.getCas());

		// blank key
		new BlankKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.gets("");
			}
		}.check();
		// null key
		new BlankKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.gets((String) null);
			}
		}.check();

		// invalid key
		new InValidKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.gets("test\r\n");
			}
		}.check();
		new InValidKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.gets("test test2");
			}
		}.check();

		// key is too long
		new TooLongKeyChecker(memcachedClient) {
			@Override
			public void call() throws Exception {
				int keyLength = memcachedClient.getProtocol() == Protocol.Text
						? 256
						: 65536;
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < keyLength; i++) {
					sb.append(i);
				}
				memcachedClient.gets(sb.toString());
			}
		}.check();

		// client is shutdown
		try {
			memcachedClient.shutdown();
			memcachedClient.gets("name");
			fail();
		} catch (MemcachedException e) {
			assertEquals("Xmemcached is stopped", e.getMessage());
		}

	}

	public void testVersion() throws Exception {
		assertTrue(memcachedClient.getVersions(5000).size() > 0);
		System.out.println(memcachedClient.getVersions());
	}

	public void testStats() throws Exception {
		assertTrue(memcachedClient.getStats().size() > 0);
		System.out.println(memcachedClient.getStats());
		memcachedClient.set("a", 0, 1);
		assertTrue(memcachedClient.getStatsByItem("items").size() > 0);
		System.out.println(memcachedClient.getStatsByItem("items"));
	}

	public void testIssue126() throws Exception {
		Map<InetSocketAddress, Map<String, String>> result = this.memcachedClient
				.getStatsByItem("detail dump");
		assertNotNull(result);
	}

	public void testFlushAll() throws Exception {
		for (int i = 0; i < 50; i++) {
			assertTrue(memcachedClient.add(String.valueOf(i), 0, i));
		}
		List<String> keys = new ArrayList<String>();
		for (int i = 0; i < 100; i++) {
			keys.add(String.valueOf(i));
		}
		Map<String, Integer> result = memcachedClient.get(keys);
		assertEquals(50, result.size());
		for (int i = 0; i < 50; i++) {
			assertEquals((Integer) i, result.get(String.valueOf(i)));
		}
		memcachedClient.flushAll();
		result = memcachedClient.get(keys);
		assertTrue(result.isEmpty());
	}

	public void testSetLoggingLevelVerbosity() throws Exception {
		if (memcachedClient.getProtocol() == Protocol.Text
				|| memcachedClient.getProtocol() == Protocol.Binary) {
			memcachedClient.setLoggingLevelVerbosity(AddrUtil
					.getAddresses(
							properties.getProperty("test.memcached.servers"))
					.get(0), 2);
			memcachedClient.setLoggingLevelVerbosityWithNoReply(AddrUtil
					.getAddresses(
							properties.getProperty("test.memcached.servers"))
					.get(0), 3);
			memcachedClient.setLoggingLevelVerbosityWithNoReply(AddrUtil
					.getAddresses(
							properties.getProperty("test.memcached.servers"))
					.get(0), 0);
		} else {
			// do nothing,binary protocol doesn't have verbosity protocol.
		}
	}

	public void testIssue150() throws Exception {
		memcachedClient.set("a", 0, 1);
		try {
			memcachedClient.incr("a", 1);
			fail();
		} catch (MemcachedException e) {
			// assertEquals("cannot increment or decrement non-numeric
			// value",e.getMessage());
		}
		memcachedClient.set("a", 0, "1");
		assertEquals(3, memcachedClient.incr("a", 2));
	}

	public void testIncr() throws Exception {
		assertEquals(0, memcachedClient.incr("a", 5));
		assertTrue(memcachedClient.set("a", 0, "1"));
		assertEquals(6, memcachedClient.incr("a", 5));
		assertEquals(10, memcachedClient.incr("a", 4));

		// test incr with initValue
		memcachedClient.delete("a");
		assertEquals(1, memcachedClient.incr("a", 5, 1));
		assertEquals(6, memcachedClient.incr("a", 5));
		assertEquals(10, memcachedClient.incr("a", 4));

		// test incr with initValue and expire time
		memcachedClient.delete("a");
		assertEquals(1, memcachedClient.incr("a", 5, 1, 1000, 1));
		Thread.sleep(2000);
		assertNull(memcachedClient.get("a"));

		// key is chinese
		assertEquals(1, memcachedClient.incr("娴��", 5, 1, 1000, 0));
		assertEquals(6, memcachedClient.incr("娴��", 5));
		assertEquals(10, memcachedClient.incr("娴��", 4));

		// blank key
		new BlankKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.incr("", 0, 1);
			}
		}.check();
		// null key
		new BlankKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.incr((String) null, 0, 1);
			}
		}.check();

		// invalid key
		new InValidKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.incr("test\r\n", 0, 1);
			}
		}.check();
		new InValidKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.incr("test test2", 0, 1);
			}
		}.check();

		// key is too long
		new TooLongKeyChecker(memcachedClient) {
			@Override
			public void call() throws Exception {
				int keyLength = memcachedClient.getProtocol() == Protocol.Text
						? 256
						: 65536;
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < keyLength; i++) {
					sb.append(i);
				}
				memcachedClient.incr(sb.toString(), 0, 1);
			}
		}.check();

	}

	public void testDecr() throws Exception {
		assertEquals(0, memcachedClient.decr("a", 5));

		assertTrue(memcachedClient.set("a", 0, "100"));
		assertEquals(50, memcachedClient.decr("a", 50));
		assertEquals(46, memcachedClient.decr("a", 4));

		// test decr with initValue
		memcachedClient.delete("a");
		assertEquals(100, memcachedClient.decr("a", 5, 100));
		assertEquals(50, memcachedClient.decr("a", 50));
		assertEquals(46, memcachedClient.decr("a", 4));

		// test decr with initValue and expire time
		memcachedClient.delete("a");
		assertEquals(1, memcachedClient.decr("a", 5, 1, 1000, 1));
		Thread.sleep(2000);
		assertNull(memcachedClient.get("a"));

		// blank key
		new BlankKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.decr("", 0, 1);
			}
		}.check();
		// null key
		new BlankKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.decr((String) null, 0, 1);
			}
		}.check();

		// invalid key
		new InValidKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.decr("test\r\n", 0, 1);
			}
		}.check();
		new InValidKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.decr("test test2", 0, 1);
			}
		}.check();

		// key is too long
		new TooLongKeyChecker(memcachedClient) {
			@Override
			public void call() throws Exception {
				int keyLength = memcachedClient.getProtocol() == Protocol.Text
						? 256
						: 65536;
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < keyLength; i++) {
					sb.append(i);
				}
				memcachedClient.decr(sb.toString(), 0, 1);
			}
		}.check();

	}

	public void testCAS() throws Exception {
		memcachedClient.add("name", 0, "dennis");
		GetsResponse<String> getsResponse = memcachedClient.gets("name");
		assertEquals("dennis", getsResponse.getValue());
		final CASOperation<String> operation = new CASOperation<String>() {

			public int getMaxTries() {
				return 1;
			}

			public String getNewValue(long currentCAS, String currentValue) {
				return "zhuang";
			}

		};
		assertTrue(memcachedClient.cas("name", getsResponse, operation));
		assertEquals("zhuang", memcachedClient.get("name"));
		getsResponse = memcachedClient.gets("name");
		memcachedClient.set("name", 0, "dennis");
		// cas fail
		assertFalse(memcachedClient.cas("name", 0, "zhuang",
				getsResponse.getCas()));
		assertEquals("dennis", memcachedClient.get("name"));

		// blank key
		new BlankKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.cas("", operation);
			}
		}.check();
		// null key
		new BlankKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.cas((String) null, operation);
			}
		}.check();

		// invalid key
		new InValidKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.cas("test\r\n", operation);
			}
		}.check();
		new InValidKeyChecker() {
			@Override
			public void call() throws Exception {
				memcachedClient.cas("test test2", operation);
			}
		}.check();

		// key is too long
		new TooLongKeyChecker(memcachedClient) {
			@Override
			public void call() throws Exception {
				int keyLength = memcachedClient.getProtocol() == Protocol.Text
						? 256
						: 65536;
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < keyLength; i++) {
					sb.append(i);
				}
				memcachedClient.cas(sb.toString(), operation);
			}
		}.check();

	}

	public void testAutoReconnect() throws Exception {
		final String key = "name";
		memcachedClient.set(key, 0, "dennis");
		assertEquals("dennis", memcachedClient.get(key));
		CountDownLatch latch = new CountDownLatch(1);
		int currentServerCount = memcachedClient.getAvaliableServers().size();
		MockErrorCommand errorCommand = null;
		if (memcachedClient.getProtocol() == Protocol.Text) {
			errorCommand = new MockErrorTextGetOneCommand(key, key.getBytes(),
					CommandType.GET_ONE, latch);
		} else {
			errorCommand = new MockErrorBinaryGetOneCommand(key, key.getBytes(),
					CommandType.GET_ONE, latch, OpCode.GET, false);
		}
		memcachedClient.getConnector().send((Command) errorCommand);
		latch.await(MemcachedClient.DEFAULT_OP_TIMEOUT, TimeUnit.MILLISECONDS);
		assertTrue(errorCommand.isDecoded());
		// wait for reconnecting
		Thread.sleep(2000 * 3);
		assertEquals(currentServerCount,
				memcachedClient.getAvaliableServers().size());
		// It works
		assertEquals("dennis", memcachedClient.get(key));
	}

	public void testOperationDecodeTimeOut() throws Exception {
		memcachedClient.set("name", 0, "dennis");
		assertEquals("dennis", memcachedClient.get("name"));
		CountDownLatch latch = new CountDownLatch(1);
		Command errorCommand = null;
		if (memcachedClient.getProtocol() == Protocol.Text) {
			errorCommand = new MockDecodeTimeoutTextGetOneCommand("name",
					"name".getBytes(), CommandType.GET_ONE, latch, 1000);
		} else {
			errorCommand = new MockDecodeTimeoutBinaryGetOneCommand("name",
					"name".getBytes(), CommandType.GET_ONE, latch, OpCode.GET,
					false, 1000);
		}
		memcachedClient.getConnector().send(errorCommand);
		// wait 100 milliseconds,the operation will be timeout
		latch.await(100, TimeUnit.MILLISECONDS);
		assertNull(errorCommand.getResult());
		Thread.sleep(1000);
		// It works.
		assertNotNull(errorCommand.getResult());
		assertEquals("dennis", memcachedClient.get("name"));
	}

	public void _testOperationEncodeTimeout() throws Exception {
		memcachedClient.set("name", 0, "dennis");
		assertEquals("dennis", memcachedClient.get("name"));
		long writeMessageCount = memcachedClient.getConnector().getStatistics()
				.getWriteMessageCount();
		CountDownLatch latch = new CountDownLatch(1);
		Command errorCommand = null;
		if (memcachedClient.getProtocol() == Protocol.Text) {
			errorCommand = new MockEncodeTimeoutTextGetOneCommand("name",
					"name".getBytes(), CommandType.GET_ONE, latch, 1000);
		} else {
			errorCommand = new MockEncodeTimeoutBinaryGetCommand("name",
					"name".getBytes(), CommandType.GET_ONE, latch, OpCode.GET,
					false, 1000);
		}

		memcachedClient.getConnector().send(errorCommand);
		// Force write thread to encode command
		errorCommand.setIoBuffer(null);
		// wait 100 milliseconds,the operation will be timeout
		if (!latch.await(100, TimeUnit.MILLISECONDS)) {
			errorCommand.cancel();
		}
		Thread.sleep(1000);
		// It is not written to channel,because it is canceled.
		assertEquals(writeMessageCount, memcachedClient.getConnector()
				.getStatistics().getWriteMessageCount());
		// It works
		assertEquals("dennis", memcachedClient.get("name"));
	}

	public void testRemoveAndAddServer() throws Exception {
		String servers = properties.getProperty("test.memcached.servers");
		memcachedClient.set("name", 0, "dennis");
		assertEquals("dennis", memcachedClient.get("name"));
		memcachedClient.removeServer(servers);

		synchronized (this) {
			while (memcachedClient.getAvaliableServers().size() > 0) {
				wait(1000);
			}
		}
		assertEquals(0, memcachedClient.getAvaliableServers().size());
		try {
			memcachedClient.get("name");
			fail();
		} catch (MemcachedException e) {
			assertEquals("There is no available connection at this moment",
					e.getMessage());
		}

		memcachedClient
				.addServer(properties.getProperty("test.memcached.servers"));
		synchronized (this) {
			while (memcachedClient.getAvaliableServers().size() < AddrUtil
					.getAddresses(servers).size()) {
				wait(1000);
			}
		}
		Thread.sleep(5000);
		assertEquals("dennis", memcachedClient.get("name"));
	}

	public void testWeightedServers() throws Exception {
		// shutdown current client
		memcachedClient.shutdown();

		MemcachedClientBuilder builder = createWeightedBuilder();
		builder.getConfiguration().setStatisticsServer(true);
		memcachedClient = builder.build();
		memcachedClient.flushAll(5000);

		Map<InetSocketAddress, Map<String, String>> oldStats = memcachedClient
				.getStats();

		for (int i = 0; i < 100; i++) {
			assertTrue(memcachedClient.set(String.valueOf(i), 0, i));
		}
		for (int i = 0; i < 100; i++) {
			assertEquals(i, memcachedClient.get(String.valueOf(i)));
		}

		List<InetSocketAddress> addressList = AddrUtil
				.getAddresses(properties.getProperty("test.memcached.servers"));
		Map<InetSocketAddress, Map<String, String>> newStats = memcachedClient
				.getStats();
		for (InetSocketAddress address : addressList) {
			int oldSets = Integer
					.parseInt(oldStats.get(address).get("cmd_set"));
			int newSets = Integer
					.parseInt(newStats.get(address).get("cmd_set"));
			System.out.println("sets:" + (newSets - oldSets));
			int oldGets = Integer
					.parseInt(oldStats.get(address).get("cmd_get"));
			int newGets = Integer
					.parseInt(newStats.get(address).get("cmd_get"));
			System.out.println("gets:" + (newGets - oldGets));
		}
	}

	public void _testErrorCommand() throws Exception {
		Command nonexisCmd = new Command() {

			@Override
			public boolean decode(MemcachedTCPSession session,
					ByteBuffer buffer) {
				return decodeError(ByteUtils.nextLine(buffer));
			}

			@Override
			public void encode() {
				ioBuffer = IoBuffer
						.wrap(ByteBuffer.wrap("test\r\n".getBytes()));
			}

		};
		nonexisCmd.setKey("test");
		nonexisCmd.setLatch(new CountDownLatch(1));
		memcachedClient.getConnector().send(nonexisCmd);
		// this.memcachedClient.flushAll();
		nonexisCmd.getLatch().await();

		assertNotNull(nonexisCmd.getException());
		assertEquals("Nonexist command,check your memcached version please.",
				nonexisCmd.getException().getMessage());
		assertTrue(
				nonexisCmd.getException() instanceof UnknownCommandException);

		memcachedClient.set("name", 0, "dennis");
		assertEquals("dennis", memcachedClient.get("name"));
	}

	public void testGetAvaliableServers() {
		Collection<InetSocketAddress> servers = memcachedClient
				.getAvaliableServers();

		List<InetSocketAddress> serverList = AddrUtil
				.getAddresses(properties.getProperty("test.memcached.servers"));
		assertEquals(servers.size(), serverList.size());
		for (InetSocketAddress address : servers) {
			assertTrue(serverList.contains(address));
		}
	}

	public void testSanitizeKey() throws Exception {
		memcachedClient.setSanitizeKeys(true);

		String key = "The string 眉@foo-bar";
		assertTrue(memcachedClient.add(key, 0, 0));
		assertEquals(0, memcachedClient.get(key));

		assertTrue(memcachedClient.replace(key, 0, 1));
		assertEquals(1, memcachedClient.get(key, 2000));

		assertTrue(memcachedClient.set(key, 0, 2));
		assertEquals((Integer) 2,
				memcachedClient.get(key, 2000, new IntegerTranscoder()));

		assertTrue(memcachedClient.set(key, 0, "xmemcached",
				new StringTranscoder()));
		assertTrue(memcachedClient.append(key, " great"));
		assertTrue(memcachedClient.prepend(key, "hello "));

		assertEquals("hello xmemcached great", memcachedClient.get(key));

		// test bulk get
		List<String> keys = new ArrayList<String>();
		for (int i = 0; i < 100; i++) {
			memcachedClient.add(key + i, 0, i);
			keys.add(key + i);
		}

		Map<String, Integer> result = memcachedClient.get(keys, 5000);
		for (int i = 0; i < 100; i++) {
			assertEquals((Integer) i, result.get(key + i));
		}

		for (int i = 0; i < 100; i++) {
			assertTrue(memcachedClient.delete(key + i));
			assertNull(memcachedClient.get(key + i));
		}

		// test cas
		memcachedClient.set(key, 0, 1);
		memcachedClient.cas(key, new CASOperation<Integer>() {

			public int getMaxTries() {
				return 1;
			}

			public Integer getNewValue(long currentCAS, Integer currentValue) {
				return currentValue + 1;
			}

		});
		assertEquals((Integer) 2,
				memcachedClient.get(key, 2000, new IntegerTranscoder()));

	}

	@Override
	public void tearDown() throws Exception {
		memcachedClient.shutdown();
	}

	public void testCounter() throws Exception {
		Counter counter = memcachedClient.getCounter("a");
		Assert.assertEquals(0, counter.get());
		Assert.assertEquals(0, counter.get());

		Assert.assertEquals(1, counter.incrementAndGet());
		Assert.assertEquals(2, counter.incrementAndGet());
		Assert.assertEquals(3, counter.incrementAndGet());

		Assert.assertEquals(2, counter.decrementAndGet());
		Assert.assertEquals(1, counter.decrementAndGet());
		Assert.assertEquals(0, counter.decrementAndGet());
		Assert.assertEquals(0, counter.decrementAndGet());

		Assert.assertEquals(4, counter.addAndGet(4));
		Assert.assertEquals(7, counter.addAndGet(3));
		Assert.assertEquals(0, counter.addAndGet(-7));

		counter.set(1000);
		Assert.assertEquals(1000, counter.get());
		Assert.assertEquals(1001, counter.incrementAndGet());

		counter = memcachedClient.getCounter("b", 100);
		Assert.assertEquals(101, counter.incrementAndGet());
		Assert.assertEquals(102, counter.incrementAndGet());
		Assert.assertEquals(101, counter.decrementAndGet());

		// test issue 74
		counter = memcachedClient.getCounter("issue74", 0);
		for (int i = 0; i < 100; i++) {
			Assert.assertEquals(i + 1, counter.incrementAndGet());
		}
		for (int i = 0; i < 100; i++) {
			counter.decrementAndGet();
		}
		Assert.assertEquals(0, counter.get());
	}

	public void testIssue142() throws Exception {
		Counter counter = this.memcachedClient.getCounter("counter", 6);
		counter.get(); //
		counter.incrementAndGet(); // counter=7
		counter.decrementAndGet(); // counter=6
		counter.addAndGet(2);// counter=8
		assertEquals(8, counter.get()); // counter=8
		assertTrue(this.memcachedClient.delete("counter"));
	}

	// public void testKeyIterator() throws Exception {
	// if (memcachedClient.getProtocol() == Protocol.Text) {
	// Collection<InetSocketAddress> avaliableServers = memcachedClient
	// .getAvaliableServers();
	// InetSocketAddress address = avaliableServers.iterator().next();
	// KeyIterator it = memcachedClient.getKeyIterator(address);
	// while (it.hasNext()) {
	// memcachedClient.delete(it.next());
	// }
	// it = memcachedClient.getKeyIterator(address);
	// Assert.assertFalse(it.hasNext());
	// try {
	// it.next();
	// Assert.fail();
	// } catch (NoSuchElementException e) {
	// Assert.assertTrue(true);
	// }
	// for (int i = 0; i < 10; i++) {
	// memcachedClient.set(String.valueOf(i), 0, i);
	// }
	// it = memcachedClient.getKeyIterator(address);
	// Assert.assertTrue(it.hasNext());
	// Assert.assertEquals(address, it.getServerAddress());
	// while (it.hasNext()) {
	// String key = it.next();
	// Assert.assertEquals(Integer.parseInt(key),
	// memcachedClient.get(key));
	// }
	// Assert.assertFalse(it.hasNext());
	// } else {
	// // ignore
	// }
	//
	// }

	public void testNamespace() throws Exception {
		String ns = "user-id";
		this.memcachedClient.withNamespace(ns,
				new MemcachedClientCallable<Void>() {

					public Void call(MemcachedClient client)
							throws MemcachedException, InterruptedException,
							TimeoutException {
						assertNull(client.get("a"));
						assertNull(client.get("b"));
						assertNull(client.get("c"));
						return null;
					}
				});

		this.memcachedClient.withNamespace(ns,
				new MemcachedClientCallable<Void>() {

					public Void call(MemcachedClient client)
							throws MemcachedException, InterruptedException,
							TimeoutException {
						assertTrue(client.set("a", 0, 1));
						assertTrue(client.set("b", 0, 2));
						assertTrue(client.set("c", 0, 3));
						return null;
					}
				});

		this.memcachedClient.withNamespace(ns,
				new MemcachedClientCallable<Void>() {

					public Void call(MemcachedClient client)
							throws MemcachedException, InterruptedException,
							TimeoutException {
						assertEquals(1, client.get("a"));
						assertEquals(2, client.get("b"));
						assertEquals(3, client.get("c"));
						return null;
					}
				});

		this.memcachedClient.invalidateNamespace(ns);
		this.memcachedClient.withNamespace(ns,
				new MemcachedClientCallable<Void>() {

					public Void call(MemcachedClient client)
							throws MemcachedException, InterruptedException,
							TimeoutException {
						assertNull(client.get("a"));
						assertNull(client.get("b"));
						assertNull(client.get("c"));
						return null;
					}
				});

	}

	public void testNamespaceWithGetMulti() throws Exception {
		String ns = "user";
		this.memcachedClient.withNamespace(ns,
				new MemcachedClientCallable<Void>() {

					public Void call(MemcachedClient client)
							throws MemcachedException, InterruptedException,
							TimeoutException {
						client.set("a", 0, 1);
						client.set("b", 0, 2);
						client.set("c", 0, 3);
						Map<String, Object> values = client
								.get(Arrays.asList("a", "b", "c"));
						assertEquals(3, values.size());
						assertEquals(1, values.get("a"));
						assertEquals(2, values.get("b"));
						assertEquals(3, values.get("c"));
						return null;
					}
				});
	}

	public void testTouch() throws Exception {
		this.memcachedClient.set("x", 1, 0);
		assertEquals(0, this.memcachedClient.get("x"));
		assertTrue(this.memcachedClient.touch("x", 1));
		assertEquals(0, this.memcachedClient.get("x"));
		assertTrue(this.memcachedClient.touch("x", 1));
		Thread.sleep(1100);
		assertNull(this.memcachedClient.get("x"));
		if (memcachedClient.getProtocol() == Protocol.Binary) {
			this.memcachedClient.set("x", 1, 0);
			assertEquals(0, this.memcachedClient.getAndTouch("x", 1));
		}

		// touch not exists
		assertFalse(memcachedClient.touch("not_exists", 0));
	}

}
