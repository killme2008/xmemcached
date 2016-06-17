package com.google.code.yanf4j.test.unittest.core.impl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.code.yanf4j.buffer.IoBuffer;
import com.google.code.yanf4j.core.CodecFactory.Encoder;
import com.google.code.yanf4j.core.impl.TextLineCodecFactory;

/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-24 10:33:59
 */

public class TextLineCodecFactoryUnitTest {
	TextLineCodecFactory textLineCodecFactory;

	@Before
	public void setUp() {
		this.textLineCodecFactory = new TextLineCodecFactory();
		TextLineCodecFactory.SPLIT.clear();
	}

	@Test
	public void testEncodeNormal() throws Exception {
		Encoder encoder = this.textLineCodecFactory.getEncoder();
		Assert.assertNotNull(encoder);
		IoBuffer buffer = encoder.encode("hello", null);
		Assert.assertNotNull(buffer);
		Assert.assertTrue(buffer.hasRemaining());
		Assert.assertArrayEquals("hello\r\n".getBytes("utf-8"), buffer.array());

	}

	@Test
	public void testEncodeEmpty() throws Exception {
		Encoder encoder = this.textLineCodecFactory.getEncoder();
		Assert.assertNull(encoder.encode(null, null));
		Assert.assertEquals(TextLineCodecFactory.SPLIT, encoder.encode("", null));
	}

	@Test
	public void decodeNormal() throws Exception {
		Encoder encoder = this.textLineCodecFactory.getEncoder();
		Assert.assertNotNull(encoder);
		IoBuffer buffer = encoder.encode("hello", null);

		String str = (String) this.textLineCodecFactory.getDecoder().decode(buffer, null);
		Assert.assertEquals("hello", str);
	}

	@Test
	public void decodeEmpty() throws Exception {
		Assert.assertNull(this.textLineCodecFactory.getDecoder().decode(null, null));
		Assert.assertEquals("", this.textLineCodecFactory.getDecoder().decode(TextLineCodecFactory.SPLIT, null));
	}

}
