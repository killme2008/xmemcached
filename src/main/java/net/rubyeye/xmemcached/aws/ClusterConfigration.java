package net.rubyeye.xmemcached.aws;

import java.io.Serializable;
import java.util.List;

/**
 * Cluster configuration retrieved from ElasticCache.
 * 
 * @author dennis
 * 
 */
public class ClusterConfigration implements Serializable {

  private static final long serialVersionUID = 6809891639636689050L;

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public List<CacheNode> getNodeList() {
    return nodeList;
  }

  public void setNodeList(List<CacheNode> nodeList) {
    this.nodeList = nodeList;
  }

  private int version;
  private List<CacheNode> nodeList;

  public ClusterConfigration(int version, List<CacheNode> nodeList) {
    super();
    this.version = version;
    this.nodeList = nodeList;
  }

  public ClusterConfigration() {
    super();
  }

  public String toString() {
    StringBuilder nodeList = new StringBuilder("{ Version: " + version + ", CacheNode List: ");
    nodeList.append(this.nodeList);
    nodeList.append("}");

    return nodeList.toString();
  }
}
