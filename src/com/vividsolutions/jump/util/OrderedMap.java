
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
 * A Map that preserves the order of its keys.
 * @deprecated use LinkedHashMap to have predictable order and TreeMap
 * to have a sorted map.
 */
@Deprecated
public class OrderedMap<U,V> implements Map<U,V> {

    private Map<U,V> map;
    private List<U> keyList;

    /**
     * Creates an OrderedMap backed by the given map.
     * @param map a Map that will be this OrderedMap's underlying Map
     */
    public OrderedMap(List<U> keyList, Map<U,V> map) {
    	this.keyList = keyList;
        this.map = map;
    }

    /**
     * Creates an OrderedMap.
     */
    public OrderedMap() {
        this(new HashMap<U,V>());
    }
    
    public OrderedMap(Map<U,V> map) {
    	this(new UniqueList<U>(), map);
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    public V get(Object key) {
        return map.get(key);
    }

    public V put(U key, V value) {
        keyList.add(key);
        return map.put(key, value);
    }

    public V remove(Object key) {
        keyList.remove(key);
        return map.remove(key);
    }

    public void putAll(Map<? extends U, ? extends V> t) {
        keyList.addAll(t.keySet());
        map.putAll(t);
    }

    public void clear() {
        keyList.clear();
        map.clear();
    }

    public Set<U> keySet() {
        return map.keySet();
    }

    /**
     * Returns the keys, in order.
     * @return the keys in the order they were (first) added
     */
    public List<U> keyList() {
        return keyList;
    }

    /**
     * Returns the values.
     * @return the values in the same order as the keys
     * @see #keyList()
     */
    public List<V> valueList() {
        return (List<V>) values();
    }

    /**
     * Returns the values.
     * @return the values in the same order as the keys
     * @see #keyList()
     */
    public Collection<V> values() {
        List<V> values = new ArrayList<>();

        for (U key : keyList) {
            values.add(map.get(key));
        }

        return values;
    }

    public Set<Map.Entry<U,V>> entrySet() {
        return map.entrySet();
    }

    public boolean equals(Object o) {
        return (o instanceof OrderedMap) && map.equals(o);
    }
}
