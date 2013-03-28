package net.rubyeye.xmemcached.helper;

public class TranscoderChecker extends AbstractChecker {
	private MockTranscoder mockTranscoder;
	private int expectCount;

	public TranscoderChecker(MockTranscoder mockTranscoder,int expectedCount) {
		super();
		this.mockTranscoder = mockTranscoder;
		this.expectCount=expectedCount;
	}

	public void check() throws Exception {
		call();
		assertEquals(expectCount, mockTranscoder.getCount());
	}

}
