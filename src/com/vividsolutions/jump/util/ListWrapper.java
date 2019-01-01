package com.vividsolutions.jump.util;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

public abstract class ListWrapper<E> extends CollectionWrapper<E> implements List<E> {

	public List<E> getList() {
		return (List<E>) getCollection();
	}

	public E get(int index) {
		return getList().get(index);
	}

	public E remove(int index) {
		return getList().remove(index);
	}

	public void add(int index, E element) {
		getList().add(index, element);
	}

	public int indexOf(Object o) {
		return getList().indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return getList().lastIndexOf(o);
	}

	public boolean addAll(int index, Collection<? extends E> c) {
		return getList().addAll(index, c);
	}

	public List<E> subList(int fromIndex, int toIndex) {
		return getList().subList(fromIndex, toIndex);
	}

	public ListIterator<E> listIterator() {
		return getList().listIterator();
	}

	public ListIterator<E> listIterator(int index) {
		return getList().listIterator(index);
	}

	public E set(int index, E element) {
		return getList().set(index, element);
	}
}