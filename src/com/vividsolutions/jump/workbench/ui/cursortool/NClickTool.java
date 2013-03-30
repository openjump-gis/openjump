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

import java.awt.event.MouseEvent;

import com.vividsolutions.jts.geom.Coordinate;


/**
 * Whereas a MultiClickTool looks for a double-click to end the gesture,
 * an NClickTool looks for a certain number of points to end the gesture.
 */
public abstract class NClickTool extends MultiClickTool {
    //This class has been tested only with n=1 and n=2. [Jon Aquino]
    private int n;

    public NClickTool(int n) {
        this.n = n;
    }
    
    public int numClicks(){
      return n;
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
        return (e.getClickCount() == 1) && shouldGestureFinish();
    }

    private boolean shouldGestureFinish() {
        return getCoordinates().size() == n;
    }

}
