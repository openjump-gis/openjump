package com.vividsolutions.jump.util;

import java.util.Collection;
import java.util.List;

public class LazyList extends ListWrapper {
	private Block collectionFactory;

	private List list;

	public LazyList(Block collectionFactory) {
		this.collectionFactory = collectionFactory;
	}

	public Collection getCollection() {
		if (list == null) {
			list = (List) collectionFactory.yield();
		}
		return list;
	}
}