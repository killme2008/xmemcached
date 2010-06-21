package com.google.code.yanf4j.core;

/**
 * Socket option
 * 
 * @author dennis
 * 
 * @param <T>
 */
public class SocketOption<T> {

	private final String name;
	private final Class<T> type;

	public SocketOption(String name, Class<T> type) {
		this.name = name;
		this.type = type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (this.name == null ? 0 : this.name.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SocketOption other = (SocketOption) obj;
		if (this.name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!this.name.equals(other.name)) {
			return false;
		}
		return true;
	}

	public String name() {
		return this.name;
	}

	public Class<T> type() {
		return this.type;
	}

	@Override
	public String toString() {
		return this.name;
	}
}
