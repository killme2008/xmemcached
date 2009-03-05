package net.rubyeye.xmemcached.test;

import java.io.Serializable;

/**
 * 
 * @author dennis
 * 
 */
public class NameClass implements Serializable {
	String firstName;
	String lastName;

	public NameClass(String firstName, String lastName) {
		super();
		this.firstName = firstName;
		this.lastName = lastName;
	}

}
