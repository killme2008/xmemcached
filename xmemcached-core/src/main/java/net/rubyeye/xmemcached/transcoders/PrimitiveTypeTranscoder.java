package net.rubyeye.xmemcached.transcoders;

public abstract class PrimitiveTypeTranscoder<T> extends
		BaseSerializingTranscoder implements Transcoder<T> {
	protected final TranscoderUtils tu = new TranscoderUtils(true);

	protected boolean primitiveAsString;

	public void setPackZeros(boolean packZeros) {
		this.tu.setPackZeros(packZeros);

	}

	public boolean isPackZeros() {
		return this.tu.isPackZeros();
	}

	public boolean isPrimitiveAsString() {
		return this.primitiveAsString;
	}

	public void setPrimitiveAsString(boolean primitiveAsString) {
		this.primitiveAsString = primitiveAsString;

	}

}
