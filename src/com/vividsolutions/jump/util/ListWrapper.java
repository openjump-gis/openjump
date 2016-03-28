package com.vividsolutions.jump.util;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

public abstract class ListWrapper<T> extends CollectionWrapper<T> implements List<T> {

	public List<T> getList() {
		return (List<T>) getCollection();
	}

	public T get(int index) {
		return getList().get(index);
	}

	public T remove(int index) {
		return getList().remove(index);
	}

	public void add(int index, T element) {
		getList().add(index, element);
	}

	public int indexOf(Object o) {
		return getList().indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return getList().lastIndexOf(o);
	}

	public boolean addAll(int index, Collection<? extends T> c) {
		return getList().addAll(index, c);
	}

	public List<T> subList(int fromIndex, int toIndex) {
		return getList().subList(fromIndex, toIndex);
	}

	public ListIterator<T> listIterator() {
		return getList().listIterator();
	}

	public ListIterator<T> listIterator(int index) {
		return getList().listIterator(index);
	}

	public T set(int index, T element) {
		return getList().set(index, element);
	}
}