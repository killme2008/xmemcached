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

import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.HashAlgorithm;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.MemcachedSessionLocator;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.command.TextCommandFactory;
import net.rubyeye.xmemcached.impl.ArrayMemcachedSessionLocator;
import net.rubyeye.xmemcached.impl.PHPMemcacheSessionLocator;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import net.rubyeye.xmemcached.utils.AddrUtil;

import com.googlecode.hibernate.memcached.Config;
import com.googlecode.hibernate.memcached.Memcache;
import com.googlecode.hibernate.memcached.MemcacheClientFactory;
import com.googlecode.hibernate.memcached.PropertiesHelper;

/**
 * Parses hibernate properties to produce a MemcachedClient.<br/>
 * See {@link com.googlecode.hibernate.memcached.MemcachedCacheProvider} for
 * property details. SPI for xmemcached.
 * 
 * @author dennis
 */
public class XmemcachedClientFactory implements MemcacheClientFactory {

	public static final String PROP_SERVERS = Config.PROP_PREFIX + "servers";
	public static final String PROP_READ_BUFFER_SIZE = Config.PROP_PREFIX
			+ "readBufferSize";
	public static final String PROP_OPERATION_TIMEOUT = Config.PROP_PREFIX
			+ "operationTimeout";
	public static final String PROP_HASH_ALGORITHM = Config.PROP_PREFIX
			+ "hashAlgorithm";
	public static final String PROP_COMMAND_FACTORY = Config.PROP_PREFIX
			+ "commandFactory";

	public static final String PROP_SESSION_LOCATOR = Config.PROP_PREFIX
			+ "sessionLocator";
	
	public static final String PROP_CONNECTION_POOL_SIZE = Config.PROP_PREFIX
            + "connectionPoolSize";
	
	public static final String PROP_CONNECT_TIMEOUT = Config.PROP_PREFIX
            + "connectTimeout";

	private final PropertiesHelper properties;

	public XmemcachedClientFactory(PropertiesHelper properties) {
		this.properties = properties;
	}

	public Memcache createMemcacheClient() throws Exception {
		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil
				.getAddresses(getServerList()));
		builder.setCommandFactory(getCommandFactory());
		builder.setSessionLocator(getSessionLocator());
		builder.getConfiguration()
				.setSessionReadBufferSize(getReadBufferSize());
		builder.setConnectionPoolSize(getConnectionPoolSize());
		builder.setConnectTimeout(getConnectTimeoutMillis());
		MemcachedClient client = builder.build();
		client.setOpTimeout(getOperationTimeoutMillis());
		return new Xmemcache(client);
	}

	protected MemcachedSessionLocator getSessionLocator() {
		if (sessionLocatorNameEquals(ArrayMemcachedSessionLocator.class)) {
			return new ArrayMemcachedSessionLocator(getHashAlgorithm());
		}

		if (sessionLocatorNameEquals(KetamaMemcachedSessionLocator.class)) {
			return new KetamaMemcachedSessionLocator(getHashAlgorithm());
		}

		if (sessionLocatorNameEquals(PHPMemcacheSessionLocator.class)) {
			return new PHPMemcacheSessionLocator(getHashAlgorithm());
		}

		throw new IllegalArgumentException("Unsupported "
				+ PROP_SESSION_LOCATOR + " value: " + getCommandFactoryName());
	}

	protected CommandFactory getCommandFactory() {
		if (commandFactoryNameEquals(TextCommandFactory.class)) {
			return new TextCommandFactory();
		}

		if (commandFactoryNameEquals(BinaryCommandFactory.class)) {
			return new BinaryCommandFactory();
		}

		throw new IllegalArgumentException("Unsupported "
				+ PROP_COMMAND_FACTORY + " value: " + getCommandFactoryName());
	}

	private boolean commandFactoryNameEquals(Class<?> cls) {
		return cls.getSimpleName().equals(getCommandFactoryName());
	}
	
	private boolean sessionLocatorNameEquals(Class<?> cls) {
		return cls.getSimpleName().equals(getSessionLocatorName());
	}

	public String getServerList() {
		return this.properties.get(PROP_SERVERS, "localhost:11211");
	}

	public int getReadBufferSize() {
		return this.properties.getInt(PROP_READ_BUFFER_SIZE,
				MemcachedClient.DEFAULT_SESSION_READ_BUFF_SIZE);
	}
	
	
    public int getConnectionPoolSize() {
        return this.properties.getInt(PROP_CONNECTION_POOL_SIZE,
                MemcachedClient.DEFAULT_CONNECTION_POOL_SIZE);
    }

	public long getOperationTimeoutMillis() {
		return this.properties.getLong(PROP_OPERATION_TIMEOUT,
				MemcachedClient.DEFAULT_OP_TIMEOUT);
	}

	public long getConnectTimeoutMillis() {
        return this.properties.getLong(PROP_CONNECT_TIMEOUT,
                MemcachedClient.DEFAULT_CONNECT_TIMEOUT);
    }
	
	public HashAlgorithm getHashAlgorithm() {
		return this.properties.getEnum(PROP_HASH_ALGORITHM,
				HashAlgorithm.class, HashAlgorithm.NATIVE_HASH);
	}

	public String getCommandFactoryName() {
		return this.properties.get(PROP_COMMAND_FACTORY,
				TextCommandFactory.class.getSimpleName());
	}

	public String getSessionLocatorName() {
		return this.properties.get(PROP_SESSION_LOCATOR,
				ArrayMemcachedSessionLocator.class.getSimpleName());
	}

	protected PropertiesHelper getProperties() {
		return this.properties;
	}
}
