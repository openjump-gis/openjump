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
	String LAYER = I18N.get("ui.GenericNames.LAYER");
	String LAYER_A = I18N.get("ui.GenericNames.LAYER_A");
	String LAYER_B = I18N.get("ui.GenericNames.LAYER_B");
	String RESULT_LAYER=I18N.get("ui.GenericNames.ResultLayer");
	String LAYER_POLYGONS = I18N.get("ui.GenericNames.LAYER-with-Polygons");
	String LAYER_GRID=I18N.get("ui.GenericNames.LayerGrid");
	String ANGLE=I18N.get("ui.GenericNames.ANGLE");
	String SOURCE_LAYER=I18N.get("ui.GenericNames.Source-Layer");
	String TARGET_LAYER=I18N.get("ui.GenericNames.Target-Layer");
	String MASK_LAYER=I18N.get("ui.GenericNames.Mask-Layer");
	String PARAMETER=I18N.get("ui.GenericNames.Parameter");
	String FEATURES=I18N.get("ui.GenericNames.features");
	String ERROR=I18N.get("ui.GenericNames.Error");
	String RELATION=I18N.get("ui.GenericNames.Relation");
	
	String INTERSECTS=I18N.get("ui.GenericNames.intersects");
	String CONTAINS=I18N.get("ui.GenericNames.contains");
	String COVERS=I18N.get("ui.GenericNames.covers");
	String COVEREDBY=I18N.get("ui.GenericNames.is-covered-by");
	String CROSSES=I18N.get("ui.GenericNames.crosses");
	String DISJOINT=I18N.get("ui.GenericNames.disjoint");
	String EQUALS=I18N.get("ui.GenericNames.equals");
	String OVERLAPS=I18N.get("ui.GenericNames.overlaps");
	String TOUCHES=I18N.get("ui.GenericNames.touches");
	String WITHIN=I18N.get("ui.GenericNames.within");
	String WITHIN_DISTANCE=I18N.get("ui.GenericNames.is-within-distance");
	String SIMILAR=I18N.get("ui.GenericNames.similar");
	String INTERSECTS_INTERIOR_POINT=I18N.get("ui.GenericNames.intersects-interior-point");
	String INTERIOR_POINT_INTERSECTS=I18N.get("ui.GenericNames.interior-point-intersects");
	
	String CALCULATE_IN_PROGRESS=I18N.get("ui.GenericNames.CalculateInProgress");
	String GLOBAL_BOX=I18N.get("ui.GenericNames.GlobalBox");
	
	String USE_SELECTED_FEATURES_ONLY=I18N.get("ui.GenericNames.Use-selected-features-only");
	String ATTRIBUTE=I18N.get("ui.GenericNames.Attribute");
	String SELECT_LAYER=I18N.get("ui.GenericNames.select-layer");
	String SELECT_ATTRIBUTE=I18N.get("ui.GenericNames.select-attribute");
}
