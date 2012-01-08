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

package org.openjump.core.ui.plugin.tools;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.cursortool.MultiClickTool;

public class MeasureM_FTool extends MultiClickTool 
{
    private final static String sDistance = I18N.get("org.openjump.core.ui.plugin.tools.MeasureM_FTool.Distance");
    private final static String sMeters = I18N.get("org.openjump.core.ui.plugin.tools.MeasureM_FTool.meters");
    private final static String sFeet = I18N.get("org.openjump.core.ui.plugin.tools.MeasureM_FTool.feet");
    
    private List savedCoordinates = new ArrayList();
    private Coordinate currCoord;
    
    public MeasureM_FTool() 
    {
        allowSnapping();
    }

    public Icon getIcon() 
    {
        return new ImageIcon(getClass().getResource("RulerM_F.gif"));
    }

    public Cursor getCursor() 
    {
        for (int i = 0; i < savedCoordinates.size(); i++)
        {
            add((Coordinate)savedCoordinates.get(i));
        }
        return createCursor(new ImageIcon(getClass().getResource("RulerCursorM_F.gif")).getImage());
    }

    public void mouseLocationChanged(MouseEvent e) 
    {
        try {
            if (isShapeOnScreen()) {
                ArrayList currentCoordinates = new ArrayList(getCoordinates());
                currentCoordinates.add(getPanel().getViewport().toModelCoordinate(e.getPoint()));
                display(currentCoordinates, getPanel());
            }

            currCoord = snap(e.getPoint());
            super.mouseLocationChanged(e);
        } 
        catch (Throwable t) 
        {
            getPanel().getContext().handleThrowable(t);
        }
    }

    public void mousePressed(MouseEvent e) 
    {
        super.mousePressed(e);
        savedCoordinates = new ArrayList(getCoordinates());
    }
    
      protected void gestureFinished() throws NoninvertibleTransformException 
      {
        reportNothingToUndoYet();
        savedCoordinates.clear();

        //Status bar is cleared before #gestureFinished is called. So redisplay
        //the length. [Jon Aquino]
        display(getCoordinates(), getPanel());
    }

    private void display(List coordinates, LayerViewPanel panel)
        throws NoninvertibleTransformException 
    {
        display(distance(coordinates), panel);
    }

    private void display(double distance, LayerViewPanel panel)
    {
        DecimalFormat df3 = new DecimalFormat("###,###,##0.0##");
        String distString = df3.format(distance / 0.3048);
        panel.getContext().setStatusMessage(sDistance+ ": " +
            panel.format(distance) + " " + sMeters + " " + " = " + distString + " feet");
    }

    private double distance(List coordinates) 
    {
        double distance = 0;

        for (int i = 1; i < coordinates.size(); i++) 
        {
            distance += ((Coordinate) coordinates.get(i - 1)).distance((Coordinate) coordinates.get(i));
        }

        if ((currCoord != null) && (coordinates.size() > 1))
        {
            distance -= ((Coordinate) coordinates.get(coordinates.size() - 2)).distance((Coordinate) coordinates.get(coordinates.size() - 1));
            distance += ((Coordinate) coordinates.get(coordinates.size() - 2)).distance(currCoord);
        }

        return distance;
    }
}
