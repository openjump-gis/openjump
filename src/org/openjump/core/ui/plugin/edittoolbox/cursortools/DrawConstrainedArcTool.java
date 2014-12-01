
/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2004 Integrated Systems Analysts, Inc.
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
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 */

package org.openjump.core.ui.plugin.edittoolbox.cursortools;

import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.openjump.core.geomutils.Arc;
import org.openjump.core.geomutils.MathVector;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.FeatureDrawingUtil;

public class DrawConstrainedArcTool extends ConstrainedMultiClickArcTool {
    private FeatureDrawingUtil featureDrawingUtil;
    final static String drawConstrainedArc =I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.DrawConstrainedArcTool.Draw-Constrained-Arc");
    final static String theArcMustHaveAtLeast3Points=I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.DrawConstrainedArcTool.The-arc-must-have-at-least-3-points");


    private DrawConstrainedArcTool(FeatureDrawingUtil featureDrawingUtil) {
        drawClosed = false;
        this.featureDrawingUtil = featureDrawingUtil;
    }

    public static CursorTool create(LayerNamePanelProxy layerNamePanelProxy) {
        FeatureDrawingUtil featureDrawingUtil = new FeatureDrawingUtil(layerNamePanelProxy);

        return featureDrawingUtil.prepare(new DrawConstrainedArcTool(
                featureDrawingUtil), true);
    }

    public String getName() {
        return drawConstrainedArc;
    }

    public Icon getIcon() {
        return new ImageIcon(getClass().getResource("DrawArcConstrained.gif"));
    }

    protected void gestureFinished() throws Exception {
        reportNothingToUndoYet();

        if (!checkArc()) {
            return;
        }

        execute(featureDrawingUtil.createAddCommand(getArc(),
                isRollingBackInvalidEdits(), getPanel(), this));
    }

    protected LineString getArc() throws NoninvertibleTransformException
    {       
        ArrayList points = new ArrayList(getCoordinates());
        
        if (points.size() > 1)
        {
            Coordinate a = (Coordinate) points.get(0);
            Coordinate b = (Coordinate) points.get(1);
            Coordinate c = tentativeCoordinate;
            
            if (points.size() > 2)
            {
                c = (Coordinate) points.get(points.size() - 1);
            }

            MathVector v1 = (new MathVector(b)).vectorBetween(new MathVector(a));
            MathVector v2 = (new MathVector(c)).vectorBetween(new MathVector(a));
            double arcAngle = v1.angleDeg(v2);
            Arc arc = new Arc(a, b, fullAngle);
            return arc.getLineString();
        }
        return null;
    }

    protected boolean checkArc() throws NoninvertibleTransformException {
        if (getCoordinates().size() < 3) {
            getPanel().getContext().warnUser(theArcMustHaveAtLeast3Points);

            return false;
        }

        IsValidOp isValidOp = new IsValidOp(getArc());

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
