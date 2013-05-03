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
package com.google.code.yanf4j.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread factory for worker thread
 * 
 * @author dennis
 * 
 */
public class WorkerThreadFactory implements ThreadFactory {
	private static final AtomicInteger poolNumber = new AtomicInteger(1);
	private final ThreadGroup group;
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final String namePrefix;

	public WorkerThreadFactory(ThreadGroup group, String prefix) {
		if (group == null) {
			SecurityManager s = System.getSecurityManager();
			this.group = (s != null) ? s.getThreadGroup() : Thread
					.currentThread().getThreadGroup();
		} else {
			this.group = group;
		}
		if (prefix == null) {
			this.namePrefix = "pool-" + poolNumber.getAndIncrement()
					+ "-thread-";
		} else {
			this.namePrefix = prefix + "-" + poolNumber.getAndIncrement()
					+ "-thread-";
		}
	}
	
	public WorkerThreadFactory(String prefix){
		this(null,prefix);
	}

	public WorkerThreadFactory() {
		this(null, null);
	}

	public Thread newThread(Runnable r) {
		Thread t = new Thread(this.group, r, this.namePrefix
				+ this.threadNumber.getAndIncrement(), 0);
		t.setDaemon(true);
		if (t.getPriority() != Thread.NORM_PRIORITY) {
			t.setPriority(Thread.NORM_PRIORITY);
		}
		return t;
	}

}
