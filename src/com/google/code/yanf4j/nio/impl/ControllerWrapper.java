package com.google.code.yanf4j.nio.impl;
/**
 *Copyright [2008-2009] [dennis zhuang]
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

import com.google.code.yanf4j.nio.Session;
import com.google.code.yanf4j.nio.Controller;

public interface ControllerWrapper extends Controller,SelectionKeyHandler,
		ControllerLifeCycle {
	public void unregisterSession(Session session);

	public void registerSession(Session session);

	public void checkStatisticsForRestart();

	public void closeChannel() throws IOException ;

	/**
	 * 是否超过流量控制
	 * 
	 * @return
	 */
	public boolean isOverFlow();
}
