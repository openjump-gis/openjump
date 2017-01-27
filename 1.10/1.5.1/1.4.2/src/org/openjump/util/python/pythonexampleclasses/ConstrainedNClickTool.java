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
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 */

package org.openjump.util.python.pythonexampleclasses;

import java.awt.event.MouseEvent;

import org.openjump.core.ui.plugin.edittoolbox.cursortools.ConstrainedMultiClickTool;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Whereas a MultiClickTool looks for a double-click to end the gesture,
 * an NClickTool looks for a certain number of points to end the gesture.
 */
public abstract class ConstrainedNClickTool extends ConstrainedMultiClickTool {
    protected int n = 1; //number of clicks at which to finish the drawing

    public ConstrainedNClickTool() 
    {
    }
    
    protected Coordinate getModelSource() {
        return (Coordinate) getCoordinates().get(0);
    }

    protected Coordinate getModelDestination() {
        return (Coordinate) getCoordinates().get(n-1);
    }    

    protected boolean isFinishingRelease(MouseEvent e) {
        //A double click will generate two events: one with click-count = 1,
        //and one with click-count = n. We just want to finish the gesture
        //once, so handle the click-count = 1 case, and ignore the others.
        //[Jon Aquino]
        return (((e.getClickCount() == 1) && shouldGestureFinish()) || super.isFinishingRelease(e));
    }

    private boolean shouldGestureFinish() {
        return (getCoordinates().size() == n);
    }

}
