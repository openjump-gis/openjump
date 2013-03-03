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

import java.awt.Cursor;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import org.openjump.core.geomutils.GeoUtils;
import org.openjump.core.geomutils.MathVector;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.cursortool.DragTool;
import com.vividsolutions.jump.workbench.ui.cursortool.QuasimodeTool;
import com.vividsolutions.jump.workbench.ui.cursortool.ShortcutsDescriptor;

public class RotateSelectedItemTool extends DragTool implements ShortcutsDescriptor {

  final static String rotateSelectedItem = I18N
      .get("org.openjump.core.ui.plugin.edittoolbox.cursortools.RotateSelectedItemTool.Rotate-Selected-Item");
  final static String angleST = I18N
      .get("org.openjump.core.ui.plugin.edittoolbox.cursortools.angle");
  final static String degrees = I18N
      .get("org.openjump.core.ui.plugin.edittoolbox.cursortools.degrees");
  // shortcut doc
  final static Map shortcuts = new HashMap();
  {
    shortcuts
        .put(
            new QuasimodeTool.ModifierKeySpec(new int[] { KeyEvent.VK_SHIFT }),
            I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.RotateSelectedItemTool.Set-Rotation-Center"));
  }

  private KeyListener cursorSwitcher = new KeyListener() {
    boolean shift = false;

    public void keyTyped(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
      shift = e.isShiftDown() && e.getKeyCode() != KeyEvent.VK_SHIFT;
      setCursor();
    }

    public void keyPressed(KeyEvent e) {
      shift = e.isShiftDown() || e.getKeyCode() == KeyEvent.VK_SHIFT;
      setCursor();
    }

    private void setCursor() {
      JPanel panel = getPanel();
      // System.out.println("rsi "+shift);
      if (panel != null) {
        panel.setCursor(shift ? crosshairCursor : rotateCursor);
      }
    }
  };

  private EnableCheckFactory checkFactory;
  private Shape selectedFeatureShape;
  private Coordinate centerCoord;
  protected boolean clockwise = true;
  private double fullAngle = 0.0;
  private Cursor rotateCursor = createCursor(new ImageIcon(getClass()
      .getResource("RotateSelCursor.gif")).getImage());;
  private Cursor crosshairCursor = Cursor
      .getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);

  // createCursor(new
  // ImageIcon(getClass().getResource("CrossHairCursor.gif")).getImage());

  public RotateSelectedItemTool(EnableCheckFactory checkFactory) {
    super();
    this.checkFactory = checkFactory;
  }

  public void activate(LayerViewPanel layerViewPanel) {
    centerCoord = null;
    super.activate(layerViewPanel);
    //System.out.println("rsi register listener " + cursorSwitcher);
    JUMPWorkbench.getInstance().getFrame().addEasyKeyListener(cursorSwitcher);
  }

  public void deactivate() {
    super.deactivate();
    JUMPWorkbench.getInstance().getFrame().removeEasyKeyListener(cursorSwitcher);
  }

  protected void gestureFinished() throws java.lang.Exception {
    reportNothingToUndoYet();
    if (!check(checkFactory.createAtLeastNItemsMustBeSelectedCheck(1)))
      return;
    ArrayList transactions = new ArrayList();
    for (Iterator i = getPanel().getSelectionManager()
        .getLayersWithSelectedItems().iterator(); i.hasNext();) {
      Layer layerWithSelectedItems = (Layer) i.next();
      transactions.add(createTransaction(layerWithSelectedItems));
    }
    EditTransaction.commit(transactions);
  }

  private EditTransaction createTransaction(Layer layer) {
    EditTransaction transaction = EditTransaction.createTransactionOnSelection(
        new EditTransaction.SelectionEditor() {
          public Geometry edit(Geometry geometryWithSelectedItems,
              Collection selectedItems) {
            for (Iterator j = selectedItems.iterator(); j.hasNext();) {
              Geometry item = (Geometry) j.next();
              rotate(item);
            }
            return geometryWithSelectedItems;
          }
        }, getPanel(), getPanel().getContext(), getName(), layer,
        isRollingBackInvalidEdits(), false);
    return transaction;
  }

  private void rotate(Geometry geometry) {
    geometry.apply(new CoordinateFilter() {
      public void filter(Coordinate coordinate) {
        double cosAngle = Math.cos(fullAngle);
        double sinAngle = Math.sin(fullAngle);
        double x = coordinate.x - centerCoord.x;
        double y = coordinate.y - centerCoord.y;
        coordinate.x = centerCoord.x + (x * cosAngle) + (y * sinAngle);
        coordinate.y = centerCoord.y + (y * cosAngle) - (x * sinAngle);
      }
    });
  }

