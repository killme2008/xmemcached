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
package net.rubyeye.xmemcached;

/**
 * CAS operation,encapsulate gets and cas commands,and supports retry times.
 * 
 * @author dennis
 * 
 */
public interface CASOperation<T> {
	/**
	 * Max retry times,If retry times is great than this value,xmemcached will
	 * throw TimeoutException
	 * 
	 * @return
	 */
	public int getMaxTries();

	/**
	 * Return the new value which you want to cas
	 * 
	 * @param currentCAS
	 * @param currentValue
	 * @return expected new value
	 */
	public T getNewValue(long currentCAS, T currentValue);
}
