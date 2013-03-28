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

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientStateListener;

import com.google.code.yanf4j.core.Controller;
import com.google.code.yanf4j.core.ControllerStateListener;

/**
 * Adapte MemcachedClientStateListener to yanf4j's ControllStateListener
 * 
 * @author dennis
 * 
 */
public class MemcachedClientStateListenerAdapter implements
		ControllerStateListener {
	private final MemcachedClientStateListener memcachedClientStateListener;
	private final MemcachedClient memcachedClient;

	public MemcachedClientStateListenerAdapter(
			MemcachedClientStateListener memcachedClientStateListener,
			MemcachedClient memcachedClient) {
		super();
		this.memcachedClientStateListener = memcachedClientStateListener;
		this.memcachedClient = memcachedClient;
	}

	public final MemcachedClientStateListener getMemcachedClientStateListener() {
		return this.memcachedClientStateListener;
	}

	public final MemcachedClient getMemcachedClient() {
		return this.memcachedClient;
	}

	
	public final void onAllSessionClosed(Controller acceptor) {

	}

	
	public final void onException(Controller acceptor, Throwable t) {
		this.memcachedClientStateListener.onException(this.memcachedClient, t);

	}

	
	public final void onReady(Controller acceptor) {

	}

	
	public final void onStarted(Controller acceptor) {
		this.memcachedClientStateListener.onStarted(this.memcachedClient);

	}

	
	public final void onStopped(Controller acceptor) {
		this.memcachedClientStateListener.onShutDown(this.memcachedClient);

	}

}
