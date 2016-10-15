package net.rubyeye.xmemcached.aws;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeoutException;

import com.google.code.yanf4j.core.Session;

import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.exception.MemcachedException;

/**
 * AWS ElasticCache Client.
 * 
 * @author dennis
 *
 */
public class AWSElasticCacheClient extends XMemcachedClient implements
		ConfigUpdateListener {

	public void onUpdate(ClusterConfigration config) {
		this.currentClusterConfiguration = config;
		// TODO update with new node list
	}

	private final ConfigurationPoller configPoller;

	public AWSElasticCacheClient(InetSocketAddress addr,
			long pollConfigIntervalMills) throws IOException {
		super(addr);
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
