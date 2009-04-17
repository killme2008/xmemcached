package net.rubyeye.xmemcached;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.IoBuffer;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleIoBuffer;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.utils.ByteUtils;
import net.spy.memcached.transcoders.CachedData;
import net.spy.memcached.transcoders.Transcoder;

/**
 * command工厂类，创建command
 *
 * @author dennis
 *
 */
public final class CommandFactory {

    private static BufferAllocator bufferAllocator = new SimpleBufferAllocator();

    public static final void setBufferAllocator(
            BufferAllocator allocator) {
        BufferAllocator oldAllocator = bufferAllocator;
        bufferAllocator = allocator;
        if (oldAllocator != null) {
            oldAllocator.dispose();
        }
    }

    /**
     * 创建delete命令
     *
     * @param key
     * @param time
     * @return
     */
    public static final Command createDeleteCommand(final String key, final byte[] keyBytes,
            final int time) {
        final CountDownLatch latch = new CountDownLatch(1);
        byte[] timeBytes = ByteUtils.getBytes(String.valueOf(time));
        final IoBuffer buffer = bufferAllocator.allocate(ByteUtils.DELETE.length + 2 + keyBytes.length + timeBytes.length + ByteUtils.CRLF.length);
        ByteUtils.setArguments(buffer, ByteUtils.DELETE, keyBytes, timeBytes);
        buffer.flip();
        Command command = new Command(key, Command.CommandType.DELETE, latch);
        command.setIoBuffer(buffer);
        return command;
    }
    static final ByteBuffer VERSION = ByteBuffer.wrap("version\r\n".getBytes());

    /**
     * 创建version command
     *
     * @return
     */
    public static final Command createVersionCommand() {
        final CountDownLatch latch = new CountDownLatch(1);
        final IoBuffer buffer = new SimpleIoBuffer(VERSION.slice());
        Command command = new Command("version", Command.CommandType.VERSION,
                latch);
        command.setIoBuffer(buffer);
        return command;
    }

    /**
     * 创建存储命令，如set,add,replace,append,prepend,cas
     *
     * @param key
     * @param exp
     * @param value
     * @param cmdType
     * @param cmd
     * @param cas
     *            cas值，仅对cas协议有效，其他都默认为-1
     * @param transcoder
     * @return
     */
    @SuppressWarnings("unchecked")
    public static final Command createStoreCommand(final String key, final byte[] keyBytes,
            final int exp, final Object value, Command.CommandType cmdType,
            final String cmd, long cas, Transcoder transcoder) {
        final CountDownLatch latch = new CountDownLatch(1);
        final CachedData data = transcoder.encode(value);
        byte[] flagBytes = ByteUtils.getBytes(String.valueOf(data.getFlags()));
        byte[] expBytes = ByteUtils.getBytes(String.valueOf(exp));
        byte[] dataLenBytes = ByteUtils.getBytes(String.valueOf(data.getData().length));
        byte[] casBytes = ByteUtils.getBytes(String.valueOf(cas));
        int size = cmd.length() + 1 + keyBytes.length + 1 + flagBytes.length + 1 + expBytes.length + 1 + data.getData().length + 2 * ByteUtils.CRLF.length + dataLenBytes.length;
        if (cmdType == Command.CommandType.CAS) {
            size += 1 + casBytes.length;
        }
        final IoBuffer buffer = bufferAllocator.allocate(size);
        if (cmdType == Command.CommandType.CAS) {
            ByteUtils.setArguments(buffer, cmd, keyBytes, flagBytes, expBytes,
                    dataLenBytes, casBytes);
        } else {
            ByteUtils.setArguments(buffer, cmd, keyBytes, flagBytes, expBytes,
                    dataLenBytes);
        }
        ByteUtils.setArguments(buffer, data.getData());
        buffer.flip();
        Command command = new Command(key, cmdType, latch);
        command.setIoBuffer(buffer);
        return command;
    }

    /**
     *创建get,gets命令
     *
     * @param key
     * @param keyBytes
     * @param cmdBytes
     *            命令的字节数组，如"get".getBytes()
     * @param cmdType
     *            命令类型
     * @return
     */
    public static final Command createGetCommand(final String key, final byte[] keyBytes,
            final byte[] cmdBytes, final Command.CommandType cmdType) {
        final CountDownLatch latch = new CountDownLatch(1);
        final IoBuffer buffer = bufferAllocator.allocate(cmdBytes.length + ByteUtils.CRLF.length + 1 + keyBytes.length);
        ByteUtils.setArguments(buffer, cmdBytes, keyBytes);
        buffer.flip();
        Command command = new Command(key, cmdType, latch);
        command.setIoBuffer(buffer);
        return command;
    }

    /**
     * 创建批量获取 command
     *
     * @param <T>
     * @param keys
     * @param latch
     * @param result
     * @param cmdBytes
     * @param cmdType
     * @param transcoder
     * @return
     */
    public static final <T> Command createGetMultiCommand(List<String> keys,
            CountDownLatch latch, Map<String, T> result, byte[] cmdBytes,
            Command.CommandType cmdType, Transcoder<T> transcoder) {
        final Command command = new Command(keys.get(0), cmdType, latch);
        command.setResult(result); // 共用一个result map
        command.setTranscoder(transcoder);
        StringBuilder sb = new StringBuilder(keys.size() * 5);
        for (String tmpKey : keys) {
            ByteUtils.checkKey(tmpKey);
            sb.append(tmpKey).append(" ");
        }
        byte[] keyBytes = ByteUtils.getBytes(sb.toString());
        final IoBuffer buffer = bufferAllocator.allocate(cmdBytes.length + ByteUtils.CRLF.length + 1 + keyBytes.length);
        ByteUtils.setArguments(buffer, cmdBytes, keyBytes);
        buffer.flip();
        command.setIoBuffer(buffer);
        return command;
    }

    public static final Command createIncrDecrCommand(final String key,final byte[] keyBytes,
            final int num, Command.CommandType cmdType, final String cmd) {
        final CountDownLatch latch = new CountDownLatch(1);
        byte[] numBytes = ByteUtils.getBytes(String.valueOf(num));
        byte[] cmdBytes = ByteUtils.getBytes(cmd);
        final IoBuffer buffer = bufferAllocator.allocate(cmd.length() + 2 + key.length() + numBytes.length + ByteUtils.CRLF.length);
        ByteUtils.setArguments(buffer, cmdBytes, keyBytes, numBytes);
        buffer.flip();
        Command command = new Command(key, cmdType, latch);
        command.setIoBuffer(buffer);
        return command;
    }
}
