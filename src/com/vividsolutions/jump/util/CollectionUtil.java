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

import com.vividsolutions.jts.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;


public class CollectionUtil {

    public CollectionUtil() {
    }

    public static <T> Collection<T> concatenate(Collection<T> a, Collection<T> b) {
        List<T> result = new ArrayList<>();
        result.addAll(a);
        result.addAll(b);
        return result;
    }

    public static <T> List<T> list(T a, T b) {
        List<T> list = new ArrayList<>();
        list.add(a);
        list.add(b);
        return list;
    }       
    
    /**
     * Returns a List of Lists: all combinations of the elements of the given List.
     * @param maxCombinationSize combinations larger than this value are discarded
     */
    public static List combinations(List original, int maxCombinationSize) {
        return combinations(original, maxCombinationSize, null);
    }

    public static <U,V> Map<V,U> inverse(Map<U,V> map) {
        Map<V,U> inverse;
        try {
            inverse = map.getClass().newInstance();
        } catch (InstantiationException|IllegalAccessException e) {
            Assert.shouldNeverReachHere(e.toString());
            return null;
        }
        for (Map.Entry<U,V> entry : map.entrySet()) {
            inverse.put(entry.getValue(), entry.getKey());
        }
        return inverse;
    }

    /**
     * Returns a List of Lists: all combinations of the elements of the given List.
     * @param maxCombinationSize combinations larger than this value are discarded
     * @param mandatoryItem an item that all returned combinations must contain,
     * or null to leave unspecified
     */
    public static <T> List<List<T>> combinations(List<T> original,
                int maxCombinationSize, T mandatoryItem) {

        List<List<T>> combinations = new ArrayList<>();

        //Combinations are given by the bits of each binary number from 1 to 2^N
        for (int i = 1; i <= ((int) Math.pow(2, original.size()) - 1); i++) {
            List<T> combination = new ArrayList<>();
            for (int j = 0; j < original.size(); j++) {
                if ((i & (int) Math.pow(2, j)) > 0) {
                    combination.add(original.get(j));
                }
            }
            if (combination.size() > maxCombinationSize) {
                continue;
            }
            if ((mandatoryItem != null) && !combination.contains(mandatoryItem)) {
                continue;
            }
            combinations.add(combination);
        }

        return combinations;
    }

    /**
     * Returns a List of Lists: all combinations of the elements of the given List.
     */
    public static <T> List<List<T>> combinations(List<T> original) {
        return combinations(original, original.size(), null);
    }

    public static <U,V> void removeKeys(Collection<U> keys, Map<U,V> map) {
        for (U key : keys) {
            map.remove(key);
        }
    }

    /**
     * The nth key corresponds to the nth value
     */
    public static List[] keysAndCorrespondingValues(Map map) {
        List keys = new ArrayList(map.keySet());
        List values = new ArrayList();
        for (Object key : keys) {
            values.add(map.get(key));
        }

        return new List[] { keys, values };
    }

    public static <T> Collection<T> concatenate(Collection<Collection<T>> collections) {
        List<T> concatenation = new ArrayList<>();
        for (Collection<T> collection : collections) {
            concatenation.addAll(collection);
        }
        return concatenation;
    }

    public static Object randomElement(List list) {
        return list.get((int) Math.floor(Math.random() * list.size()));
    }

    public static SortedSet<Integer> reverseSortedSet(int[] ints) {
        TreeSet<Integer> sortedSet = new TreeSet<>(Collections.reverseOrder());
        for (int i : ints) {
            sortedSet.add(i);
        }

        return sortedSet;
    }

    public static List reverse(List list) {
        Collections.reverse(list);

        return list;
    }

    /**
     * Data is evenly discarded or duplicated to attain the new size
     */
    public static <T> Collection stretch(Collection<T> source,
                Collection<T> destination, int destinationSize) {
        Assert.isTrue(destination.isEmpty());

        List<T> originalList = source instanceof List ?
                (List<T>) source : new ArrayList<T>(source);
        for (int i = 0; i < destinationSize; i++) {
            destination.add(originalList.get(
                    (int) Math.round(
                        i * originalList.size() / (double) destinationSize)));
        }

        return destination;
    }

    public static Object ifNotIn(Object o, Collection c, Object alternative) {
        return c.contains(o) ? o : alternative;
    }

    public static void setIfNull(int i, List<String> list, String value) {
        if (i >= list.size()) {
            resize(list, i + 1);
        }
        if (list.get(i) != null) {
            return;
        }
        list.set(i, value);
    }

    public static void resize(List list, int newSize) {
        if (newSize < list.size()) {
            list.subList(newSize, list.size()).clear();
        } else {
            list.addAll(Collections.nCopies(newSize - list.size(), null));
        }
    }

    public static boolean containsReference(Object[] objects, Object o) {
        return indexOf(o, objects) > -1;
    }

    public static int indexOf(Object o, Object[] objects) {
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == o) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Brute force, for when HashSet and TreeSet won't work (e.g. #hashCode
     * implementation isn't appropriate). The original Collection is not modified.
     */
    public static <T> Collection<T> removeDuplicates(Collection<T> original) {
        List<T> result = new ArrayList<>();
        for (T item : original) {
            if (!result.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }

    public static void addIfNotNull(Object item, Collection collection) {
        if (item != null) {
            collection.add(item);
        }
    }

    /**
     * Modifies and returns the collection.
     */
    public static Collection filterByClass(Collection collection, Class c) {
        for (Iterator i = collection.iterator(); i.hasNext();) {
            Object item = i.next();
            if (!c.isInstance(item)) {
                i.remove();
            }
        }

        return collection;
    }

    public static Map createMap(Object[] alternatingKeysAndValues) {
        return createMap(HashMap.class, alternatingKeysAndValues);
    }

    public static Map<Object,Object> createMap(Class mapClass,
        Object[] alternatingKeysAndValues) {
        Map<Object,Object> map = null;
        try {
            map = (Map<Object,Object>) mapClass.newInstance();
            for (int i = 0; i < alternatingKeysAndValues.length; i += 2) {
                map.put(alternatingKeysAndValues[i], alternatingKeysAndValues[i + 1]);
            }
        } catch (Exception e) {
            Assert.shouldNeverReachHere(e.toString());
        }
        return map;
    }

    /**
     * The Smalltalk #collect method.
     */
    public static <T> Collection<T> collect(Collection<T> collection, Block block) {
        ArrayList<T> result = new ArrayList<>();
        for (Object object : collection) {
            result.add((T)block.yield(object));
        }

        return result;
    }

    /**
     * The Smalltalk #select method.
     */
    public static Collection select(Collection collection, Block block) {
        List<Object> result = new ArrayList<>();
        for (Object item : collection) {
            if (Boolean.TRUE.equals(block.yield(item))) {
                result.add(item);
            }
        }
        return result;
    }

    public static Object get(Class c, Map<Class,Object> map) {
        if (map.keySet().contains(c)) {
            return map.get(c);
        }
        for (Class<?> clazz : map.keySet()) {
            if (clazz.isAssignableFrom(c)) {
                return map.get(clazz);
            }
        }

        return null;
    }
}
