package net.rubyeye.xmemcached.test.unittest.monitor;

public class Mock implements MockMBean {

	
	public String say(String name) {
		return "hello," + name;
	}

}
