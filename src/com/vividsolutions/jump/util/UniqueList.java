
/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */

package com.vividsolutions.jump.util;

import java.util.*;


/**
 * A List that ignores duplicates. Note: performance is not optimized - a simple linear
 * search is performed.
 */
public class UniqueList<E> implements List<E> {
    private List<E> list;

    /**
     * Creates a UniqueList.
     */
    public UniqueList() {
        this(new ArrayList<E>());
    }

    /**
     * Creates a UniqueList backed by the given List.
     * @param list a List that will be this UniqueList's underlying List
     */
    public UniqueList(List<E> list) {
        this.list = list;
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public boolean contains(Object o) {
        return list.contains(o);
    }

    public Iterator<E> iterator() {
        return list.iterator();
    }

    public Object[] toArray() {
        return list.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return list.toArray(a);
    }

    public boolean add(E o) {
        if (list.contains(o)) {
            return false;
        }

        return list.add(o);
    }

    public boolean remove(Object o) {
        return list.remove(o);
    }

    public boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    public boolean addAll(Collection<? extends E> c) {
    	return addAll(size(), c);
    }

    public boolean addAll(int index, Collection<? extends E> c) {
		List<E> itemsToAdd = new ArrayList<>(c);
		itemsToAdd.removeAll(this);
		return list.addAll(index, itemsToAdd);    	
    }

    public boolean removeAll(Collection<?> c) {
        return list.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return list.retainAll(c);
    }

    public void clear() {
        list.clear();
    }

    public boolean equals(Object o) {
        return o instanceof UniqueList && list.equals(o);
    }

    public E get(int index) {
        return list.get(index);
    }

    public E set(int index, E element) {
        return list.set(index, element);
    }

    public void add(int index, E element) {
        if (list.contains(element)) {
            return;
        }

        list.add(index, element);
    }

    public E remove(int index) {
        return list.remove(index);
    }

    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    public ListIterator<E> listIterator() {
        return list.listIterator();
    }

    public ListIterator<E> listIterator(int index) {
        return list.listIterator(index);
    }

    public List<E> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }
}
