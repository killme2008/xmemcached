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
package net.rubyeye.xmemcached.utils.hibernate;

import java.util.Arrays;
import java.util.Map;

import net.rubyeye.xmemcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.hibernate.memcached.LoggingMemcacheExceptionHandler;
import com.googlecode.hibernate.memcached.Memcache;
import com.googlecode.hibernate.memcached.MemcacheExceptionHandler;
import com.googlecode.hibernate.memcached.utils.StringUtils;
/**
 * Hibernate memcached implementation
 * @author boyan
 *
 */
public class Xmemcache implements Memcache {
	private static final Logger log = LoggerFactory.getLogger(Xmemcache.class);
	private MemcacheExceptionHandler exceptionHandler = new LoggingMemcacheExceptionHandler();

	private final MemcachedClient memcachedClient;

	public Xmemcache(MemcachedClient memcachedClient) {
		this.memcachedClient = memcachedClient;
	}

	public Object get(String key) {
		try {
			log.debug("MemcachedClient.get({})", key);
			return this.memcachedClient.get(key);
		} catch (Exception e) {
			this.exceptionHandler.handleErrorOnGet(key, e);
		}
		return null;
	}

	public Map<String, Object> getMulti(String... keys) {
		try {
			return this.memcachedClient.get(Arrays.asList(keys));
		} catch (Exception e) {
			this.exceptionHandler.handleErrorOnGet(
					StringUtils.join(keys, ", "), e);
		}
		return null;
	}

	public void set(String key, int cacheTimeSeconds, Object o) {
		log.debug("MemcachedClient.set({})", key);
		try {
			this.memcachedClient.set(key, cacheTimeSeconds, o);
		} catch (Exception e) {
			this.exceptionHandler.handleErrorOnSet(key, cacheTimeSeconds, o, e);
		}
	}

	public void delete(String key) {
		try {
			this.memcachedClient.delete(key);
		} catch (Exception e) {
			this.exceptionHandler.handleErrorOnDelete(key, e);
		}
	}

	public void incr(String key, int factor, int startingValue) {
		try {
			this.memcachedClient.incr(key, factor, startingValue);
		} catch (Exception e) {
			this.exceptionHandler.handleErrorOnIncr(key, factor, startingValue,
					e);
		}
	}

	public void shutdown() {
		log.debug("Shutting down XMemcachedClient");
		try {
			this.memcachedClient.shutdown();
		} catch (Exception e) {
			log.error("Shut down XMemcachedClient error", e);
		}

	}

	public void setExceptionHandler(MemcacheExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}
}
