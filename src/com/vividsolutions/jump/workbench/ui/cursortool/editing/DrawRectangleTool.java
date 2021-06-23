
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

import javax.swing.Icon;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.RectangleTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class DrawRectangleTool extends RectangleTool {
	private FeatureDrawingUtil featureDrawingUtil;

	private DrawRectangleTool(WorkbenchContext context, FeatureDrawingUtil featureDrawingUtil) {
		super(context);
		this.featureDrawingUtil = featureDrawingUtil;
	}

	public static CursorTool create(WorkbenchContext context) {
		FeatureDrawingUtil featureDrawingUtil =
			new FeatureDrawingUtil(context);

		return featureDrawingUtil.prepare(
			new DrawRectangleTool(context, featureDrawingUtil),
			true);
	}

	public Icon getIcon() {
		return IconLoader.icon("DrawRectangle.gif");
	}

	protected void gestureFinished() throws Exception {
		reportNothingToUndoYet();

		if (getModelSource().equals(getModelDestination())) {
			return;
		}
		featureDrawingUtil.drawRing(
			getRectangle(),
			isRollingBackInvalidEdits(),
			this,
			getPanel());
	}
}
