package com.google.code.yanf4j.test.unittest.core.impl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.code.yanf4j.buffer.IoBuffer;
import com.google.code.yanf4j.core.CodecFactory.Encoder;
import com.google.code.yanf4j.core.impl.ByteBufferCodecFactory;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-24 ÉÏÎç10:49:54
 */

public class ByteBufferCodecFactoryUnitTest {
    ByteBufferCodecFactory codecFactory;


    @Before
    public void setUp() {
        this.codecFactory = new ByteBufferCodecFactory();
    }


    @Test
    public void testEncodeNormal() throws Exception {
        Encoder encoder = this.codecFactory.getEncoder();
        Assert.assertNotNull(encoder);
        IoBuffer buffer = encoder.encode(IoBuffer.wrap("hello".getBytes("utf-8")), null);
        Assert.assertNotNull(buffer);
        Assert.assertTrue(buffer.hasRemaining());
        Assert.assertArrayEquals("hello".getBytes("utf-8"), buffer.array());

    }


    @Test
    public void testEncodeEmpty() throws Exception {
        Encoder encoder = this.codecFactory.getEncoder();
        Assert.assertNull(encoder.encode(null, null));
        Assert.assertEquals(IoBuffer.allocate(0), encoder.encode(IoBuffer.allocate(0), null));
    }


    @Test
    public void decodeNormal() throws Exception {
        Encoder encoder = this.codecFactory.getEncoder();
        Assert.assertNotNull(encoder);
        IoBuffer buffer = encoder.encode(IoBuffer.wrap("hello".getBytes("utf-8")), null);

        IoBuffer decodeBuffer = (IoBuffer) this.codecFactory.getDecoder().decode(buffer, null);
        Assert.assertEquals(IoBuffer.wrap("hello".getBytes("utf-8")), decodeBuffer);
    }


    @Test
    public void decodeEmpty() throws Exception {
        Assert.assertNull(this.codecFactory.getDecoder().decode(null, null));
        Assert.assertEquals(IoBuffer.allocate(0), this.codecFactory.getDecoder().decode(IoBuffer.allocate(0), null));
    }


    @Test
    public void testDirectEncoder() throws Exception {
        this.codecFactory = new ByteBufferCodecFactory(true);
        IoBuffer msg = IoBuffer.allocate(100);
        IoBuffer buffer = this.codecFactory.getEncoder().encode(msg, null);
        Assert.assertTrue(buffer.isDirect());
    }

}
