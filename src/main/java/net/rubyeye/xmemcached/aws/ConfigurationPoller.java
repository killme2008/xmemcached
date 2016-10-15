package net.rubyeye.xmemcached.aws;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.rubyeye.xmemcached.MemcachedClient;

/**
 * AWS ElastiCache configuration poller
 * 
 * @author dennis
 *
 */
public class ConfigurationPoller implements Runnable {

	/**
	 * Return current ElasticCache node list.
	 * 
	 * @return
	 */
	public List<CacheNode> getCurrentNodeList() {
		return currentNodeList;
	}

	private MemcachedClient client;

	private int pollIntervalMills;

	private ScheduledExecutorService scheduledExecutorService;

	private volatile List<CacheNode> currentNodeList = Collections.emptyList();

	public ConfigurationPoller(MemcachedClient client, int pollIntervalMills) {
		super();
		this.client = client;
		this.pollIntervalMills = pollIntervalMills;
		this.scheduledExecutorService = Executors
				.newSingleThreadScheduledExecutor();
		this.scheduledExecutorService.scheduleWithFixedDelay(this,
				this.pollIntervalMills, this.pollIntervalMills,
				TimeUnit.MILLISECONDS);
	}

	public void run() {
		
	}

}
