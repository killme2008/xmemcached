package net.rubyeye.xmemcached.helper;

/**
 * 单元测试包装接口，用于检测异常是否正确
 * 
 * @author boyan
 * 
 */
public interface ExceptionChecker {
	public void call() throws Exception;
	public void check() throws Exception;
}
