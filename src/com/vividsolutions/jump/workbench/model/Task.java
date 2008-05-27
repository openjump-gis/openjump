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

package com.vividsolutions.jump.workbench.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

/**
 * Whatever the user needs to do a particular task. Currently a thin wrapper
 * around a LayerManager.
 */
// I wonder if this class should be named "Project" instead. [Jon Aquino]
public class Task implements LayerManagerProxy {
  private String name = "";

  private LayerManager layerManager;

  private List<NameListener> nameListeners = new ArrayList<NameListener>();

  private File projectFile = null;

  /** The map of task properties. */
  private Map<QName, Object> properties = new HashMap<QName, Object>();

  // No parameters so it can be created by Java2XML.
  public Task() {
    this.layerManager = new LayerManager(this);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
    fireNameChanged(name);
  }

  public void add(NameListener nameListener) {
    nameListeners.add(nameListener);
  }

  private void fireNameChanged(String name) {
    for (NameListener nameListener : nameListeners) {
      nameListener.taskNameChanged(name);
    }
  }

  public File getProjectFile() {
    return projectFile;
  }

  public void setProjectFile(File projectFile) {
    this.projectFile = projectFile;
  }

  public LayerManager getLayerManager() {
    return layerManager;
  }

  @SuppressWarnings("unchecked")
  public Collection<Category> getCategories() {
    return getLayerManager().getCategories();
  }

  /**
   * Called by Java2XML
   */
  @SuppressWarnings("unchecked")
  public void addCategory(Category category) {
    getLayerManager().addCategory(category.getName());

    Category actual = getLayerManager().getCategory(category.getName());

    for (Layerable layerable : (Collection<Layerable>)category.getLayerables()) {
      actual.addPersistentLayerable(layerable);
    }
  }

  /**
   * Set the value for the named property.
   * 
   * @param name The name of the property.
   * @param value The value for the property.
   */
  public void setProperty(QName name, Object value) {
    properties.put(name, value);
  }

  /**
   * <p>
   * Get the value for the named property casting it to the return value.
   * </p>
   * <p>
   * Instead of:
   * </p>
   * 
   * <pre>
   * Integer i = (Integer)task.getProperty(...)
   * </pre>
   * 
   * <p>
   * You can use the following:
   * </p>
   * 
   * <pre>
   * Integer i = task.getProperty(...)
   * </pre>
   * 
   * @param name The name of the property.
   * @return value The value for the property.
   */
  @SuppressWarnings("unchecked")
  public <T> T getProperty(QName name) {
    return (T)properties.get(name);
  }

  /**
   * Get all the task properties.
   * 
   * @return The task properties.
   */
  public Map<QName, Object> getProperties() {
    return properties;
  }

  /**
   * Set all the task properties.
   * 
   * @param properties The task properties.
   */
  public void setProperties(Map<QName, Object> properties) {
    this.properties.putAll(properties);
  }

  public String toString() {
    return getName();
  }

  /**
   * Interface: NameListener must respond to task name changing.
   */
  public static interface NameListener {
    public void taskNameChanged(String name);
  }

}
