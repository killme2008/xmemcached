/**
 *Copyright [2008] [dennis zhuang]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package com.google.code.yanf4j.nio.util;

import com.google.code.yanf4j.nio.Dispatcher;
import com.google.code.yanf4j.nio.impl.PoolDispatcher;
import com.google.code.yanf4j.nio.impl.SimpleDispatcher;

public class DispatcherFactory {
	public static Dispatcher newDispatcher(int size) {
		if (size > 0)
			return new PoolDispatcher(size);
		else
			return new SimpleDispatcher();
	}
}
