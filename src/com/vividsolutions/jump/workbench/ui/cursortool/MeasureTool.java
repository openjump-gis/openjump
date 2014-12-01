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

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.Icon;

import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.images.famfam.IconLoaderFamFam;

/**
 * A tool which displays cumlative length, angle and cumlative area of
 * a linestring drawn on the screen.
 *
 * @author Martin Davis
 * @version 1.0
 */
public class MeasureTool
    extends MultiClickTool
{

  public MeasureTool() {
    allowSnapping();
    setMetricsDisplay(new CoordinateListMetrics());
    setCloseRing(true);
  }

  public Icon getIcon() {
    //return IconLoaderFamFam.icon("Ruler_small.png");
    return IconLoader.icon("Ruler.gif");
  }

  public Cursor getCursor() {
    return createCursor(IconLoader.icon("RulerCursor.gif").getImage());
  }

  public void mouseLocationChanged(MouseEvent e) {
    try {
      if (isShapeOnScreen()) {
        ArrayList currentCoordinates = new ArrayList(getCoordinates());
        currentCoordinates.add(getPanel().getViewport().toModelCoordinate(e.getPoint()));
      }
      super.mouseLocationChanged(e);
    } catch (Throwable t) {
      getPanel().getContext().handleThrowable(t);
    }
  }

  protected void gestureFinished()
  {
    reportNothingToUndoYet();
    //Status bar is cleared before #gestureFinished is called. So redisplay the metrics
    getMetrics().displayMetrics(getCoordinates(), getPanel());
  }

}