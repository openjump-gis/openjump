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

package org.openjump.core.ui.plugin.raster;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;

import org.openjump.core.rasterimage.RasterImageLayer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.cursortool.MultiClickTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

//import org.openjump.core.rasterimage.sextante.OpenJUMPSextanteRasterLayer;

public class ProfileGraphTool extends MultiClickTool {

    private final static String sDistance = I18N
            .get("org.openjump.core.ui.plugin.tools.MeasureM_FTool.Distance");
    /**
     * 2015_01_31. Giuseppe Aruta Add new panel which display profile info:
     * length, mean slope, coordinates of starting and ending points, cell
     * dimension, cell statistics.
     */

    private List<Coordinate> savedCoordinates = new ArrayList<Coordinate>();
    public static Coordinate currCoord;
    public Coordinate[] coordinates;

    public ProfileGraphTool() {
        allowSnapping();
    }

    @Override
    public Icon getIcon() {
        return IconLoader.icon("profile.png");
    }

    @Override
    public Cursor getCursor() {
        for (int i = 0; i < savedCoordinates.size(); i++) {
            add(savedCoordinates.get(i));
        }
        return createCursor(IconLoader.icon("profile_icon.gif").getImage());
    }

    @Override
    public void mouseLocationChanged(MouseEvent e) {
        try {
            if (isShapeOnScreen()) {
                @SuppressWarnings("unchecked")
                final ArrayList<Coordinate> currentCoordinates = new ArrayList<Coordinate>(
                        getCoordinates());
                currentCoordinates.add(getPanel().getViewport()
                        .toModelCoordinate(e.getPoint()));
                display(currentCoordinates, getPanel());
            }
            currCoord = snap(e.getPoint());
            super.mouseLocationChanged(e);
        } catch (final Throwable t) {
            getPanel().getContext().handleThrowable(t);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        savedCoordinates = new ArrayList<Coordinate>(getCoordinates());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void gestureFinished() throws NoninvertibleTransformException,
            IOException, RasterImageLayer.RasterDataNotFoundException {

        reportNothingToUndoYet();
        savedCoordinates.clear();
        display(getCoordinates(), getPanel());
        if (ProfileGraphGUI.getLayer() == null) {
            getPanel()
                    .getContext()
                    .warnUser(
                            I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.no-layer-selected"));
            return;
        }

        coordinates = new Coordinate[getCoordinates().size()];
        int i = 0;
        for (final Iterator<Coordinate> iterator = getCoordinates().iterator(); iterator
                .hasNext();) {
            final Coordinate c = iterator.next();
            coordinates[i] = c;
            i++;
        }
        ProfileGraphGUI.calculateProfile(coordinates);
    }

    private void display(List<Coordinate> coordinates, LayerViewPanel panel)
            throws NoninvertibleTransformException {
        display(distance(coordinates), panel);
    }

    private void display(double distance, LayerViewPanel panel) {
        final DecimalFormat df3 = new DecimalFormat("###,###,##0.0##");
        final String distString = df3.format(distance / 0.3048);
        panel.getContext().setStatusMessage(
                sDistance + ": " + panel.format(distance) + " " + " m" + " "
                        + " (" + distString + " ft)");
    }

    private double distance(List<Coordinate> coordinates) {
        double distance = 0;
        for (int i = 1; i < coordinates.size(); i++) {
            distance += coordinates.get(i - 1).distance(coordinates.get(i));
        }
        if ((currCoord != null) && (coordinates.size() > 1)) {
            distance -= coordinates.get(coordinates.size() - 2).distance(
                    coordinates.get(coordinates.size() - 1));
            distance += coordinates.get(coordinates.size() - 2).distance(
                    currCoord);
        }
        return distance;
    }

}
