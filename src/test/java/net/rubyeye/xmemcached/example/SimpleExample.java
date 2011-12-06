package net.rubyeye.xmemcached.example;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import net.rubyeye.xmemcached.KeyIterator;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.utils.AddrUtil;


/**
 * Simple example for xmemcached
 * 
 * @author boyan
 * 
 */
public class SimpleExample {
    static String specilaKey =
            "emhhbmd5b25nYmluIDAwMTEzMDI3O2ZhbndlbmthaSAwMDE1MjkxOTt4aWFvcGVuZyA2MTA3OTt3YW5nZmVuZyA1OTQ2NTtjdWljaGFuZ3poaSAwMDE1MDIwMjtzdW5qaWUgMDAxMTUxMjc7Z3VvcGVuZ2JvIDAwMTEyOTQxO2xpbnl1biA2MTAwNDtsaXV5b25nIDAwMTYwNTM3O2xpdXpoZW5ncm9uZyAwMDEyMTAyNTt0dXhpYSAwMDE1Mjg3MDtkZW5ncm9uZyA1NTE0MztjaGVuamluIDAwMTM0MTk1O2xpa2VqaW5nIDAwMTIxOTczO2h1YW5nY2hlbmdkZSAwMDE2ODk5Njt6aGFuZ2ZhbiAwMDE2MzYxMzt0dW1pbmhhaSAwMDExODcwNTtwZWlqaW5nIDAwMTA1Mzc2O2x2amlhbmhvbmcgNjg2Njk7bHVvZnVyb25nIDY5NTE3O2xpdXRpZWp1biAwMDExNDQ5NztoYW54aW53ZWkgNTQ4MTA7RU5Hc2NvcGVJbnRyYW5ldA==";


    static String genKey(int len, Object v) {
        StringBuilder sb = new StringBuilder();
        while (sb.length() < len) {
            sb.append(v);
        }
        return sb.toString();
    }


    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Useage:java SimpleExample [servers]");
            System.exit(1);
        }
        MemcachedClient memcachedClient = getMemcachedClient(args[0]);
        if (memcachedClient == null) {
            throw new NullPointerException("Null MemcachedClient,please check memcached has been started");
        }
        try {
            // 设置一个长度超过250的key
            System.out.println(memcachedClient.get(specilaKey));
            memcachedClient.set(specilaKey, 0, 1);

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            // 设置一个正常的key
            memcachedClient.set(genKey(111, "b"), 0, 1);
            // 查看取值是否正确
            System.out.println("value:" + memcachedClient.get(genKey(111, "b")));
            // Thread.sleep(Integer.MAX_VALUE);
            memcachedClient.shutdown();
        }
        catch (Exception e) {
            System.err.println("Shutdown MemcachedClient fail");
            e.printStackTrace();
        }
    }


    public static MemcachedClient getMemcachedClient(String servers) {
        try {
            // use text protocol by default
            MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(servers));
            builder.setCommandFactory(new BinaryCommandFactory());
            return builder.build();
            // MemcachedClient client = new XMemcachedClient();
            // client.addServer(servers);
            // return client;
        }
        catch (IOException e) {
            System.err.println("Create MemcachedClient fail");
            e.printStackTrace();
        }
        return null;
    }
}
