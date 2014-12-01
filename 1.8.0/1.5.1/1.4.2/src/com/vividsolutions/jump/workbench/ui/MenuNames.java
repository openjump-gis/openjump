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
package com.vividsolutions.jump.workbench.ui;

import com.vividsolutions.jump.I18N;

/**
 * First level menus names of JUMP
 * @author Basile Chandesris - <chandesris@pt-consulting.lu>
 */
public interface MenuNames {
  public static String FILE = I18N.get("ui.MenuNames.FILE");
  public static String FILE_NEW = I18N.get("ui.MenuNames.FILE.NEW");
	public static String FILE_SAVEVIEW = I18N.get("ui.MenuNames.FILE.SAVEVIEW");
	public static String EDIT = I18N.get("ui.MenuNames.EDIT");
	public static String TOOLS_EDIT_ATTRIBUTES = I18N.get("ui.MenuNames.EDIT") +  " " + I18N.get("ui.MenuNames.ATTRIBUTS");
	public static String TOOLS_EDIT_GEOMETRY = I18N.get("ui.MenuNames.EDIT") +  " " + I18N.get("ui.MenuNames.GEOMETRY");
	public static String VIEW = I18N.get("ui.MenuNames.VIEW");
	public static String LAYER = I18N.get("ui.MenuNames.LAYER");
	public static String TOOLS = I18N.get("ui.MenuNames.TOOLS");
	public static String TOOLS_ADVANCED = I18N.get("ui.MenuNames.TOOLS.ADVANCED");
	public static String TOOLS_ANALYSIS = I18N.get("ui.MenuNames.TOOLS.ANALYSIS");
	public static String TOOLS_GENERALIZATION = I18N.get("ui.MenuNames.TOOLS.GENERALIZATION");
	public static String TOOLS_GENERATE = I18N.get("ui.MenuNames.TOOLS.GENERATE");
	public static String TOOLS_JOIN = I18N.get("ui.MenuNames.TOOLS.JOIN");
	public static String TOOLS_QA = I18N.get("ui.MenuNames.TOOLS.QA");
	public static String TOOLS_WARP = I18N.get("ui.MenuNames.TOOLS.WARP");
	//public static String TOOLS_PROGRAMMING =I18N.get("ui.MenuNames.TOOLS.PROGRAMMING");
	public static String TOOLS_QUERIES =I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.menu");
	public static String WINDOW = I18N.get("ui.MenuNames.WINDOW");
	public static String RASTERTOOLS = I18N.get("ui.MenuNames.RASTER");
    // Michael Michaud 2008-04-06
    // New menu for synchronization
    public static String WINDOW_SYNCHRONIZATION = I18N.get("ui.MenuNames.WINDOW.SYNCHRONIZATION");
	public static String HELP = I18N.get("ui.MenuNames.HELP");
	public static String PLUGINS = I18N.get("ui.MenuNames.PLUGINS");
	
    // Michael Michaud 2007-03-23
    // New menu for plugin manager, beanshell console, scripts
	public static String CUSTOMIZE = I18N.get("ui.MenuNames.CUSTOMIZE");
	
	//erwan begin 2005-12-01 --- SIGLE	
	//Menu
	public static String ATTRIBUTS = I18N.get("ui.MenuNames.ATTRIBUTS");
	public static String SELECTION = I18N.get("ui.MenuNames.SELECTION");
	public static String RASTER = I18N.get("ui.MenuNames.RASTER");
	public static String STATISTICS = I18N.get("ui.MenuNames.STATISTICS");
	public static String GEOPROCESSING = "SIGLE-" + I18N.get("ui.MenuNames.GEOPROCESSING");
	
	//Submenu
	public static String ONELAYER = I18N.get("ui.MenuNames.ONELAYER");
	public static String TWOLAYERS = I18N.get("ui.MenuNames.TWOLAYERS");
	
	public static String CONVERT = I18N.get("ui.MenuNames.CONVERT");
	public static String EXTRACT = I18N.get("ui.MenuNames.EXTRACT");
	public static String MERGE = I18N.get("ui.MenuNames.MERGE");
	public static String GENERALIZATION = I18N.get("ui.MenuNames.GENERALIZATION");
	public static String TOPOLOGY = I18N.get("ui.MenuNames.TOPOLOGY");
	//public static String QA = I18N.get("ui.MenuNames.QA"); // see MenuNames.TOOLS_QA
	public static String DELETE = I18N.get("ui.MenuNames.DELETE");
	public static String DETECT = I18N.get("ui.MenuNames.DETECT");
	public static String PLOT = I18N.get("ui.MenuNames.PLOT");
		
	//	erwan end 2005-12-01
	// Submenus for LayerName PopupMenu
	public static String STYLE = I18N.get("ui.MenuNames.STYLE");
	public static String DATASTORE = I18N.get("ui.MenuNames.DATASTORE");

}
