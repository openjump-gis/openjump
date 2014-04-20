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

package com.vividsolutions.jump.workbench;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.vividsolutions.jump.workbench.ui.ErrorHandler;

public class WorkbenchPropertiesFile implements WorkbenchProperties {
  private ErrorHandler errorHandler;
  private List<File> propertyFiles;
  private HashMap<String, HashMap<String, HashMap<String, Object>>> properties = new HashMap();

  public WorkbenchPropertiesFile(File file, ErrorHandler errorHandler)
      throws JDOMException, IOException {
    this(Arrays.asList(new File[] { file }), errorHandler);
  }

  public WorkbenchPropertiesFile(List<File> files, ErrorHandler errorHandler)
      throws JDOMException, IOException {
    this.propertyFiles = Collections.unmodifiableList(files);
    this.errorHandler = errorHandler;
    init();
  }

  private void init() {
    SAXBuilder builder = new SAXBuilder();
    properties = new LinkedHashMap();
    for (File file : propertyFiles) {
      Document document;
      try {
        document = builder.build(file);
      }
      catch (JDOMException e) {
        errorHandler.handleThrowable(e);
        continue;
      }
      catch (IOException e) {
        errorHandler.handleThrowable(e);
        continue;
      }
      // should be <workbench/>
      Element root = document.getRootElement();
      if (root != null) {
        xmlAddToMap(root, properties, -1);
      }
    }
    //System.out.println("WBPF properties: " + properties);
  }

  private Integer xmlAddToMap(Element root_element, HashMap map, Integer i) {

    // int i = -1;
    for (Element element : (List<Element>) root_element.getChildren()) {
      i++;
      // i = root_element.isRootElement() ? i : index;
      String elementName = element.getName();
      String elementValue = element.getTextTrim();
      // if (elementValue.contains("PasteL"))
      // System.out.println();
      HashMap<String, Object> elementAttribs = new LinkedHashMap<String, Object>();
      // add attributes
      List<Attribute> xmlAttribs = element.getAttributes();
      for (Attribute xmlAttrib : xmlAttribs) {
        elementAttribs.put(xmlAttrib.getName(), xmlAttrib.getValue());
      }

      int order_id = i;
      // add sub tags
      if (element.getChildren().size() > 0)
        i = xmlAddToMap(element, elementAttribs, i);

      // deal with lists
      boolean isList = map.containsKey("type")
          && map.get("type").toString().toLowerCase().equals("list");
//      if (elementName.equals("item"))
//        System.out.println();
      if (isList) {
        Object existingEntry = map.get("list");
        List existingList;
        if (existingEntry instanceof List)
          existingList = (List) existingEntry;
        else {
          existingList = new ArrayList<String>();
          map.put("list", existingList);
        }
        existingList.add(!elementValue.isEmpty() ? elementValue : elementName);
        continue;
      }

      String key = "";
      if (!elementValue.isEmpty())
        key = elementValue;
      else if (elementAttribs.containsKey("id"))
        key = elementAttribs.get("id").toString();
      // use auto asc id for separators
      else if (elementName.equals(WorkbenchProperties.KEY_SEPARATOR)) {
        Object sepMap = map.get(elementName);
        int id;
        if (sepMap instanceof Map) {
          id = ((Map) sepMap).size();
        }
        else {
          id = 0;
        }
        key = Integer.toString(id);
      }

      // always add order_id, if missing
      if (!elementAttribs.containsKey(WorkbenchProperties.ATTR_ORDERID))
        elementAttribs.put(WorkbenchProperties.ATTR_ORDERID,
            Integer.toString(order_id));

      Object existingEntry = map.get(elementName);
      if (existingEntry instanceof Map) {
        Map existingEntryMap = (Map) existingEntry;
        Map map2fill = existingEntryMap;
        // override or map per id
        if (!key.isEmpty()) {
          Object existingSubEntry = existingEntryMap.get(key);
          if (existingSubEntry instanceof Map) {
            map2fill = (Map) existingSubEntry;
          }
          else {
            map2fill = new LinkedHashMap();
            existingEntryMap.put(key, map2fill);
          }
        }
        map2fill = mergeMapsRecursively(map2fill, elementAttribs);
        // map2fill.putAll(elementAttribs);
      }
      else {
        if (key.isEmpty())
          map.put(elementName, elementAttribs);
        else {
          Map map2fill = new LinkedHashMap();
          map2fill.put(key, elementAttribs);
          map.put(elementName, map2fill);
        }
      }
    }

    return i;
  }

