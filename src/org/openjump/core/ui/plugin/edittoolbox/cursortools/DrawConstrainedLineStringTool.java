
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

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.FeatureDrawingUtil;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

public class DrawConstrainedLineStringTool extends ConstrainedMultiClickTool {
    private FeatureDrawingUtil featureDrawingUtil;
    final static String drawConstrainedLineString =I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.DrawConstrainedLineStringTool.Draw-Constrained-LineString");
    final static String TheLinestringMustHaveAtLeast2Points =I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.DrawConstrainedLineStringTool.The-linestring-must-have-at-least-2-points");
    
    protected DrawConstrainedLineStringTool(FeatureDrawingUtil featureDrawingUtil) {
        drawClosed = false;
        this.featureDrawingUtil = featureDrawingUtil;
    }

    public static CursorTool create(LayerNamePanelProxy layerNamePanelProxy) {
        FeatureDrawingUtil featureDrawingUtil = new FeatureDrawingUtil(layerNamePanelProxy);

        return featureDrawingUtil.prepare(new DrawConstrainedLineStringTool(
                featureDrawingUtil), true);
    }

    public String getName() {
    	//Specify name explicitly, otherwise it will be "Draw Line String" [Jon Aquino]
        return drawConstrainedLineString;
    }

    public Icon getIcon() {
        return new ImageIcon(getClass().getResource("DrawLinestringConstrained.gif"));
    }

    protected void gestureFinished() throws Exception {
        reportNothingToUndoYet();

        if (!checkLineString()) {
            return;
        }

        execute(featureDrawingUtil.createAddCommand(getLineString(),
                isRollingBackInvalidEdits(), getPanel(), this));
    }

    protected LineString getLineString() throws NoninvertibleTransformException {
        return new GeometryFactory().createLineString(toArray(
                getCoordinates()));
    }

    protected boolean checkLineString() throws NoninvertibleTransformException {
        if (getCoordinates().size() < 2) {
            getPanel().getContext().warnUser(TheLinestringMustHaveAtLeast2Points);

            return false;
        }

        IsValidOp isValidOp = new IsValidOp(getLineString());

        if (!isValidOp.isValid()) {
            getPanel().getContext().warnUser(isValidOp.getValidationError()
                                                      .getMessage());

            if (PersistentBlackboardPlugIn.get(getWorkbench().getContext())
                    .get(EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, false)) {
                return false;
            }
        }

        return true;
    }
}
