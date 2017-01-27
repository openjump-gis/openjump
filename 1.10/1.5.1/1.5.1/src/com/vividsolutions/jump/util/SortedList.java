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

public class SortedList implements List {

    private List list;

    /**
     * Creates a UniqueList.
     */
    public SortedList() {
        this(new ArrayList());
    }

    /**
     * Creates a SortedList backed by the given List.
     * @param list a List that will be this SortedList's underlying List
     */
    public SortedList(List list) {
        this.list = list;
    }
    
    private void sort() {
    	Collections.sort(list);
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

    public Iterator iterator() {
        return list.iterator();
    }

    public Object[] toArray() {
        return list.toArray();
    }

    public Object[] toArray(Object[] a) {
        return list.toArray(a);
    }

    public boolean add(Object o) {
        try {
            return list.add(o);
        } finally {
            sort();
        }
    }

    public boolean remove(Object o) {
        try {
            return list.remove(o);
        } finally {
            sort();
        }
    }

    public boolean containsAll(Collection c) {
        return list.containsAll(c);
    }

    public boolean addAll(Collection c) {
        try {
            return list.addAll(c);
        } finally {
            sort();
        }
    }

    public boolean addAll(int index, Collection c) {
        try {
            return list.addAll(index, c);
        } finally {
            sort();
        }
    }

    public boolean removeAll(Collection c) {
        try {
            return list.removeAll(c);
        } finally {
            sort();
        }
    }

    public boolean retainAll(Collection c) {
        try {
            return list.retainAll(c);
        } finally {
            sort();
        }
    }

    public void clear() {
        list.clear();
    }

    public Object get(int index) {
        return list.get(index);
    }

    public Object set(int index, Object element) {
        try {
            return list.set(index, element);
        } finally {
            sort();
        }
    }

    public void add(int index, Object element) {
        try {
            list.add(index, element);
        } finally {
            sort();
        }
    }

    public Object remove(int index) {
        try {
            return list.remove(index);
        } finally {
            sort();
        }
    }

    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    public ListIterator listIterator() {
        return list.listIterator();
    }

    public ListIterator listIterator(int index) {
        return list.listIterator(index);
    }

    public List subList(int fromIndex, int toIndex) {
        //Not yet supported because we would need to track changes to the
        //sublist because those changes modify the underlying list. [Jon Aquino]
        throw new UnsupportedOperationException();
    }

}
