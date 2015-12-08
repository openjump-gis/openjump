
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

import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.cursortool.*;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.snap.SnapToLineStringBeingEditedPolicy;
import com.vividsolutions.jump.workbench.ui.snap.SnapToFeaturesPolicy;
import com.vividsolutions.jump.workbench.ui.snap.SnapToGridPolicy;
import com.vividsolutions.jump.workbench.ui.snap.SnapToVerticesPolicy;


public class DrawLineStringTool extends MultiClickTool {
    private FeatureDrawingUtil featureDrawingUtil;

    private DrawLineStringTool(FeatureDrawingUtil featureDrawingUtil) {
        this.featureDrawingUtil = featureDrawingUtil;
        setMetricsDisplay(new CoordinateListMetrics());
    }

    public static CursorTool create(LayerNamePanelProxy layerNamePanelProxy) {
        FeatureDrawingUtil featureDrawingUtil = new FeatureDrawingUtil(layerNamePanelProxy);

        return featureDrawingUtil.prepare(new DrawLineStringTool(
                featureDrawingUtil), true);
    }

    public String getName() {
    	//Specify name explicitly, otherwise it will be "Draw Line String" [Jon Aquino]
        return I18N.get("ui.cursortool.editing.DrawLineString.draw-linestring");
    }

    public Icon getIcon() {
        return IconLoader.icon("DrawLineString.gif");
    }

    protected void gestureFinished() throws Exception {
      reportNothingToUndoYet();
  
      if (!checkLineString()) {
        return;
      }
  
      // execute(featureDrawingUtil.createAddCommand(getLineString(),
      // isRollingBackInvalidEdits(), getPanel(), this));
  
      featureDrawingUtil.drawLineString(getLineString(),
          isRollingBackInvalidEdits(), this, getPanel());
    }

    protected List createStandardSnappingPolicies(Blackboard blackboard) {
        List policies = new ArrayList();
        policies.add(new SnapToVerticesPolicy(blackboard));
        policies.add(new SnapToFeaturesPolicy(blackboard));
        policies.add(new SnapToGridPolicy(blackboard));
        policies.add(new SnapToLineStringBeingEditedPolicy(blackboard, this));
        return policies;
    }

    protected LineString getLineString() throws NoninvertibleTransformException {
        return new GeometryFactory().createLineString(toArray(
                getCoordinates()));
    }

    protected boolean checkLineString() throws NoninvertibleTransformException {
        if (getCoordinates().size() < 2) {
            getPanel().getContext().warnUser(I18N.get("ui.cursortool.editing.DrawLineString.the-linestring-must-have-at-least-2-points"));

            return false;
        }

        IsValidOp isValidOp = new IsValidOp(getLineString());

        if (!isValidOp.isValid()) {
            getPanel().getContext().warnUser(isValidOp.getValidationError()
                                                      .getMessage());

            if (getWorkbench().getBlackboard().get(EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, false)) {
                return false;
            }
        }

        return true;
    }
}
