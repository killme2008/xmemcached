package net.rubyeye.xmemcached.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.google.code.yanf4j.core.Session;

import net.rubyeye.xmemcached.MemcachedSessionLocator;

/**
 * A random session locator,it can be used in kestrel.
 * 
 * @author dennis<killme2008@gmail.com>
 * 
 */
public class RandomMemcachedSessionLocaltor implements MemcachedSessionLocator {
	private transient volatile List<Session> sessions = Collections.emptyList();
	private final Random rand = new Random();

	public Session getSessionByKey(String key) {
		List<Session> copiedOnWrite = sessions;
		if (copiedOnWrite == null || copiedOnWrite.isEmpty())
			return null;
		return copiedOnWrite.get(rand.nextInt(copiedOnWrite.size()));
	}

	public void updateSessions(Collection<Session> list) {
		this.sessions = new ArrayList<Session>(list);

	}

	public void setFailureMode(boolean failureMode) {
		// ignore
	}

}
