package net.rubyeye.xmemcached.monitor;

/**
 * OptimiezerMBean,用于jmx控制
 * 
 * @author dennis
 * 
 */
public interface OptimiezerMBean {
	int getMergeFactor();

	boolean isOptimiezeGet();

	boolean isOptimiezeMergeBuffer();

	void setMergeFactor(int mergeFactor);

	void setOptimiezeGet(boolean optimiezeGet);

	void setOptimiezeMergeBuffer(boolean optimiezeMergeBuffer);
}
