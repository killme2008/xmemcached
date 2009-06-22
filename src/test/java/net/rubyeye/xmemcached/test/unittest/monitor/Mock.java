package net.rubyeye.xmemcached.test.unittest.monitor;

public class Mock implements MockMBean {

	@Override
	public String say(String name) {
		return "hello," + name;
	}

}