  public Cursor getCursor() {
    return rotateCursor;
  }

  public Icon getIcon() {
    return new ImageIcon(getClass().getResource("RotateSel.gif"));
  }

  public void mouseMoved(MouseEvent e) {
    super.mouseMoved(e);
  }

  public String getName() {
    return rotateSelectedItem;
  }

  public void mousePressed(MouseEvent e) {
    try {
      if (!check(checkFactory.createSelectedItemsLayersMustBeEditableCheck())) {
        return;
      }

      if (e.isShiftDown()) {
        centerCoord = getPanel().getViewport().toModelCoordinate(e.getPoint());
      } else {
        if (!check(checkFactory.createAtLeastNItemsMustBeSelectedCheck(1)))
          return;

        selectedFeatureShape = createSelectedItemsShape();
        super.mousePressed(e);
      }
    } catch (Throwable t) {
      getPanel().getContext().handleThrowable(t);
    }
  }

  private Shape createSelectedItemsShape()
      throws NoninvertibleTransformException {
    Collection selectedGeos = (getPanel().getSelectionManager()
        .getSelectedItems());
    Geometry geo = ((Geometry) selectedGeos.iterator().next());
    Geometry[] allGeoms = new Geometry[selectedGeos.size()];
    int i = 0;
    for (Iterator j = selectedGeos.iterator(); j.hasNext();)
      allGeoms[i++] = (Geometry) j.next();

    GeometryFactory geoFac = new GeometryFactory();
    geo = geoFac.createGeometryCollection(allGeoms);

    if (centerCoord == null) {
      centerCoord = geo.getCentroid().getCoordinate();
    }
    return getPanel().getJava2DConverter().toShape(geo);
  }

  protected Shape getShape() throws Exception {
    AffineTransform transform = new AffineTransform();
    Point2D centerPt = getPanel().getViewport().toViewPoint(
        new Point2D.Double(centerCoord.x, centerCoord.y));
    Point2D initialPt = getViewSource();
    Point2D currPt = getViewDestination();
    MathVector center = new MathVector(centerPt.getX(), centerPt.getY());
    MathVector initial = new MathVector(initialPt.getX(), initialPt.getY());
    MathVector curr = new MathVector(currPt.getX(), currPt.getY());
    MathVector initVec = initial.vectorBetween(center);
    MathVector currVec = curr.vectorBetween(center);
    double arcAngle = initVec.angleRad(currVec);
    Coordinate initialCoord = getPanel().getViewport().toModelCoordinate(
        initialPt);
    Coordinate currCoord = getPanel().getViewport().toModelCoordinate(currPt);

    boolean toRight = (GeoUtils.pointToRight(currCoord, centerCoord,
        initialCoord));
    boolean cwQuad = ((fullAngle >= 0.0) && (fullAngle <= 90.0) && clockwise);
    boolean ccwQuad = ((fullAngle < 0.0) && (fullAngle >= -90.0) && !clockwise);

    if ((arcAngle <= 90.0) && (cwQuad || ccwQuad)) {
      if (toRight)
        clockwise = true;
      else
        clockwise = false;
    }

    if ((fullAngle > 90.0) || (fullAngle < -90)) {
      if ((clockwise && !toRight) || (!clockwise && toRight))
        fullAngle = 360 - arcAngle;
      else
        fullAngle = arcAngle;
    } else {
      fullAngle = arcAngle;
    }

    if (!clockwise)
      fullAngle = -fullAngle;

    DecimalFormat df2 = new DecimalFormat("##0.0#");
    getPanel().getContext().setStatusMessage(
        angleST + ": " + df2.format(Math.toDegrees(fullAngle)) + " " + degrees);
    // getPanel().getContext().setStatusMessage("angle = " +
    // df2.format(Math.toDegrees(fullAngle)) + " degrees");
    // getPanel().getContext().setStatusMessage("angle = " +
    // getPanel().format(Math.toDegrees(fullAngle)) + " degrees");
    transform.rotate(fullAngle, centerPt.getX(), centerPt.getY());
    return transform.createTransformedShape(selectedFeatureShape);
  }

  public final Map describeShortcuts() {
    return shortcuts;
  }

}
