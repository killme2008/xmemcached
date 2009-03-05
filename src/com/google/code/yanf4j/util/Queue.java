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
package com.google.code.yanf4j.util;

import java.util.concurrent.locks.Lock;


public interface Queue<T> {

	public abstract boolean push(T obj)throws InterruptedException;

	public abstract T pop()throws InterruptedException;

	public abstract T peek();

	public abstract void clear();

	public abstract boolean isEmpty();

	public abstract int size();

	public Lock getLock();

	public T[] drainToArray();

	public boolean push(T obj, long timeout) throws InterruptedException;

	public T pop(long timeout) throws InterruptedException;

	public int getLowWaterMark();

	public void setLowWaterMark(int lowWaterMark);

	public int getHighWaterMark();

	public void setHighWaterMark(int highWaterMark);

	public boolean isFull();

}