package com.vividsolutions.jump.util;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

public abstract class ListWrapper extends CollectionWrapper implements List {
	public List getList() {
		return (List) getCollection();
	}

	public Object get(int index) {
		return getList().get(index);
	}

	public Object remove(int index) {
		return getList().remove(index);
	}

	public void add(int index, Object element) {
		getList().add(index, element);
	}

	public int indexOf(Object o) {
		return getList().indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return getList().lastIndexOf(o);
	}

	public boolean addAll(int index, Collection c) {
		return getList().addAll(index, c);
	}

	public List subList(int fromIndex, int toIndex) {
		return getList().subList(fromIndex, toIndex);
	}

	public ListIterator listIterator() {
		return getList().listIterator();
	}

	public ListIterator listIterator(int index) {
		return getList().listIterator(index);
	}

	public Object set(int index, Object element) {
		return getList().set(index, element);
	}
}