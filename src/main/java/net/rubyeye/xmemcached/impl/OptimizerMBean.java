package net.rubyeye.xmemcached.impl;


/**
 * OptimizerMBean,used for changing the optimizer's factor
 *
 * @author dennis
 *
 */
public interface OptimizerMBean {
	public int getMergeFactor();

	public boolean isOptimizeGet();

	public boolean isOptimizeMergeBuffer();

	public void setMergeFactor(int mergeFactor);

	public void setOptimizeGet(boolean optimiezeGet);

	public void setOptimizeMergeBuffer(boolean optimiezeMergeBuffer);


}
