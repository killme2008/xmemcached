package net.rubyeye.xmemcached.monitor;

/**
 * MemcachedClient insntance name holder
 * 
 * @author dennis
 * 
 */
public class MemcachedClientNameHolder {
	private static ThreadLocal<String> cacheName = new ThreadLocal<String>();

	public static void setName(String name) {
		cacheName.set(name);
	}
	
	public static String getName(){
		return cacheName.get();
	}

	public static void clear() {
		cacheName.remove();
	}

}
