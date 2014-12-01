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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class ConstrainedMoveVertexTool extends ConstrainedDragTool {
	
	final static String  constrainedMoveVertex =I18N.get("org.openjump.core.ui.plugin.edittoolbox.ConstrainedMoveVertexTool.Constrained-Move-Vertex");
    final static String  noEditableSelectionHandlesHere =I18N.get("org.openjump.core.ui.plugin.edittoolbox.ConstrainedMoveVertexTool.No-editable-selection-handles-here");
    
    public final static int TOLERANCE = 5;
    private EnableCheckFactory checkFactory;
    //private int vertexIndex; //used in getShape
    private Coordinate prevPoint = null; //used in getShape
    private Coordinate nextPoint = null; //used in getShape

    public ConstrainedMoveVertexTool(EnableCheckFactory checkFactory) {
        this.checkFactory = checkFactory;
        setColor(new Color(194, 179, 205));
        setStroke(new BasicStroke(5));
        allowSnapping();
    }

    public Cursor getCursor() {
        
        return createCursor(IconLoader.icon("MoveVertexCursor3.gif").getImage());
    }

    public Icon getIcon() {
        return new ImageIcon(getClass().getResource("MoveVertexConstrained.gif"));
    }

    public String getName(){
    	return constrainedMoveVertex;
    }
    
    protected void gestureFinished() throws Exception {
        reportNothingToUndoYet();

        //#execute(UndoableCommand) will be called. [Jon Aquino]
        moveVertices(getModelSource(), getModelDestination());
    }

    public void mousePressed(final MouseEvent e) {
        try {
            if (!check(checkFactory.createAtLeastNLayersMustBeEditableCheck(1))) {
                return;
            }
            if (!check(checkFactory.createExactlyNItemsMustBeSelectedCheck(1))) {
                return;
            }
            if (!check(new EnableCheck() {
                public String check(JComponent component) {
                    try {
                        return !nearSelectionHandle(e.getPoint())
                            ? noEditableSelectionHandlesHere
                            : null;
                            } catch (Exception e) {
                        return e.toString(); }
                }

            })) {
                return;
            }
            super.mousePressed(e);
        } catch (Throwable t) {
            getPanel().getContext().handleThrowable(t);
        }
    }

    private boolean nearSelectionHandle(Point2D p) throws NoninvertibleTransformException {
        final Envelope buffer = vertexBuffer(getPanel().getViewport().toModelCoordinate(p));
        final boolean[] result = new boolean[] { false };
        for (Iterator i = getPanel().getSelectionManager().getLayersWithSelectedItems().iterator();
            i.hasNext();
            ) {
            Layer layer = (Layer) i.next();
            if (!layer.isEditable()) {
                continue;
            }
            for (Iterator j = getPanel().getSelectionManager().getSelectedItems(layer).iterator();
                j.hasNext();
                ) {
                Geometry item = (Geometry) j.next();
                item.apply(new CoordinateFilter() {
                    public void filter(Coordinate coord) {
                        if (buffer.contains(coord)) {
                            result[0] = true;
                        }
                    }
                });
                
                if (result[0])
                {                   
                    getCoordinates(item, buffer);
                }
            }
        }
        return result[0];
    }

    private int getVertexIndex(LineString poly, Envelope buffer)
    {
        int numPts = poly.getNumPoints();
        for (int i = 0; i < numPts; i++)
            if (buffer.contains(poly.getCoordinateN(i)))
                {return i;}
        return 0;
    }
    
    private void loadLine(LineString line, Envelope buffer)
    {
        if (line != null)
        {
            int numPts = line.getNumPoints();
            int vertexIndex = getVertexIndex(line, buffer);
        
            for (int i = 0; i < vertexIndex; i++)
                {coordinates.add(line.getCoordinateN(i));}
            
            if (vertexIndex == 0) 
                prevPoint = null;
            else
                prevPoint = line.getCoordinateN(vertexIndex - 1);
            
            if (vertexIndex == line.getNumPoints() - 1) 
                nextPoint = null;
            else
                nextPoint = line.getCoordinateN(vertexIndex + 1);
        }
    }
   
    private void loadPoly(LineString poly, Envelope buffer)
    {
        if (poly != null)
        {
            int numPts = poly.getNumPoints();
            int vertexIndex = getVertexIndex(poly, buffer);
            int startPt = vertexIndex + 2 - numPts;
            int endPt = vertexIndex - 1;
            if (vertexIndex == 0) {endPt = numPts - 2;}
                
            for (int i = startPt; i <= endPt; i++)
            {
                int index = i;
                if (index < 0) {index += (numPts - 1);}
                coordinates.add(poly.getCoordinateN(index));
            }
            
            prevPoint = (Coordinate)coordinates.get(0);
            nextPoint = (Coordinate)coordinates.get(coordinates.size() - 1);
        }
    }
    
    private void getCoordinates(Geometry geometry, Envelope buffer)
    {
        coordinates.clear();
        
        if (geometry instanceof LineString) //open poly
        {              
            //java.awt.Toolkit.getDefaultToolkit().beep();
            loadLine((LineString)geometry, buffer);
            return;
        }
        
        if (geometry instanceof LinearRing) //closed poly (no holes)
        {
            loadPoly((LinearRing)geometry, buffer);
            return;
        }
        
        if (geometry instanceof Polygon) //poly with 0 or more holes
        {
            loadPoly(((Polygon)geometry).getExteriorRing(), buffer);
            return;
        }
        
        if (geometry instanceof MultiPoint)
        {
            coordinates.add(((Point) geometry).getCoordinate());
            prevPoint = null;
            nextPoint = null;
            return;
        }
        
//        else if (geometry instanceof MultiLineString)
//        {
//            writeMultiPoly((MultiLineString) geometry, idStr, writer);
//        }
//        else if (geometry instanceof MultiPolygon)
//        {
//            writeMultiPolygon((MultiPolygon) geometry, idStr, writer);
//        }
//        else if (geometry instanceof GeometryCollection)
//        {
//            writeGroup((GeometryCollection) geometry, idStr, writer);
//        }
//        else
//        {
//            Assert.shouldNeverReachHere("Unsupported Geometry implementation:" + geometry.getClass());
//        }
        return;
    }
    
    private Envelope vertexBuffer(Coordinate c) throws NoninvertibleTransformException {
        double tolerance = TOLERANCE / getPanel().getViewport().getScale();
        return vertexBuffer(c, tolerance);
    }

    public void moveVertices(Coordinate initialLocation, Coordinate finalLocation)
        throws Exception {
        final Envelope oldVertexBuffer = vertexBuffer(initialLocation);
        final Coordinate newVertex = finalLocation;
        ArrayList transactions = new ArrayList();
        for (Iterator i = getPanel().getSelectionManager().getLayersWithSelectedItems().iterator();
            i.hasNext();
            ) {
            Layer layerWithSelectedItems = (Layer) i.next();
            if (!layerWithSelectedItems.isEditable()) {
                continue;
            }
            transactions.add(createTransaction(layerWithSelectedItems, oldVertexBuffer, newVertex));
        }
        EditTransaction.commit(transactions);
    }

    private EditTransaction createTransaction(
        Layer layer,
        final Envelope oldVertexBuffer,
        final Coordinate newVertex) {
        return EditTransaction.createTransactionOnSelection(new EditTransaction.SelectionEditor() {
            public Geometry edit(Geometry geometryWithSelectedItems, Collection selectedItems) {
                for (Iterator j = selectedItems.iterator(); j.hasNext();) {
                    Geometry item = (Geometry) j.next();
                    edit(item);
                }
                return geometryWithSelectedItems;
            }
            private void edit(Geometry selectedItem) {
                selectedItem.apply(new CoordinateFilter() {
                    public void filter(Coordinate coordinate) {
                        if (oldVertexBuffer.contains(coordinate)) {
                            coordinate.x = newVertex.x;
                            coordinate.y = newVertex.y;
                        }
                    }
                });
            }
        }, getPanel(), getPanel().getContext(), getName(), layer, isRollingBackInvalidEdits(), false);
    }

    protected Shape getShape(Point2D source, Point2D destination) throws Exception
    {
        if ((prevPoint == null) && (nextPoint == null))
        {
            double radius = 20;
            return new Ellipse2D.Double(
            destination.getX() - (radius / 2),
            destination.getY() - (radius / 2),
            radius,
            radius);
        }
        else
        {
            GeneralPath path = new GeneralPath();
            
            if (prevPoint == null)
            {
                path.moveTo((int) destination.getX(), (int) destination.getY());
            }
            else
            {
                Point2D firstPoint = getPanel().getViewport().toViewPoint(prevPoint);
                path.moveTo((float) firstPoint.getX(), (float) firstPoint.getY());
                path.lineTo((int) destination.getX(), (int) destination.getY());
            }
            
            if (nextPoint != null)
            {
                Point2D lastPoint = getPanel().getViewport().toViewPoint(nextPoint);
                path.lineTo((int) lastPoint.getX(), (int) lastPoint.getY());
            }
            
            return path;
        }
    }

    private Envelope vertexBuffer(Coordinate vertex, double tolerance) {
        return new Envelope(
            vertex.x - tolerance,
            vertex.x + tolerance,
            vertex.y - tolerance,
            vertex.y + tolerance);
    }
}
