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
package com.google.code.yanf4j.util;

import java.util.Properties;

/**
 * java.util.Property utils
 * 
 * @author dennis
 * 
 */
public class PropertyUtils {

	public static int getPropertyAsInteger(Properties props, String propName) {
		return Integer.parseInt(PropertyUtils.getProperty(props, propName));
	}

	public static String getProperty(Properties props, String name) {
		return props.getProperty(name).trim();
	}

	public static boolean getPropertyAsBoolean(Properties props, String name) {
		return Boolean.valueOf(getProperty(props, name));
	}

	public static long getPropertyAsLong(Properties props, String name) {
		return Long.parseLong(getProperty(props, name));
	}

	public static short getPropertyAsShort(Properties props, String name) {
		return Short.parseShort(getProperty(props, name));
	}

	public static byte getPropertyAsByte(Properties props, String name) {
		return Byte.parseByte(getProperty(props, name));
	}
}
