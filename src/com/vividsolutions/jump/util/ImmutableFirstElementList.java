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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Can't add, replace, or remove the first element in the list.
 */
public class ImmutableFirstElementList<T> implements List<T> {

	private List<T> list = new ArrayList<T>();
	
	public ImmutableFirstElementList(T firstElement) {
		list.add(firstElement);
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

    public Iterator<T> iterator() {
    	//Prevent Iterator#remove. [Jon Aquino]
        return Collections.unmodifiableList(list).iterator();
    }

    public Object[] toArray() {
        return list.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return list.toArray(a);
    }

    public boolean add(T o) {
        return list.add(o);
    }

    public boolean remove(Object o) {
    	//An element equal to the first element may exist later in the list. [Jon Aquino] 
		return list.subList(1, list.size()).remove(o);
    }

    public boolean containsAll(Collection c) {
        return list.containsAll(c);
    }

    public boolean addAll(Collection<? extends T> c) {
        return list.addAll(c);
    }

    public boolean addAll(int index, Collection c) {
		return list.addAll(index == 0 ? 1 : index, c);
    }

    public boolean removeAll(Collection<?> c) {
		return list.subList(1, list.size()).remove(c);
    }

    public boolean retainAll(Collection<?> c) {
		return list.subList(1, list.size()).retainAll(c);
    }

    public void clear() {
		list.subList(1, list.size()).clear();
    }

    public T get(int index) {
        return list.get(index);
    }

    public T set(int index, T element) {
        if (index == 0) {
        	return get(0);
        }
        return list.set(index, element);
    }

    public void add(int index, T element) {
		list.add(index == 0 ? 1 : index, element);
    }

    public T remove(int index) {
        if (index == 0) { return get(0);
        }
        return list.remove(index);
    }

    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    public ListIterator listIterator() {
		//Prevent Iterator#remove. [Jon Aquino]
		return Collections.unmodifiableList(list).listIterator();    	
    }

    public ListIterator listIterator(int index) {
		//Prevent Iterator#remove. [Jon Aquino]
		return Collections.unmodifiableList(list).listIterator(index);    	
    }

    public List subList(int fromIndex, int toIndex) {
    	if (fromIndex > 0) {
    		return list.subList(fromIndex, toIndex);
    	}
    	//A bit heavy-handed, but we don't want the first element to be
    	//modified, moved, or removed. Future: allow the other elements
    	//to be modified, moved, or removed. [Jon Aquino]
		return Collections.unmodifiableList(list).subList(fromIndex, toIndex);    	    	
    }

}
