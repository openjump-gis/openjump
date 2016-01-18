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
public class MenuNames {
    public static final String FILE = I18N.get("ui.MenuNames.FILE");
    public static final String FILE_NEW = I18N.get("ui.MenuNames.FILE.NEW");
    public static final String FILE_SAVEVIEW = I18N.get("ui.MenuNames.FILE.SAVEVIEW");
    public static final String EDIT = I18N.get("ui.MenuNames.EDIT");
    public static final String TOOLS_EDIT_ATTRIBUTES = I18N.get("ui.MenuNames.EDIT") +  " " + I18N.get("ui.MenuNames.ATTRIBUTS");
    public static final String TOOLS_EDIT_GEOMETRY = I18N.get("ui.MenuNames.EDIT") +  " " + I18N.get("ui.MenuNames.GEOMETRY");
    public static final String VIEW = I18N.get("ui.MenuNames.VIEW");
    public static final String MAP_DECORATIONS = I18N.get("ui.MenuNames.VIEW.MAP_DECORATIONS");
    public static final String LAYER = I18N.get("ui.MenuNames.LAYER");
    public static final String TOOLS = I18N.get("ui.MenuNames.TOOLS");
    //public static final String TOOLS_ADVANCED = I18N.get("ui.MenuNames.TOOLS.ADVANCED"); // not used, not translated
    public static final String TOOLS_ANALYSIS = I18N.get("ui.MenuNames.TOOLS.ANALYSIS");
    public static final String TOOLS_GENERALIZATION = I18N.get("ui.MenuNames.TOOLS.GENERALIZATION");
    public static final String TOOLS_GENERATE = I18N.get("ui.MenuNames.TOOLS.GENERATE");
    public static final String TOOLS_LINEARREFERENCING = I18N.get("ui.MenuNames.TOOLS.LINEARREFERENCING");
    public static final String TOOLS_JOIN = I18N.get("ui.MenuNames.TOOLS.JOIN");
    public static final String TOOLS_QA = I18N.get("ui.MenuNames.TOOLS.QA");
    public static final String TOOLS_WARP = I18N.get("ui.MenuNames.TOOLS.WARP");
    public static final String TOOLS_OTHERS = I18N
            .get("ui.MenuNames.TOOLS.OTHERS");
    //public static final String TOOLS_PROGRAMMING =I18N.get("ui.MenuNames.TOOLS.PROGRAMMING");
    public static final String TOOLS_QUERIES =I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.menu");

    public static final String WINDOW = I18N.get("ui.MenuNames.WINDOW");
    public static final String RASTERTOOLS = I18N.get("ui.MenuNames.RASTER");
    public static final String PRINT = I18N.get("ui.MenuNames.PRINT");
    
    // Michael Michaud 2008-04-06
    // New menu for synchronization
    public static final String WINDOW_SYNCHRONIZATION = I18N.get("ui.MenuNames.WINDOW.SYNCHRONIZATION");
    public static final String HELP = I18N.get("ui.MenuNames.HELP");
    public static final String PLUGINS = I18N.get("ui.MenuNames.PLUGINS");
    
    // Michael Michaud 2007-03-23
    // New menu for plugin manager, beanshell console, scripts
    public static final String CUSTOMIZE = I18N.get("ui.MenuNames.CUSTOMIZE");
    
    //erwan begin 2005-12-01 --- SIGLE	
    //Menu
    public static final String ATTRIBUTS = I18N.get("ui.MenuNames.ATTRIBUTS");
    public static final String SELECTION = I18N.get("ui.MenuNames.SELECTION");
    public static final String RASTER = I18N.get("ui.MenuNames.RASTER");
    public static final String RASTER_VECTORIALIZE = I18N
            .get("ui.MenuNames.RASTER.VECTORIALIZE");
    public static final String RASTER_SINGLE_BAND_RASTER = I18N
            .get("ui.MenuNames.RASTER.SINGLE_BAND_RASTER");
    public static final String RASTER_NODATA = I18N
            .get("org.openjump.core.ui.plugin.raster.nodata.menu");
    public static final String STATISTICS = I18N.get("ui.MenuNames.STATISTICS");
    public static final String GEOPROCESSING = "SIGLE-" + I18N.get("ui.MenuNames.GEOPROCESSING");
    
    //Submenu
    //public static final String ONELAYER = I18N.get("ui.MenuNames.ONELAYER");
    //public static final String TWOLAYERS = I18N.get("ui.MenuNames.TWOLAYERS");
    
    public static final String CONVERT = I18N.get("ui.MenuNames.CONVERT");
    public static final String EXTRACT = I18N.get("ui.MenuNames.EXTRACT");
    public static final String MERGE = I18N.get("ui.MenuNames.MERGE");
    public static final String GENERALIZATION = I18N.get("ui.MenuNames.GENERALIZATION");
    public static final String TOPOLOGY = I18N.get("ui.MenuNames.TOPOLOGY");
    //public static final String QA = I18N.get("ui.MenuNames.QA"); // see MenuNames.TOOLS_QA
    public static final String DELETE = I18N.get("ui.MenuNames.DELETE");
    public static final String DETECT = I18N.get("ui.MenuNames.DETECT");
    public static final String PLOT = I18N.get("ui.MenuNames.PLOT");
    	
    //	erwan end 2005-12-01
    // Submenus for LayerName PopupMenu
    public static final String STYLE = I18N.get("ui.MenuNames.STYLE");
    public static final String DATASTORE = I18N.get("ui.MenuNames.DATASTORE");
    public static final String SCHEMA = I18N.get("ui.MenuNames.SCHEMA");
    public static final String ZOOM = I18N.get("ui.MenuNames.ZOOM");
    
    public static final String LAYERVIEWPANEL_POPUP = I18N.get("ui.MenuNames.LAYERVIEWPANEL_POPUP");
    public static final String ATTRIBUTEPANEL_POPUP = I18N.get("ui.MenuNames.ATTRIBUTEPANEL_POPUP");
    public static final String LAYERNAMEPANEL_LAYER_POPUP = I18N.get("ui.MenuNames.LAYERNAMEPANEL_LAYER_POPUP");
    public static final String LAYERNAMEPANEL_CATEGORY_POPUP = I18N.get("ui.MenuNames.LAYERNAMEPANEL_CATEGORY_POPUP");
}
