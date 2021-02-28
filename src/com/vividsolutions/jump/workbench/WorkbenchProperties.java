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
  String KEY_PLUGIN = "plug-in";
  String KEY_CONFIGURATION = "extension";
  String KEY_INPUT_DRIVER = "input-driver";
  String KEY_OUTPUT_DRIVER = "output-driver";
  String KEY_SEPARATOR = "separator";
  String KEY_LAYOUT = "layout";

  String KEY_MENUS = "menus";
  String KEY_MAINMENU = "main-menu";
  String KEY_CATEGORYPOPUP = "category-popup";
  String KEY_LAYERNAMEPOPUP = "layername-popup";
  String KEY_LAYERVIEWPOPUP = "layerview-popup";
  String KEY_LAYERNAMEPOPUP_WMS = "layername-popup-wms";
  String KEY_LAYERNAMEPOPUP_RASTER = "layername-popup-raster";
  String KEY_ATTRIBUTETABPOPUP = "attributetab-popup";
  String KEY_MAINTOOLBAR = "main-toolbar";

  String ATTR_CHECKBOX = "checkbox";
  String ATTR_MENUTYPE = "menutype";
  String ATTR_INITIALIZE = "initialize";
  String ATTR_INSTALL = "install";
  String ATTR_NAME = "name";
  String ATTR_ICON = "icon";
  String ATTR_MENUPATH = "menupath";
  String ATTR_TYPE = "type";
  String ATTR_TYPE_VALUE_LIST = "list";
  String ATTR_POSITION = "position";
  String ATTR_ORDERID = "order_id";

  String ATTR_VALUE_FALSE = "false";
  String ATTR_VALUE_TRUE = "true";

  List getSettingsList(String[] strings);
  Map getSettings(String[] keys);

  String getSetting(String[] keys);

  List<String> getPlugInClassNames();

  List<String> getConfigurationClassNames();

  @Deprecated
  List getInputDriverClasses() throws ClassNotFoundException;

  @Deprecated
  List getOutputDriverClasses() throws ClassNotFoundException;

  @Deprecated
  List getConfigurationClasses() throws ClassNotFoundException;

}
