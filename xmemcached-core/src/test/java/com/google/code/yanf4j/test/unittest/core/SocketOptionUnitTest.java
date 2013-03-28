package com.google.code.yanf4j.test.unittest.core;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import com.google.code.yanf4j.core.SocketOption;
import com.google.code.yanf4j.core.impl.StandardSocketOption;

public class SocketOptionUnitTest {
	@Test
	public void testType() {
		Assert.assertEquals(Integer.class, StandardSocketOption.SO_LINGER.type());
		Assert.assertEquals(Boolean.class, StandardSocketOption.SO_KEEPALIVE.type());
		Assert.assertEquals(Integer.class, StandardSocketOption.SO_RCVBUF.type());
		Assert.assertEquals(Integer.class, StandardSocketOption.SO_SNDBUF.type());
		Assert.assertEquals(Boolean.class, StandardSocketOption.SO_REUSEADDR.type());
		Assert.assertEquals(Boolean.class, StandardSocketOption.TCP_NODELAY.type());
	}
	
	@Test
	public void testPutInMap(){
		Map<SocketOption, Object> map=new HashMap<SocketOption, Object>();
		map.put(StandardSocketOption.SO_KEEPALIVE, true);
		map.put(StandardSocketOption.SO_RCVBUF, 4096);
		map.put(StandardSocketOption.SO_SNDBUF, 4096);
		map.put(StandardSocketOption.TCP_NODELAY, false);
		
		Assert.assertEquals(4096, map.get(StandardSocketOption.SO_RCVBUF));
		Assert.assertEquals(4096, map.get(StandardSocketOption.SO_SNDBUF));
		Assert.assertEquals(false, map.get(StandardSocketOption.TCP_NODELAY));
		Assert.assertEquals(true, map.get(StandardSocketOption.SO_KEEPALIVE));
	}
}
