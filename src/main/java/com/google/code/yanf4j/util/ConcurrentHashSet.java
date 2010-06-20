package com.google.code.yanf4j.util;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A {@link ConcurrentHashMap}-backed {@link Set}.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 597692 $, $Date: 2007-11-23 08:56:32 -0700 (Fri, 23 Nov 2007) $
 */
public class ConcurrentHashSet<E> extends MapBackedSet<E> {

	private static final long serialVersionUID = 8518578988740277828L;

	public ConcurrentHashSet() {
		super(new ConcurrentHashMap<E, Boolean>());
	}

	public ConcurrentHashSet(Collection<E> c) {
		super(new ConcurrentHashMap<E, Boolean>(), c);
	}

	@Override
	public boolean add(E o) {
		Boolean answer = ((ConcurrentMap<E, Boolean>) map).putIfAbsent(o,
				Boolean.TRUE);
		return answer == null;
	}
}
