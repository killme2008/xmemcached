package net.rubyeye.xmemcached.test.legacy;

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

	public static final long serialVersionUID = -5404950940509405049l;

}
