package net.rubyeye.xmemcached.transcoders;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Transcoder that serializes and unserializes list type.
 */
public class ListTranscoder extends BaseSerializingTranscoder implements
		Transcoder<Object> {
	private static final int flags = SerializingTranscoder.SPECIAL_LIST;
	private final TranscoderUtils tu = new TranscoderUtils(true);

	@Override
	public Object decode(CachedData d) {
		if (d.getFlag() == flags) {
			List result = new ArrayList();
			byte[] data = d.getData();
			if (data.length >= 4) {
				ByteBuffer buffer = ByteBuffer.wrap(data);
				while (buffer.remaining() >= 4) {
					int elementLength = buffer.getInt();
					byte[] elementData = new byte[elementLength];
					buffer.get(elementData);
					Object element = deserialize(elementData);
					if (element != null) {
						result.add(element);
					}
				}
			}
			return result;
		} else {
			return null;
		}
	}

	@Override
	public CachedData encode(Object o) {
		ListElement listElement = (ListElement) o;
		Object element = listElement.element;
		byte data[] = serialize(o);
		return new CachedData(flags, data);
	}

}
