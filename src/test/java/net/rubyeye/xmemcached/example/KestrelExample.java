package net.rubyeye.xmemcached.example;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;

public class KestrelExample {
  public static void main(String []args)throws Exception{
	  XMemcachedClientBuilder builder=new XMemcachedClientBuilder();
	  builder.setSessionLocator(new KetamaMemcachedSessionLocator());
	  MemcachedClient xmc =builder.build();
	  xmc.addServer("61.152.188.80",22133);
	  xmc.setOptimizeGet(false);
	  xmc.setOpTimeout(60000);
	  System.out.println(xmc.get("test_queue_2"));//正常
	  System.out.println(xmc.get("test_queue/t=250"));//异常
  }
}
