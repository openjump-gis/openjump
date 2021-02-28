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

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.locationtech.jts.util.Assert;


/**
 *  A Map whose values are Collections.
 */
public class CollectionMap<K,V> implements Map<K,Collection<V>> {

  private Map<K,Collection<V>> map;
  private Class<? extends Collection> collectionClass = ArrayList.class;

  /**
   * Creates a CollectionMap backed by the given Map class.
   * @param mapClass a Class that implements Map
   */
  public CollectionMap(Class<? extends Map> mapClass) {
    try {
      map = mapClass.getDeclaredConstructor().newInstance();
    } catch (InstantiationException|IllegalAccessException|NoSuchMethodException|InvocationTargetException e) {
      Assert.shouldNeverReachHere();
    }
  }

  public CollectionMap(Class<? extends Map> mapClass, Class<Collection> collectionClass) {
    this.collectionClass = collectionClass;
    try {
      map = mapClass.getDeclaredConstructor().newInstance();
    } catch (InstantiationException|IllegalAccessException|NoSuchMethodException|InvocationTargetException e) {
      Assert.shouldNeverReachHere();
    }
  }

  /**
   * Creates a CollectionMap.
   */
  public CollectionMap() {
    this(HashMap.class);
  }

  private Collection<V> getItemsInternal(K key) {
    Collection<V> collection = map.get(key);
    if (collection == null) {
      try {
        collection = collectionClass.getDeclaredConstructor().newInstance();
      } catch (InstantiationException|IllegalAccessException|NoSuchMethodException|InvocationTargetException e) {
        e.printStackTrace();
        Assert.shouldNeverReachHere();
      }
      map.put(key, collection);
    }
    return collection;
  }

  /**
   * Adds the item to the Collection at the given key, creating a new Collection if
   * necessary.
   * @param key the key to the Collection to which the item should be added
   * @param item the item to add
   */
  public void addItem(K key, V item) {
    getItemsInternal(key).add(item);
  }

  public void removeItem(K key, V item) {
    getItemsInternal(key).remove(item);
  }

  @Override
  public void clear() {
    map.clear();
  }

  /**
   * Adds the items to the Collection at the given key, creating a new Collection if
   * necessary.
   * @param key the key to the Collection to which the items should be added
   * @param items the items to add
   */
  public void addItems(K key, Collection<V> items) {
    for (V item : items) {
      addItem(key, item);
    }
  }

  public void addItems(CollectionMap<K,V> other) {
    for (K key : other.keySet()) {
      addItems(key, other.getItems(key));
    }
  }

  /**
   * Returns the values.
   * @return a view of the values, backed by this CollectionMap
   */
  @Override
  public Collection<Collection<V>> values() {
    return map.values();
  }

  /**
   * Returns the keys.
   * @return a view of the keys, backed by this CollectionMap
   */
  @Override
  public Set<K> keySet() {
    return map.keySet();
  }

  /**
   * Returns the number of mappings.
   * @return the number of key-value pairs
   */
  @Override
  public int size() {
    return map.size();
  }

  @Override
  public Collection<V> get(Object key) {
    try {
      return getItems((K)key);
    } catch(ClassCastException e) {
      return null;
    }
  }

  public Collection<V> getItems(K key) {
    return Collections.unmodifiableCollection(getItemsInternal(key));
  }

  @Override
  public Collection<V> remove(Object key) {
    return map.remove(key);
  }

  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  @Override
  public Set<Entry<K, Collection<V>>> entrySet() {
    return map.entrySet();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public Collection<V> put(K key, Collection<V> value) {
    return map.put(key, value);
  }

  @Override
  public void putAll(Map<? extends K, ? extends Collection<V>> map) {
    for (K key : map.keySet()) {
      //Delegate to #put so that the assertion is made. [Jon Aquino]
      put(key, map.get(key));
    }
  }

  public void removeItems(K key, Collection<V> items) {
    getItemsInternal(key).removeAll(items);
  }

  public Map<K,? extends Collection<V>> getMap() {
    return map;
  }

}
