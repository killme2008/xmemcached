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

import net.rubyeye.xmemcached.utils.InetSocketAddressWrapper;

import com.google.code.yanf4j.core.impl.FutureImpl;

/**
 * Connect operation future
 * 
 * @author boyan
 * 
 */
public class ConnectFuture extends FutureImpl<Boolean> {

	private final InetSocketAddressWrapper inetSocketAddressWrapper;

	public ConnectFuture(InetSocketAddressWrapper inetSocketAddressWrapper) {
		super();
		this.inetSocketAddressWrapper = inetSocketAddressWrapper;
	}

	public InetSocketAddressWrapper getInetSocketAddressWrapper() {
		return this.inetSocketAddressWrapper;
	}


}
