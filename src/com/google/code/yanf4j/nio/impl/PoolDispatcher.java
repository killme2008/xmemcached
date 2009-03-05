package com.google.code.yanf4j.nio.impl;
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
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.google.code.yanf4j.nio.Dispatcher;

public class PoolDispatcher implements Dispatcher {
	private ThreadPoolExecutor threadPool;

	public PoolDispatcher(int poolSize) {
		this.threadPool = new ThreadPoolExecutor(poolSize, poolSize, 0,
				TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(
						poolSize * 10));
		this.threadPool
				.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
	}

	public void dispatch(Runnable r) {
		this.threadPool.execute(r);
	}

	public void close() throws IOException {
		this.threadPool.shutdownNow();
	}

}
