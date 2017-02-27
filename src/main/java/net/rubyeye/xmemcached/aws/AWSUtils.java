package net.rubyeye.xmemcached.aws;

import java.util.ArrayList;
import java.util.List;

import net.rubyeye.xmemcached.utils.ByteUtils;

/**
 * AWS get config command
 * 
 * @author dennis
 *
 */
public class AWSUtils {

	private static final String DELIMITER = "|";

	/**
	 * Parse response string to ClusterConfiguration instance.
	 * 
	 * @param line
	 * @return
	 */
	public static ClusterConfigration parseConfiguration(String line) {
		String[] lines = line.trim().split("(?:\\r?\\n)");
		if (lines.length < 2) {
			throw new IllegalArgumentException("Incorrect config response:"
					+ line);
		}
		String configversion = lines[0];
		String nodeListStr = lines[1];
		if (!ByteUtils.isNumber(configversion)) {
			throw new IllegalArgumentException("Invalid configversion: "
					+ configversion + ", it should be a number.");
		}
		String[] nodeStrs = nodeListStr.split("(?:\\s)+");
		int version = Integer.parseInt(configversion);
		List<CacheNode> nodeList = new ArrayList<CacheNode>(nodeStrs.length);
		for (String nodeStr : nodeStrs) {
			if (nodeStr.equals("")) {
				continue;
			}

			int firstDelimiter = nodeStr.indexOf(DELIMITER);
			int secondDelimiter = nodeStr.lastIndexOf(DELIMITER);
			if (firstDelimiter < 1 || firstDelimiter == secondDelimiter) {
				throw new IllegalArgumentException("Invalid server ''"
						+ nodeStr + "'' in response:  " + line);
			}
			String hostName = nodeStr.substring(0, firstDelimiter).trim();
			String ipAddress = nodeStr.substring(firstDelimiter + 1,
					secondDelimiter).trim();
			String portNum = nodeStr.substring(secondDelimiter + 1).trim();
			int port = Integer.parseInt(portNum);
			nodeList.add(new CacheNode(hostName, ipAddress, port));
		}

		return new ClusterConfigration(version, nodeList);

	}

}
