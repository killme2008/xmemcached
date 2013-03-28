/**
 *Copyright [2009-2010] [dennis zhuang]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package com.google.code.yanf4j.nio;

import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;

import com.google.code.yanf4j.core.EventType;
import com.google.code.yanf4j.core.Session;

/**
 * Nio connection
 * 
 * @author dennis
 * 
 */
public interface NioSession extends Session {
	/**
	 * When io event occured
	 * 
	 * @param event
	 * @param selector
	 */
	public void onEvent(EventType event, Selector selector);

	/**
	 * Enable read event
	 * 
	 * @param selector
	 */
	public void enableRead(Selector selector);

	/**
	 * Enable write event
	 * 
	 * @param selector
	 */
	public void enableWrite(Selector selector);

	/**
	 * return the channel for this connection
	 * 
	 * @return
	 */

	public SelectableChannel channel();
}
