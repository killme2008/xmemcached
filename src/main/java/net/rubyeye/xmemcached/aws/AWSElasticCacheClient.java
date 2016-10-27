package net.rubyeye.xmemcached.aws;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.UserDataHandler;

import com.google.code.yanf4j.core.Session;

import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.TextCommandFactory;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.utils.InetSocketAddressWrapper;

/**
 * AWS ElasticCache Client.
 * 
 * @author dennis
 *
 */
public class AWSElasticCacheClient extends XMemcachedClient implements
		ConfigUpdateListener {

	private static final Logger log = LoggerFactory
			.getLogger(AWSElasticCacheClient.class);

	private boolean firstTimeUpdate = true;

	private InetSocketAddress configAddr;

	public synchronized void onUpdate(ClusterConfigration config) {

		if (firstTimeUpdate) {
			firstTimeUpdate = false;
			removeConfigAddr();
		}

		List<CacheNode> oldList = this.currentClusterConfiguration != null ? this.currentClusterConfiguration
				.getNodeList() : Collections.EMPTY_LIST;
		List<CacheNode> newList = config.getNodeList();

		List<CacheNode> addNodes = new ArrayList<CacheNode>();
		List<CacheNode> removeNodes = new ArrayList<CacheNode>();

		for (CacheNode node : newList) {
			if (!oldList.contains(node)) {
				addNodes.add(node);
			}
		}

		for (CacheNode node : oldList) {
			if (!newList.contains(node)) {
				removeNodes.add(node);
			}
		}

		// Begin to update server list
		for (CacheNode node : addNodes) {
			try {
				this.connect(new InetSocketAddressWrapper(node
						.getInetSocketAddress(), this.configPoller
						.getCacheNodeOrder(node), 1, null));
			} catch (IOException e) {
				log.error("Connect to " + node + "failed.", e);
			}
		}

		for (CacheNode node : removeNodes) {
			this.removeAddr(node.getInetSocketAddress());
		}

		this.currentClusterConfiguration = config;
	}

	private void removeConfigAddr() {
		this.removeAddr(configAddr);
		while (this.getConnector().getSessionByAddress(configAddr) != null
				&& this.getConnector().getSessionByAddress(configAddr).size() > 0) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private final ConfigurationPoller configPoller;

	public static final long DEFAULT_POLL_CONFIG_INTERVAL_MS = 60000;

	public AWSElasticCacheClient(InetSocketAddress addr) throws IOException {
		this(addr, DEFAULT_POLL_CONFIG_INTERVAL_MS);
	}

	public AWSElasticCacheClient(InetSocketAddress addr,
			long pollConfigIntervalMills) throws IOException {
		this(addr, pollConfigIntervalMills, new TextCommandFactory());
	}

	public AWSElasticCacheClient(InetSocketAddress addr,
			long pollConfigIntervalMills, CommandFactory commandFactory)
			throws IOException {
		super(addr, 1, commandFactory);
		// Use failure mode by default.
		this.commandFactory = commandFactory;
		this.setFailureMode(true);
		this.configAddr = addr;
		this.configPoller = new ConfigurationPoller(this,
				pollConfigIntervalMills);
		// Run at once to get config at startup.
		// It will call onUpdate in the same thread.
		this.configPoller.run();
		if (this.currentClusterConfiguration == null) {
			throw new IllegalStateException(
					"Retrieve ElasticCache config from `" + addr.toString()
							+ "` failed.");
		}
		this.configPoller.start();
	}

	private volatile ClusterConfigration currentClusterConfiguration;

	/**
	 * Get cluster config from cache node by network command.
	 * 
	 * @return
	 */
	public ClusterConfigration getConfig() throws MemcachedException,
			InterruptedException, TimeoutException {
		return this.getConfig("cluster");
	}

	/**
	 * Get config by key from cache node by network command.
	 * 
	 * @return
	 */
	public ClusterConfigration getConfig(String key) throws MemcachedException,
			InterruptedException, TimeoutException {
		Command cmd = this.commandFactory.createAWSElasticCacheConfigCommand(
				"get", key);
		final Session session = this.sendCommand(cmd);
		this.latchWait(cmd, opTimeout, session);
		cmd.getIoBuffer().free();
		this.checkException(cmd);
		String result = (String) cmd.getResult();
		if (result == null) {
			throw new MemcachedException(
					"Operation fail,may be caused by networking or timeout");
		}
		return AWSUtils.parseConfiguration(result);
	}

	/**
	 * Get the current using configuration in memory.
	 * 
	 * @return
	 */
	public ClusterConfigration getCurrentConfig() {
		return this.currentClusterConfiguration;
	}
}
