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

/**
 * Auto reconnect request
 * 
 * @author dennis
 * 
 */
public final class ReconnectRequest {

	private InetSocketAddressWrapper inetSocketAddressWrapper;
	private int tries;

	private int weight;

	public ReconnectRequest(InetSocketAddressWrapper inetSocketAddressWrapper, int tries, int weight) {
		super();
		this.setInetSocketAddressWrapper(inetSocketAddressWrapper);
		this.setTries(tries); // record reconnect times
		this.weight = weight;
	}


	public final InetSocketAddressWrapper getInetSocketAddressWrapper() {
		return this.inetSocketAddressWrapper;
	}


	public final void setInetSocketAddressWrapper(
			InetSocketAddressWrapper inetSocketAddressWrapper) {
		this.inetSocketAddressWrapper = inetSocketAddressWrapper;
	}


	public final void setTries(int tries) {
		this.tries = tries;
	}

	public final int getTries() {
		return this.tries;
	}

	public final int getWeight() {
		return this.weight;
	}

	public final void setWeight(int weight) {
		this.weight = weight;
	}

}