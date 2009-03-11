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
 * cas操作接口
 * @author dennis
 *
 */
public interface CASOperation {
	/**
	 * 最大重试次数
	 * 
	 * @return
	 */
	public int getMaxTries();

	/**
	 * 根据当前value和cas返回想要设置的新value
	 * 
	 * @param currentCAS
	 * @param currentValue
	 * @return 新的期望设置值
	 */
	public Object getNewValue(long currentCAS, Object currentValue);
}
