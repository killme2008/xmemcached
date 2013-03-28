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
package com.google.code.yanf4j.nio;

import java.nio.channels.SelectableChannel;
import java.util.Queue;

import com.google.code.yanf4j.core.CodecFactory;
import com.google.code.yanf4j.core.Dispatcher;
import com.google.code.yanf4j.core.Handler;
import com.google.code.yanf4j.core.SessionConfig;
import com.google.code.yanf4j.core.WriteMessage;
import com.google.code.yanf4j.nio.impl.SelectorManager;
import com.google.code.yanf4j.statistics.Statistics;

/**
 * Nio session configuration
 * 
 * @author dennis
 * 
 */
public class NioSessionConfig extends SessionConfig {

	public final SelectableChannel selectableChannel;
	public final SelectorManager selectorManager;

	public NioSessionConfig(SelectableChannel sc, Handler handler,
			SelectorManager reactor, CodecFactory codecFactory,
			Statistics statistics, Queue<WriteMessage> queue,
			Dispatcher dispatchMessageDispatcher,
			boolean handleReadWriteConcurrently, long sessionTimeout,
			long sessionIdleTimeout) {
		super(handler, codecFactory, statistics, queue,
				dispatchMessageDispatcher, handleReadWriteConcurrently,
				sessionTimeout, sessionIdleTimeout);
		this.selectableChannel = sc;
		this.selectorManager = reactor;
	}

}
