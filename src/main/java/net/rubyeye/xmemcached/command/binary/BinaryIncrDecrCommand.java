package net.rubyeye.xmemcached.command.binary;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.command.CommandType;

public class BinaryIncrDecrCommand extends BaseBinaryCommand{

	public BinaryIncrDecrCommand(String key, byte[] keyBytes, int num,
			CommandType cmdType, boolean noreply) {
		super(key, keyBytes, cmdType, new CountDownLatch(1), 0, 0, null, noreply, null);
		// TODO Auto-generated constructor stub
	}

}
