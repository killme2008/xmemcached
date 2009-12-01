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
package net.rubyeye.xmemcached.impl;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Connect operation future
 * 
 * @author boyan
 * 
 */
public class ConnectFuture implements Future<Boolean> {

	private int weight;
	private boolean connected = false;
	private boolean done = false;
	private boolean cancel = false;
	private final Lock lock = new ReentrantLock();
	private final Condition notDone = this.lock.newCondition();
	private volatile Exception exception;
	private final InetSocketAddress inetSocketAddress;

	public ConnectFuture(InetSocketAddress inetSocketAddress, int weight) {
		super();
		this.inetSocketAddress = inetSocketAddress;
		this.weight = weight;
	}

	public final InetSocketAddress getInetSocketAddress() {
		return this.inetSocketAddress;
	}

	public final int getWeight() {
		return this.weight;
	}

	public final void setWeight(int weight) {
		this.weight = weight;
	}

	public boolean isConnected() {
		this.lock.lock();
		try {
			return this.connected;
		} finally {
			this.lock.unlock();
		}
	}

	public void setConnected(boolean connected) {
		this.lock.lock();
		try {
			this.connected = connected;
			this.done = true;
			this.notDone.signalAll();
		} finally {
			this.lock.unlock();
		}
	}

	public Exception getException() {
		this.lock.lock();
		try {
			return this.exception;
		} finally {
			this.lock.unlock();
		}
	}

	public void setException(Exception exception) {
		this.lock.lock();
		try {
			this.exception = exception;
			this.done = true;
			this.notDone.signalAll();
		} finally {
			this.lock.unlock();
		}
	}

	public boolean cancel(boolean mayInterruptIfRunning) {
		this.lock.lock();
		try {
			this.cancel = true;
			return this.cancel;
		} finally {
			this.lock.unlock();
		}
	}

	public Boolean get() throws InterruptedException, ExecutionException {
		this.lock.lock();
		try {
			while (!this.done) {
				this.notDone.await();
			}
			if (this.exception != null) {
				throw new ExecutionException(this.exception);
			}
			return this.connected;
		} finally {
			this.lock.unlock();
		}
	}

	public Boolean get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		this.lock.lock();
		try {

			while (!this.done) {
				if (!this.notDone.await(timeout, unit)) {
					throw new TimeoutException("connect timeout");
				}
			}
			if (this.exception != null) {
				throw new ExecutionException(this.exception);
			}
			return this.connected;
		} finally {
			this.lock.unlock();
		}
	}

	public boolean isCancelled() {
		this.lock.lock();
		try {
			return this.cancel;
		} finally {
			this.lock.unlock();
		}
	}

	public boolean isDone() {
		this.lock.lock();
		try {
			return this.done;
		} finally {
			this.lock.unlock();
		}
	}
}
