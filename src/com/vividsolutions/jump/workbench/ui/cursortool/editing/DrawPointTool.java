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
package com.vividsolutions.jump.workbench.ui.cursortool.editing;

import java.awt.Shape;
import java.awt.geom.NoninvertibleTransformException;

import javax.swing.Icon;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.NClickTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class DrawPointTool extends NClickTool {

	private FeatureDrawingUtil featureDrawingUtil;

	private DrawPointTool(FeatureDrawingUtil featureDrawingUtil) {
		super(1);
		this.featureDrawingUtil = featureDrawingUtil;
	}
	
	protected Shape getShape() throws NoninvertibleTransformException {
		//Don't want anything to show up when the user drags. [Jon Aquino]
		return null;
	}

	public static CursorTool create(LayerNamePanelProxy layerNamePanelProxy) {
		FeatureDrawingUtil featureDrawingUtil =
			new FeatureDrawingUtil(layerNamePanelProxy);

		//Don't allow snapping. The user will get confused if he tries to draw
		//a point near another point and sees nothing happen because
		//snapping is happening. [Jon Aquino]
        
        //Don't agree : if snapping is requested, snapping must happens.
        //With transparency and points appearing different from simple vertices
        //I feel that adding snapped points is not so confusing [mmichaud 2007-08-16]
		return featureDrawingUtil.prepare(
			new DrawPointTool(featureDrawingUtil), true);
	}

	public Icon getIcon() {
		return IconLoader.icon("DrawPoint.gif");
	}

	protected void gestureFinished() throws Exception {
		reportNothingToUndoYet();

		execute(
			featureDrawingUtil.createAddCommand(
				getPoint(),
				isRollingBackInvalidEdits(),
				getPanel(),
				this));
	}

	protected Point getPoint()
		throws NoninvertibleTransformException {
		return new GeometryFactory().createPoint(
			(Coordinate)getCoordinates().get(0));
	}	

}
