package com.vividsolutions.jump.util;

import java.util.Collection;
import java.util.List;

public class LazyList<E> extends ListWrapper<E> {
	private Block collectionFactory;

	private List<E> list;

	public LazyList(Block collectionFactory) {
		this.collectionFactory = collectionFactory;
	}

	public Collection<E> getCollection() {
		if (list == null) {
			list = (List<E>) collectionFactory.yield();
		}
		return list;
	}
}