  /**
   * merge 2 maps recursively eg. map1
   * key={menus={main-menu={menupath=MenuNames.LAYER, install=true}}} map2
   * key={menus={category-popup={install=true}}} merged map1
   * key={menus={main-menu={menupath=MenuNames.LAYER, install=true},
   * category-popup={install=true}}}
   * 
   * @param map1
   * @param map2
   * @return merged map1
   */
  private Map mergeMapsRecursively(Map map1, Map map2) {
    Set keySet = new HashSet(map1.keySet());
    keySet.addAll(map2.keySet());
    for (Object key : keySet) {
      Object val1 = map1.get(key);
      Object val2 = map2.get(key);
      if (val1 instanceof Map && val2 instanceof Map) {
        mergeMapsRecursively((Map) val1, (Map) val2);
      }
      else if (val1 != null && val2 != null) {
        // never override once defined order id's
        if (!key.equals(ATTR_ORDERID))
          map1.put(key, val1 + ";" + val2);
      }
      else if (val1 == null) {
        map1.put(key, val2);
      }
    }
    return map1;
  }

  public List getSettingsList(String[] keys) {
    Object settings = getRawSetting(keys);
    return settings instanceof List ? (List) settings : new ArrayList();
  }

  public Map getSettings(String[] keys) {
    Object settings = getRawSetting(keys);
    return settings instanceof Map ? (Map) settings : new HashMap();
  }

  public String getSetting(String[] keys) {
    Object value = getRawSetting(keys);
    return value != null ? value.toString() : "";
  }

  private Object getRawSetting(String[] keys) {
    Object map = properties;
    for (String key : keys) {
      if (map instanceof Map)
        map = ((Map) map).get(key);
      else
        return null;
    }
    if (map instanceof LinkedHashMap)
      return ((LinkedHashMap) map).clone();
    else
      return map;
  }

  // utility method for deprecated get*Classes() methods
  private List<Class> getClasses(String key) throws ClassNotFoundException {
    ArrayList<Class> clazzes = new ArrayList();
    for (String className : getClassNames(key)) {
      clazzes.add(Class.forName(className));
    }
    return clazzes;
  }

  // for legacy settings the id is in effect a class name
  private List<String> getClassNames(String key) {
    return getIds(key);
  }

  private List<String> getIds(String key) {
    HashMap<String, ?> settings = properties.get(key);
    if (settings == null)
      return new ArrayList<String>();
    List<String> classNames = new ArrayList<String>(settings.keySet());
    return Collections.unmodifiableList(classNames);
  }

  public List<String> getPlugInClassNames() {
    return getClassNames(KEY_PLUGIN);
  }

  public List<String> getConfigurationClassNames() {
    return getClassNames(KEY_CONFIGURATION);
  }

  @Deprecated
  public List getInputDriverClasses() throws ClassNotFoundException {
    return getClasses(KEY_INPUT_DRIVER);
  }

  @Deprecated
  public List getOutputDriverClasses() throws ClassNotFoundException {
    return getClasses(KEY_OUTPUT_DRIVER);
  }

  @Deprecated
  public List getConfigurationClasses() throws ClassNotFoundException {
    return getClasses(KEY_CONFIGURATION);
  }

  @Override
  public String toString() {
    return properties instanceof Map ? properties.toString()
        : "{no properties set}";
  }

}
