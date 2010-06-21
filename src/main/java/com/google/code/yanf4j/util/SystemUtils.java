package com.google.code.yanf4j.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;

/**
 * System utils
 * 
 * @author dennis
 * 
 */
public final class SystemUtils {

	private SystemUtils() {

	}

	public static final String OS_NAME = System.getProperty("os.name");

	private static boolean isLinuxPlatform = false;

	static {
		if (OS_NAME != null && OS_NAME.toLowerCase().indexOf("linux") >= 0) {
			isLinuxPlatform = true;
		}
	}
	public static final String JAVA_VERSION = System
			.getProperty("java.version");
	private static boolean isAfterJava6u4Version = false;
	static {
		if (JAVA_VERSION != null) {
			// java4 or java5
			if (JAVA_VERSION.indexOf("1.4.") >= 0
					|| JAVA_VERSION.indexOf("1.5.") >= 0) {
				isAfterJava6u4Version = false;
			} else if (JAVA_VERSION.indexOf("1.6.") >= 0) {
				int index = JAVA_VERSION.indexOf("_");
				if (index > 0) {
					String subVersionStr = JAVA_VERSION.substring(index + 1);
					if (subVersionStr != null && subVersionStr.length() > 0) {
						try {
							int subVersion = Integer.parseInt(subVersionStr);
							if (subVersion >= 4) {
								isAfterJava6u4Version = true;
							}
						} catch (NumberFormatException e) {

						}
					}
				}
				// after java6
			} else {
				isAfterJava6u4Version = true;
			}
		}
	}

	public static final boolean isLinuxPlatform() {
		return isLinuxPlatform;
	}

	public static final boolean isAfterJava6u4Version() {
		return isAfterJava6u4Version;
	}

	public static void main(String[] args) {
		System.out.println(isAfterJava6u4Version());
	}

	public static final int getSystemThreadCount() {
		int cpus = getCpuProcessorCount();
		return cpus - 1 > 0 ? cpus - 1 : cpus;
	}

	public static final int getCpuProcessorCount() {
		return Runtime.getRuntime().availableProcessors();
	}

	public static final Selector openSelector() throws IOException {
		Selector result = null;
		// check if it is linux os
		if (SystemUtils.isLinuxPlatform()) {
			try {
				Class<?> providerClazz = Class
						.forName("sun.nio.ch.EPollSelectorProvider");
				if (providerClazz != null) {
					try {
						Method method = providerClazz.getMethod("provider");
						if (method != null) {
							SelectorProvider selectorProvider = (SelectorProvider) method
									.invoke(null);
							if (selectorProvider != null) {
								result = selectorProvider.openSelector();
							}
						}
					} catch (Exception e) {
						// ignore
					}
				}
			} catch (Exception e) {
				// ignore
			}
		}
		if (result == null) {
			result = Selector.open();
		}
		return result;

	}

}
