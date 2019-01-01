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

import java.util.List;
import java.util.Map;

public interface WorkbenchProperties {
  public static final String KEY_PLUGIN = "plug-in";
  public static final String KEY_CONFIGURATION = "extension";
  public static final String KEY_INPUT_DRIVER = "input-driver";
  public static final String KEY_OUTPUT_DRIVER = "output-driver";
  public static final String KEY_SEPARATOR = "separator";
  public static final String KEY_LAYOUT = "layout";

  public static final String KEY_MENUS = "menus";
  public static final String KEY_MAINMENU = "main-menu";
  public static final String KEY_CATEGORYPOPUP = "category-popup";
  public static final String KEY_LAYERNAMEPOPUP = "layername-popup";
  public static final String KEY_LAYERVIEWPOPUP = "layerview-popup";
  public static final String KEY_LAYERNAMEPOPUP_WMS = "layername-popup-wms";
  public static final String KEY_LAYERNAMEPOPUP_RASTER = "layername-popup-raster";
  public static final String KEY_ATTRIBUTETABPOPUP = "attributetab-popup";
  public static final String KEY_MAINTOOLBAR = "main-toolbar";

  public static final String ATTR_CHECKBOX = "checkbox";
  public static final String ATTR_MENUTYPE = "menutype";
  public static final String ATTR_INITIALIZE = "initialize";
  public static final String ATTR_INSTALL = "install";
  public static final String ATTR_NAME = "name";
  public static final String ATTR_ICON = "icon";
  public static final String ATTR_MENUPATH = "menupath";
  public static final String ATTR_TYPE = "type";
  public static final String ATTR_TYPE_VALUE_LIST = "list";
  public static final String ATTR_POSITION = "position";
  public static final String ATTR_ORDERID = "order_id";

  public static final String ATTR_VALUE_TRUE = "true";
  public static final String ATTR_VALUE_FALSE = "false";

  public List getSettingsList(String[] strings);
  public Map getSettings(String[] keys);

  public String getSetting(String[] keys);

  public List<String> getPlugInClassNames();

  public List<String> getConfigurationClassNames();

  @Deprecated
  public List getInputDriverClasses() throws ClassNotFoundException;

  @Deprecated
  public List getOutputDriverClasses() throws ClassNotFoundException;

  @Deprecated
  public List getConfigurationClasses() throws ClassNotFoundException;


}
