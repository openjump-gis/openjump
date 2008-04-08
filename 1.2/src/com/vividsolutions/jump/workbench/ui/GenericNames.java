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
 * Use these for generic names as layer, layer A, function...
 * 
 * @author Basile Chandesris - <chandesris@pt-consulting.lu>
 */
public interface GenericNames {
	public static String LAYER = I18N.get("ui.GenericNames.LAYER");
	public static String LAYER_A = I18N.get("ui.GenericNames.LAYER_A");
	public static String LAYER_B = I18N.get("ui.GenericNames.LAYER_B");
	public static String RESULT_LAYER=I18N.get("ui.GenericNames.ResultLayer");
	public static String LAYER_GRID=I18N.get("ui.GenericNames.LayerGrid");
	public static String ANGLE=I18N.get("ui.GenericNames.ANGLE");
	public static String SOURCE_LAYER=I18N.get("ui.GenericNames.Source-Layer");
	public static String TARGET_LAYER=I18N.get("ui.GenericNames.Target-Layer");
	public static String MASK_LAYER=I18N.get("ui.GenericNames.Mask-Layer");
	public static String PARAMETER=I18N.get("ui.GenericNames.Parameter");	
	public static String FEATURES=I18N.get("ui.GenericNames.features");
	public static String ERROR=I18N.get("ui.GenericNames.Error");
	public static String RELATION=I18N.get("ui.GenericNames.Relation");
	
	public static String INTERSECTS=I18N.get("ui.GenericNames.intersects");
	public static String CONTAINS=I18N.get("ui.GenericNames.contains");
	public static String COVERS=I18N.get("ui.GenericNames.covers");	
	public static String COVEREDBY=I18N.get("ui.GenericNames.is-covered-by");
	public static String CROSSES=I18N.get("ui.GenericNames.crosses");	
	public static String DISJOINT=I18N.get("ui.GenericNames.disjoint");
	public static String EQUALS=I18N.get("ui.GenericNames.equals");
	public static String OVERLAPS=I18N.get("ui.GenericNames.overlaps");
	public static String TOUCHES=I18N.get("ui.GenericNames.touches");	
	public static String WITHIN=I18N.get("ui.GenericNames.within");
	public static String WITHIN_DISTANCE=I18N.get("ui.GenericNames.is-within-distance");	
	public static String SIMILAR=I18N.get("ui.GenericNames.similar");
	
	public static String CALCULATE_IN_PROGRESS=I18N.get("ui.GenericNames.CalculateInProgress");
	public static String GLOBAL_BOX=I18N.get("ui.GenericNames.GlobalBox");
	
	public static String USE_SELECTED_FEATURES_ONLY=I18N.get("jump.plugin.edit.PolygonizerPlugIn.Use-selected-features-only");
	public static String ATTRIBUTE=I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Attribute");
}
