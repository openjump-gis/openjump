package com.vividsolutions.jump.util;

import java.util.Collection;
import java.util.Iterator;

public abstract class CollectionWrapper implements Collection {
	public abstract Collection getCollection();

	public int size() {
		return getCollection().size();
	}

	public void clear() {
		getCollection().clear();
	}

	public boolean isEmpty() {
		return getCollection().isEmpty();
	}

	public Object[] toArray() {
		return getCollection().toArray();
	}

	public boolean add(Object o) {
		return getCollection().add(o);
	}

	public boolean contains(Object o) {
		return getCollection().contains(o);
	}

	public boolean remove(Object o) {
		return getCollection().remove(o);
	}

	public boolean addAll(Collection c) {
		return getCollection().addAll(c);
	}

	public boolean containsAll(Collection c) {
		return getCollection().containsAll(c);
	}

	public boolean removeAll(Collection c) {
		return getCollection().removeAll(c);
	}

	public boolean retainAll(Collection c) {
		return getCollection().retainAll(c);
	}

	public Iterator iterator() {
		return getCollection().iterator();
	}

	public Object[] toArray(Object[] a) {
		return getCollection().toArray(a);
	}
}