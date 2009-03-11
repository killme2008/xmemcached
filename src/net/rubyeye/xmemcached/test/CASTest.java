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
package net.rubyeye.xmemcached.test;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.CASOperation;
import net.rubyeye.xmemcached.XMemcachedClient;
/**
 * 测试CAS
 * @author dennis

 */
class CASThread extends Thread {
	private XMemcachedClient mc;
	private CountDownLatch cd;

	public CASThread(XMemcachedClient mc, CountDownLatch cdl) {
		super();
		this.mc = mc;
		this.cd = cdl;

	}

	public void run() {
		try {
			if (mc.cas("a", 0, new CASOperation() {
				@Override
				public int getMaxTries() {
					return 100;
				}

				@Override
				public Object getNewValue(long currentCAS, Object currentValue) {
					System.out.println("currentValue=" + currentValue
							+ ",currentCAS=" + currentCAS);
					return ((Integer) currentValue).intValue() + 1;
				}

			}))
				this.cd.countDown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

public class CASTest {
	static int NUM = 100;

	public static void main(String[] args) throws Exception {
		XMemcachedClient mc = new XMemcachedClient();
		mc.addServer("localhost", 12000);
		// 设置初始值为0
		mc.set("a", 0, 0);
		CountDownLatch cdl = new CountDownLatch(NUM);
		// 开NUM个线程递增变量a
		for (int i = 0; i < NUM; i++)
			new CASThread(mc, cdl).start();

		cdl.await();
		// 打印结果,最后结果应该为NUM
		System.out.println("result=" + mc.get("a"));
		mc.shutdown();
	}
}
