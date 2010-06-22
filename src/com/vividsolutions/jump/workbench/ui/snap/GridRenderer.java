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

package com.vividsolutions.jump.workbench.ui.snap;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.renderer.SimpleRenderer;

public class GridRenderer extends SimpleRenderer {
    public final static String CONTENT_ID = "GRID";
    public final static String ENABLED_KEY = GridRenderer.class +" - ENABLED";
    public final static String DOTS_ENABLED_KEY = GridRenderer.class +" - DOTS ENABLED";
    public final static String LINES_ENABLED_KEY = GridRenderer.class +" - LINES ENABLED";
    private Blackboard blackboard;

    public GridRenderer(Blackboard blackboard, LayerViewPanel panel) {
        super(CONTENT_ID, panel);
        this.blackboard = blackboard;
    }

    protected void paint(Graphics2D g) throws NoninvertibleTransformException {

        if (!blackboard.get(ENABLED_KEY, false)) {
            return;
        }

        double gridSize = blackboard.get(SnapToGridPolicy.GRID_SIZE_KEY, 20d);
        double viewGridSize = gridSize * panel.getViewport().getScale();

        if (viewGridSize < 5) {
            return;
        }

        g.setColor(Color.lightGray);

        double minModelX =
            Math.floor(panel.getViewport().getEnvelopeInModelCoordinates().getMinX() / gridSize)
                * gridSize;
        double maxModelX =
            Math.ceil(panel.getViewport().getEnvelopeInModelCoordinates().getMaxX() / gridSize)
                * gridSize;
        double minModelY =
            Math.floor(panel.getViewport().getEnvelopeInModelCoordinates().getMinY() / gridSize)
                * gridSize;
        double maxModelY =
            Math.ceil(panel.getViewport().getEnvelopeInModelCoordinates().getMaxY() / gridSize)
                * gridSize;

        if (blackboard.get(DOTS_ENABLED_KEY, false)) {
            paintDots(g, gridSize, minModelX, maxModelX, minModelY, maxModelY);
        }

        if (blackboard.get(LINES_ENABLED_KEY, false)) {
            paintLines(g, gridSize, minModelX, maxModelX, minModelY, maxModelY);
        }
    }

    private void paintDots(
        Graphics2D g,
        double gridSize,
        double minModelX,
        double maxModelX,
        double minModelY,
        double maxModelY)
        throws NoninvertibleTransformException {
        for (double x = minModelX; x < maxModelX; x += gridSize) {
            for (double y = minModelY; y < maxModelY; y += gridSize) {
                Point2D p = panel.getViewport().toViewPoint(new Coordinate(x, y));
                g.drawLine((int) p.getX(), (int) p.getY(), (int) p.getX(), (int) p.getY());
            }
        }
    }

    private Stroke stroke =
        new BasicStroke(
            1,
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_BEVEL,
            0,
            new float[] { 1, 2 },
            0);

    private void paintLines(
        Graphics2D g,
        double gridSize,
        double minModelX,
        double maxModelX,
        double minModelY,
        double maxModelY)
        throws NoninvertibleTransformException {
        g.setStroke(stroke);
        // Long dashed lines are very long to draw [bug ]
        // Don't draw it out of the panel [mmichaud 2010-06-22] 
        Point2D minXY = panel.getViewport().toViewPoint(new Coordinate(minModelX, minModelY));
        Point2D maxXY = panel.getViewport().toViewPoint(new Coordinate(maxModelX, maxModelY));
        int minViewX = Math.max(-1, (int)minXY.getX());
        int maxViewX = Math.min(panel.getWidth() + 1, (int)maxXY.getX());
        // Second Math.min is there because min view coordinate = max model coordinate
        int minViewY = Math.max(-1, (int)Math.min(minXY.getY(), maxXY.getY()));
        // Second Math.max is there because max view coordinate = min model coordinate
        int maxViewY = Math.min(panel.getHeight() + 1, (int)Math.max(minXY.getY(), maxXY.getY()));
        
        for (double x = minModelX; x < maxModelX; x += gridSize) {
            int viewX = (int)panel.getViewport().toViewPoint(new Coordinate(x, minModelY)).getX();
            if (viewX < 0 || viewX > panel.getWidth()) continue;
            g.drawLine(viewX, minViewY, viewX, maxViewY);
        }

        for (double y = minModelY; y < maxModelY; y += gridSize) {
            int viewY = (int)panel.getViewport().toViewPoint(new Coordinate(minModelX, y)).getY();
            if (viewY < 0 || viewY > panel.getHeight()) continue;
            g.drawLine(minViewX, viewY, maxViewX, viewY);
        }
    }
}
