package net.rubyeye.xmemcached.command.binary;

public enum BinaryDecodeStatus {
	NONE, READ_HEADER, READ_EXTRAS, READ_KEY, READ_VALUE,DONE,IGNORE
}
