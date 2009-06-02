package net.rubyeye.xmemcached.test.unittest.commands.text;

import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.TextCommandFactory;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import junit.framework.TestCase;

public class BaseTextCommandUnitTest extends TestCase {
	protected CommandFactory commandFactory;
	protected BufferAllocator bufferAllocator;
	
	public void setUp(){
		this.bufferAllocator=new SimpleBufferAllocator();
		this.commandFactory=new TextCommandFactory();
	}
}
