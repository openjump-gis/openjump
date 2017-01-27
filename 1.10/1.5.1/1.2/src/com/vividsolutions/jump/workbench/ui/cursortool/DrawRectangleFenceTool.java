
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

package com.vividsolutions.jump.workbench.ui.cursortool;

import java.awt.Color;
import java.awt.Cursor;

import javax.swing.Icon;

import com.vividsolutions.jump.workbench.model.FenceLayerFinder;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;


public class DrawRectangleFenceTool extends RectangleTool {
    public final static Color COLOR = Color.black;

    public DrawRectangleFenceTool() {
        setColor(COLOR);
    }

    public Icon getIcon() {
        return IconLoader.icon("Box.gif");
    }

    public Cursor getCursor() {
        return createCursor(IconLoader.icon("FenceCursor.gif").getImage());
    }

    protected void gestureFinished() throws Exception {
        reportNothingToUndoYet();

        //Don't want viewport to change at this stage. [Jon Aquino]
        getPanel().setViewportInitialized(true);

        FenceLayerFinder fenceLayerFinder = new FenceLayerFinder(getPanel());
        fenceLayerFinder.setFence(getRectangle());

        if (!fenceLayerFinder.getLayer().isVisible()) {
            fenceLayerFinder.getLayer().setVisible(true);
        }
    }
}
