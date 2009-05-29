package net.rubyeye.xmemcached.impl;


/**
 * OptimiezerMBean,used for changing the optimizer's factor
 *
 * @author dennis
 *
 */
public interface OptimiezerMBean {
	public int getMergeFactor();

	public boolean isOptimiezeGet();

	public boolean isOptimiezeMergeBuffer();

	public void setMergeFactor(int mergeFactor);

	public void setOptimiezeGet(boolean optimiezeGet);

	public void setOptimiezeMergeBuffer(boolean optimiezeMergeBuffer);


}
