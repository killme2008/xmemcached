package net.rubyeye.xmemcached.autodiscovery;

/**
 * Auto Discovery config update event listener.
 * 
 * @author dennis
 * 
 */
public interface ConfigUpdateListener {

  /**
   * Called when config is changed.
   * 
   * @param config the new config
   */
  public void onUpdate(ClusterConfiguration config);
}
