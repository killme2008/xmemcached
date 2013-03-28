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
package net.rubyeye.xmemcached;

import java.net.InetSocketAddress;

/**
 * MemcachedClient state listener.When client startup,shutdown,connected to a
 * memcached server or disconnected happened,client will notify the listener
 * instance which implemented this interface.Please don't do any operations
 * which may block in these callback methods.
 * 
 * @author dennis
 * 
 */
public interface MemcachedClientStateListener {
	/**
	 * After client is started.
	 * 
	 * @param memcachedClient
	 */
	public void onStarted(MemcachedClient memcachedClient);

	/**
	 * After client is shutdown.
	 * 
	 * @param memcachedClient
	 */
	public void onShutDown(MemcachedClient memcachedClient);

	/**
	 * After a memcached server is connected,don't do any operations may block
	 * here.
	 * 
	 * @param memcachedClient
	 * @param inetSocketAddress
	 */
	public void onConnected(MemcachedClient memcachedClient,
			InetSocketAddress inetSocketAddress);

	/**
	 * After a memcached server is disconnected,don't do any operations may
	 * block here.
	 * 
	 * @param memcachedClient
	 * @param inetSocketAddress
	 */
	public void onDisconnected(MemcachedClient memcachedClient,
			InetSocketAddress inetSocketAddress);

	/**
	 * When exceptions occur
	 * 
	 * @param memcachedClient
	 * @param throwable
	 */
	public void onException(MemcachedClient memcachedClient, Throwable throwable);
}
