/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package net.rubyeye.xmemcached.example;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.CASOperation;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.utils.AddrUtil;

/**
 * CASOperation example
 * 
 * @author dennis
 */
class CASThread extends Thread {
	/**
	 * Increase Operation
	 * 
	 * @author dennis
	 * 
	 */
	static final class IncrmentOperation implements CASOperation<Integer> {

		public int getMaxTries() {
			return Integer.MAX_VALUE; // Max repeat times
		}

		public Integer getNewValue(long currentCAS, Integer currentValue) {
			return currentValue + 1;
		}
	}

	private MemcachedClient mc;
	private CountDownLatch cd;

	public CASThread(MemcachedClient mc, CountDownLatch cdl) {
		super();
		this.mc = mc;
		this.cd = cdl;

	}

	@Override
	public void run() {
		try {
			for (int i = 0; i < 100; i++)
				if (this.mc.cas("a", 0, new IncrmentOperation())) {
					this.cd.countDown();
				}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

public class CASExample {

	public static void main(String[] args) throws Exception {
		//125489
		if (args.length < 2) {
			System.err.println("Usage:java CASTest [threadNum] [server]");
			System.exit(1);
		}
		int NUM = Integer.parseInt(args[0]);
		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddresses(args[1]));
		// use binary protocol
		builder.setCommandFactory(new BinaryCommandFactory());
		MemcachedClient mc = builder.build();
		// initial value is 0
		mc.set("a", 0, 0);
		CountDownLatch cdl = new CountDownLatch(NUM * 100);
		long start = System.currentTimeMillis();
		// start Num threads to increase 'a'
		for (int i = 0; i < NUM; i++) {
			new CASThread(mc, cdl).start();
		}

		cdl.await();
		System.out.println("test cas,timed:"
				+ (System.currentTimeMillis() - start));
		// print result,must equals to NUM
		System.out.println("result=" + mc.get("a"));
		mc.shutdown();
	}
}
