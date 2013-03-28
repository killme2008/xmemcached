package net.rubyeye.xmemcached.test.unittest.transcoder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.TokyoTyrantTranscoder;
import junit.framework.TestCase;

public class TokyoTyrantTranscoderUnitTest extends TestCase {
	TokyoTyrantTranscoder tokyoTyrantTranscoder;

	public void setUp() {
		tokyoTyrantTranscoder = new TokyoTyrantTranscoder(5*1024*1024);

	}

	static class Person implements Serializable {
		private String name;
		private int age;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + age;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Person other = (Person) obj;
			if (age != other.age)
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

	}
	
	public void testEncodeDecodeLargeValue(){
		List<Long> list=new ArrayList<Long>(1000000);
		for(long i=0;i<1000000;i++){
			list.add(i);
		}
		CachedData cachedData = tokyoTyrantTranscoder.encode(list);
		List<Long> decodeList = (List<Long>) tokyoTyrantTranscoder.decode(cachedData);
		assertNotSame(list, decodeList);
		assertEquals(list, decodeList);
		
		List<Long> decodeList2 = (List<Long>) tokyoTyrantTranscoder.decode(cachedData);
		assertNotSame(list, decodeList2);
		assertNotSame(decodeList2, decodeList);
		assertEquals(list, decodeList2);
	}

	public void testDecodeManayTimes() {
		Person p = new Person();
		p.name = "xmc";
		p.age = 1;
		CachedData cachedData = tokyoTyrantTranscoder.encode(p);
		Person decodePerson = (Person) tokyoTyrantTranscoder.decode(cachedData);
		assertNotSame(p, decodePerson);
		assertEquals(p, decodePerson);
		
		Person decodePerson2 = (Person) tokyoTyrantTranscoder.decode(cachedData);
		assertNotSame(p, decodePerson2);
		assertNotSame(decodePerson, decodePerson2);
		assertEquals(p, decodePerson2);

	}

	public void testEncodeDecode() {
		// simple type
		CachedData cachedData = tokyoTyrantTranscoder.encode(1);
		assertEquals(8, cachedData.getData().length);
		assertEquals(1, tokyoTyrantTranscoder.decode(cachedData));

		long currentTimeMillis = System.currentTimeMillis();
		cachedData = tokyoTyrantTranscoder.encode(currentTimeMillis);
		assertEquals(12, cachedData.getData().length);
		assertEquals(currentTimeMillis, tokyoTyrantTranscoder
				.decode(cachedData));

		cachedData = tokyoTyrantTranscoder.encode("hello");
		assertEquals(9, cachedData.getData().length);
		assertEquals("hello", tokyoTyrantTranscoder.decode(cachedData));

		cachedData = tokyoTyrantTranscoder.encode(2.3d);
		assertEquals(12, cachedData.getData().length);
		assertEquals(2.3d, tokyoTyrantTranscoder.decode(cachedData));

		// collection
		List<String> list = new ArrayList<String>();
		list.add("1");
		cachedData = tokyoTyrantTranscoder.encode(list);
		int oldLength = cachedData.getData().length;
		List<String> decodedList = (List) tokyoTyrantTranscoder
				.decode(cachedData);
		assertEquals(1, decodedList.size());
		assertTrue(decodedList.contains("1"));

		// compress
		tokyoTyrantTranscoder.setCompressionThreshold(1);
		cachedData = tokyoTyrantTranscoder.encode(list);
		decodedList = (List) tokyoTyrantTranscoder.decode(cachedData);
		assertEquals(1, decodedList.size());
		assertNotSame(decodedList, list);
		assertTrue(decodedList.contains("1"));

		tokyoTyrantTranscoder.setCompressionThreshold(16 * 1024);
		// serialize type

		Person p = new Person();
		p.name = "xmc";
		p.age = 1;
		cachedData = tokyoTyrantTranscoder.encode(p);

		Person decodePerson = (Person) tokyoTyrantTranscoder.decode(cachedData);

		assertNotSame(p, decodePerson);

		assertEquals(p, decodePerson);

	}
}
