/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package net.rubyeye.xmemcached.transcoders;

import net.rubyeye.xmemcached.exception.MemcachedDecodeException;

/**
 * Transcoder for TokyoTyrant.Add 4-bytes flag before value.
 * 
 * @author boyan
 * 
 */
public class TokyoTyrantTranscoder implements Transcoder<Object> {
	private final SerializingTranscoder serializingTranscoder;

	public TokyoTyrantTranscoder(int maxSize) {
		serializingTranscoder = new SerializingTranscoder(maxSize);
		serializingTranscoder.setPackZeros(false);
	}

	public TokyoTyrantTranscoder() {
		serializingTranscoder = new SerializingTranscoder();
		serializingTranscoder.setPackZeros(false);
	}

	public final Object decode(CachedData d) {
		byte[] compositeData = d.getData();
		if (compositeData.length <= 4)
			throw new MemcachedDecodeException(
					"There are no four bytes before value for TokyoTyrantTranscoder");
		byte[] flagBytes = new byte[4];
		byte[] realData = new byte[compositeData.length - 4];
		System.arraycopy(compositeData, 0, flagBytes, 0, 4);
		System.arraycopy(compositeData, 4, realData, 0,
				compositeData.length - 4);
		int flag = serializingTranscoder.getTranscoderUtils().decodeInt(
				flagBytes);
		d.setFlag(flag);
		if ((flag & SerializingTranscoder.COMPRESSED) != 0) {
			realData = serializingTranscoder.decompress(realData);
		}
		flag = flag & SerializingTranscoder.SPECIAL_MASK;
		return serializingTranscoder.decode0(d, realData, flag);
	}
	
	

	public void setCompressionMode(CompressionMode compressMode) {
		this.serializingTranscoder.setCompressionMode(compressMode);		
	}

	public final CachedData encode(Object o) {
		CachedData result = serializingTranscoder.encode(o);
		byte[] realData = result.getData();
		int flag = result.getFlag();
		byte[] flagBytes = serializingTranscoder.getTranscoderUtils()
				.encodeInt(flag);

		byte[] compisiteData = new byte[4 + realData.length];
		System.arraycopy(flagBytes, 0, compisiteData, 0, 4);
		System.arraycopy(realData, 0, compisiteData, 4, realData.length);

		result.setData(compisiteData);
		return result;
	}

	public final int getMaxSize() {
		return serializingTranscoder.getMaxSize();
	}

	public boolean isPackZeros() {
		return serializingTranscoder.isPackZeros();
	}

	public boolean isPrimitiveAsString() {
		return serializingTranscoder.isPrimitiveAsString();
	}

	public void setCharset(String to) {
		serializingTranscoder.setCharset(to);
	}

	public void setCompressionThreshold(int to) {
		serializingTranscoder.setCompressionThreshold(to);
	}

	public void setPackZeros(boolean packZeros) {
		throw new UnsupportedOperationException(
				"TokyoTyrantTranscoder doesn't support pack zeros");
	}

	public void setPrimitiveAsString(boolean primitiveAsString) {
		throw new UnsupportedOperationException(
				"TokyoTyrantTranscoder doesn't support save primitive type as String");
	}
}
